package com.keyrico.keyrisdk.telemetry

import java.lang.Exception

object TelemetryManager {

    fun sendEvent(message: String) {
        // Sending Telemetry event
    }

    fun sendErrorEvent(message: String, exception: Exception) {
        // Sending Telemetry error event (stackTrace)
    }
}
