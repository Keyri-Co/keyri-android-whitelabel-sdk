package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.services.api.data.ApiData
import com.keyrico.keyrisdk.services.api.data.BrowserData
import com.keyrico.keyrisdk.services.api.data.SessionConfirmationRequest
import com.keyrico.keyrisdk.utils.makeApiCall
import com.keyrico.keyrisdk.utils.provideApiService
import kotlinx.parcelize.Parcelize

@Suppress("unused")
@Parcelize
data class Session(
    @SerializedName("WidgetOrigin")
    val widgetOrigin: String,

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("WidgetUserAgent")
    val widgetUserAgent: WidgetUserAgent?,

    @SerializedName("userParameters")
    val userParameters: UserParameters?,

    @SerializedName("IPAddressMobile")
    val iPAddressMobile: String,

    @SerializedName("IPAddressWidget")
    val iPAddressWidget: String,

    @SerializedName("riskAnalytics")
    val riskAnalytics: RiskAnalytics?,

    @SerializedName("browserPublicKey")
    internal val browserPublicKey: String,

    @SerializedName("__salt")
    internal val salt: String,

    @SerializedName("__hash")
    internal val hash: String
) : Parcelable {

    suspend fun confirm(publicUserId: String?, payload: String): Result<Boolean> =
        finishSession(publicUserId, payload)

    suspend fun deny(publicUserId: String?, payload: String): Result<Boolean> =
        finishSession(publicUserId, payload)

    private suspend fun finishSession(publicUserId: String?, payload: String): Result<Boolean> {
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
                errors = false,
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
