package com.example.keyrisdk.services.crypto

import android.annotation.SuppressLint
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import com.example.keyrisdk.utils.toByteArrayFromBase64String
import com.example.keyrisdk.utils.toStringBase64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

class CryptoService {

    private fun getKeyStore() = KeyStore
        .getInstance(ANDROID_KEYSTORE)
        .also { it.load(null) }

    // TODO Temp fot testing, remove later
    var publicKey = byteArrayOf()
    var secretKey = byteArrayOf()

    fun generateSecret(backendPublicKey: String) {
        if(publicKey.isEmpty() || secretKey.isEmpty()) {
            generateECDHSecret(backendPublicKey)
        }
    }

    private fun generateECDHSecret(backendPublicKey: String): String {
        val publicBytes: ByteArray = Base64.decode(backendPublicKey, Base64.NO_WRAP)
        val pubKeySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val publicKey = keyFactory.generatePublic(pubKeySpec)

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val eCGenParameterSpec = ECGenParameterSpec("prime256v1")

        kpg.initialize(eCGenParameterSpec)

        val keyPair = kpg.generateKeyPair()
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(publicKey, true)

        val secretKey = keyAgreement.generateSecret(KeyProperties.KEY_ALGORITHM_AES)
        val keyStoreEntry = KeyStore.SecretKeyEntry(secretKey)
        val keyProtection =
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setDigests(KeyProperties.DIGEST_MD5)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .build()

        // TODO Remove
        this.secretKey = secretKey.encoded
        this.publicKey = keyPair.public.encoded

        // TODO Save as Private, not Secret (need some life-hacks to fix)
        getKeyStore().setEntry(AES_KEY_NAME, keyStoreEntry, keyProtection)

        return keyPair.public.encoded.toStringBase64()
    }

    /**
     * Encrypts a string using AES symmetric encryption.
     * @data string to encrypt
     *
     * @return encrypted string converted into Base64
     */
    fun encryptAes(data: String): String {
//        val secretKeyEntry = getKeyStore().getEntry(AES_KEY_NAME, null) as KeyStore.SecretKeyEntry
//        val secretKeyBytes = secretKeyEntry.secretKey.encoded
        val dataBytes = data.encodeToByteArray()
//        val encrypted = encryptAes(secretKeyBytes, dataBytes)
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
    private fun encryptAes(secretKeyBytes: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val key = SecretKeySpec(
            MessageDigest.getInstance(KeyProperties.DIGEST_MD5).digest(secretKeyBytes),
            KeyProperties.KEY_ALGORITHM_AES
        )

        cipher.init(Cipher.ENCRYPT_MODE, key)

        return cipher.doFinal(data)
    }

    /**
     * Decrypts a string using AES symmetric encryption.
     * @data string to decrypt
     *
     * @return decrypted string converted
     */
    fun decryptAes(data: String): String {
//        val secretKeyEntry = getKeyStore().getEntry(AES_KEY_NAME, null) as KeyStore.SecretKeyEntry
//        val secretKeyBytes = secretKeyEntry.secretKey.encoded
        val dataBytes = data.toByteArrayFromBase64String()
//        val decrypted = decryptAes(secretKeyBytes, dataBytes)
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
    private fun decryptAes(secretKeyBytes: ByteArray, data: ByteArray?): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val key = SecretKeySpec(
            MessageDigest.getInstance(KeyProperties.DIGEST_MD5).digest(secretKeyBytes),
            KeyProperties.KEY_ALGORITHM_AES
        )

        cipher.init(Cipher.DECRYPT_MODE, key)

        return cipher.doFinal(data)
    }

    // TODO Need to Refactor
    fun getPublicKey(): String {
        return publicKey.toStringBase64()

//        return getKeyStore().getCertificate("SHA256withRSA").publicKey.encoded.toStringBase64()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/ECB/PKCS5Padding"
        private const val AES_KEY_NAME = "keyri_ks_v01"
    }
}
