package com.keyrico.keyrisdk.exception

sealed class KeyriSdkException(override val message: String) : Throwable(message = message)

data class NetworkException(override val message: String) : KeyriSdkException(message)

data class AuthorizationException(override val message: String) : KeyriSdkException(message)

data class InternalServerException(override val message: String) : KeyriSdkException(message)
