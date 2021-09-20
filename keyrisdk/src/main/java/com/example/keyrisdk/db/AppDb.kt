package com.example.keyrisdk.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.keyrisdk.entity.Account

@Database(
    entities = [Account::class], version = 1
)
abstract class AppDb : RoomDatabase() {

    abstract fun userDao(): UserDao

}