package com.keyrico.keyrisdk.services

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.Random
import com.keyrico.keyrisdk.utils.toByteArrayFromBase64String
import com.keyrico.keyrisdk.utils.toStringBase64
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

internal class CryptoService {

    init {
        createAnonymousECDSAKeypair()
    }

    fun generateAssociationKey(publicUserId: String): String {
        return createECDSAKeypair(ECDSA_KEYPAIR + publicUserId)
    }

    fun listAssociationKey(): List<String> = getKeyStore().aliases().toList()
        .filter { it.contains(ECDSA_KEYPAIR) && it != ECDSA_KEYPAIR }

    fun getAssociationKey(publicUserId: String?): String {
        val alias = publicUserId?.let { ECDSA_KEYPAIR + it } ?: ECDSA_KEYPAIR
        val certificate = getKeyStore().getCertificate(alias)

        return certificate.publicKey.encoded.toStringBase64()
    }

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

        val keyPair = kpg.generateKeyPair()
        val keyAgreement = KeyAgreement.getInstance("ECDH")

        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(publicKey, true)

        val secretKeyBytes = keyAgreement.generateSecret()
        val secretKey = SecretKeySpec(secretKeyBytes, KeyProperties.KEY_ALGORITHM_AES)

        return computeHkdf(data, keyPair.public.encoded, secretKey)
    }

    fun signMessage(publicUserId: String?, message: String): String {
        val alias = publicUserId?.let { ECDSA_KEYPAIR + it } ?: ECDSA_KEYPAIR
        val privateKeyEntry = getKeyStore().getEntry(alias, null) as KeyStore.PrivateKeyEntry
        val privateKey = privateKeyEntry.privateKey

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

        signature.initSign(privateKey)
        signature.update(message.encodeToByteArray())

        return signature.sign().toStringBase64()
    }

    private fun createECDSAKeypair(alias: String): String {
        if (getKeyStore().containsAlias(alias)) {
            val certificate = getKeyStore().getCertificate(alias)

            return certificate.publicKey.encoded.toStringBase64()
        }

        val keyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)

        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512
                ).build()

        keyPairGenerator.initialize(keyGenParameterSpec)

        return keyPairGenerator.generateKeyPair().public.encoded.toStringBase64()
    }

    private fun createAnonymousECDSAKeypair(): String = createECDSAKeypair(ECDSA_KEYPAIR)

    private fun computeHkdf(
        data: String,
        publicKeyBytes: ByteArray,
        secretKey: SecretKey
    ): EncryptionOutput {
        val salt = Random.randBytes(SALT_SIZE)
        val finalKeyBytes =
            Hkdf.computeHkdf(MAC_ALGORITHM, secretKey.encoded, salt, byteArrayOf(), KEY_SIZE)
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
        val cipherText = cipher.doFinal(message)

        return EncryptionOutput(
            publicKeyBytes.toStringBase64(),
            cipherText.toStringBase64(),
            salt.toStringBase64(),
            iv.toStringBase64()
        )
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

    class EncryptionOutput(
        val publicKey: String,
        val cipherText: String,
        val salt: String,
        val iv: String
    )

    companion object {
        private const val HEAD_256 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val MAC_ALGORITHM = "HMACSHA256"
        private const val ECDSA_KEYPAIR = "ECDSA_KEYPAIR"
        private const val EC_CURVE = "prime256v1"

        private const val SALT_SIZE = 12
        private const val KEY_SIZE = 32
    }
}
