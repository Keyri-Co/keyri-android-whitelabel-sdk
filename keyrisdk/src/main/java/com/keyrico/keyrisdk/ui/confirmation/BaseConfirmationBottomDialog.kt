package com.keyrico.keyrisdk.ui.confirmation

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.keyrico.keyrisdk.R
import com.keyrico.keyrisdk.entity.Session

abstract class BaseConfirmationBottomDialog(
    protected open val session: Session,
    protected open val onResult: (isAccepted: Boolean) -> Unit
) : BottomSheetDialogFragment() {

    protected var accepted = false

    abstract fun initUI()

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    override fun onDismiss(dialog: DialogInterface) {
        onResult(accepted)
        super.onDismiss(dialog)
    }
}
