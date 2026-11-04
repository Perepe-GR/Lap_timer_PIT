package com.ejemplo.laptimer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Forwarder to ResultsActivity (My Sessions tab). */
class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = Intent(this, ResultsActivity::class.java)
        i.putExtra(ResultsActivity.EXTRA_TAB, ResultsActivity.TAB_SESSIONS)
        startActivity(i)
        finish()
    }
}
