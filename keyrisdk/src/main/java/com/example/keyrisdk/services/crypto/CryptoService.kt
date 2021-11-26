package com.example.keyrisdk.services.crypto

import android.annotation.SuppressLint
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import android.util.Log
import com.example.keyrisdk.utils.fromBase64
import com.example.keyrisdk.utils.toByteArrayFromBase64String
import com.example.keyrisdk.utils.toStringBase64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.EllipticCurve
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SealedObject
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CryptoService {

    init {
        createRsaKeyPairIfNeeded()
        createAesKeyIfNeeded() // TODO Will be removed, because ECDH will generate secret
    }

    // TODO Encrypt by RSA and save to Prefs
    private var ivBytes = byteArrayOf()

    /**
     * Creates RSA keypair in android keystore if it doesn't exist
     */
    private fun createRsaKeyPairIfNeeded() {
        val keyStore = getKeyStore()
        if (!keyStore.containsAlias(RSA_KEYPAIR_NAME)) createEncryptionRsaKeyPair()
    }

    private fun createAesKeyIfNeeded() {
        val keyStore = getKeyStore()
//        if (!keyStore.containsAlias(AES_KEY_NAME)) createEncryptionAesSecretKey()
    }

    /**
     * Creates RSA keypair in android keystore
     */
    private fun createEncryptionRsaKeyPair() {
        val keyGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")

        val parameterSpec = KeyGenParameterSpec.Builder(
            RSA_KEYPAIR_NAME,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT or
                    KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(1024)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()

        keyGenerator.initialize(parameterSpec)
        keyGenerator.generateKeyPair()
    }

    private fun createEncryptionAesSecretKey() {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            AES_KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            // TODO Add Key size
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getKeyStore() = KeyStore
        .getInstance(ANDROID_KEYSTORE)
        .also { it.load(null) }

    fun initECDH() {
        val myPublic =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAElbh2dI2lnDQdFIzBmvkqyNM+louqRIyy1eajG9H6l01AevozvnqRgqakXEeykXe/adqutu+PsHXWjlCCTRMJMQ=="
        val private =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgCzAvcqq6gANPNTm37ryjM//uf4LUrUT848N6M3JXiaahRANCAASVuHZ0jaWcNB0UjMGa+SrI0z6Wi6pEjLLV5qMb0fqXTUB6+jO+epGCpqRcR7KRd79p2q6274+wddaOUIJNEwkx"

        val public =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEoufIbhsshUSEcHNSD6I1PEJYLzgg6sorbt1UymXuBjH252xej0QPy9Yc34TdWc7PDmttSMtC+hwwXbcoiwKCBQ=="

        val publicBytes: ByteArray = Base64.decode(public, Base64.NO_WRAP)
        val pubKeySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val publicKey = keyFactory.generatePublic(pubKeySpec)

        val privateBytes: ByteArray = Base64.decode(private, Base64.NO_WRAP)
        val privKeySpec = PKCS8EncodedKeySpec(privateBytes)
        val privKeyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val privateKey = privKeyFactory.generatePrivate(privKeySpec)

        initECDH(publicKey, privateKey)
    }

    private fun initECDH(publicKey: PublicKey, privateKey: PrivateKey) {
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        val secretKey = keyAgreement.generateSecret(KeyProperties.KEY_ALGORITHM_AES)
        val keyStoreEntry = KeyStore.SecretKeyEntry(secretKey)
        val keyProtection =
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT) // TODO Check for needed Purposes
                .build()

        // TODO Here Need To Fix Error
        getKeyStore().setEntry(AES_KEY_NAME, keyStoreEntry, keyProtection)

        Log.e("SECRET KEY", secretKey.encoded.toStringBase64())
    }

    fun verifyECDH() {

        // TODO Test it

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val eCGenParameterSpec = ECGenParameterSpec("prime256v1")

        kpg.initialize(eCGenParameterSpec)

        kpg.genKeyPair()

        val keyPair = kpg.generateKeyPair()
        val keyAgreement = KeyAgreement.getInstance("ECDH")

//        keyAgreement.init(keyPair.private)
//        keyAgreement.doPhase(keyPair.public, true)

        val kpg2 = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val eCGenParameterSpec2 = ECGenParameterSpec("prime256v1")

        kpg2.initialize(eCGenParameterSpec2)

        val keyPair2 = kpg.generateKeyPair()
        val keyAgreement2 = KeyAgreement.getInstance("ECDH")

        keyAgreement2.init(keyPair2.private)
        keyAgreement2.doPhase(keyPair.public, true)

        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(keyPair2.public, true)

        val secret = keyAgreement.generateSecret(KeyProperties.KEY_ALGORITHM_AES)

        // TODO Need to understand how to store secret key
        val secret2 = keyAgreement2.generateSecret(KeyProperties.KEY_ALGORITHM_AES)

        Log.e("PUBLIC", keyPair.public.encoded.toStringBase64())
        Log.e("PUBLIC 2", keyPair2.public.encoded.toStringBase64())
        Log.e("PRIVATE", keyPair.private.encoded.toStringBase64())
        Log.e("PRIVATE 2", keyPair2.private.encoded.toStringBase64())
        Log.e("SECRET", secret.encoded.toStringBase64())
        Log.e("SECRET 2", secret2.encoded.toStringBase64())
    }

    /**
     * Encrypts a string using RSA asymmetric encryption.
     * @data string to encrypt
     *
     * @return encrypted string encoded in Base64
     */
    fun encryptRsa(data: String): String {
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
//        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
//        val publicKey = getKeyStore().getCertificate(RSA_KEYPAIR_NAME).publicKey
//        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
//
//        return cipher.doFinal(data)

        // TODO Remove code below
        val pk =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC9YtFKhZ2cqPIx6slBeneDAn+I9+zKCrH3+MpqjyIExyt3SB3WUxnjJE1Gvl0m46RWBo1W6VysYNUYPRAS/lBQteUsGjdRXmm0QsfFk8sdHIIimecU9ETceIKXNpqGlX85z9r8cl04937mQP1Dez3sKk5Ig3H0O4nsk1Ae0QV/VwIDAQAB"
//        val publicKey = getKeyStore().getCertificate(RSA_KEYPAIR_NAME).publicKey
//        val keyText = publicKey.encoded.toStringBase64()
//
//        Log.e("KEY TEXT", "key part: $keyText")

        val publicBytes: ByteArray = Base64.decode(pk, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val pubKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)

        cipher.init(Cipher.ENCRYPT_MODE, pubKey)

        val WD = cipher.doFinal(data)

        Log.e("ENCRYPTED DATA DENIS KEY", WD.toStringBase64())

        val publicKey = getKeyStore().getCertificate(RSA_KEYPAIR_NAME).publicKey

        val cipher2 = Cipher.getInstance(RSA_TRANSFORMATION)

        cipher2.init(Cipher.ENCRYPT_MODE, publicKey)

        val WD2 = cipher2.doFinal()

        val keyText = publicKey.encoded.toStringBase64()

        Log.e("ENCRYPTED DATA MU KEY", WD2.toStringBase64())
        Log.e("MU PUB KEY", keyText)

        return WD
    }

    /**
     * Decrypts a string using RSA asymmetric encryption.
     * @data string to decrypt (encoded in Base64)
     *
     * @return decrypted string
     */
    fun decryptRsa(data: String): String {
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
        val privateKey = getKeyStore().getKey(RSA_KEYPAIR_NAME, null) as PrivateKey
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
        val secretKeyEntry = getKeyStore().getEntry(AES_KEY_NAME, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey
        return encryptAes(secretKey, data)
    }

    /**
     * Encrypts a string using AES symmetric encryption.
     * @key secret key encoded in Base64
     * @data string to encrypt
     *
     * @return encrypted string converted into Base64
     */
    private fun encryptAes(secretKey: SecretKey, data: String): String {
        var temp = data

        while (temp.toByteArray().size % 16 != 0) {
            temp += "\u0020"
        }

        val encrypted = encryptAes(secretKey, temp.toByteArray())
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
    private fun encryptAes(secretKey: SecretKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        ivBytes = cipher.iv

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
        val secretKeyEntry = getKeyStore().getEntry(AES_KEY_NAME, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey
        return decryptAes(secretKey, data)
    }

    /**
     * Decrypts a string using AES symmetric encryption.
     * @key secret key encoded in Base64
     * @data string to decrypt encoded in Base64
     *
     * @return decrypted string
     */
    private fun decryptAes(key: SecretKey, data: String): String {
        val enc = data.toByteArray().fromBase64()
        val decrypted = decryptAes(key, enc)
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
    private fun decryptAes(key: SecretKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = IvParameterSpec(ivBytes)

        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(data)
    }

    fun getPublicKey(): String {
        return getKeyStore().getCertificate(RSA_SIGNATURE).publicKey.encoded.toStringBase64()
    }

    // TODO Add impl (need provider) OR use Mac instead :)
    fun encryptSeal(message: String, publicKey: String): String {
//        val publicKeyBytes = publicKey.toByteArrayFromBase64String()
//        val messageBytes = message.toByteArray(Charsets.UTF_8)
//        val messageLength = messageBytes.size
//        val cipherText = ByteArray(Box.SEALBYTES + messageLength)
//        sodium.cryptoBoxSeal(cipherText, messageBytes, messageLength.toLong(), publicKeyBytes)
//        return cipherText.toStringBase64()

        val publicKeyBytes = publicKey.toByteArrayFromBase64String()
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val messageLength = messageBytes.size
        val cipherText = ByteArray(48 + messageLength)

        val pubKeySpec = X509EncodedKeySpec(publicKeyBytes)

        val provider = Security.getProvider("")
        val cipher = Cipher.getInstance("", provider)

        val privateKey = getKeyStore().getKey(RSA_KEYPAIR_NAME, null) as PrivateKey

        cipher.init(Cipher.ENCRYPT_MODE, privateKey)

        val sealedObject = SealedObject(message, cipher)

        return sealedObject.toString()
    }

    fun createSignature(message: String): String {
        val privateKey = getKeyStore().getKey(RSA_KEYPAIR_NAME, null) as PrivateKey
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val signature = Signature.getInstance(RSA_SIGNATURE)

        signature.initSign(privateKey)
        signature.update(messageBytes)

        val signatureBytes = signature.sign()

        return signatureBytes.toStringBase64()
    }

    // Fun for testing createSignature result
    fun verifySignature(signature: String): Boolean {
        val signatureBytes = signature.toByteArrayFromBase64String()
        val sign = Signature.getInstance(RSA_SIGNATURE)
        val pubKey = getKeyStore().getCertificate(RSA_KEYPAIR_NAME).publicKey

        sign.initVerify(pubKey)

        // Pass original message here
//        sign.update("Message".toByteArray(Charsets.UTF_8))

        return sign.verify(signatureBytes)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/CBC/NoPadding"
        private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val RSA_SIGNATURE = "SHA256withRSA"

        /**
         * Android keystore name for RSA keypair
         */
        private const val RSA_KEYPAIR_NAME = "keyri_ks_v01"
        private const val AES_KEY_NAME = "keyri_ks_v02"
    }
}
