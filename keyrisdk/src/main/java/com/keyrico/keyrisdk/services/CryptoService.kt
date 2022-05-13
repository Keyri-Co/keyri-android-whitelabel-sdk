package com.keyrico.keyrisdk.services

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.Random
import com.google.gson.JsonObject
import com.keyrico.keyrisdk.utils.toByteArrayFromBase64String
import com.keyrico.keyrisdk.utils.toStringBase64
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class CryptoService(private val preferences: SharedPreferences) {

    fun generateAssociationKey(publicUserId: String): String {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        kpg.initialize(ECGenParameterSpec(EC_CURVE))

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


    // HKDF Start
    // ---------
    fun encryptHkdf(backendPublicKey: String, data: String): EncryptionOutput {
        val publicBytes = backendPublicKey.toByteArrayFromBase64String()

        val publicKey = publicBytes.takeIf { it.size <= 65 }?.let {
            generateP256PublicKeyFromUncompressedW(it)
        } ?: let {
            val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            keyFactory.generatePublic(X509EncodedKeySpec(publicBytes)) as ECPublicKey
        }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        kpg.initialize(ECGenParameterSpec(EC_CURVE))

        val keyAgreement = KeyAgreement.getInstance("ECDH")

        val keyPair = kpg.generateKeyPair()

        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(publicKey, true)

        val secretKeyBytes = keyAgreement.generateSecret()
        val secretKey = SecretKeySpec(secretKeyBytes, KeyProperties.KEY_ALGORITHM_AES)

        return encryptHkdf(data, keyPair.public.encoded, secretKey)
    }

    private fun encryptHkdf(
        data: String,
        publicKeyBytes: ByteArray,
        secretKey: SecretKey
    ): EncryptionOutput {
        val salt = Random.randBytes(IV_SIZE)
        val keyBytes = secretKey.encoded
        val finalKeyBytes = Hkdf.computeHkdf(MAC_ALGORITHM, keyBytes, salt, byteArrayOf(), KEY_SIZE)
        val finalKey = SecretKeySpec(finalKeyBytes, KeyProperties.KEY_ALGORITHM_AES)

        return encrypt(data.encodeToByteArray(), finalKey, publicKeyBytes, salt)
    }

    private fun encrypt(
        message: ByteArray,
        key: SecretKey,
        publicKeyBytes: ByteArray,
        salt: ByteArray
    ): EncryptionOutput {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)

        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv.copyOf()
        val result = cipher.doFinal(message)

        return EncryptionOutput(
            publicKeyBytes.toStringBase64(),
            result.toStringBase64(),
            salt.toStringBase64(),
            iv.toStringBase64()
        )
    }

    class EncryptionOutput(
        val publicKey: String,
        val ciphertext: String,
        val salt: String,
        val iv: String
    )
    // ---------
    // HKDF End


    // ECDSA Start
    // ---------
    fun signMessage(message: String, privateKey: ECPrivateKey): String {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

        signature.initSign(privateKey)
        signature.update(message.toByteArray())

        return signature.sign().toStringBase64()
    }

    fun verifyMessage(message: String, signed: String, publicKey: ECPublicKey): Boolean {
        val verify = Signature.getInstance(SIGNATURE_ALGORITHM)

        verify.initVerify(publicKey)
        verify.update(message.toByteArray())

        return verify.verify(signed.toByteArray())
    }
    // ---------
    // ECDSA End


    fun createSignature(publicUserId: String): String {
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder("EC_KEYPAIR", KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512
                ).build()

        keyPairGenerator.initialize(keyGenParameterSpec)

        val keyPair = keyPairGenerator.generateKeyPair()
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

        val message = JsonObject().also {
            it.addProperty("publicUserId", publicUserId)
            it.addProperty("timestamp", System.currentTimeMillis())
        }

        signature.initSign(keyPair.private)
        signature.update(message.toString().encodeToByteArray())

        return signature.sign().toStringBase64()
    }
    // ---------
    // ECDSA End


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

        val publicKey = publicBytes.takeIf { it.size <= 65 }?.let {
            generateP256PublicKeyFromUncompressedW(it)
        } ?: let {
            val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            keyFactory.generatePublic(X509EncodedKeySpec(publicBytes)) as ECPublicKey
        }

        val encryptedPrivate = getEncryptedPrivate(publicUserId)?.toByteArrayFromBase64String()
            ?: throw IllegalStateException("Private key is null")

        val secretKey =
            getSecretKey(publicUserId) ?: throw IllegalStateException("Secret key is null")

        val privateString = decryptAes(secretKey, encryptedPrivate, publicUserId).toStringBase64()
        val privateBytes = Base64.decode(privateString, Base64.NO_WRAP)

        val privateKey = object : PrivateKey {
            override fun getAlgorithm() = KeyProperties.KEY_ALGORITHM_EC

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
            return rawToEncodedECPublicKey(w)
        }

        return generateP256PublicKeyFromFlatW(w.copyOfRange(1, w.size))
    }

    private fun rawToEncodedECPublicKey(rawBytes: ByteArray): ECPublicKey {
        val kf = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val x = rawBytes.copyOfRange(0, rawBytes.size / 2)
        val y = rawBytes.copyOfRange(rawBytes.size / 2, rawBytes.size)
        val w = ECPoint(BigInteger(1, x), BigInteger(1, y))

        val keySpec = ECPublicKeySpec(w, ecParameterSpecForCurve())

        return kf.generatePublic(keySpec) as ECPublicKey
    }

    private fun ecParameterSpecForCurve(): ECParameterSpec {
        val params = AlgorithmParameters.getInstance(KeyProperties.KEY_ALGORITHM_EC)

        params.init(ECGenParameterSpec(EC_CURVE))

        return params.getParameterSpec(ECParameterSpec::class.java)
    }

    companion object {
        private const val HEAD_256 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE"
        private const val AES_TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val MAC_ALGORITHM = "HMACSHA256"
        private const val AES_KEY_NAME = "AES_KEY_NAME"
        private const val IV_KEY_NAME = "IV_KEY_NAME"
        private const val KEY_PUBLIC = "public"
        private const val KEY_PRIVATE = "private"
        private const val EC_CURVE = "prime256v1"

        private const val IV_SIZE = 12
        private const val KEY_SIZE = 32
    }
}
