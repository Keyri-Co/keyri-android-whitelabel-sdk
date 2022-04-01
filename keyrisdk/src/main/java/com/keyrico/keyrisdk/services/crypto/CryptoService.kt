package com.keyrico.keyrisdk.services.crypto

import android.content.SharedPreferences
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import androidx.core.content.edit
import com.keyrico.keyrisdk.utils.toByteArrayFromBase64String
import com.keyrico.keyrisdk.utils.toStringBase64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CryptoService(private val preferences: SharedPreferences) {

    fun generateAssociationKey(publicUserId: String, backendPublicKey: String) {
        if (getKeyStore().containsAlias(AES_KEY_NAME + publicUserId)) return

        val publicBytes = Base64.decode(backendPublicKey, Base64.NO_WRAP)
        val publicKey = generateP256PublicKeyFromUncompressedW(publicBytes)
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        kpg.initialize(ECGenParameterSpec("prime256v1"))

        val keyPair = kpg.generateKeyPair()
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(publicKey, true)

        val secretKey = keyAgreement.generateSecret(KeyProperties.KEY_ALGORITHM_AES)

        saveSecretKey(secretKey, publicUserId)
        savePublicKey(keyPair.public, publicUserId)
    }

    fun getAssociationKey(publicUserId: String): String {
        val secretKeyBytes = getSecretKey(publicUserId)
        val encryptedKey = getEncryptedString(publicUserId) ?: throw Exception("Error")
        val key =
            decryptAes(secretKeyBytes, encryptedKey.toByteArrayFromBase64String(), publicUserId)

        return key.toStringBase64()
    }

    fun getIV(publicUserId: String): String? {
        return preferences.getString(IV_KEY_NAME + publicUserId, null)
    }

    fun saveIV(iv: String, publicUserId: String) {
        if (getIV(publicUserId) != null) return

        preferences.edit(true) {
            putString(IV_KEY_NAME + publicUserId, iv)
        }
    }

    fun encryptAes(data: String, publicUserId: String): String {
        val secretKey = getSecretKey(publicUserId)
        val dataBytes = data.encodeToByteArray()
        val encrypted = encryptAes(secretKey, dataBytes, publicUserId)

        return encrypted.toStringBase64()
    }

    fun decryptAes(data: String, publicUserId: String): String {
        val secretKey = getSecretKey(publicUserId)
        val dataBytes = data.toByteArrayFromBase64String()
        val decrypted = decryptAes(secretKey, dataBytes, publicUserId)

        return decrypted.decodeToString()
    }

    private fun saveSecretKey(secretKey: SecretKey, publicUserId: String) {
        val entry = KeyStore.SecretKeyEntry(secretKey)
        val keyStore = getKeyStore()
        val keyProtection =
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setRandomizedEncryptionRequired(false)
                .build()

        keyStore.setEntry(AES_KEY_NAME + publicUserId, entry, keyProtection)
    }

    private fun savePublicKey(publicKey: PublicKey, publicUserId: String) {
        val secretKey = getSecretKey(publicUserId)
        val encryptedKey = encryptAes(secretKey, publicKey.encoded, publicUserId)
        val encryptedKeyString = encryptedKey.toStringBase64()

        preferences.edit(true) {
            putString(EC_KEY_NAME + publicUserId, encryptedKeyString)
        }
    }

    private fun encryptAes(secretKey: SecretKey, data: ByteArray, publicUserId: String): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = getIV(publicUserId)

        if (iv != null) {
            val ivParameterSpec = IvParameterSpec(iv.toByteArrayFromBase64String())

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            saveIV(cipher.iv, publicUserId)
        }

        return cipher.doFinal(data)
    }

    private fun decryptAes(secretKey: SecretKey, data: ByteArray, publicUserId: String): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = getIV(publicUserId)?.toByteArrayFromBase64String() ?: byteArrayOf()
        val ivParameterSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return cipher.doFinal(data)
    }

    private fun getSecretKey(publicUserId: String): SecretKey {
        val secretKeyEntry =
            getKeyStore().getEntry(AES_KEY_NAME + publicUserId, null) as KeyStore.SecretKeyEntry

        return secretKeyEntry.secretKey
    }

    private fun getEncryptedString(publicUserId: String): String? {
        return preferences.getString(EC_KEY_NAME + publicUserId, null)
    }

    private fun saveIV(iv: ByteArray, publicUserId: String) {
        preferences.edit(true) {
            putString(IV_KEY_NAME + publicUserId, iv.toStringBase64())
        }
    }

    private fun getKeyStore() = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }

    private fun generateP256PublicKeyFromFlatW(w: ByteArray): ECPublicKey {
        val head = Base64.decode(HEAD_256, Base64.NO_WRAP)
        val encodedKey = ByteArray(head.size + w.size)

        System.arraycopy(head, 0, encodedKey, 0, head.size)
        System.arraycopy(w, 0, encodedKey, head.size, w.size)

        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val keySpec = X509EncodedKeySpec(encodedKey)

        return keyFactory.generatePublic(keySpec) as ECPublicKey
    }

    private fun generateP256PublicKeyFromUncompressedW(w: ByteArray): ECPublicKey {
        if (w[0].toInt() != 0x04) {
            throw InvalidKeySpecException("W is not an uncompressed key")
        }

        return generateP256PublicKeyFromFlatW(w.copyOfRange(1, w.size))
    }

    companion object {
        private const val HEAD_256 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val AES_KEY_NAME = "AES_KEY_NAME"
        private const val EC_KEY_NAME = "EC_KEY_NAME"
        private const val IV_KEY_NAME = "IV_KEY_NAME"
    }
}
