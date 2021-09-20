package com.example.keyrisdk.services

import com.example.keyrisdk.db.UserDao
import com.example.keyrisdk.entity.Account
import com.example.keyrisdk.services.crypto.CryptoService

/**
 * Local storage powered by Room.
 * Encrypts sensitive data before storing
 */
class StorageService(
    private val userDao: UserDao,
    private val cryptoService: CryptoService
) {

    /**
     * Stores account in local storage with encrypted userId
     */
    fun addAccount(account: Account) {
        val encryptedUserId = cryptoService.encryptAes(account.userId)
        val encryptedAccount = account.copy(userId = encryptedUserId)
        userDao.addOrUpdateAccount(encryptedAccount)
    }

    /**
     * Retrieves all accounts
     */
    fun getAccounts(serviceId: String) =
        userDao
            .getAccountsByServiceId(serviceId)
            .map {
                it.copy(userId = cryptoService.decryptAes(it.userId))
            }

}