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

    private lateinit var context: Context
    private lateinit var keyriSdk: KeyriSdk

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        keyriSdk = KeyriSdk(
            context,
            "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj",
            "BOenio0DXyG31mAgUCwhdslelckmxzM7nNOyWAjkuo7skr1FhP7m2L8PaSRgIEH5ja9p+CwEIIKGqR4Hx5Ezam4=",
            "misc.keyri.com"
        )
    }

    @Test
    fun testDomain() = runBlocking {
        var sessionId: String? = null

        launchActivity<WebViewActivity>().use {
            val result = it.result

            if (result.resultCode == Activity.RESULT_OK) {
                result.resultData.getStringExtra(SESSION_ID)?.let { id ->
                    sessionId = id
                }
            }
        }

        Assert.assertNotNull(sessionId)

        sessionId?.let {
            Log.d("Keyri", "SESSION ID: $it")

            val session = keyriSdk.handleSessionId(it)

            Assert.assertEquals(session.widgetOrigin, "misc.keyri.com")

            keyriSdk.challengeSession(
                "some-public-user-id",
                it,
                "Secure custom",
                "Public custom",
            )
        } ?: throw IllegalStateException("Couldn't scan sessionId with QR")
    }
}
