package com.keyrico.keyrisdk.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.keyrico.keyrisdk.R

class ConfirmationScreenBottomSheetDialog(
    private val onAccept: (String) -> Unit,
    private val onDecline: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_confirmation_dialog_layout, container, false)

    fun creteItems() {

    }
}
