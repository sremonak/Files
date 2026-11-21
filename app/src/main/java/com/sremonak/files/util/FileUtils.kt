package com.sremonak.files.util

import android.webkit.MimeTypeMap
import com.sremonak.files.data.model.FileCategory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif", "tiff", "tif", "ico", "avif")
    private val VIDEO_EXT = setOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "wmv", "flv", "m4v", "mpg", "mpeg", "ts")
    private val AUDIO_EXT = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "opus", "wma", "aiff", "mid", "midi")
    private val DOC_EXT = setOf("txt", "doc", "docx", "odt", "rtf", "md", "csv", "xls", "xlsx", "ods", "ppt", "pptx", "odp", "epub", "mobi")
    private val ARCHIVE_EXT = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "jar", "war")
    private val CODE_EXT = setOf("kt", "kts", "java", "c", "cpp", "h", "hpp", "py", "js", "ts", "html", "css", "json", "xml", "gradle", "sh", "php", "rb", "go", "rs", "swift", "sql", "yml", "yaml", "toml", "ini", "log")
    private val FONT_EXT = setOf("ttf", "otf", "woff", "woff2", "eot")

    private val TEXT_EXT = setOf(
        "txt", "md", "log", "json", "xml", "html", "htm", "css", "js", "ts", "kt", "kts", "java",
        "c", "cpp", "h", "hpp", "py", "sh", "php", "rb", "go", "rs", "swift", "sql", "yml", "yaml",
        "toml", "ini", "cfg", "conf", "csv", "gradle", "properties", "env", "gitignore"
    )

    fun extensionOf(name: String): String {
        val idx = name.lastIndexOf('.')
        return if (idx > 0 && idx < name.length - 1) name.substring(idx + 1).lowercase(Locale.ROOT) else ""
    }

    fun mimeFor(file: File): String? {
        val ext = extensionOf(file.name)
        if (ext.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    fun categoryFor(file: File): FileCategory {
        if (file.isDirectory) return FileCategory.OTHER
        val ext = extensionOf(file.name)
        return when {
            ext in IMAGE_EXT -> FileCategory.IMAGE
            ext in VIDEO_EXT -> FileCategory.VIDEO
            ext in AUDIO_EXT -> FileCategory.AUDIO
            ext == "pdf" -> FileCategory.PDF
            ext == "apk" || ext == "aab" -> FileCategory.APK
            ext in ARCHIVE_EXT -> FileCategory.ARCHIVE
            ext in CODE_EXT -> FileCategory.CODE
            ext in FONT_EXT -> FileCategory.FONT
            ext in DOC_EXT -> FileCategory.DOCUMENT
            else -> {
                val mime = mimeFor(file) ?: return FileCategory.OTHER
                when {
                    mime.startsWith("image/") -> FileCategory.IMAGE
                    mime.startsWith("video/") -> FileCategory.VIDEO
                    mime.startsWith("audio/") -> FileCategory.AUDIO
                    mime.startsWith("text/") || mime.startsWith("application/") -> FileCategory.DOCUMENT
                    else -> FileCategory.OTHER
                }
            }
        }
    }

    fun isTextFile(file: File): Boolean = extensionOf(file.name) in TEXT_EXT

    fun isHidden(file: File): Boolean = file.name.startsWith(".")

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        return String.format(Locale.US, "%.1f %s", value, units[unit])
    }

    fun formatDate(millis: Long): String {
        if (millis <= 0) return "—"
        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return fmt.format(Date(millis))
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "—"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun formatBitrate(kbps: Long): String {
        if (kbps <= 0) return "—"
        return if (kbps >= 1000) String.format(Locale.US, "%.1f Mbps", kbps / 1000.0)
        else "$kbps kbps"
    }

    /** Returns a non-conflicting file inside [dir] for [name], e.g. "a (1).txt". */
    fun uniqueFileIn(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val ext = extensionOf(name)
        val base = if (ext.isEmpty()) name else name.substring(0, name.length - ext.length - 1)
        var i = 1
        while (true) {
            val nextName = if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext"
            candidate = File(dir, nextName)
            if (!candidate.exists()) return candidate
            i++
        }
    }

    fun humanCount(n: Long): String {
        return when {
            n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
            n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
            else -> n.toString()
        }
    }
}
