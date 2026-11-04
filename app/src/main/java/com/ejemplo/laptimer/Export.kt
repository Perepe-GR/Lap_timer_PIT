package com.ejemplo.laptimer

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Export {

    fun lapsCsv(r: SessionRecord): String {
        val sb = StringBuilder()
        sb.append("lap;time;ms")
        for (i in 1..r.sectorCount) sb.append(";S").append(i)
        sb.append('\n')
        for (l in r.laps) {
            sb.append(l.number).append(';').append(fmtMs(l.lapMs)).append(';').append(l.lapMs)
            for (s in l.splits) sb.append(';').append(fmtMs(s))
            sb.append('\n')
        }
        return sb.toString()
    }

    /** GPX con una de cada 5 muestras (5 Hz) para que el archivo no sea enorme. */
    fun gpx(r: SessionRecord, tr: Trace): String {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        iso.timeZone = TimeZone.getTimeZone("UTC")
        val name = r.circuitName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"LapTimer\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("<trk><name>").append(name).append("</name><trkseg>\n")
        var i = 0
        while (i < tr.size) {
            sb.append("<trkpt lat=\"").append(String.format(Locale.US, "%.7f", tr.lat[i]))
            sb.append("\" lon=\"").append(String.format(Locale.US, "%.7f", tr.lon[i])).append("\">")
            sb.append("<time>").append(iso.format(Date(r.startEpochMs + tr.t[i]))).append("</time>")
            sb.append("</trkpt>\n")
            i += 5
        }
        sb.append("</trkseg></trk></gpx>\n")
        return sb.toString()
    }

    /** Comparte un archivo que ya está en cacheDir/export. */
    fun shareFile(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val i = Intent(Intent.ACTION_SEND)
        i.type = mime
        i.putExtra(Intent.EXTRA_STREAM, uri)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(i, "Compartir"))
    }

    /** Escribe el texto en un archivo temporal y abre el selector de compartir de Android. */
    fun share(context: Context, fileName: String, text: String, mime: String) {
        val dir = File(context.cacheDir, "export")
        dir.mkdirs()
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val f = File(dir, safe)
        f.writeText(text)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
        val i = Intent(Intent.ACTION_SEND)
        i.type = mime
        i.putExtra(Intent.EXTRA_STREAM, uri)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(i, "Compartir"))
    }
}
