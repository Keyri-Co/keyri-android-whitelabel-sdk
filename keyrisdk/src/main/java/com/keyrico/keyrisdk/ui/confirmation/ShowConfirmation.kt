package com.keyrico.keyrisdk.ui.confirmation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.keyrico.keyrisdk.ui.auth.AuthWithScannerState
import com.keyrico.keyrisdk.ui.confirmation.ConfirmationActivity.Companion.KEY_AUTH_STATE
import com.keyrico.keyrisdk.ui.confirmation.ConfirmationActivity.Companion.KEY_CONFIRMATION_RESULT

internal class ShowConfirmation : ActivityResultContract<AuthWithScannerState.Confirmation, Boolean>() {

    override fun createIntent(context: Context, input: AuthWithScannerState.Confirmation): Intent {
        return Intent(context, ConfirmationActivity::class.java).apply {
            putExtra(KEY_AUTH_STATE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return intent?.takeIf { resultCode == Activity.RESULT_OK }
            ?.getBooleanExtra(KEY_CONFIRMATION_RESULT, false) ?: false
    }
}
