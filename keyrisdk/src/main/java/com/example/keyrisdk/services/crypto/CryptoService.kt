package com.example.keyrisdk.services.crypto

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import androidx.core.content.edit
import com.example.keyrisdk.utils.toByteArrayFromBase64String
import com.example.keyrisdk.utils.toStringBase64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import com.example.keyrisdk.exception.NotInitializedException
import javax.crypto.spec.IvParameterSpec

class CryptoService(private val preferences: SharedPreferences) {

    private fun getKeyStore() = KeyStore
        .getInstance(ANDROID_KEYSTORE)
        .also { it.load(null) }

    fun generateECDHSecret(backendPublicKey: String) {
        if (getKeyStore().containsAlias(AES_KEY_NAME)) return

        val publicBytes = Base64.decode(backendPublicKey, Base64.NO_WRAP)
        val pubKeySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val publicKey = keyFactory.generatePublic(pubKeySpec)

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        kpg.initialize(ECGenParameterSpec("prime256v1"))

        val keyPair = kpg.generateKeyPair()
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(publicKey, true)

        val secretKey = keyAgreement.generateSecret(KeyProperties.KEY_ALGORITHM_AES)

        saveSecretKey(secretKey)
        savePublicKey(keyPair.public)
    }

    private fun saveSecretKey(secretKey: SecretKey) {
        val entry = KeyStore.SecretKeyEntry(secretKey)
        val keyStore = getKeyStore()
        val keyProtection =
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setRandomizedEncryptionRequired(false)
                .build()

        keyStore.setEntry(AES_KEY_NAME, entry, keyProtection)
    }

    private fun savePublicKey(publicKey: PublicKey) {
        val secretKey = getSecretKey()
        val encryptedKey = encryptAes(secretKey, publicKey.encoded)
        val encryptedKeyString = encryptedKey.toStringBase64()

        preferences.edit(true) {
            putString(EC_KEY_NAME, encryptedKeyString)
        }
    }

    /**
     * Encrypts a string using AES symmetric encryption.
     * @data string to encrypt
     *
     * @return encrypted string converted into Base64
     */
    fun encryptAes(data: String): String {
        val secretKey = getSecretKey()
        val dataBytes = data.encodeToByteArray()
        val encrypted = encryptAes(secretKey, dataBytes)

        return encrypted.toStringBase64()
    }

    /**
     * Encrypts given bytes using AES symmetric encryption.
     * @secretKeyBytes secret key bytes
     * @data bytes to encrypt
     *
     * @return encrypted bytes
     */
    @SuppressLint("GetInstance")
    private fun encryptAes(secretKey: SecretKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = getIV()

        if (iv != null) {
            val ivParameterSpec = IvParameterSpec(iv.toByteArrayFromBase64String())

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            saveIV(cipher.iv)
        }

        return cipher.doFinal(data)
    }

    /**
     * Decrypts a string using AES symmetric encryption.
     * @data string to decrypt
     *
     * @return decrypted string converted
     */
    fun decryptAes(data: String): String {
        val secretKey = getSecretKey()
        val dataBytes = data.toByteArrayFromBase64String()
        val decrypted = decryptAes(secretKey, dataBytes)

        return decrypted.decodeToString()
    }

    /**
     * Decrypts given bytes using AES symmetric encryption.
     * @key secret key bytes
     * @data bytes to decrypt
     *
     * @return decrypted bytes
     */
    @SuppressLint("GetInstance")
    private fun decryptAes(secretKey: SecretKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = getIV()?.toByteArrayFromBase64String() ?: byteArrayOf()
        val ivParameterSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return cipher.doFinal(data)
    }

    private fun getSecretKey(): SecretKey {
        val secretKeyEntry = getKeyStore().getEntry(AES_KEY_NAME, null) as KeyStore.SecretKeyEntry

        return secretKeyEntry.secretKey
    }

    private fun getEncryptedString(): String? {
        return preferences.getString(EC_KEY_NAME, null)
    }

    private fun saveIV(iv: ByteArray) {
        preferences.edit(true) {
            putString(IV_KEY_NAME, iv.toStringBase64())
        }
    }

    fun getPublicKey(): String {
        val secretKeyBytes = getSecretKey()
        val encryptedKey = getEncryptedString() ?: throw NotInitializedException
        val key = decryptAes(secretKeyBytes, encryptedKey.toByteArrayFromBase64String())

        return key.toStringBase64()
    }

    fun getIV(): String? {
        return preferences.getString(IV_KEY_NAME, null)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val AES_KEY_NAME = "AES_KEY_NAME"
        private const val EC_KEY_NAME = "EC_KEY_NAME"
        private const val IV_KEY_NAME = "IV_KEY_NAME"
    }
}
