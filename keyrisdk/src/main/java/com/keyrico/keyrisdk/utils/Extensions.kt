package com.keyrico.keyrisdk.utils

import android.util.Base64

fun ByteArray.toStringBase64() = String(toBase64())
fun String.toByteArrayFromBase64String(): ByteArray = this.toByteArray(Charsets.UTF_8).fromBase64()

fun ByteArray.toBase64(): ByteArray = Base64.encode(this, Base64.NO_WRAP)
fun ByteArray.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
