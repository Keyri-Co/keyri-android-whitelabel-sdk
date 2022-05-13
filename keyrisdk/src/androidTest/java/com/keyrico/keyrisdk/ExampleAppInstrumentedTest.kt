package com.keyrico.keyrisdk

import android.app.Activity
import android.content.Context
import android.content.Intent
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
    private lateinit var context: Context

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

            val intent = Intent(context, DialogActivity::class.java).apply {
                putExtra(DialogActivity.SESSION, session)
            }

            launchActivity<DialogActivity>(intent).use { scenario ->
                Assert.assertEquals(scenario.result.resultCode, Activity.RESULT_OK)
            }

            keyriSdk.approveSession(
                "some-public-user-id",
                "some-username",
                key ?: throw IllegalStateException("Couldn't scan key with QR"),
                it,
                "Secure custom",
                "Public custom",
            )
        } ?: throw IllegalStateException("Couldn't scan sessionId with QR")
    }
}
