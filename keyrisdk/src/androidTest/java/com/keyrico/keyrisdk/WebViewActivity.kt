package com.keyrico.keyrisdk

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)

        setContentView(webView)

        webView.settings.javaScriptEnabled = true

        webView.loadUrl("https://misc.keyri.com")

        webView.postDelayed({
            val picture: Picture = webView.capturePicture()
            val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)

            picture.draw(c)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
                .build()

            val image = InputImage.fromBitmap(bitmap, 0)

            BarcodeScanning.getClient(options)
                .process(image)
                .addOnSuccessListener { barcodes ->
                    val scannedData = barcodes.firstOrNull()?.displayValue

                    Log.d("Keyri", "Scanned data: $scannedData")

                    val sessionId = scannedData?.toUri()?.getQueryParameters("sessionId")?.firstOrNull()

                    Log.d("Keyri", "Session ID: $sessionId")

                    val resultIntent = Intent().apply {
                        putExtra(SESSION_ID, sessionId)
                    }

                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
        }, 10_000L)
    }

    companion object {
        const val SESSION_ID = "SESSION_ID"
    }
}
