package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
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

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("WidgetUserAgent")
    val widgetUserAgent: String?,

    @SerializedName("action")
    val action: String,

    @SerializedName("IPDataMobile")
    val iPDataMobile: IPData?,

    @SerializedName("IPDataWidget")
    val iPDataWidget: IPData?,

    @SerializedName("riskAnalytics")
    val riskAnalytics: RiskAnalytics?,

    @SerializedName("browserPublicKey")
    internal val browserPublicKey: String,

    @SerializedName("__salt")
    internal val salt: String,

    @SerializedName("__hash")
    internal val hash: String
) : Parcelable {

    suspend fun confirm(publicUserId: String?, payload: String) =
        finishSession(publicUserId, payload)

    suspend fun deny(publicUserId: String?, payload: String) =
        finishSession(publicUserId, payload)

    private suspend fun finishSession(publicUserId: String?, payload: String) {
        val cryptoService = CryptoService()

        val cipher = cryptoService.encryptHkdf(browserPublicKey, payload)
        val associationKey = cryptoService.getAssociationKey(publicUserId)

        val apiData = ApiData(publicUserId, associationKey)

        val browserData = BrowserData(
            publicKey = cipher.publicKey,
            cipherText = cipher.cipherText,
            salt = cipher.salt,
            iv = cipher.iv
        )

        val request = SessionConfirmationRequest(
            salt = salt,
            hash = hash,
            error = false,
            errorMsg = "",
            apiData = apiData,
            browserData = browserData
        )

        makeApiCall { provideApiService().approveSession(sessionId, request) }.body()
    }
}
