package com.keyrico.keyrisdk.utils

import android.util.Base64

internal fun ByteArray.toStringBase64() = Base64.encode(this, Base64.NO_WRAP).toString()
internal fun String.toByteArrayFromBase64String(): ByteArray =
    Base64.decode(toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
