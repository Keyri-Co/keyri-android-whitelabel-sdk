package com.keyrico.keyrisdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Keyri SDK configuration
 */
@Parcelize
data class KeyriConfig(
    val appKey: String,
    val publicKey: String,
    val callbackUrl: String,
    val allowMultipleAccounts: Boolean = false,
    val isDebug: Boolean = false
) : Parcelable
