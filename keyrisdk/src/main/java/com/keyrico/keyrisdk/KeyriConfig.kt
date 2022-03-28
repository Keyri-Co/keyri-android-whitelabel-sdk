package com.keyrico.keyrisdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Keyri SDK configuration
 */
@Parcelize
data class KeyriConfig(
    val publicKey: String,
    val callbackUrl: String,
    val domainName: String,
    val allowMultipleAccounts: Boolean = false
) : Parcelable
