package com.ejemplo.laptimer

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Base for full-screen menu screens, with the app style: dark background, rounded corner cards,
 * neon lime accent. Each screen populates [content] (cards) and [footer] (buttons).
 */
abstract class PageActivity : AppCompatActivity() {

    internal open val colBg get() = ThemeManager.getTheme(this).bg
    internal open val colCard get() = ThemeManager.getTheme(this).cardBg
    internal val colField = Color.parseColor("#1E242B")
    internal open val colLime get() = ThemeManager.getTheme(this).accent
    internal open val colMuted get() = ThemeManager.getTheme(this).muted
    internal open val colText get() = ThemeManager.getTheme(this).text
    internal open val colRed get() = ThemeManager.getTheme(this).alert

    internal lateinit var content: LinearLayout
    internal lateinit var footer: LinearLayout

    protected abstract val pageTitle: String

    /** Populates [content] and [footer]. Called again with [rebuild] when data changes. */
    protected abstract fun build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(colBg)

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(16), dp(10), dp(16), dp(6))
        val back = TextView(this)
        back.text = "‹  BACK"
        back.textSize = 15f
        back.typeface = Typeface.DEFAULT_BOLD
        back.setTextColor(colLime)
        back.setPadding(0, dp(6), dp(16), dp(6))
        back.setOnClickListener { finish() }
        header.addView(back)
        val title = TextView(this)
        title.text = pageTitle
        title.textSize = 20f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(colText)
        header.addView(title)
        root.addView(header)

        val scroll = ScrollView(this)
        scroll.isFillViewport = false
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(4), dp(16), dp(12))
        scroll.addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        footer = LinearLayout(this)
        footer.orientation = LinearLayout.HORIZONTAL
        footer.setPadding(dp(16), dp(8), dp(16), dp(10))
        root.addView(footer)

        setContentView(root)
        build()
        refreshFooter()
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.onThemeChanged = { runOnUiThread { recreate() } }
    }

    override fun onPause() {
        ThemeManager.onThemeChanged = null
        super.onPause()
    }

    /** Clears and rebuilds the screen. */
    protected fun rebuild() {
        content.removeAllViews()
        footer.removeAllViews()
        build()
        refreshFooter()
    }

    fun refreshFooter() {
        footer.visibility = if (footer.childCount == 0) View.GONE else View.VISIBLE
    }

    // ---------- Utilities ----------

    internal fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    internal fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    internal fun roundBg(color: Int, radiusDp: Int, strokeColor: Int = 0, strokeDp: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        if (strokeDp > 0) g.setStroke(dp(strokeDp), strokeColor)
        return g
    }
}
