package com.keyrico.keyrisdk

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.keyrico.keyrisdk.WebViewActivity.Companion.TEST_RESULTS
import com.keyrico.keyrisdk.services.CryptoService
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

        Assert.assertEquals(sessionDialog?.get("mobileIpDataVisible"), true)
        Assert.assertEquals(sessionDialog?.get("widgetIpDataVisible"), true)
        Assert.assertEquals(sessionDialog?.get("userAgentVisible"), true)
        Assert.assertEquals(sessionDialog?.get("buttonsVisible"), true)
    }

    @Test
    fun `4_testDeniedSession`() {
        val sessionDialog = testResults?.sessionDeniedDialog

        Assert.assertEquals(sessionDialog?.get("mobileIpDataVisible"), true)
        Assert.assertEquals(sessionDialog?.get("widgetIpDataVisible"), true)
        Assert.assertEquals(sessionDialog?.get("userAgentVisible"), true)
        Assert.assertEquals(sessionDialog?.get("buttonsVisible"), false)
    }

    @Test
    fun `5_testWarningSession`() {
        val sessionDialog = testResults?.sessionWarningDialog

        Assert.assertEquals(sessionDialog?.get("mobileIpDataVisible"), true)
        Assert.assertEquals(sessionDialog?.get("widgetIpDataVisible"), true)
        Assert.assertEquals(sessionDialog?.get("userAgentVisible"), true)
        Assert.assertEquals(sessionDialog?.get("buttonsVisible"), true)
    }

    @Test
    fun `6_testNoIpDataSession`() {
        val sessionDialog = testResults?.sessionNoIpDataDialog

        Assert.assertEquals(sessionDialog?.get("mobileIpDataVisible"), false)
        Assert.assertEquals(sessionDialog?.get("widgetIpDataVisible"), false)
        Assert.assertEquals(sessionDialog?.get("userAgentVisible"), true)
        Assert.assertEquals(sessionDialog?.get("buttonsVisible"), true)
    }

    @Test
    fun `7_testWithoutRiskPermissionSession`() {
        val sessionDialog = testResults?.sessionWithoutRiskPermissionDialog

        Assert.assertEquals(sessionDialog?.get("mobileIpDataVisible"), false)
        Assert.assertEquals(sessionDialog?.get("widgetIpDataVisible"), false)
        Assert.assertEquals(sessionDialog?.get("userAgentVisible"), false)
        Assert.assertEquals(sessionDialog?.get("buttonsVisible"), true)
    }

    @Test
    fun `8_testCryptoServiceDifferentKeys`() {
        val cryptoService = CryptoService()

        val publicKey =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEHT7SM0JL8PPhAOQ+cFJn5hWPPSFKGxbVfp3htpjMUvQ9SM4fhtFUVryoKMz7z5/+MFxW96Sb9FKtq9z7mvJ1w=="
        val rawPublicKey =
            "BBB0+0jNCS/Dz4QDkPnBSZ+YVjz0hShsW1X6d4baYzFL0PUjOH4bRVFa8qCjM+8+f/jBcVvekm/RSravc+5rydc="
        val rawPublicKeyLite =
            "EHT7SM0JL8PPhAOQ+cFJn5hWPPSFKGxbVfp3htpjMUvQ9SM4fhtFUVryoKMz7z5/+MFxW96Sb9FKtq9z7mvJ1w=="

        val data = "Hello World!"

        val publicKeyCipher = cryptoService.encryptHkdf(publicKey,  data)
        val rawPublicKeyCipher = cryptoService.encryptHkdf(rawPublicKey,  data)
        val rawPublicKeyLiteCipher = cryptoService.encryptHkdf(rawPublicKeyLite,  data)

        Assert.assertNotNull(publicKeyCipher)
        Assert.assertNotNull(rawPublicKeyCipher)
        Assert.assertNotNull(rawPublicKeyLiteCipher)
    }

    companion object {
        private var testResults: TestResults? = null

        const val APP_KEY = "APP_KEY"
        const val WEB_VIEW_URL = "WEB_VIEW_URL"
    }
}
