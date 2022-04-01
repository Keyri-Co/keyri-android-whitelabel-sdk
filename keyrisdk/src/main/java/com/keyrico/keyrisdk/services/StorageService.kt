package com.keyrico.keyrisdk.services

import android.content.SharedPreferences
import androidx.core.content.edit
import com.keyrico.keyrisdk.db.UserDao
import com.keyrico.keyrisdk.entity.Account
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.services.crypto.CryptoService

/**
 * Local storage powered by Room and Preferences.
 * Encrypts sensitive data before storing.
 */
class StorageService(
    private val preferences: SharedPreferences,
    private val userDao: UserDao,
    private val cryptoService: CryptoService
) {

    /**
     * Stores account in local storage with encrypted userId
     */
    suspend fun addAccount(account: Account, publicUserId: String) {
        val encryptedUserId = cryptoService.encryptAes(account.userId, publicUserId)
        val encryptedAccount = account.copy(userId = encryptedUserId)

        userDao.addOrUpdateAccount(encryptedAccount)
    }

    /**
     * Retrieves all accounts for specific serviceId
     */
    suspend fun getAccounts(serviceId: String) =
        userDao
            .getAccountsByServiceId(serviceId)
            .map { it.copy(userId = cryptoService.decryptAes(it.userId)) }

    /**
     * Retrieves all accounts
     */
    suspend fun getAllAccounts() =
        userDao
            .getAllAccounts()
            .map { it.copy(userId = cryptoService.decryptAes(it.userId)) }

    /**
     * Removing passed account
     */
    suspend fun removeAccount(serviceId: String, account: PublicAccount) {
        userDao.removeAccount(serviceId, account.username, account.custom)
    }

    fun getDeviceId() = preferences.getString(KEY_DEVICE_ID, null)

    fun setDeviceId(deviceId: String) {
        preferences.edit(commit = true) {
            putString(KEY_DEVICE_ID, deviceId)
        }
    }

    companion object {
        const val KEY_DEVICE_ID = "key_device_id"
    }
}
