package com.ejemplo.laptimer.logic

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.ejemplo.laptimer.MainActivity
import kotlin.math.abs

fun MainActivity.askExit() {
    if (!checkUnlocked()) return
    AlertDialog.Builder(this)
        .setTitle("Close Session?")
        .setMessage("Timing will stop.")
        .setNegativeButton("Continue", null)
        .setPositiveButton("Close") { _, _ ->
            AlertDialog.Builder(this)
                .setTitle("Confirm Again")
                .setMessage("Are you sure you want to close the session?")
                .setNegativeButton("Keep Running", null)
                .setPositiveButton("YES, CLOSE") { _, _ -> finish() }
                .show()
        }
        .show()
}

fun MainActivity.handleSwipe(x0: Float, y0: Float, x1: Float, y1: Float) {
    val dm = resources.displayMetrics
    val dx = x1 - x0
    val dy = y1 - y0
    val min = 70f * dm.density
    val w = dm.widthPixels
    val h = dm.heightPixels
    val vertical = abs(dy) > abs(dx) * 1.5f
    val horizontal = abs(dx) > abs(dy) * 1.5f
    if (vertical && dy > min && y0 < h * 0.35f) {
        showTopBar()
    } else if (vertical && dy < -min && bindings.topBar.visibility == View.VISIBLE) {
        hideTopBarNow()
    } else if (horizontal && abs(dx) > min * 1.4f && x0 > w * 0.25f && x0 < w * 0.75f) {
        toggleView(dx < 0)
    }
}

fun MainActivity.showTopBar() {
    bindings.topBar.visibility = View.VISIBLE
    bindings.topBar.removeCallbacks(hideTop)
    bindings.topBar.postDelayed(hideTop, 5_000L)
}

fun MainActivity.hideTopBarNow() {
    bindings.topBar.removeCallbacks(hideTop)
    bindings.topBar.visibility = View.GONE
}

fun MainActivity.toggleView(next: Boolean) {
    viewIndex = (viewIndex + (if (next) 1 else 2)) % 3
    bindings.viewMain.visibility = if (viewIndex == 0) View.VISIBLE else View.GONE
    bindings.viewSimple.visibility = if (viewIndex == 1) View.VISIBLE else View.GONE
    bindings.viewMap.visibility = if (viewIndex == 2) View.VISIBLE else View.GONE
}
