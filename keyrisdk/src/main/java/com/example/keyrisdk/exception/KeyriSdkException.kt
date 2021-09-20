package com.example.keyrisdk.exception

import com.example.keyrisdk.R

sealed class KeyriSdkException(
    open val errorMessage: Int = R.string.keyri_empty
) : Throwable()

object NotInitializedException : KeyriSdkException(R.string.keyri_err_not_initialized)

object NetworkException : KeyriSdkException(R.string.keyri_err_network)

object ServerUnreachableException : KeyriSdkException(R.string.keyri_err_server_unreachable)

data class InternalServerException(val httpCode: Int? = null) :
    KeyriSdkException(R.string.keyri_err_internal_server)

data class ServerErrorException(val httpCode: Int? = null, val errorResponse: String?) :
    KeyriSdkException()

object WrongConfigException : KeyriSdkException(R.string.keyri_err_wrong_config)

object AccountNotFoundException : KeyriSdkException(R.string.keyri_err_account_not_found)
