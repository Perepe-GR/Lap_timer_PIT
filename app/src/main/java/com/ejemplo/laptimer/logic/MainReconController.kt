package com.ejemplo.laptimer.logic

import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.session.*

fun MainActivity.startRecon(name: String) {
    reconName = name
    draftFinish = null
    draftSectors.clear()
    renderBottom()
    refreshTop()
    showTopBar()
}

fun MainActivity.exitRecon() {
    reconName = null
    draftFinish = null
    draftSectors.clear()
    renderBottom()
    refreshTop()
}

fun MainActivity.currentPoint(): GatePoint? {
    val course = lastCourse
    if (!hasFix || course == null) {
        toast("No heading fix: move forward for a few seconds")
        return null
    }
    return GatePoint(lastLat, lastLon, course)
}

fun MainActivity.markFinish() {
    if (!checkUnlocked()) return
    val p = currentPoint() ?: return
    draftFinish = p
    refreshTop()
    toast("FINISH marked")
}

fun MainActivity.markSector() {
    if (!checkUnlocked()) return
    if (draftFinish == null) {
        toast("Mark FINISH first")
        return
    }
    val p = currentPoint() ?: return
    draftSectors.add(p)
    refreshTop()
    toast("Sector marked")
}

fun MainActivity.saveCircuit() {
    if (!checkUnlocked()) return
    val name = reconName ?: return
    val finish = draftFinish
    if (finish == null) {
        toast("Mark FINISH first")
        return
    }
    val c = Circuit(name, finish, draftSectors.toList())
    store.save(c)
    exitResonAndSelect(c)
}

fun MainActivity.exitResonAndSelect(c: Circuit) {
    exitRecon()
    selectCircuit(c)
    toast("Circuit saved: ${c.sectorCount} sectors")
}
