package com.ejemplo.laptimer

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

/** Backup management: create (with/without GPS traces) and restore. */
class BackupActivity : PageActivity() {
    override val pageTitle: String = "Backup & Restore"

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) doRestore(uri)
        }

    override fun build() {
        val c0 = card()
        note(c0, "Backups include circuits, reference marks, sessions, settings, and room codes. Keep private.")
        optionCard("Create backup with GPS traces", "Includes full GPS trace data; larger file size.", false) { makeBackup(true) }
        optionCard("Create backup without GPS traces", "Lightweight: excludes raw GPS position traces.", false) { makeBackup(false) }
        optionCard("Restore from backup file", "Select a previously exported backup file.", false) {
            restoreLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun makeBackup(traces: Boolean) {
        try {
            val f = Backup.create(this, traces)
            Export.shareFile(this, f, "application/zip")
        } catch (e: Exception) {
            toast("Could not create backup: ${e.message}")
        }
    }

    private fun doRestore(uri: Uri) {
        try {
            val n = Backup.restore(this, uri)
            toast("Backup restored successfully: $n files")
        } catch (e: Exception) {
            toast("Could not restore backup: ${e.message}")
        }
    }
}
