package com.example.keyrisdk.db

import androidx.room.*
import com.example.keyrisdk.entity.Account

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdateAccount(account: Account)

    @Query("SELECT * FROM Accounts WHERE serviceId = :serviceId")
    suspend fun getAccountsByServiceId(serviceId: String): List<Account>

    @Query("SELECT * FROM Accounts")
    suspend fun getAllAccounts(): List<Account>

    @Query("DELETE FROM Accounts WHERE serviceId = :serviceId AND username = :username AND custom =:custom")
    suspend fun removeAccount(serviceId: String, username: String, custom: String?)
}
