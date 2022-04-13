package com.keyrico.keyrisdk.exception

import com.keyrico.keyrisdk.R

sealed class KeyriSdkException(open val errorMessage: Int = R.string.keyri_empty) : Throwable()

object NetworkException : KeyriSdkException(R.string.keyri_err_network)

object AuthorizationException : KeyriSdkException(R.string.keyri_err_authorization)

data class InternalServerException(val httpCode: Int? = null) :
    KeyriSdkException(R.string.keyri_err_internal_server)

data class ServerErrorException(val httpCode: Int? = null, val errorResponse: String?) :
    KeyriSdkException()

data class RiskErrorsException(val riskErrors: List<String> = emptyList()) :
    KeyriSdkException(R.string.keyri_err_risk_errors)
