package com.keyrico.keyrisdk.entity.session.service

import com.google.gson.annotations.SerializedName

data class OriginalDomain(

    @SerializedName("domainName")
    val domainName: String?,

    @SerializedName("verifiedRecord")
    val verifiedRecord: String?,

    @SerializedName("isDomainApproved")
    val isDomainApproved: Boolean
)
