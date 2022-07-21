package com.keyrico.keyrisdk.telemetry

import android.os.Build
import com.google.gson.JsonObject

object TelemetryManager {

    fun sendEvent(tag: String, message: String) {
        val metadata = getMetadata(tag)

        // Sending Telemetry event
    }

    fun sendErrorEvent(tag: String, message: String, exception: Exception) {
        val metadata = getMetadata(tag)

        // Sending Telemetry error event (stackTrace)
    }

    private fun getMetadata(tag: String): String {
        return JsonObject().apply {
//            addProperty("SDK version", BuildConfig.VERSION_NAME) // How to pass it
            addProperty("platform", "Android")
            addProperty("platform version", Build.VERSION.SDK_INT)
            addProperty("device model", getDeviceName())
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("tag", tag)
        }.toString()
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        return if (model.lowercase().startsWith(manufacturer.lowercase())) {
            model.uppercase()
        } else {
            manufacturer.uppercase() + " " + model
        }
    }
}
