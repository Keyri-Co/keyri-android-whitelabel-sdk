package com.example.keyrisdk.services.crypto

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * Crypto box persistent holder.
 */
class CryptoBoxHolder(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private fun applyString(key: String, value: String) {
        prefs.edit(commit = true) {
            putString(key, value)
        }
    }

    fun getCryptoBox(): CryptoBox? {
        val privateKey = prefs.getString(KEY_SC, null) ?: return null
        val publicKey = prefs.getString(KEY_PK, null) ?: return null
        return CryptoBox(
            privateKey,
            publicKey
        )
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