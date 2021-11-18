package com.example.keyrisdk.services.crypto

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Crypto box persistent holder.
 */
class CryptoBoxHolder(private val preferences: SharedPreferences) {

    private fun applyString(key: String, value: String) {
        preferences.edit(commit = true) {
            putString(key, value)
        }
    }

    fun getCryptoBox(): CryptoBox? {
        val privateKey = preferences.getString(KEY_SC, null) ?: return null
        val publicKey = preferences.getString(KEY_PK, null) ?: return null

        return CryptoBox(privateKey, publicKey)
    }

    fun setCryptoBox(cryptoBox: CryptoBox) {
        applyString(KEY_SC, cryptoBox.privateKey)
        applyString(KEY_PK, cryptoBox.publicKey)
    }

    companion object {
        const val KEY_SC = "key_sc"
        const val KEY_PK = "key_pk"
    }
}
