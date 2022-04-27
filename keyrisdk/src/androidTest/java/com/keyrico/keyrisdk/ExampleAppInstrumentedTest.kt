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
    private lateinit var sessionId: String

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        keyriSdk = KeyriSdk(
            context,
            "BOenio0DXyG31mAgUCwhdslelckmxzM7nNOyWAjkuo7skr1FhP7m2L8PaSRgIEH5ja9p+CwEIIKGqR4Hx5Ezam4=",
            "misc.keyri.com"
        )
    }

    @Test
    fun testDomain() = runBlocking {
        launchActivity<WebViewActivity>().use {
            val result = it.result

            if (result.resultCode == Activity.RESULT_OK) {
                result.resultData.getStringExtra(SESSION_ID)?.let { id ->
                    sessionId = id
                }
            }
        }

        if (!this@ExampleAppInstrumentedTest::sessionId.isInitialized) {
            throw IllegalStateException("Couldn't scan sessionId with QR")
        }

        Log.d("Keyri", "SESSION ID: $sessionId")

        val session = keyriSdk.handleSessionId(sessionId)

        Assert.assertEquals(session.widgetOrigin, "misc.keyri.com")

        keyriSdk.challengeSession(
            "some-public-user-id",
            sessionId,
            "Secure custom",
            "Public custom",
        )
    }
}
