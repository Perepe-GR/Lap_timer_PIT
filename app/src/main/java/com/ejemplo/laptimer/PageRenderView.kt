package com.ejemplo.laptimer

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Extension functions and helpers for PageActivity components. */

fun PageActivity.card(title: String? = null): LinearLayout {
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.background = roundBg(colCard, 16)
    c.setPadding(dp(16), dp(12), dp(16), dp(14))
    if (title != null) {
        val t = TextView(this)
        t.text = title.uppercase()
        t.textSize = 12f
        t.letterSpacing = 0.1f
        t.typeface = Typeface.DEFAULT_BOLD
        t.setTextColor(colMuted)
        c.addView(t)
    }
    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    lp.topMargin = dp(10)
    content.addView(c, lp)
    return c
}

fun PageActivity.label(parent: LinearLayout, text: String): TextView {
    val v = TextView(this)
    v.text = text; v.textSize = 12f; v.setTextColor(colMuted); v.setPadding(0, dp(10), 0, dp(4))
    parent.addView(v)
    return v
}

fun PageActivity.note(parent: LinearLayout, text: String): TextView {
    val v = TextView(this)
    v.text = text; v.textSize = 14f; v.setTextColor(colText); v.setPadding(0, dp(8), 0, 0)
    parent.addView(v)
    return v
}

private fun PageActivity.styleField(e: EditText) {
    e.setTextColor(colText); e.setHintTextColor(colMuted); e.textSize = 16f
    e.background = roundBg(colField, 10); e.setPadding(dp(12), dp(10), dp(12), dp(10))
    e.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
}

fun PageActivity.newText(value: String, hint: String = "", flags: Int = InputType.TYPE_CLASS_TEXT): EditText {
    val e = EditText(this); e.inputType = flags; e.hint = hint; e.setText(value); styleField(e); return e
}

fun PageActivity.newNum(value: String, decimal: Boolean = false): EditText {
    val flags = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
    return newText(value, "", flags)
}

fun PageActivity.textField(parent: LinearLayout, labelText: String, value: String, hint: String = "", flags: Int = InputType.TYPE_CLASS_TEXT): EditText {
    label(parent, labelText)
    val e = newText(value, hint, flags)
    parent.addView(e, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    return e
}

fun PageActivity.numField(parent: LinearLayout, labelText: String, value: String, decimal: Boolean = false): EditText {
    label(parent, labelText)
    val e = newNum(value, decimal)
    parent.addView(e, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    return e
}

fun PageActivity.check(parent: LinearLayout, text: String, checked: Boolean): CheckBox {
    val c = CheckBox(this); c.text = text; c.isChecked = checked; c.setTextColor(colText); c.textSize = 15f
    c.setPadding(dp(4), dp(8), 0, dp(8))
    val accent = ThemeManager.getTheme(this).accent
    val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
    val colors = intArrayOf(accent, colMuted)
    androidx.core.widget.CompoundButtonCompat.setButtonTintList(c, ColorStateList(states, colors))
    parent.addView(c)
    return c
}

fun PageActivity.block(labelText: String, field: View): LinearLayout {
    val b = LinearLayout(this); b.orientation = LinearLayout.VERTICAL
    val t = TextView(this); t.text = labelText; t.textSize = 12f; t.setTextColor(colMuted); t.setPadding(0, dp(10), 0, dp(4))
    b.addView(t); b.addView(field, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    return b
}

fun PageActivity.rowOf(parent: LinearLayout, vararg views: View) {
    val r = LinearLayout(this); r.orientation = LinearLayout.HORIZONTAL
    for ((i, v) in views.withIndex()) {
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        if (i > 0) lp.marginStart = dp(10)
        r.addView(v, lp)
    }
    parent.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
}

fun PageActivity.button(text: String, primary: Boolean = false, action: () -> Unit): TextView {
    val b = TextView(this); b.text = text; b.gravity = Gravity.CENTER; b.textSize = 15f; b.typeface = Typeface.DEFAULT_BOLD
    b.setTextColor(if (primary) colBg else colText)
    b.background = roundBg(if (primary) colLime else colField, 14)
    b.setPadding(dp(14), dp(12), dp(14), dp(12))
    b.isClickable = true
    b.setOnClickListener { action() }
    return b
}

fun PageActivity.addFooter(vararg buttons: TextView) {
    for ((i, b) in buttons.withIndex()) {
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        if (i > 0) lp.marginStart = dp(10)
        footer.addView(b, lp)
    }
}

fun PageActivity.addButton(parent: LinearLayout, b: TextView) {
    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    lp.topMargin = dp(10)
    parent.addView(b, lp)
}

fun PageActivity.segmentedTabs(
    parent: LinearLayout,
    options: List<Triple<String, String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    val bar = LinearLayout(this); bar.orientation = LinearLayout.HORIZONTAL
    bar.background = roundBg(colField, 12); bar.setPadding(dp(4), dp(4), dp(4), dp(4))

    for ((key, title, _) in options) {
        val selected = (key == selectedKey)
        val btn = TextView(this); btn.text = title.uppercase(); btn.gravity = Gravity.CENTER
        btn.textSize = 13f; btn.typeface = Typeface.DEFAULT_BOLD
        btn.setTextColor(if (selected) colBg else colText)
        btn.background = roundBg(if (selected) colLime else colCard, 8)
        btn.setPadding(dp(12), dp(10), dp(12), dp(10))
        btn.isClickable = true
        btn.setOnClickListener { onSelect(key) }
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(dp(2), 0, dp(2), 0)
        bar.addView(btn, lp)
    }
    parent.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
}

fun PageActivity.optionCard(title: String, desc: String, selected: Boolean, action: () -> Unit): LinearLayout {
    val c = LinearLayout(this); c.orientation = LinearLayout.VERTICAL
    c.background = roundBg(colCard, 16, if (selected) colLime else colCard, 2)
    c.setPadding(dp(16), dp(14), dp(16), dp(14))
    val t = TextView(this); t.text = title; t.textSize = 18f; t.typeface = Typeface.DEFAULT_BOLD
    t.setTextColor(if (selected) colLime else colText)
    c.addView(t)
    if (desc.isNotEmpty()) {
        val d = TextView(this); d.text = desc; d.textSize = 14f; d.setTextColor(colMuted); d.setPadding(0, dp(4), 0, 0)
        c.addView(d)
    }
    c.isClickable = true
    c.setOnClickListener { action() }
    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    lp.topMargin = dp(10)
    content.addView(c, lp)
    return c
}
