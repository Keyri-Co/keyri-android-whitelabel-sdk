package com.keyrico.keyrisdk.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyrico.keyrisdk.R
import com.keyrico.keyrisdk.databinding.ActivityAuthWithScannerBinding
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal class AuthWithScannerActivity : AppCompatActivity() {

    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = window.decorView.let { view ->
            if (displayId == this@AuthWithScannerActivity.displayId) {
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        }
    }

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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openScanner()
            }
        }

    private lateinit var binding: ActivityAuthWithScannerBinding

    private val viewModel by viewModels<AuthWithScannerVM>()

    private var autoFocusEnabled = true
    private var flashEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthWithScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUi()
    }

    private fun initializeUi() {

    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        initCamera()
    }

    private fun initCamera() {
        displayManager.registerDisplayListener(displayListener, null)

        binding.scannerPreview.post {
            displayId = binding.scannerPreview.display.displayId

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this))
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

        imageAnalyzer?.setAnalyzer(cameraExecutor, initQrAnalyzer())
        cameraProvider?.unbindAll()

        try {
            camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            preview?.setSurfaceProvider(binding.scannerPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.d("Keyri", "Failed to init Camera")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initQrAnalyzer(): ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { imageProxy ->
//        imageProxy.image?.takeIf { viewModel.loading().value != true }?.let { mediaImage ->
//            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//            BarcodeScanning.getClient(options).process(image)
//                .addOnSuccessListener { barcodes ->
//                    barcodes.firstOrNull()
//                        ?.displayValue
//                        ?.let(::processScannedData)
//                }
//                .addOnCompleteListener {
//                    imageProxy.close()
//                }
//        } ?: imageProxy.close()
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)

        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun processScannedData(scannedData: String) {
        Log.d("Keyri", "QR processed: $scannedData")

        try {
            // Try to parse link and process it
            processLink(scannedData.toUri())
        } catch (e: java.lang.Exception) {
            Log.d("Keyri", "Not valid link: $scannedData")
        }
    }

    private fun processLink(uri: Uri?) {
        uri?.getQueryParameters("sessionid")?.firstOrNull()?.let { sessionId ->

            cameraProvider?.unbindAll()
//            viewModel.onReadSessionId(sessionId)
        } ?: Log.e("Keyri", "Failed to process link")
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

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
