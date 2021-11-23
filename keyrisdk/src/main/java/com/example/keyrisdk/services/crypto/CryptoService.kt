package com.example.keyrisdk.services.crypto

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.keyrisdk.utils.fromBase64
import com.example.keyrisdk.utils.toByteArrayFromBase64String
import com.example.keyrisdk.utils.toStringBase64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.SealedObject
import javax.crypto.spec.SecretKeySpec

class CryptoService(preferences: SharedPreferences) {

    private val cryptoBoxHolder = CryptoBoxHolder(preferences)

    init {
        createRsaKeyPairIfNeeded()
    }

    /**
     * Creates RSA keypair in android keystore if it doesn't exist
     */
    private fun createRsaKeyPairIfNeeded() {
        val keyStore = getKeyStore()
        if (!keyStore.containsAlias(KEYPAIR_NAME)) createEncryptionRsaKeyPair()
    }

    /**
     * Creates RSA keypair in android keystore
     */
    private fun createEncryptionRsaKeyPair() {
        val keyGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEYPAIR_NAME,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT or
                    KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA512
            )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()

        keyGenerator.initialize(parameterSpec)
        keyGenerator.generateKeyPair()
    }

    private fun getKeyStore() = KeyStore
        .getInstance(ANDROID_KEYSTORE)
        .also { it.load(null) }

    /**
     *  Extracts and decrypts a crypto box keypair from persistent storage
     */
    private fun getCryptoBox(): CryptoBox {
        val cryptoBox = cryptoBoxHolder.getCryptoBox() ?: throw IllegalStateException()
        return cryptoBox.copy(privateKey = decryptRsa(cryptoBox.privateKey))
    }

    // TODO Here we need to use our generated RSA key
    fun getCryptoBoxPublicKey() = getCryptoBox().publicKey

    /**
     * Encrypts a string using RSA asymmetric encryption.
     * @data string to encrypt
     *
     * @return encrypted string encoded in Base64
     */
    private fun encryptRsa(data: String): String {
        val plainBytes = data.toByteArray()
        val encryptedBytes = encryptRsa(plainBytes)
        return encryptedBytes.toStringBase64()
    }

    /**
     * Encrypts given bytes using RSA asymmetric encryption.
     * @data bytes to encrypt
     *
     * @return encrypted bytes
     */
    private fun encryptRsa(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        val publicKey = getKeyStore().getCertificate(KEYPAIR_NAME).publicKey
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    /**
     * Decrypts a string using RSA asymmetric encryption.
     * @data string to decrypt (encoded in Base64)
     *
     * @return decrypted string
     */
    private fun decryptRsa(data: String): String {
        val encryptedBytes = data.toByteArrayFromBase64String()
        val decryptedBytes = decryptRsa(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Decrypts given bytes using RSA asymmetric encryption.
     * @data bytes to decrypt
     *
     * @return decrypted bytes
     */
    private fun decryptRsa(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        val privateKey = getKeyStore().getKey(KEYPAIR_NAME, null) as PrivateKey
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(data)
    }

    /**
     * Encrypts a string using AES symmetric encryption.
     * Uses crypto box private key as a secret key
     * @data string to encrypt
     *
     * @return encrypted string converted into Base64
     */
    fun encryptAes(data: String): String {
        val key = getCryptoBox().privateKey.take(32)
        return encryptAes(key, data)
    }

    /**
     * Encrypts a string using AES symmetric encryption.
     * @key secret key encoded in Base64
     * @data string to encrypt
     *
     * @return encrypted string converted into Base64
     */
    private fun encryptAes(key: String, data: String): String {
        val keyData = key.toByteArrayFromBase64String()
        val encrypted = encryptAes(keyData, data.toByteArray())
        return encrypted.toStringBase64()
    }

    /**
     * Encrypts given bytes using AES symmetric encryption.
     * @key secret key
     * @data bytes to encrypt
     *
     * @return encrypted bytes
     */
    @SuppressLint("GetInstance")
    private fun encryptAes(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = SecretKeySpec(key, KeyProperties.KEY_ALGORITHM_AES)
        cipher.init(Cipher.ENCRYPT_MODE, spec)
        return cipher.doFinal(data)
    }

    /**
     * Decrypts a string using AES symmetric encryption.
     * Uses crypto box private key as a secret key
     * @data string to decrypt
     *
     * @return decrypted string converted into Base64
     */
    fun decryptAes(data: String): String {
        val key = getCryptoBox().privateKey.take(32)
        return decryptAes(key, data)
    }

    /**
     * Decrypts a string using AES symmetric encryption.
     * @key secret key encoded in Base64
     * @data string to decrypt encoded in Base64
     *
     * @return decrypted string
     */
    private fun decryptAes(key: String, data: String): String {
        val keyData = key.toByteArrayFromBase64String()
        val enc = data.toByteArray().fromBase64()
        val decrypted = decryptAes(keyData, enc)
        return String(decrypted)
    }

    /**
     * Decrypts given bytes using AES symmetric encryption.
     * @key secret key
     * @data bytes to decrypt
     *
     * @return decrypted bytes
     */
    @SuppressLint("GetInstance")
    private fun decryptAes(key: ByteArray, data: ByteArray): ByteArray {
        val spec = SecretKeySpec(key, KeyProperties.KEY_ALGORITHM_AES)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, spec)
        return cipher.doFinal(data)
    }

    fun encryptSeal(message: String, publicKey: String): String {
        val publicKeyBytes = publicKey.toByteArrayFromBase64String()
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val messageLength = messageBytes.size
        val cipherText = ByteArray(48 + messageLength)

        val provider = Security.getProvider("")
        val cipher = Cipher.getInstance("", provider)

        val privateKey = getKeyStore().getKey(KEYPAIR_NAME, null) as PrivateKey

        cipher.init(Cipher.ENCRYPT_MODE, privateKey)

        val sealedObject = SealedObject(message, cipher)

        return sealedObject.toString()
    }

    fun createSignature(message: String): String {
        val privateKey = getKeyStore().getKey(KEYPAIR_NAME, null) as PrivateKey
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val signature = Signature.getInstance(RSA_SIGNATURE)

        signature.initSign(privateKey)
        signature.update(messageBytes)

        val signatureBytes = signature.sign()

        return signatureBytes.toStringBase64()
    }

    fun verifySignature(signature: String): Boolean {
        val signatureBytes = signature.toByteArrayFromBase64String()
        val messageBytes = ByteArray(signatureBytes.size - 64)
        val sign = Signature.getInstance(RSA_SIGNATURE)
        val pubKey = getKeyStore().getCertificate(KEYPAIR_NAME).publicKey

        sign.initVerify(getKeyStore().getCertificate(KEYPAIR_NAME))
//        sign.initVerify(pubKey)
        sign.update(messageBytes)

        return sign.verify(signatureBytes)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/ECB/PKCS7Padding"
        private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val RSA_SIGNATURE = "SHA256withRSA"

        /**
         * Android keystore name for RSA keypair
         */
        private const val KEYPAIR_NAME = "keyri_ks_v01"
    }
}

