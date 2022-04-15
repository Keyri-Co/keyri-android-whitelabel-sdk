package com.keyri.ui.main

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
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.lifecycle.Observer
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyri.databinding.ActivityAuthBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()

    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = window.decorView.let { view ->
            if (displayId == this@MainActivity.displayId) {
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openScanner()
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

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.loading().observe(this, Observer(::onLoading))

        initializeUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun initializeUi() {
        binding.bAuthQr.setOnClickListener { openScanner() }
    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        initCamera()
    }

    private fun initCamera() {
        binding.scannerPreview.isGone = false
        binding.actionsPanel.isGone = true

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
        imageProxy.image?.takeIf { viewModel.loading().value != true }?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

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

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)

        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun onLoading(isLoading: Boolean) {
        with(binding) {
            actionsPanel.isGone = false

            imageAnalyzer?.clearAnalyzer()

            if (isLoading) {
                panelContent.visibility = View.GONE
                flProgress.progress.visibility = View.VISIBLE
            } else {
                panelContent.visibility = View.VISIBLE
                flProgress.progress.visibility = View.GONE
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

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
            viewModel.onReadSessionId(sessionId)
        } ?: Log.e("Keyri", "Failed to process link")
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
