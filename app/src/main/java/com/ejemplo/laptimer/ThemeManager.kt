package com.ejemplo.laptimer

import android.content.Context
import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppTheme(
    val id: String,
    val name: String,
    val description: String,
    val accent: Int,
    val alert: Int,
    val cardBg: Int = Color.parseColor("#12161B"),
    val bg: Int = Color.parseColor("#0B0D10"),
    val muted: Int = Color.parseColor("#9AA3AE"),
    val text: Int = Color.parseColor("#F2F4F7")
)

object ThemeManager {
    val THEME_CLASSIC = AppTheme(
        id = "classic",
        name = "F1 Neon Lime (Original)",
        description = "Maximum contrast with circuit neon lime and pure red",
        accent = Color.parseColor("#C6F432"),
        alert = Color.parseColor("#FF0000")
    )

    val THEME_GR86 = AppTheme(
        id = "gr86",
        name = "Toyota GR86 Gazoo Racing",
        description = "Circuit Pack edition: Gazoo Racing red, pure white and carbon fiber background",
        accent = Color.parseColor("#E0041B"),
        alert = Color.parseColor("#FF0000"),
        cardBg = Color.parseColor("#15181C"),
        bg = Color.parseColor("#0A0B0D")
    )

    val THEME_RACING_BLUE = AppTheme(
        id = "racing_blue",
        name = "Racing Cyan / Gulf GT",
        description = "Classic endurance style: electric cyan blue and pure red racing accent",
        accent = Color.parseColor("#00E5FF"),
        alert = Color.parseColor("#FF0000")
    )

    val THEMES = listOf(THEME_CLASSIC, THEME_GR86, THEME_RACING_BLUE)

    private val _currentTheme = MutableStateFlow(THEME_GR86)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        _currentTheme.value = getTheme(context)
    }

    fun getTheme(context: Context): AppTheme {
        val sp = context.getSharedPreferences("laptimer_prefs", Context.MODE_PRIVATE)
        val id = sp.getString("app_theme_id", "gr86")
        return THEMES.firstOrNull { it.id == id } ?: THEME_GR86
    }

    fun setTheme(context: Context, themeId: String) {
        val sp = context.getSharedPreferences("laptimer_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("app_theme_id", themeId).apply()
        val t = THEMES.firstOrNull { it.id == themeId } ?: THEME_CLASSIC
        _currentTheme.value = t
        onThemeChanged?.invoke()
    }

    var onThemeChanged: (() -> Unit)? = null
}
