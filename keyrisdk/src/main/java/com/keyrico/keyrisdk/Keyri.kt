package com.keyrico.keyrisdk

import androidx.fragment.app.FragmentManager
import com.keyrico.keyrisdk.entity.session.Session
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.ui.confirmation.ConfirmationBottomDialog
import com.keyrico.keyrisdk.utils.makeApiCall
import com.keyrico.keyrisdk.utils.provideApiService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

@Suppress("unused")
class Keyri {

    private val cryptoService by lazy(::CryptoService)

    fun generateAssociationKey(publicUserId: String): String =
        cryptoService.generateAssociationKey(publicUserId)

    fun getUserSignature(publicUserId: String?, data: String): String =
        cryptoService.signMessage(publicUserId, data)

    fun listAssociationKey(): List<String> = cryptoService.listAssociationKey()

    fun getAssociationKey(publicUserId: String?): String =
        cryptoService.getAssociationKey(publicUserId)

    suspend fun initiateQrSession(
        appKey: String,
        sessionId: String,
        publicUserId: String?
    ): Result<Session> {
        return makeApiCall { provideApiService().getSession(sessionId, appKey) }
            .map { it.toSession(publicUserId) }
    }

    suspend fun initializeDefaultScreen(
        fm: FragmentManager,
        session: Session,
        payload: String
    ): Result<Boolean> {
        return callbackFlow {
            var callback: ((Result<Boolean>) -> Unit)? = { trySend(it) }

            ConfirmationBottomDialog(session, payload, callback)
                .show(fm, ConfirmationBottomDialog::class.java.name)

            awaitClose { callback = null }
        }.first()
    }
}
