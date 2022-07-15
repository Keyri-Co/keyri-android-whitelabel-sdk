package com.keyrico.compose

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeDetectorProcessor {

    private val detector: BarcodeScanner
    private val executor = TaskExecutors.MAIN_THREAD

    init {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
            .build()

        detector = BarcodeScanning.getClient(options)
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun processImageProxy(image: ImageProxy, onScanResult: (Result<String>) -> Unit = {}) {
        image.image?.let {
            detector.process(InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees))
                .addOnSuccessListener(executor) { barcodes ->
                    barcodes.firstOrNull()?.displayValue?.let { barcode ->
                        onScanResult(Result.success(barcode))
                    }
                }
                .addOnFailureListener(executor) { exception: Exception ->
                    Log.e("Keyri", "Error detecting barcode", exception)

                    onScanResult(Result.failure(exception))
                }
                .addOnCompleteListener { image.close() }
        }
    }
}
