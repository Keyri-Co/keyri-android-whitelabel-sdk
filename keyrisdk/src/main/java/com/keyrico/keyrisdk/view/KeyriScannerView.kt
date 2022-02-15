package com.keyrico.keyrisdk.view

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
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyrico.keyrisdk.R
import com.keyrico.keyrisdk.databinding.LayoutKeyriScannerViewBinding
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.entity.Service
import com.keyrico.keyrisdk.exception.AccountNotFoundException
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.exception.CameraPermissionNotGrantedException
import com.keyrico.keyrisdk.exception.KeyriScannerViewInitializationException
import com.keyrico.keyrisdk.exception.KeyriSdkException
import com.keyrico.keyrisdk.exception.MultipleAccountsNotAllowedException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Custom Scanner View which encapsulates the authorization of the desktop user agent.
 */
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
        imageProxy.image?.takeIf { !isLoading }?.let { mediaImage ->
            val image =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            BarcodeScanning.getClient(options).process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()
                        ?.displayValue
                        ?.let(::processScannedData)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } ?: imageProxy.close()
    }

    private var binding: LayoutKeyriScannerViewBinding =
        LayoutKeyriScannerViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var isLoading = false
        set(value) {
            field = value
            onLoading(field)
        }

    private var autoFocusEnabled = true
    private var flashEnabled = false

    /**
     * Call [initView] to initialize View.
     *
     * @keyriScannerViewParams parameters for initialization.
     */
    @Throws(KeyriScannerViewInitializationException::class)
    fun initView(keyriScannerViewParams: KeyriScannerViewParams) {
        if (keyriScannerViewParams.keyriSdk.allowMultipleAccounts && keyriScannerViewParams.onChooseAccount == null) {
            throw KeyriScannerViewInitializationException
        }

        this.keyriScannerViewParams = keyriScannerViewParams

        openScanner()
        initButtons()
    }

    /**
     * Call [continueAuth] to continue authorization process after selecting account.
     *
     * @publicAccount selected account.
     * @sessionId sessionId from @onChooseAccount callback.
     * @service service from @onChooseAccount callback.
     */
    fun continueAuth(publicAccount: PublicAccount, sessionId: String, service: Service) {
        launch {
            isLoading = true
            authAccount(publicAccount, sessionId, service)
            isLoading = false
        }
    }

    private fun processScannedData(scannedData: String) {
        Log.d("Keyri", "QR processed: $scannedData")

        try {
            // Try to parse link and process it
            scannedData.toUri()
                .getQueryParameters("sessionId")
                ?.firstOrNull()
                ?.let { sessionId ->
                    imageAnalyzer?.clearAnalyzer()
                    authenticate(sessionId)
                }
        } catch (e: Exception) {
            Log.d("Keyri", "Not valid link: $scannedData")
        }
    }

    private fun authenticate(sessionId: String) {
        launch {
            isLoading = true

            try {
                val params = requireNotNull(keyriScannerViewParams)
                val session = params.keyriSdk.onReadSessionId(sessionId)

                if (session.isNewUser) {
                    try {
                        params.keyriSdk.signup(
                            session.username,
                            sessionId,
                            session.service,
                            params.customArgument
                        )
                        onAuthCompleted()
                    } catch (e: Throwable) {
                        if (e is MultipleAccountsNotAllowedException) {
                            withContext(Dispatchers.Main) {
                                onAccountAlreadyExists(sessionId)
                            }
                        } else {
                            throw e
                        }
                    }
                } else {
                    val accounts = params.keyriSdk.accounts()

                    when {
                        accounts.isEmpty() -> throw AccountNotFoundException
                        accounts.size == 1 -> {
                            authAccount(accounts.first(), sessionId, session.service)
                            onAuthCompleted()
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                params.onChooseAccount?.invoke(accounts, sessionId, session.service)
                            }
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
        val params = requireNotNull(keyriScannerViewParams)

        try {
            params.keyriSdk.login(account, sessionId, service, params.customArgument)
            onAuthCompleted()
        } catch (e: Throwable) {
            Log.d("Keyri", "Authentication exception $e")

            val errorMessage = if (e is KeyriSdkException) e else AuthorizationException

            onError(errorMessage)
        }
    }

    private fun removeExistingAccountAndInitNewSession(sessionId: String) {
        launch {
            val params = requireNotNull(keyriScannerViewParams)

            params.keyriSdk.accounts().firstOrNull()?.let { account ->
                params.keyriSdk.removeAccount(account)
                authenticate(sessionId)
            }
        }
    }

    private fun onAccountAlreadyExists(sessionId: String) {
        keyriScannerViewParams?.onAccountAlreadyExists?.invoke()?.takeIf { it }?.let {
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

    @SuppressLint("ClickableViewAccessibility")
    private fun initButtons() {
        binding.ivAutofocus.setOnClickListener {
            if (autoFocusEnabled) {
                val preview = binding.scannerPreview

                preview.setOnTouchListener { _, event ->
                    return@setOnTouchListener when (event.action) {
                        MotionEvent.ACTION_DOWN -> true
                        MotionEvent.ACTION_UP -> {
                            val factory = SurfaceOrientedMeteringPointFactory(
                                preview.width.toFloat(),
                                preview.height.toFloat()
                            )
                            val autoFocusPoint = factory.createPoint(event.x, event.y)
                            try {
                                camera?.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(
                                        autoFocusPoint,
                                        FocusMeteringAction.FLAG_AF
                                    ).apply {
                                        // Focus only when the user tap the preview
                                        disableAutoCancel()
                                    }.build()
                                )
                            } catch (e: CameraInfoUnavailableException) {
                                Log.d("Keyri", "cannot access camera, $e")
                            }
                            true
                        }
                        else -> false
                    }
                }
            } else {
                val autoFocusPoint =
                    SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(.5f, .5f)

                try {
                    val autoFocusAction =
                        FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
                            .apply {
                                // Start auto-focusing after 2 seconds
                                setAutoCancelDuration(2, TimeUnit.SECONDS)
                            }.build()

                    camera?.cameraControl?.startFocusAndMetering(autoFocusAction)
                } catch (e: CameraInfoUnavailableException) {
                    Log.d("Keyri", "cannot access camera, $e")
                }
            }

            val focusRes =
                if (autoFocusEnabled) R.drawable.ic_autofocus_off else R.drawable.ic_autofocus_on

            binding.ivAutofocus.setImageResource(focusRes)

            autoFocusEnabled = !autoFocusEnabled
        }

        binding.ivFlash.setOnClickListener {
            camera?.let {
                val fleshRes = if (flashEnabled) R.drawable.ic_flash_off else R.drawable.ic_flash_on

                binding.ivFlash.setImageResource(fleshRes)

                it.cameraControl.enableTorch(!flashEnabled)

                flashEnabled = !flashEnabled
            }
        }
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
        launch(Dispatchers.Main) {
            keyriScannerViewParams?.onError?.invoke(exception)
                ?: onMessage(context.getString(exception.errorMessage))
        }
    }

    private fun onAuthCompleted() {
        launch(Dispatchers.Main) {
            keyriScannerViewParams?.onCompleted?.invoke()
                ?: onMessage(context.getString(R.string.keyri_auth_complete))
        }
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
                        view.display?.rotation?.let { rotation ->
                            imageAnalyzer?.targetRotation = rotation
                        }
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
            .setTargetResolution(Size(320, 320))
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
        launch(Dispatchers.Main) {
            keyriScannerViewParams?.onLoading?.invoke(isLoading) ?: with(binding) {
                flProgress.progress.isVisible = isLoading

                if (isLoading) {
                    imageAnalyzer?.clearAnalyzer()
//                    cameraProvider?.unbindAll()
                } else {
                    bindCameraUseCases()
                }
            }
        }
    }

    private fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit
    ) {
        keyriScannerViewParams?.activity?.lifecycleScope?.launch(dispatcher) { block() }
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
