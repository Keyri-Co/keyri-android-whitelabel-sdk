package com.example.keyrisdk

/**
 * Keyri SDK configuration
 */
data class KeyriConfig(
    val appKey: String,
    val publicKey: String?,
    val callbackUrl: String,
    val allowMultipleAccounts: Boolean = false
)
