package com.keyri

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.annotations.SerializedName
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.exception.AccountNotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4::class)
class ExampleAppInstrumentedTest {

    private var scannedData: String? = null

    private val config by lazy {
        KeyriConfig(
            appKey = BuildConfig.APP_KEY,
            rpPublicKey = BuildConfig.RP_PUBLIC_KEY,
            callbackUrl = BuildConfig.KEYRI_CALLBACK_URL,
            allowMultipleAccounts = false
        )
    }

    private lateinit var context: Context
    private lateinit var keyriSdk: KeyriSdk

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        keyriSdk = KeyriSdk(context, config)
    }

    @Test
    fun `1_getScannedData`() = runBlocking {
        val devServiceId = "5ef32caaaccd766719387f08"
        val sessionQr = provideApiService().createSession(devServiceId)
        val qr = sessionQr.qr.replace("data:image/png;base64,", "")

        val decodedString: ByteArray = Base64.decode(qr, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
            .build()

        BarcodeScanning.getClient(options)
            .process(bitmap, 0)
            .addOnSuccessListener { barcodes ->
                scannedData = barcodes.mapNotNull { it }.firstOrNull()?.displayValue

                Log.d("Keyri", "SCANNED DATA: $scannedData")
            }

        delay(10_000L)

        Assert.assertNotNull(scannedData)
    }

    @Test
    fun `2_authenticate`() = runBlocking {
        val sessionId =
            requireNotNull(scannedData?.toUri()?.getQueryParameters("sessionId")?.firstOrNull())

        Log.d("Keyri", "SESSION ID: $sessionId")

        val session = keyriSdk.handleSessionId(sessionId)

        if (session.isNewUser) {
            keyriSdk.sessionSignup(session.username ?: "", sessionId, session.service, "Custom", true)
        } else {
            val account = keyriSdk.getAccounts().firstOrNull() ?: throw AccountNotFoundException
            keyriSdk.sessionLogin(account, sessionId, session.service, "Custom", true)
        }

        val userName = keyriSdk.getAccounts().first()

        delay(10_000L)

        Assert.assertEquals(session.username, userName)
    }

    private fun provideApiService(): TestApiService {
        val okHttpClientBuilder = OkHttpClient.Builder()

        okHttpClientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)

        if (com.keyrico.keyrisdk.BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl("")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientBuilder.build())
            .build()
            .create(TestApiService::class.java)
    }

    interface TestApiService {

        @Headers("x-api-key: BOenio0DXyG31mAgUCwhdslelckmxzM7nNOyWAjkuo7skr1FhP7m2L8PaSRgIEH5ja9p+CwEIIKGqR4Hx5Ezam4=")
        @GET("api/create-session")
        suspend fun createSession(@Query("serviceId") serviceId: String): QRResponse
    }

    data class QRResponse(@SerializedName("QR") val qr: String)

    companion object {
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 60L
    }
}
