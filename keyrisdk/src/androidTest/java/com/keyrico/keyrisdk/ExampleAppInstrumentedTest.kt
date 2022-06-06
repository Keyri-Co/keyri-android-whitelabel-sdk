package com.keyrico.keyrisdk

import android.app.Activity
import android.content.Intent
import android.security.keystore.KeyProperties
import androidx.test.core.app.launchActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.keyrico.keyrisdk.WebViewActivity.Companion.TEST_RESULTS
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.utils.toByteArrayFromBase64String
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec

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

        val publicKeyCipher = cryptoService.encryptHkdf(publicKey, data)
        val rawPublicKeyCipher = cryptoService.encryptHkdf(rawPublicKey, data)
        val rawPublicKeyLiteCipher = cryptoService.encryptHkdf(rawPublicKeyLite, data)

        Assert.assertNotNull(publicKeyCipher)
        Assert.assertNotNull(rawPublicKeyCipher)
        Assert.assertNotNull(rawPublicKeyLiteCipher)
    }

    @Test
    fun `9_testCryptoServiceAssociationKeys`() {
        val cryptoService = CryptoService()

        val anonymousAssociationKey = cryptoService.getAssociationKey(null)
        val associationKeyForUnknownUser = cryptoService.getAssociationKey("Unknown user ID")
        val associationKeyForUnknownUserTwice = cryptoService.getAssociationKey("Unknown user ID")

        cryptoService.generateAssociationKey("User ID")

        val newKeys = cryptoService.listAssociationKey()

        Assert.assertNotNull(anonymousAssociationKey)
        Assert.assertNotNull(associationKeyForUnknownUser)
        Assert.assertEquals(associationKeyForUnknownUser, associationKeyForUnknownUserTwice)
        Assert.assertNotEquals(0, newKeys.size)
    }

    @Test
    fun `91_testCryptoServiceUserSignature`() {
        val cryptoService = CryptoService()

        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val signature = Signature.getInstance("SHA256withECDSA")

        val anonymousMessageToSign = "Anonymous message to sign"
        val anonymousSignedMessage = cryptoService.signMessage(null, anonymousMessageToSign)
        val anonymousAssociationKey = cryptoService.getAssociationKey(null)

        val encodedAnonymousKey = anonymousAssociationKey.toByteArrayFromBase64String()
        val anonymousPublic =
            keyFactory.generatePublic(X509EncodedKeySpec(encodedAnonymousKey)) as ECPublicKey

        signature.initVerify(anonymousPublic)
        signature.update(anonymousMessageToSign.encodeToByteArray())

        val anonymousVerified =
            signature.verify(anonymousSignedMessage.toByteArrayFromBase64String())

        val userMessageToSign = "Message to sign"
        val userID = "Public-UID"
        val userSignedMessage = cryptoService.signMessage(userID, userMessageToSign)
        val userAssociationKey = cryptoService.getAssociationKey(userID)

        val encodedUserKey = userAssociationKey.toByteArrayFromBase64String()
        val userPublic =
            keyFactory.generatePublic(X509EncodedKeySpec(encodedUserKey)) as ECPublicKey

        signature.initVerify(userPublic)
        signature.update(userMessageToSign.encodeToByteArray())

        val userVerified = signature.verify(userSignedMessage.toByteArrayFromBase64String())

        Assert.assertTrue(anonymousVerified)
        Assert.assertTrue(userVerified)
    }

    companion object {
        private var testResults: TestResults? = null

        const val APP_KEY = "APP_KEY"
        const val WEB_VIEW_URL = "WEB_VIEW_URL"
    }
}
