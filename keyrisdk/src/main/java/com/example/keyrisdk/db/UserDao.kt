package com.example.keyrisdk.db

import androidx.room.*
import com.example.keyrisdk.entity.Account

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addOrUpdateAccount(account: Account)

    @Query("SELECT * FROM Accounts WHERE serviceId = :serviceId")
    fun getAccountsByServiceId(serviceId: String): List<Account>

    @Query("SELECT * FROM Accounts")
    fun getAllAccounts(): List<Account>

    @Query("DELETE FROM Accounts WHERE serviceId = :serviceId AND username = :username AND custom =:custom")
    fun removeAccount(serviceId: String, username: String, custom: String?)
}
