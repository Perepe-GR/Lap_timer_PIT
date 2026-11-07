package com.ejemplo.laptimer

import android.widget.ArrayAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun RecordsActivity.adapter(items: List<String>): ArrayAdapter<String> {
    val ad = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
    ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    return ad
}

data class RecordsStatsResult(
    val infoText: String,
    val perSession: List<Double>,
    val soFar: List<Double>
)

fun computeRecordsStats(list: List<SessionRecord>, driver: String?): RecordsStatsResult {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val nSectors = list.last().sectorCount
    var bestMs = Long.MAX_VALUE
    var bestSession: SessionRecord? = null
    val bestSectors = LongArray(nSectors) { Long.MAX_VALUE }
    var laps = 0
    for (s in list) {
        laps += s.laps.size
        for (l in s.laps) {
            if (l.lapMs < bestMs) {
                bestMs = l.lapMs
                bestSession = s
            }
            if (s.sectorCount == nSectors) {
                for (i in l.splits.indices) {
                    if (i < nSectors && l.splits[i] < bestSectors[i]) bestSectors[i] = l.splits[i]
                }
            }
        }
    }
    val sb = StringBuilder()
    sb.append("Sessions: ").append(list.size).append(" · Laps: ").append(laps).append('\n')
    if (bestSession != null) {
        sb.append("BEST LAP: ").append(fmtMs(bestMs)).append("  (")
            .append(date.format(Date(bestSession.startEpochMs)))
            .append(driverTag(bestSession)).append(")\n")
    }
    if (bestSectors.all { it != Long.MAX_VALUE }) {
        sb.append("BEST sectors: ")
        for (i in 0 until nSectors) {
            sb.append("S").append(i + 1).append(' ')
                .append(String.format(Locale.US, "%.2f", bestSectors[i] / 1000.0)).append("   ")
        }
        sb.append("\nOptimal: ").append(fmtMs(bestSectors.sum()))
    }

    val perSession = ArrayList<Double>()
    val soFar = ArrayList<Double>()
    var running = Double.MAX_VALUE
    for (s in list) {
        val b = s.bestLap() ?: continue
        val sec = b.lapMs / 1000.0
        if (sec < running) running = sec
        perSession.add(sec)
        soFar.add(running)
    }
    return RecordsStatsResult(sb.toString(), perSession, soFar)
}
