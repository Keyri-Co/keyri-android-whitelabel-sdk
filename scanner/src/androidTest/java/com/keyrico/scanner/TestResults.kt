package com.keyrico.scanner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TestResults(
    val sessionId: String? = null,
    val sessionRegularDialog: Map<String, Boolean> = emptyMap(),
    val sessionDeniedDialog: Map<String, Boolean> = emptyMap(),
    val sessionWarningDialog: Map<String, Boolean> = emptyMap(),
    val sessionNoIpDataDialog: Map<String, Boolean> = emptyMap(),
    val sessionWithoutRiskPermissionDialog: Map<String, Boolean> = emptyMap()
) : Parcelable
