package com.keyrico.keyrisdk

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.test.core.app.launchActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.keyrico.keyrisdk.WebViewActivity.Companion.SESSION_ID
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExampleAppInstrumentedTest {

    private lateinit var keyriSdk: KeyriSdk
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context

        keyriSdk = KeyriSdk(context, APP_KEY, SERVICE_DOMAIN)
    }

    @Test
    fun testDomain() = runBlocking {
        var sessionId: String? = null

        launchActivity<WebViewActivity>().use { scenario ->
            scenario.result.takeIf { it.resultCode == Activity.RESULT_OK }?.resultData?.let { data ->
                sessionId = data.getStringExtra(SESSION_ID)
            }
        }

        Assert.assertNotNull(sessionId)

        sessionId?.let {
            Log.d("Keyri", "Session ID: $it")

            val session = keyriSdk.initiateSession(it)

            Assert.assertEquals(session.widgetOrigin, SERVICE_DOMAIN)

            keyriSdk.approveSession(
                "some-public-user-id",
                "some-username",
                session.browserPublicKey,
                it,
                "Secure custom",
                "Public custom",
            )
        } ?: throw IllegalStateException("Couldn't scan sessionId with QR")
    }

    companion object {
        private const val APP_KEY = "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj"
        private const val SERVICE_DOMAIN = "misc.keyri.com"
    }
}
