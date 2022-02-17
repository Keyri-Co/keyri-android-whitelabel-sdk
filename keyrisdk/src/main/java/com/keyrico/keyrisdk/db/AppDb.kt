package com.keyrico.keyrisdk.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.keyrico.keyrisdk.entity.Account

@Database(entities = [Account::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {

    abstract fun userDao(): UserDao
}
