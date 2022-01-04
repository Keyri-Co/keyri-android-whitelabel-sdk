package com.example.keyrisdk.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.keyrisdk.R
import com.example.keyrisdk.databinding.LayoutKeyriScannerViewBinding
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.AuthorizationException
import com.example.keyrisdk.exception.CameraPermissionNotGrantedException
import com.example.keyrisdk.exception.KeyriSdkException
import com.example.keyrisdk.exception.MultipleAccountsNotAllowedException
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

class KeyriScannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var keyriScannerViewParams: KeyriScannerViewParams? = null

    private val displayManager by lazy { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private var displayId: Int = -1
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null

    private val options by lazy {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
            .build()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private val qrAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
        imageProxy.image?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            BarcodeScanning.getClient(options).process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()
                        ?.displayValue
                        ?.takeIf { !isLoading }
                        ?.let(::processScannedData)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private val adapter by lazy { KeyriAccountsAdapter(::onAccountClicked) }

    private var binding: LayoutKeyriScannerViewBinding =
        LayoutKeyriScannerViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var isLoading = false
        set(value) {
            field = value
            onLoading(field)
        }

    fun initView(keyriScannerViewParams: KeyriScannerViewParams) {
        this.keyriScannerViewParams = keyriScannerViewParams

        openScanner()
    }

    private fun processScannedData(scannedData: String) {
        Log.d("Keyri", "QR processed: $scannedData")

        try {
            // Try to parse link and process it
            scannedData.toUri()
                .getQueryParameters("sessionId")
                ?.firstOrNull()
                ?.let { sessionId ->
                    cameraProvider?.unbindAll()
                    authenticate(sessionId)
                }
        } catch (e: Exception) {
            Log.d("Keyri", "Not valid link: $scannedData")
        }
    }

    private fun authenticate(sessionId: String) {
        keyriScannerViewParams?.activity?.lifecycleScope?.launch {
            isLoading = true

            try {
                val session =
                    keyriScannerViewParams?.keyriSdk?.onReadSessionId(sessionId) ?: return@launch

                if (session.isNewUser) {
                    try {
                        keyriScannerViewParams?.keyriSdk?.signup(
                            session.username,
                            sessionId,
                            session.service,
                            keyriScannerViewParams?.customArgument
                        )
                        onAuthCompleted()
                    } catch (e: Throwable) {
                        if (e is MultipleAccountsNotAllowedException) {
                            onAccountAlreadyExists(sessionId)
                        } else {
                            throw e
                        }
                    }
                } else {
                    val accounts = keyriScannerViewParams?.keyriSdk?.accounts() ?: return@launch

                    when {
                        accounts.isEmpty() -> throw AccountNotFoundException
                        accounts.size == 1 -> {
                            authAccount(accounts.first(), sessionId, session.service)
                            onAuthCompleted()
                        }
                        else -> {
                            chooseAccount(accounts, sessionId, session.service)
                            return@launch
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.d("Keyri", "Authentication exception $e")

                val errorMessage = if (e is KeyriSdkException) e else AuthorizationException

                onError(errorMessage)
            }

            isLoading = false
        }
    }

    private suspend fun authAccount(account: PublicAccount, sessionId: String, service: Service) {
        keyriScannerViewParams?.let { params ->
            try {
                params.keyriSdk.login(account, sessionId, service, params.customArgument)
                onAuthCompleted()
            } catch (e: Throwable) {
                Log.d("Keyri", "Authentication exception $e")

                val errorMessage = if (e is KeyriSdkException) e else AuthorizationException

                onError(errorMessage)
            }
        }
    }

    private suspend fun chooseAccount(
        accounts: List<PublicAccount>,
        sessionId: String,
        service: Service
    ) {
        keyriScannerViewParams?.onChooseAccount?.invoke(accounts)?.let { account ->
            authAccount(account, sessionId, service)
        } ?: adapter.apply {
            binding.apply {
                rlChooseAccount.isVisible = true
                flProgress.progress.isVisible = false
                flContent.isVisible = false

                rvAccounts.adapter = adapter
            }

            submitList(accounts)

            this.service = service
            this.sessionId = sessionId
        }
    }

    private fun onAccountClicked(account: PublicAccount, sessionId: String, service: Service) {
        binding.rlChooseAccount.isVisible = false

        keyriScannerViewParams?.activity?.lifecycleScope?.launch {
            authAccount(account, sessionId, service)
        }
    }

    private fun removeExistingAccountAndInitNewSession(sessionId: String) {
        keyriScannerViewParams?.let { params ->
            params.activity.lifecycleScope.launch {
                params.keyriSdk.accounts().firstOrNull()?.let { account ->
                    params.keyriSdk.removeAccount(account)
                    authenticate(sessionId)
                }
            }
        }
    }

    private fun onAccountAlreadyExists(sessionId: String) {
        keyriScannerViewParams?.onAccountAlreadyExists?.invoke(sessionId)?.takeIf { it }?.let {
            removeExistingAccountAndInitNewSession(sessionId)
        } ?: AlertDialog.Builder(context)
            .setTitle(R.string.keyri_you_already_have_account)
            .setMessage(R.string.keyri_do_you_want_to_replace_account)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                removeExistingAccountAndInitNewSession(sessionId)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                onError(MultipleAccountsNotAllowedException)
            }
            .show()
    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        initCamera()
    }

    private fun requestCameraPermission() {
        keyriScannerViewParams?.activity?.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openScanner()
            } else {
                onError(CameraPermissionNotGrantedException)
            }
        }?.launch(Manifest.permission.CAMERA)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun onError(exception: KeyriSdkException) {
        keyriScannerViewParams?.onError?.invoke(exception)
            ?: onMessage(context.getString(exception.errorMessage))
    }

    private fun onAuthCompleted() {
        keyriScannerViewParams?.onCompleted?.invoke()
            ?: onMessage(context.getString(R.string.keyri_auth_complete))
    }

    private fun onMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun initCamera() {
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                keyriScannerViewParams?.activity?.window?.decorView?.let { view ->
                    if (displayId == this@KeyriScannerView.displayId) {
                        imageAnalyzer?.targetRotation = view.display.rotation
                    }
                }
            }
        }

        displayManager.registerDisplayListener(displayListener, null)

        binding.scannerPreview.post {
            displayId = binding.scannerPreview.display.displayId

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun bindCameraUseCases() {
        val metrics = DisplayMetrics().also { binding.scannerPreview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.scannerPreview.display.rotation
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(480, 480))
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor, qrAnalyzer)
        cameraProvider?.unbindAll()

        try {
            keyriScannerViewParams?.activity?.let { activity ->
                camera = cameraProvider?.bindToLifecycle(
                    activity,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            }

            preview?.setSurfaceProvider(binding.scannerPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.d("Keyri", "Failed to init Camera")
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)

        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun onLoading(isLoading: Boolean) {
        keyriScannerViewParams?.onLoading?.invoke(isLoading) ?: with(binding) {
            flContent.isVisible = !isLoading
            flProgress.progress.isVisible = isLoading
            rlChooseAccount.isVisible = false

            if (isLoading) {
                imageAnalyzer?.clearAnalyzer()
                cameraProvider?.unbindAll()
            } else {
                bindCameraUseCases()
            }
        }
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
