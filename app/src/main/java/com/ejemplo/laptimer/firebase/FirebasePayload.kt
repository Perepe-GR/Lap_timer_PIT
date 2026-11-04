package com.ejemplo.laptimer.firebase

import com.ejemplo.laptimer.net.StintTarget

class PitMessage(val key: String, val text: String, val bg: String)

class SwapRequest(
    val id: String,
    val next: String,
    val laps: Int,
    val minMin: Int,
    val maxMin: Int,
    val pitSec: Int = 0,
    val confirmed: Boolean = false
)

enum class NetStatus { OFF, OK, ERROR }
