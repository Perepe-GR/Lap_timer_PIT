package com.ejemplo.laptimer

enum class ReceiverModel(val displayName: String, val hz: Int?) {
    UBLOX_7("u-blox 7", 10),
    UBLOX_8("u-blox 8/M8", 25),
    UBLOX_M9("u-blox M9", 25),
    GENERIC("Generic", 10),
    AUTO("Auto", null)
}

object ReceptorConfig {
    var currentModel: ReceiverModel = ReceiverModel.AUTO

    fun frequencyDisplay(): String {
        val h = currentModel.hz
        return if (h != null) "${h}Hz" else "Auto"
    }
}
