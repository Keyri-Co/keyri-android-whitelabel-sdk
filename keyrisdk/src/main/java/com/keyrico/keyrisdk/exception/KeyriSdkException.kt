package com.keyrico.keyrisdk.exception

import com.keyrico.keyrisdk.R

sealed class KeyriSdkException(open val errorMessage: Int = R.string.keyri_empty) : Throwable()

object NotInitializedException : KeyriSdkException(R.string.keyri_err_not_initialized)

object NetworkException : KeyriSdkException(R.string.keyri_err_network)

object AuthorizationException : KeyriSdkException(R.string.keyri_err_authorization)

data class InternalServerException(val httpCode: Int? = null) :
    KeyriSdkException(R.string.keyri_err_internal_server)

data class ServerErrorException(val httpCode: Int? = null, val errorResponse: String?) :
    KeyriSdkException()

object WrongConfigException : KeyriSdkException(R.string.keyri_err_wrong_config)

object AccountNotFoundException : KeyriSdkException(R.string.keyri_err_account_not_found)

object MultipleAccountsNotAllowedException :
    KeyriSdkException(R.string.keyri_multiple_accounts_not_allowed)

object CameraPermissionNotGrantedException : KeyriSdkException(R.string.keyri_camera_not_granted)

object KeyriScannerViewInitializationException :
    KeyriSdkException(R.string.keyri_view_initialization_error)

data class RiskErrorsException(val riskErrors: List<String> = emptyList()) : KeyriSdkException(R.string.keyri_err_risk_errors)
