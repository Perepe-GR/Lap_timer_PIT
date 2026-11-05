package com.ejemplo.laptimer

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Configuration and choice adapter helpers for PageActivity screens. */

fun PageActivity.choice(
    parent: LinearLayout,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: ((String) -> Unit)? = null
): () -> String {
    var current = selectedKey
    val views = ArrayList<TextView>()
    fun paint() {
        for ((i, v) in views.withIndex()) {
            val sel = options[i].first == current
            v.background = roundBg(colField, 12, if (sel) colLime else colField, 2)
            v.setTextColor(if (sel) colLime else colText)
        }
    }
    for ((key, text) in options) {
        val v = TextView(this)
        v.text = text
        v.textSize = 15f
        v.setPadding(dp(14), dp(12), dp(14), dp(12))
        v.setOnClickListener {
            current = key
            paint()
            onSelected?.invoke(key)
        }
        views.add(v)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(8)
        parent.addView(v, lp)
    }
    paint()
    return { current }
}
