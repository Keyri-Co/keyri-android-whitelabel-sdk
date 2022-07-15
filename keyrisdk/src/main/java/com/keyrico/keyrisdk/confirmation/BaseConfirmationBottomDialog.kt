package com.keyrico.keyrisdk.confirmation

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.keyrico.keyrisdk.R
import com.keyrico.keyrisdk.entity.session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseConfirmationBottomDialog(
    protected open val session: Session,
    protected open val payload: String,
    protected open val onResult: ((Result<Boolean>) -> Unit)?
) : BottomSheetDialogFragment() {

    protected var isAccepted = false

    abstract fun initUI()

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.lifecycleScope?.launch(Dispatchers.IO) {
            val result = if (isAccepted) {
                session.confirm(payload)
            } else {
                session.deny(payload)
            }

            onResult?.invoke(result)
        }
        super.onDismiss(dialog)
    }
}
