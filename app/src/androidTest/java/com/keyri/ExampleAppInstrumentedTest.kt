package com.keyri

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyrico.keyrisdk.KeyriConfig
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.exception.AccountNotFoundException
import java.net.URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class ExampleAppInstrumentedTest {

    private var scannedData: String? = null

    private val config by lazy {
        KeyriConfig(
            appKey = BuildConfig.APP_KEY,
            publicKey = BuildConfig.PUBLIC_KEY,
            callbackUrl = BuildConfig.KEYRI_CALLBACK_URL,
            allowMultipleAccounts = false
        )
    }

    private val context = Mockito.mock(Context::class.java)

    private val keyriSdk = KeyriSdk(context, config)

    @Test
    fun getScannedData() = runBlocking {
        val url = URL(BuildConfig.IMAGE_URL)
        val bitmap = BitmapFactory.decodeStream(url.openStream())
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
            .build()

        BarcodeScanning.getClient(options).process(image)
            .addOnSuccessListener { barcodes ->
                scannedData = barcodes.firstOrNull()?.displayValue

                Log.d("Keyri", "SCANNED DATA: ${scannedData.toString()}")
            }

        delay(10_000L)

        Assert.assertNotNull(scannedData)
    }

    @Test
    fun authenticate() = runBlocking {
        val sessionId =
            requireNotNull(scannedData?.toUri()?.getQueryParameters("sessionId")?.firstOrNull())

        Log.d("Keyri", "SESSION ID: $sessionId")

        val session = keyriSdk.onReadSessionId(sessionId)

        if (session.isNewUser) {
            keyriSdk.signup(session.username, sessionId, session.service, "Custom")
        } else {
            val account = keyriSdk.accounts().firstOrNull() ?: throw AccountNotFoundException
            keyriSdk.login(account, sessionId, session.service, "Custom")
        }

        val userName = keyriSdk.accounts().first()

        delay(10_000L)

        Assert.assertEquals(session.username, userName)
    }
}
