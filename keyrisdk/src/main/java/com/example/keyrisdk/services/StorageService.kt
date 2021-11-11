package com.example.keyrisdk.services

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.example.keyrisdk.db.UserDao
import com.example.keyrisdk.entity.Account
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.services.crypto.CryptoService

/**
 * Local storage powered by Room.
 * Encrypts sensitive data before storing
 */
class StorageService(
    context: Context,
    private val userDao: UserDao,
    private val cryptoService: CryptoService
) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private fun applyString(key: String, value: String) {
        prefs.edit(commit = true) {
            putString(key, value)
        }
    }

    /**
     * Stores account in local storage with encrypted userId
     */
    fun addAccount(account: Account) {
        val encryptedUserId = cryptoService.encryptAes(account.userId)
        val encryptedAccount = account.copy(userId = encryptedUserId)
        userDao.addOrUpdateAccount(encryptedAccount)
    }

    /**
     * Retrieves all accounts for specific serviceId
     */
    fun getAccounts(serviceId: String) =
        userDao
            .getAccountsByServiceId(serviceId)
            .map {
                it.copy(userId = cryptoService.decryptAes(it.userId))
            }

    /**
     * Retrieves all accounts
     */
    fun getAllAccounts() =
        userDao
            .getAllAccounts()
            .map {
                it.copy(userId = cryptoService.decryptAes(it.userId))
            }

    /**
     * Removing passed account
     */
    fun removeAccount(serviceId: String, account: PublicAccount) {
        userDao.removeAccount(serviceId, account.username, account.custom)
    }

    fun getDeviceId() = prefs.getString(KEY_DEVICE_ID, null)

    fun setDeviceId(deviceId: String) {
        applyString(KEY_DEVICE_ID, deviceId)
    }

    companion object {
        const val KEY_DEVICE_ID = "key_device_id"
    }
}
