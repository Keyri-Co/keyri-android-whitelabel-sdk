package com.keyrico.keyrisdk.services.crypto

import android.content.SharedPreferences
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import androidx.core.content.edit
import com.keyrico.keyrisdk.exception.NotInitializedException
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
import javax.crypto.spec.SecretKeySpec

class CryptoService(private val preferences: SharedPreferences) {

    /**
     * Function for generating AES secret key with ECDH key agreement.
     *
     * @backendPublicKey base64 string of backend EC public key.
     */
    fun generateSecretKey(backendPublicKey: String) {
        if (getKeyStore().containsAlias(AES_KEY_NAME)) return

        val publicBytes = Base64.decode(backendPublicKey, Base64.NO_WRAP)
        val publicKey = generateP256PublicKeyFromUncompressedW(publicBytes)
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

    /**
     * Function for retrieving EC public key. Must be called after [generateSecretKey] invocation.
     */
    fun getPublicKey(): String {
        val secretKeyBytes = getSecretKey()
        val encryptedKey = getEncryptedString() ?: throw NotInitializedException
        val key = decryptAes(secretKeyBytes, encryptedKey.toByteArrayFromBase64String())

        return key.toStringBase64()
    }

    fun getIV(): String? {
        return preferences.getString(IV_KEY_NAME, null)
    }

    /**
     * Encrypts a string using AES symmetric encryption.
     *
     * @data string to encrypt.
     * @return encrypted string in Base64 encoding.
     */
    fun encryptAes(data: String): String {
        val secretKey = getSecretKey()
        val dataBytes = data.encodeToByteArray()
        val encrypted = encryptAes(secretKey, dataBytes)

        return encrypted.toStringBase64()
    }

    fun encryptAes(data: String, key: String): String {
        val decodedKey: ByteArray = Base64.decode(key, Base64.NO_WRAP)
        val secretKey: SecretKey = SecretKeySpec(decodedKey, KeyProperties.KEY_ALGORITHM_AES)

        val dataBytes = data.encodeToByteArray()
        val encrypted = encryptAes(secretKey, dataBytes)

        return encrypted.toStringBase64()
    }

    /**
     * Decrypts a string using AES symmetric encryption.
     *
     * @data string to decrypt (Base64 encoding).
     * @return decrypted string.
     */
    fun decryptAes(data: String): String {
        val secretKey = getSecretKey()
        val dataBytes = data.toByteArrayFromBase64String()
        val decrypted = decryptAes(secretKey, dataBytes)

        return decrypted.decodeToString()
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

    private fun getKeyStore() = KeyStore
        .getInstance(ANDROID_KEYSTORE)
        .also { it.load(null) }

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
