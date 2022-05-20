package com.keyrico.keyrisdk

import androidx.fragment.app.FragmentManager
import com.google.gson.JsonObject
import com.keyrico.keyrisdk.entity.Session
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.ui.confirmation.ConfirmationBottomDialog
import com.keyrico.keyrisdk.utils.makeApiCall
import com.keyrico.keyrisdk.utils.provideApiService
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

@Suppress("unused")
class KeyriSdk {

    private val cryptoService by lazy(::CryptoService)

    fun generateAssociationKey(publicUserId: String): String =
        cryptoService.generateAssociationKey(publicUserId)

    fun getUserSignature(publicUserId: String?, customSignedData: String?): String {
        val messageForSign = JsonObject().also { jsonObject ->
            customSignedData?.let { jsonObject.addProperty("customSignedData", it) }
            jsonObject.addProperty("timestamp", System.currentTimeMillis())
        }.toString()

        return cryptoService.signMessage(publicUserId, messageForSign)
    }

    fun listAssociationKey(): List<String> = cryptoService.listAssociationKey()

    fun getAssociationKey(publicUserId: String): String =
        cryptoService.getAssociationKey(publicUserId)

    suspend fun initiateQrSession(sessionId: String, appKey: String): Session {
        return makeApiCall { provideApiService().getSession(sessionId, appKey) }.getOrThrow()
    }

    suspend fun initializeDefaultScreen(fm: FragmentManager, session: Session): Boolean {
        return callbackFlow {
            ConfirmationBottomDialog(session) { trySend(it) }
                .show(fm, ConfirmationBottomDialog::class.java.name)
        }.first()
    }
}
