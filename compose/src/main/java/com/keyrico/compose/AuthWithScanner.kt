package com.keyrico.compose

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScannerCompose() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { AndroidViewContext ->
            PreviewView(AndroidViewContext).apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Preview is incorrectly scaled in Compose on some devices without this
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val preview: Preview = Preview.Builder()
                    .build()
                    .also {
                        // Attach the viewfinder's surface provider to preview use case
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                @SuppressLint("UnsafeOptInUsageError")
                val qrAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
                    imageProxy.image
                        ?.let {
                            val image =
                                InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)

                            BarcodeScanning.getClient(options).process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()
                                        ?.displayValue
                                        ?.let { sessionId ->
                                            Log.e("Analyzed image", sessionId)

//                                            let(::processScannedData)
                                        }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } ?: imageProxy.close()
                }

                val rotation = previewView.display.rotation

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(320, 320))
                    .setTargetRotation(rotation)
                    .build()

                imageAnalyzer.setAnalyzer(cameraExecutor, qrAnalyzer)

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exception: Exception) {
                    Log.e("Keyri", "Failed to bind camera")
                }
            }, ContextCompat.getMainExecutor(context))
        },
    )
}

private val options by lazy {
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
        .build()
}