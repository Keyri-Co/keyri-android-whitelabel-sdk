package com.keyrico.keyrisdk

import android.security.keystore.KeyProperties
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.utils.toByteArrayFromBase64String
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import org.junit.Assert
import org.junit.Test

class KeyriSdkUnitTest {

    @Test
    fun `1_testCryptoServiceDifferentKeys`() {
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
    fun `2_testCryptoServiceAssociationKeys`() {
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
    fun `3_testCryptoServiceUserSignature`() {
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
}
