package com.example.keyrisdk.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Accounts")
data class Account(

    @PrimaryKey
    @SerializedName("userId")
    val userId: String,

    @SerializedName("serviceId")
    val serviceId: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("custom")
    val custom: String?
)
