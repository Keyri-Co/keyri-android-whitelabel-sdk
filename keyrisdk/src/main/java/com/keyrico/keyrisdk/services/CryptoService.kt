package com.keyrico.keyrisdk.services

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.google.crypto.tink.aead.subtle.AesGcmSiv
import com.google.crypto.tink.subtle.Hkdf
import com.google.gson.JsonObject
import com.keyrico.keyrisdk.utils.toByteArrayFromBase64String
import com.keyrico.keyrisdk.utils.toStringBase64
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

internal class CryptoService(private val preferences: SharedPreferences) {

    fun generateAssociationKey(publicUserId: String): String {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        kpg.initialize(ECGenParameterSpec("prime256v1"))

        val keyPair = kpg.generateKeyPair()

        val keyGenerator: KeyGenerator = KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            AES_KEY_NAME + publicUserId,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setRandomizedEncryptionRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)

        val secretKey = keyGenerator.generateKey()

        val encryptedPublicKey = encryptAes(secretKey, keyPair.public.encoded, publicUserId)
        val encryptedPrivateKey = encryptAes(secretKey, keyPair.private.encoded, publicUserId)

        saveEncryptedPublic(publicUserId, encryptedPublicKey)
        saveEncryptedPrivate(publicUserId, encryptedPrivateKey)

        return encryptedPublicKey.toStringBase64()
    }

    fun getAssociationKey(publicUserId: String): String? {
        val encryptedPublic =
            getEncryptedPublic(publicUserId)?.toByteArrayFromBase64String() ?: return null
        val secretKey = getSecretKey(publicUserId) ?: return null

        return decryptAes(secretKey, encryptedPublic, publicUserId).toStringBase64()
    }

    fun createSignature(publicUserId: String): String {
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder("EC_KEYPAIR", KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("prime256v1"))
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512
                ).build()

        keyPairGenerator.initialize(keyGenParameterSpec)

        val keyPair = keyPairGenerator.generateKeyPair()
        val signature = Signature.getInstance("SHA256withECDSA")

        val message = JsonObject().also {
            it.addProperty("publicUserId", publicUserId)
            it.addProperty("timestamp", System.currentTimeMillis())
        }

        signature.initSign(keyPair.private)
        signature.update(message.toString().encodeToByteArray())

        return signature.sign().toStringBase64()
    }

    fun hkdf(
        ciphertext: String,
        salt: ByteArray,
        secretKey: SecretKey,
        publicKey: ECPublicKey,
        backendPublicKey: ECPublicKey
    ): String {
        val info = ByteArrayOutputStream()

        info.write("ECDH prime256v1 AES-256-GCM-SIV\u0000".encodeToByteArray())
        info.write(publicKey.encoded)
        info.write(backendPublicKey.encoded)

        val hkdf = Hkdf.computeHkdf("HMACSHA256", secretKey.encoded, salt, info.toByteArray(), 32)
        val key = AesGcmSiv(hkdf)
        val associatedData = byteArrayOf()

        return key.decrypt(ciphertext.encodeToByteArray(), associatedData).toStringBase64()
    }

    fun getIV(publicUserId: String): String? {
        return preferences.getString(IV_KEY_NAME + publicUserId, null)
    }

    fun encryptAes(data: String, publicUserId: String, rpPublicKey: String): String {
        val aesKey = createSessionSecretKey(publicUserId, rpPublicKey)

        val dataBytes = data.encodeToByteArray()
        val encrypted = encryptAes(aesKey, dataBytes, publicUserId)

        return encrypted.toStringBase64()
    }

    private fun createSessionSecretKey(publicUserId: String, rpPublicKey: String): SecretKey {
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        val publicBytes = Base64.decode(rpPublicKey, Base64.NO_WRAP)
        val publicKey = generateP256PublicKeyFromUncompressedW(publicBytes)

        val encryptedPrivate = getEncryptedPrivate(publicUserId)?.toByteArrayFromBase64String()
            ?: throw IllegalStateException("Private key is null")

        val secretKey =
            getSecretKey(publicUserId) ?: throw IllegalStateException("Secret key is null")

        val privateString = decryptAes(secretKey, encryptedPrivate, publicUserId).toStringBase64()
        val privateBytes = Base64.decode(privateString, Base64.NO_WRAP)

        val privateKey = object : PrivateKey {
            override fun getAlgorithm() = "EC"

            override fun getFormat() = "PKCS#8"

            override fun getEncoded(): ByteArray = privateBytes
        }

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        return keyAgreement.generateSecret(KeyProperties.KEY_ALGORITHM_AES)
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
        val iv = getIV(publicUserId)?.toByteArrayFromBase64String()
            ?: throw IllegalStateException("IV is null")
        val ivParameterSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return cipher.doFinal(data)
    }

    private fun getSecretKey(publicUserId: String): SecretKey? {
        val secretKeyEntry =
            getKeyStore().getEntry(AES_KEY_NAME + publicUserId, null) as? KeyStore.SecretKeyEntry

        return secretKeyEntry?.secretKey
    }

    private fun getEncryptedPublic(publicUserId: String): String? {
        return getEncryptedString(KEY_PUBLIC + publicUserId)
    }

    private fun getEncryptedPrivate(publicUserId: String): String? {
        return getEncryptedString(KEY_PRIVATE + publicUserId)
    }

    private fun getEncryptedString(key: String): String? {
        return preferences.getString(key, null)
    }

    private fun saveEncryptedPublic(publicUserId: String, data: ByteArray) {
        saveEncryptedString(KEY_PUBLIC + publicUserId, data.toStringBase64())
    }

    private fun saveEncryptedPrivate(publicUserId: String, data: ByteArray) {
        saveEncryptedString(KEY_PRIVATE + publicUserId, data.toStringBase64())
    }

    private fun saveIV(iv: ByteArray, publicUserId: String) {
        saveEncryptedString(IV_KEY_NAME + publicUserId, iv.toStringBase64())
    }

    private fun saveEncryptedString(key: String, value: String) {
        preferences.edit(true) { putString(key, value) }
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
        private const val IV_KEY_NAME = "IV_KEY_NAME"
        private const val KEY_PUBLIC = "public"
        private const val KEY_PRIVATE = "private"
    }
}
