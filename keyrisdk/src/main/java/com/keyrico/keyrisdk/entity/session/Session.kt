package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.services.api.data.ApiData
import com.keyrico.keyrisdk.services.api.data.BrowserData
import com.keyrico.keyrisdk.services.api.data.SessionConfirmationRequest
import com.keyrico.keyrisdk.utils.makeApiCall
import com.keyrico.keyrisdk.utils.provideApiService
import kotlinx.parcelize.Parcelize

@Parcelize
data class Session(
    val widgetOrigin: String,
    val sessionId: String,
    val widgetUserAgent: WidgetUserAgent?,
    val userParameters: UserParameters?,
    val iPAddressMobile: String,
    val iPAddressWidget: String,
    val riskAnalytics: RiskAnalytics?,
    private val browserPublicKey: String,
    private val salt: String,
    private val hash: String,
    private val payload: String,
    private val publicUserId: String?
) : Parcelable {

    suspend fun confirm(): Result<Boolean> = finishSession(isConfirmed = true)

    suspend fun deny(): Result<Boolean> = finishSession(isConfirmed = false)

    private suspend fun finishSession(isConfirmed: Boolean): Result<Boolean> {
        return try {
            val cryptoService = CryptoService()

            val cipher = cryptoService.encryptHkdf(browserPublicKey, payload)
            val associationKey = cryptoService.getAssociationKey(publicUserId)

            val apiData = ApiData(publicUserId, associationKey)

            val browserData = BrowserData(
                publicKey = cipher.publicKey,
                ciphertext = cipher.cipherText,
                salt = cipher.salt,
                iv = cipher.iv
            )

            val request = SessionConfirmationRequest(
                salt = salt,
                hash = hash,
                errors = isConfirmed,
                errorMsg = "",
                apiData = apiData,
                browserData = browserData
            )

            val result = makeApiCall { provideApiService().approveSession(sessionId, request) }

            if (result.isSuccess) {
                Result.success(result.isSuccess)
            } else {
                val error =
                    result.exceptionOrNull() ?: AuthorizationException("Unable to authorize")

                Result.failure(error)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
