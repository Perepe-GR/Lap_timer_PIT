package com.ejemplo.laptimer

import java.util.Locale

internal fun mmssOf(ms: Long): String {
    val t = ms / 1000
    return String.format(Locale.US, "%d:%02d", t / 60, t % 60)
}

/** Race Mode Selection: Segmented Buttons for Trackday, Sprint, or Long Run setup. */
class ModePageActivity : PageActivity() {
    override val pageTitle: String = "Race Mode & Setup"

    override fun build() {
        val settings = NetSettings(this)

        val modes = listOf(
            Triple("trackday", "Track Day", "Free practice: unlimited laps with no target time."),
            Triple("sprint", "Sprint", "Race with target laps or total duration limit."),
            Triple("long_run", "Long Run", "Extended practice session with stint management.")
        )

        // Top Area (30%): Segmented Buttons for mutually exclusive options
        val topCard = card("Race Mode Selection")
        segmentedTabs(topCard, modes, settings.raceMode) { selected ->
            settings.raceMode = selected
            toast("Active mode: ${modes.firstOrNull { it.first == selected }?.second ?: selected}")
            rebuild()
        }

        // Bottom Area (70%): Dynamic parameters form with mode description header
        val activeMode = modes.firstOrNull { it.first == settings.raceMode } ?: modes[0]
        val formCard = card("${activeMode.second} Parameters")

        note(formCard, activeMode.third)

        val etCar = textField(formCard, "Car Name (Optional)", settings.carName, "Car name")
        val etDriver = textField(formCard, "Driver Name (Required)", settings.driverName, "Driver name")

        if (settings.raceMode == "sprint") {
            val etLaps = numField(formCard, "Target Laps (0 = unlimited)", settings.sprintLaps.toString())
            val etMin = numField(formCard, "Maximum Duration (minutes; 0 = unlimited)", settings.sprintMin.toString())
            note(formCard, "The race will end as soon as either limit is reached.")
            addButton(formCard, button("SAVE SPRINT SETUP", true) {
                val driver = etDriver.text.toString().trim()
                if (driver.isBlank()) {
                    toast("Driver name is required")
                    return@button
                }
                settings.carName = etCar.text.toString().trim()
                settings.driverName = driver
                settings.sprintLaps = etLaps.text.toString().toIntOrNull() ?: 0
                settings.sprintMin = etMin.text.toString().toIntOrNull() ?: 0
                toast("Sprint setup saved successfully")
            })
        } else {
            val btnText = if (settings.raceMode == "trackday") "SAVE TRACK DAY SETUP" else "SAVE LONG RUN SETUP"
            addButton(formCard, button(btnText, true) {
                val driver = etDriver.text.toString().trim()
                if (driver.isBlank()) {
                    toast("Driver name is required")
                    return@button
                }
                settings.carName = etCar.text.toString().trim()
                settings.driverName = driver
                toast("Setup saved successfully")
            })
        }
    }
}
