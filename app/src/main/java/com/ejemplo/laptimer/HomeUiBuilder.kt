package com.ejemplo.laptimer

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ejemplo.laptimer.endurance.lobby.EnduranceLobbyActivity

fun HomeActivity.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun HomeActivity.openPage(target: Class<*>) = startActivity(Intent(this, target))

fun HomeActivity.roundBg(color: Int, radiusDp: Int): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(color)
    g.cornerRadius = dp(radiusDp).toFloat()
    return g
}

fun HomeActivity.menuButton(text: String, primary: Boolean, action: () -> Unit): TextView {
    val b = TextView(this)
    b.text = text
    b.gravity = Gravity.CENTER
    b.textSize = if (primary) 22f else 15f
    b.typeface = Typeface.DEFAULT_BOLD
    b.setTextColor(if (primary) colBg else colText)
    b.background = roundBg(if (primary) lime else colCard, 16)
    b.isClickable = true
    b.setOnClickListener { action() }
    return b
}

fun HomeActivity.statusLine(): TextView {
    val t = TextView(this)
    t.textSize = 15f
    t.setTextColor(muted)
    t.setPadding(0, dp(8), 0, 0)
    return t
}

fun HomeActivity.buildUi(): View {
    val root = LinearLayout(this)
    root.orientation = LinearLayout.HORIZONTAL
    root.setBackgroundColor(colBg)
    root.setPadding(dp(16), dp(12), dp(16), dp(12))

    val left = LinearLayout(this)
    left.orientation = LinearLayout.VERTICAL
    left.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_carbon_fiber)
    left.setPadding(dp(20), dp(16), dp(20), dp(16))

    val head = LinearLayout(this)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER_VERTICAL
    val logo = ImageView(this)
    logo.setImageResource(R.mipmap.ic_launcher)
    head.addView(logo, LinearLayout.LayoutParams(dp(56), dp(56)))
    val title = TextView(this)
    val versionName = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "2.6.0"
    } catch (_: Exception) {
        "2.6.0"
    }
    val displayVersion = if (versionName.startsWith("v", true)) versionName else "v$versionName"
    title.text = "Lap Timer $displayVersion"
    title.textSize = 24f
    title.typeface = Typeface.DEFAULT_BOLD
    title.setTextColor(colText)
    head.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12), 0, 0, 0) })

    val badge = ImageView(this)
    badge.setImageResource(R.drawable.ic_gr_badge)
    head.addView(badge, LinearLayout.LayoutParams(dp(56), dp(28)))
    left.addView(head)

    val subTitle = TextView(this)
    subTitle.text = "TOYOTA GAZOO RACING · GR86 EDITION"
    subTitle.textSize = 11f
    subTitle.typeface = Typeface.DEFAULT_BOLD
    subTitle.setTextColor(Color.parseColor("#E0041B"))
    subTitle.setPadding(dp(2), dp(4), 0, dp(12))
    left.addView(subTitle)

    txtCircuit = statusLine()
    txtAntenna = statusLine()
    txtPit = statusLine()
    txtMode = statusLine()
    left.addView(txtCircuit)
    left.addView(txtAntenna)
    left.addView(txtPit)
    left.addView(txtMode)

    left.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
    left.addView(
        menuButton("START SESSION", true) {
            startActivity(Intent(this, MainActivity::class.java))
        },
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72))
    )
    root.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f))

    val right = LinearLayout(this)
    right.orientation = LinearLayout.VERTICAL

    fun row(vararg items: Triple<String, String, () -> Unit>) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        for ((i, item) in items.withIndex()) {
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            if (i > 0) lp.marginStart = dp(12)
            r.addView(cardButton(item.first, item.second, item.third), lp)
        }
        val rlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        if (right.childCount > 0) rlp.topMargin = dp(12)
        right.addView(r, rlp)
    }

    row(
        Triple("SOLO", "Practice / Sprint session") {
            openPage(ModePageActivity::class.java)
        },
        Triple("ENDURANCE", "Stint strategy & multi-device lobby") {
            openPage(EnduranceLobbyActivity::class.java)
        }
    )
    row(
        Triple("RESULTS", "Sessions, best laps & stats") {
            openPage(ResultsActivity::class.java)
        },
        Triple("SETTINGS", "Screen, pit, cloud & backup") {
            openPage(SessionSettingsActivity::class.java)
        }
    )
    row(
        Triple("DEMO MODE", "Virtual simulator practice") {
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("demo", true)
            startActivity(i)
        },
        Triple("CIRCUITS", "Select, edit or create maps") {
            openPage(CircuitsActivity::class.java)
        }
    )

    val rlp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f)
    rlp.marginStart = dp(14)
    root.addView(right, rlp)
    return root
}

fun HomeActivity.cardButton(title: String, subtitle: String, action: () -> Unit): LinearLayout {
    val layout = LinearLayout(this)
    layout.orientation = LinearLayout.VERTICAL
    layout.gravity = Gravity.CENTER_VERTICAL
    layout.background = roundBg(colCard, 16)
    layout.setPadding(dp(16), dp(10), dp(16), dp(10))
    layout.isClickable = true
    layout.isFocusable = true

    val t = TextView(this)
    t.text = title
    t.textSize = 17f
    t.typeface = Typeface.DEFAULT_BOLD
    t.setTextColor(colText)

    val sub = TextView(this)
    sub.text = subtitle
    sub.textSize = 12f
    sub.setTextColor(muted)
    sub.setPadding(0, dp(2), 0, 0)

    layout.addView(t)
    layout.addView(sub)
    layout.setOnClickListener { action() }
    return layout
}
