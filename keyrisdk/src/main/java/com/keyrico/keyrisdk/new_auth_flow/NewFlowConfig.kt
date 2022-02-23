package com.keyrico.keyrisdk.new_auth_flow

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class NewFlowConfig(
    val appKey: String,
    val publicKey: String,
    val domainName: String,
    val allowMultipleAccounts: Boolean = false
) : Parcelable
