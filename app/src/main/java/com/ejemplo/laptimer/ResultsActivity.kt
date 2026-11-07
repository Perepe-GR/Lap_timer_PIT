package com.ejemplo.laptimer

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ejemplo.laptimer.results.HistoryTab
import com.ejemplo.laptimer.results.RecordsTab
import com.ejemplo.laptimer.results.StatsTab

/**
 * Unified Results Activity: 3 tabs ("My Sessions" | "Best Laps" | "Stats").
 * Supports opening directly to a specific tab via EXTRA_TAB (0, 1, or 2).
 */
class ResultsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TAB = "EXTRA_TAB"
        const val TAB_SESSIONS = 0
        const val TAB_RECORDS = 1
        const val TAB_STATS = 2
    }

    private lateinit var store: SessionStore
    private lateinit var historyTab: HistoryTab
    private lateinit var recordsTab: RecordsTab
    private lateinit var statsTab: StatsTab

    private lateinit var tabButtons: List<TextView>
    private lateinit var container: FrameLayout
    private var activeTab = TAB_SESSIONS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SessionStore(this)
        historyTab = HistoryTab(this, store)
        recordsTab = RecordsTab(this, store)
        statsTab = StatsTab(this, store)

        val theme = ThemeManager.getTheme(this)
        val pad = (14 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(theme.bg)
        root.setPadding(pad, pad, pad, pad)

        // Title & Back Row
        val titleRow = LinearLayout(this)
        titleRow.orientation = LinearLayout.HORIZONTAL
        titleRow.gravity = Gravity.CENTER_VERTICAL

        val back = TextView(this)
        back.text = "‹  BACK"
        back.textSize = 15f
        back.typeface = Typeface.DEFAULT_BOLD
        back.setTextColor(theme.accent)
        back.setPadding(0, 0, pad, 0)
        back.setOnClickListener { finish() }
        titleRow.addView(back)

        val title = TextView(this)
        title.text = "Results"
        title.textSize = 20f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(theme.text)
        titleRow.addView(title)
        root.addView(titleRow)

        // Tab Selector Row
        val tabRow = LinearLayout(this)
        tabRow.orientation = LinearLayout.HORIZONTAL
        tabRow.setPadding(0, pad / 2, 0, pad / 2)

        val btn0 = makeTabButton("My Sessions") { selectTab(TAB_SESSIONS) }
        val btn1 = makeTabButton("Best Laps") { selectTab(TAB_RECORDS) }
        val btn2 = makeTabButton("Stats") { selectTab(TAB_STATS) }
        tabButtons = listOf(btn0, btn1, btn2)

        for ((i, btn) in tabButtons.withIndex()) {
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) lp.marginStart = pad / 2
            tabRow.addView(btn, lp)
        }
        root.addView(tabRow)

        // Main Container Frame
        container = FrameLayout(this)
        root.addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        val targetTab = intent.getIntExtra(EXTRA_TAB, TAB_SESSIONS)
        selectTab(targetTab.coerceIn(TAB_SESSIONS, TAB_STATS))
    }

    override fun onResume() {
        super.onResume()
        when (activeTab) {
            TAB_SESSIONS -> historyTab.reload()
            TAB_RECORDS -> recordsTab.reload()
            TAB_STATS -> statsTab.reload()
        }
    }

    private fun makeTabButton(label: String, action: () -> Unit): TextView {
        val b = TextView(this)
        b.text = label
        b.gravity = Gravity.CENTER
        b.textSize = 14f
        b.typeface = Typeface.DEFAULT_BOLD
        b.setPadding(0, (10 * resources.displayMetrics.density).toInt(), 0, (10 * resources.displayMetrics.density).toInt())
        b.setOnClickListener { action() }
        return b
    }

    private fun selectTab(index: Int) {
        activeTab = index
        container.removeAllViews()
        when (index) {
            TAB_SESSIONS -> {
                historyTab.reload()
                container.addView(historyTab.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            TAB_RECORDS -> {
                recordsTab.reload()
                container.addView(recordsTab.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            TAB_STATS -> {
                statsTab.reload()
                container.addView(statsTab.view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }
        updateTabStyles()
    }

    private fun updateTabStyles() {
        val theme = ThemeManager.getTheme(this)
        val radius = (10 * resources.displayMetrics.density)
        for ((i, btn) in tabButtons.withIndex()) {
            val selected = i == activeTab
            val g = GradientDrawable()
            g.setColor(if (selected) theme.accent else theme.cardBg)
            g.cornerRadius = radius
            btn.background = g
            btn.setTextColor(if (selected) theme.bg else theme.text)
        }
    }
}
