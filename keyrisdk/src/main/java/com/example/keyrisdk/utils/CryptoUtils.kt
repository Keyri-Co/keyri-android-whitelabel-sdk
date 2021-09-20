package com.example.keyrisdk.utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.charset.Charset
import java.security.*
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

object CryptoUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    @RequiresApi(Build.VERSION_CODES.M)
    fun getInitializedCipherForEncryption(keyName: String): Cipher {
        val cipher = getCipher()

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(keyName)) {
            createKeyPair(keyName)
        }

        val key = keyStore.getCertificate(keyName).publicKey
        val unrestrictedPublicKey: PublicKey =
            KeyFactory.getInstance(key.algorithm).generatePublic(
                X509EncodedKeySpec(key.encoded)
            )
        val spec = OAEPParameterSpec(
            "SHA-256", "MGF1",
            MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, unrestrictedPublicKey, spec)
        return cipher
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getInitializedCipherForDecryption(keyName: String): Cipher {
        val cipher = getCipher()

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(keyName)) {
            createKeyPair(keyName)
        }

        val key = keyStore.getKey(keyName, null) as PrivateKey
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher
    }

    fun encryptData(plaintext: String, cipher: Cipher): String {
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charset.forName("UTF-8")))
        return String(Base64.encode(ciphertext, Base64.NO_WRAP))
    }

    fun decryptData(ciphertext: ByteArray, cipher: Cipher): String {
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charset.forName("UTF-8"))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getCipher() = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")


    @RequiresApi(Build.VERSION_CODES.M)
    private fun createKeyPair(keyName: String) {
        Log.d("Keyri", "Creating key pair")
        val keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        keyGenerator.initialize(
            KeyGenParameterSpec.Builder(
                keyName,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA512
                )
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build()
        )
        keyGenerator.generateKeyPair()
        Log.d("Keyri", "Key pair has been created")
    }

    fun getPrivateKey(keyName: String): PrivateKey {
        Log.d("Keyri", "Getting private key")
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(keyName)) {
            createKeyPair(keyName)
        }

        val key = keyStore.getKey(keyName, null) as PrivateKey
        Log.d("Keyri", "Private key obtained, algorithm = {${key.algorithm}} format = {${key.format}}")
        return key
    }

}
