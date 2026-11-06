package com.ejemplo.laptimer.firebase

class PitMessage(
    val key: String,
    val text: String,
    val bg: String,
    val flag: String = ""
)

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
