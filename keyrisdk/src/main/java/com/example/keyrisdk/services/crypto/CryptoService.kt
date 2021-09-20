package com.example.keyrisdk.services.crypto

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.keyrisdk.utils.fromBase64
import com.example.keyrisdk.utils.toByteArrayFromBase64String
import com.example.keyrisdk.utils.toStringBase64
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class CryptoService(context: Context) {

    private val sodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
    private val cryptoBoxHolder =
        CryptoBoxHolder(context)

    init {
        createRsaKeyPairIfNeeded()
        createCryptoBoxIfNeeded()
    }

    /**
     * Creates RSA keypair in android keystore if it doesn't exist
     */
    private fun createRsaKeyPairIfNeeded() {
        val keyStore = getKeyStore()
        if (keyStore.containsAlias(KEYPAIR_NAME)) return

        createRsaKeyPair()
    }

    /**
     * Creates RSA keypair in android keystore
     */
    private fun createRsaKeyPair() {
        val keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        keyGenerator.initialize(
            KeyGenParameterSpec.Builder(
                KEYPAIR_NAME,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA512
                )
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()
        )
        keyGenerator.generateKeyPair()
    }

    private fun getKeyStore() = KeyStore
        .getInstance(ANDROID_KEYSTORE)
        .also { it.load(null) }

    /**
     * Creates keypair for public-key authentication as per X25519 if it doesn't exist
     * and stores it into persistent storage (in encrypted form)
     */
    private fun createCryptoBoxIfNeeded() {
        if (cryptoBoxHolder.getCryptoBox() != null) return

        val cryptoBox = createCryptoBox()
        val encryptedCryptoBox = cryptoBox.copy(
            privateKey = encryptRsa(cryptoBox.privateKey)
        )

        cryptoBoxHolder.setCryptoBox(encryptedCryptoBox)
    }

    /**
     * Creates keypair for public-key authentication as per X25519
     */
    private fun createCryptoBox(): CryptoBox {
        val sodium = SodiumAndroid()
        val lazySodium = LazySodiumAndroid(sodium, StandardCharsets.UTF_8)
        val keyPair = lazySodium.cryptoBoxKeypair()
        val privateKey = keyPair.secretKey.asBytes.toStringBase64()
        val publicKey = keyPair.publicKey.asBytes.toStringBase64()
        return CryptoBox(
            privateKey,
            publicKey
        )
    }

    /**
     *  Extracts and decrypts a crypto box keypair from persistent storage
     */
    private fun getCryptoBox(): CryptoBox {
        val cryptoBox = cryptoBoxHolder.getCryptoBox() ?: throw IllegalStateException()
        return cryptoBox.copy(privateKey = decryptRsa(cryptoBox.privateKey))
    }

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
        val key = getCryptoBox().privateKey
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
        val key = getCryptoBox().privateKey
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

    fun encryptCryptoBoxEasy(message: String, publicKey: String): Pair<String, String> {
        val cryptoBox = getCryptoBox()

        val privateKeyBytes = cryptoBox.privateKey.toByteArrayFromBase64String()
        val publicKeyBytes = publicKey.toByteArrayFromBase64String()

        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val messageLength = messageBytes.size
        val cipherText = ByteArray(Box.MACBYTES + messageLength)
        val nonce = ByteArray(Box.NONCEBYTES)

        sodium.cryptoBoxEasy(cipherText, messageBytes, messageLength.toLong(), nonce, publicKeyBytes, privateKeyBytes)
        val cipherTextBase64 = cipherText.toStringBase64()
        val nonceBase64 = nonce.toStringBase64()

        return Pair(cipherTextBase64, nonceBase64)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/ECB/PKCS7Padding"
        private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

        /**
         * Android keystore name for RSA keypair
         */
        private const val KEYPAIR_NAME = "keyri_ks_v01"
    }

}