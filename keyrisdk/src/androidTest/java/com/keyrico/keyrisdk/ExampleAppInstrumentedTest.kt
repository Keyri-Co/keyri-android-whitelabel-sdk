package com.keyrico.keyrisdk

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.keyrico.keyrisdk.WebViewActivity.Companion.TEST_RESULTS
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4::class)
class ExampleAppInstrumentedTest {

    @Test
    fun `1_setup`() = runBlocking {
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().context,
            WebViewActivity::class.java
        ).apply {
            putExtra(WEB_VIEW_URL, "https://misc.keyri.com")
            putExtra(APP_KEY, "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj")
        }

        launchActivity<WebViewActivity>(intent).use { scenario ->
            scenario.result.resultData.setExtrasClassLoader(TestResults::class.java.classLoader)

            testResults = scenario.result
                .takeIf { it.resultCode == Activity.RESULT_OK }
                ?.resultData
                ?.takeIf { it.hasExtra(TEST_RESULTS) }
                ?.getParcelableExtra(TEST_RESULTS)
        }

        Assert.assertNotNull(testResults)
    }

    @Test
    fun `2_testSessionIdNotNull`() {
        Assert.assertNotNull(testResults?.sessionId)
    }

    @Test
    fun `3_testRegularSession`() {
        val sessionDialog = testResults?.sessionRegularDialog

        Assert.assertEquals(true, sessionDialog?.get("mobileIpDataVisible"))
        Assert.assertEquals(true, sessionDialog?.get("widgetIpDataVisible"))
        Assert.assertEquals(true, sessionDialog?.get("userAgentVisible"))
        Assert.assertEquals(true, sessionDialog?.get("buttonsVisible"))
    }

    @Test
    fun `4_testDeniedSession`() {
        Assert.assertEquals(false, testResults?.sessionDeniedDialog?.get("buttonsVisible"))
    }

    @Test
    fun `5_testWarningSession`() {
        val sessionDialog = testResults?.sessionWarningDialog

        Assert.assertEquals(true, sessionDialog?.get("mobileIpDataVisible"))
        Assert.assertEquals(true, sessionDialog?.get("widgetIpDataVisible"))
        Assert.assertEquals(true, sessionDialog?.get("userAgentVisible"))
        Assert.assertEquals(true, sessionDialog?.get("buttonsVisible"))
    }

    @Test
    fun `6_testNoIpDataSession`() {
        val sessionDialog = testResults?.sessionNoIpDataDialog

        Assert.assertEquals(false, sessionDialog?.get("mobileIpDataVisible"))
        Assert.assertEquals(false, sessionDialog?.get("widgetIpDataVisible"))
        Assert.assertEquals(true, sessionDialog?.get("userAgentVisible"))
        Assert.assertEquals(true, sessionDialog?.get("buttonsVisible"))
    }

    @Test
    fun `7_testWithoutRiskPermissionSession`() {
        val sessionDialog = testResults?.sessionWithoutRiskPermissionDialog

        Assert.assertEquals(false, sessionDialog?.get("mobileIpDataVisible"))
        Assert.assertEquals(false, sessionDialog?.get("widgetIpDataVisible"))
        Assert.assertEquals(false, sessionDialog?.get("userAgentVisible"))
        Assert.assertEquals(true, sessionDialog?.get("buttonsVisible"))
    }

    companion object {
        private var testResults: TestResults? = null

        const val APP_KEY = "APP_KEY"
        const val WEB_VIEW_URL = "WEB_VIEW_URL"
    }
}
