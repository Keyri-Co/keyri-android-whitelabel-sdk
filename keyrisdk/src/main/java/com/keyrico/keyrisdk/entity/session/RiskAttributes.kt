package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RiskAttributes(
    val distance: Long?,
    val isDifferentCountry: Boolean?,
    val isKnownAbuser: Boolean?,
    val isIcloudRelay: Boolean?,
    val isKnownAttacker: Boolean?,
    val isAnonymous: Boolean?,
    val isThreat: Boolean?,
    val isBogon: Boolean?,
    val blocklists: Boolean?,
    val isDatacenter: Boolean?,
    val isTor: Boolean?,
    val isProxy: Boolean?
) : Parcelable
