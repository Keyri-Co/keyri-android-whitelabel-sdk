package com.keyrico.keyrisdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import com.keyrico.keyrisdk.ui.auth.AuthWithScannerActivity
import com.keyrico.keyrisdk.ui.auth.AuthWithScannerActivity.Companion.KEY_AUTH_PARAMS
import kotlinx.parcelize.Parcelize

class ShowEasyKeyriAuth : ActivityResultContract<EasyKeyriAuthParams, Boolean>() {

    override fun createIntent(context: Context, input: EasyKeyriAuthParams): Intent {
        return Intent(context, AuthWithScannerActivity::class.java).apply {
            putExtra(KEY_AUTH_PARAMS, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}

@Parcelize
data class EasyKeyriAuthParams(
    val appKey: String,
    val rpPublicKey: String,
    val serviceDomain: String,
    val publicUserId: String,
    val publicCustom: String?,
    val secureCustom: String?
) : Parcelable
