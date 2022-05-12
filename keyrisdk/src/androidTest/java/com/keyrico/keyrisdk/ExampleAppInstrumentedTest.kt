package com.keyrico.keyrisdk

import android.app.Activity
import android.util.Log
import androidx.test.core.app.launchActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.keyrico.keyrisdk.WebViewActivity.Companion.KEY
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

    @Before
    fun setup() {
        keyriSdk = KeyriSdk(
            InstrumentationRegistry.getInstrumentation().context,
            "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj",
            "BOenio0DXyG31mAgUCwhdslelckmxzM7nNOyWAjkuo7skr1FhP7m2L8PaSRgIEH5ja9p+CwEIIKGqR4Hx5Ezam4=",
            "misc.keyri.com"
        )
    }

    @Test
    fun testDomain() = runBlocking {
        var sessionId: String? = null
        var key: String? = null

        launchActivity<WebViewActivity>().use { scenario ->
            scenario.result.takeIf { it.resultCode == Activity.RESULT_OK }?.resultData?.let { data ->
                sessionId = data.getStringExtra(SESSION_ID)
                key = data.getStringExtra(KEY)
            }
        }

        Assert.assertNotNull(sessionId)

        sessionId?.let {
            Log.d("Keyri", "SESSION ID: $it")

            val session = keyriSdk.initiateSession(it)

            Assert.assertEquals(session.widgetOrigin, "misc.keyri.com")

            keyriSdk.approveSession(
                "some-public-user-id",
                key ?: throw IllegalStateException("Couldn't scan key with QR"),
                it,
                "Secure custom",
                "Public custom",
            )
        } ?: throw IllegalStateException("Couldn't scan sessionId with QR")
    }
}
