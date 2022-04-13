package com.keyrico.keyrisdk.services

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.keyrico.keyrisdk.utils.toByteArrayFromBase64String
import com.keyrico.keyrisdk.utils.toStringBase64
import java.lang.IllegalStateException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
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
        val encryptedPublicUserId =
            encryptAes(secretKey, publicUserId.encodeToByteArray(), publicUserId)

        Log.e("Saving", "userId: $publicUserId pub: ${keyPair.public.encoded.toStringBase64()} priv: ${keyPair.private.encoded.toStringBase64()}")

        // TODO Uncomment
//        saveEncryptedPublic(
//            encryptedPublicUserId.toStringBase64(),
//            encryptedPublicKey.toStringBase64()
//        )
//        saveEncryptedPrivate(
//            encryptedPublicUserId.toStringBase64(),
//            encryptedPrivateKey.toStringBase64()
//        )

        saveEncryptedPublic(publicUserId, encryptedPublicKey.toStringBase64())
        saveEncryptedPrivate(publicUserId, encryptedPrivateKey.toStringBase64())

        return encryptedPublicKey.toStringBase64()
    }

    fun getAssociationKey(publicUserId: String): String? {
        val encryptedPrivate = getEncryptedPublic(publicUserId)?.encodeToByteArray() ?: return null
        val secretKey = getSecretKey(publicUserId) ?: return null

        return decryptAes(secretKey, encryptedPrivate, publicUserId).toStringBase64()
    }

    fun getIV(publicUserId: String): String? {
        return preferences.getString(IV_KEY_NAME + publicUserId, null)
    }

    fun encryptAes(data: String, publicUserId: String, rpPublicKey: String): String {
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        val publicBytes = Base64.decode(rpPublicKey, Base64.NO_WRAP)
        val publicKey = generateP256PublicKeyFromUncompressedW(publicBytes)

        val encryptedPrivate = getEncryptedPrivate(publicUserId)?.encodeToByteArray()
            ?: throw IllegalStateException("Private key is null")

        val secretKey =
            getSecretKey(publicUserId) ?: throw IllegalStateException("Secret key is null")

        val privateString = decryptAes(secretKey, encryptedPrivate, publicUserId)

        val privateBytes = Base64.decode(privateString, Base64.NO_WRAP)
        val privateKey = generateP256PublicKeyFromUncompressedW(privateBytes)

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        val aesKey = keyAgreement.generateSecret(KeyProperties.KEY_ALGORITHM_AES)

        val dataBytes = data.encodeToByteArray()
        val encrypted = encryptAes(aesKey, dataBytes, publicUserId)

        return encrypted.toStringBase64()
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

    private fun getSecretKey(publicUserId: String): SecretKey? {
        val secretKeyEntry =
            getKeyStore().getEntry(AES_KEY_NAME + publicUserId, null) as? KeyStore.SecretKeyEntry

        return secretKeyEntry?.secretKey
    }

    private fun getEncryptedPublic(publicUserId: String): String? {
        val secretKey = getSecretKey(publicUserId) ?: return null
        val encryptedPublicUserId =
            encryptAes(secretKey, publicUserId.encodeToByteArray(), publicUserId)

        Log.e("Get Enc $KEY_PUBLIC$encryptedPublicUserId", getEncryptedString(KEY_PUBLIC + encryptedPublicUserId) ?: "null")

        return getEncryptedString(KEY_PUBLIC + encryptedPublicUserId)
    }

    private fun getEncryptedPrivate(publicUserId: String): String? {
        val secretKey = getSecretKey(publicUserId) ?: return null

        val encryptedPublicUserId =
            encryptAes(secretKey, publicUserId.encodeToByteArray(), publicUserId)

        Log.e("Get Enc $KEY_PRIVATE$encryptedPublicUserId", getEncryptedString(KEY_PRIVATE + encryptedPublicUserId) ?: "null")

        return getEncryptedString(KEY_PRIVATE + encryptedPublicUserId)
    }

    private fun getEncryptedString(key: String): String? {
        return preferences.getString(key, null)
    }

    private fun saveEncryptedPublic(encryptedUserId: String, data: String) {
        // TODO Remove logs
        Log.e("Save Enc $KEY_PUBLIC$encryptedUserId", data)

        saveEncryptedString(KEY_PUBLIC + encryptedUserId, data)
    }

    private fun saveEncryptedPrivate(encryptedUserId: String, data: String) {
        // TODO Remove logs
        Log.e("Save Enc $KEY_PRIVATE$encryptedUserId", data)

        saveEncryptedString(KEY_PRIVATE + encryptedUserId, data)
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
