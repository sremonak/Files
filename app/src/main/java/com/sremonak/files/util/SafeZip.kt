package com.sremonak.files.util

import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/** Extract only into a newly created directory; remove partial output on any failure. */
object SafeZip {
    const val MAX_ENTRIES = 10_000
    const val MAX_BYTES = 2L * 1024 * 1024 * 1024

    fun extract(zip: File, target: File, maxBytes: Long = MAX_BYTES, maxEntries: Int = MAX_ENTRIES): Boolean {
        if (!target.mkdir()) return false
        return try {
            val root = target.canonicalPath + File.separator
            var count = 0
            var total = 0L
            ZipInputStream(zip.inputStream().buffered()).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (++count > maxEntries) throw IOException("Too many archive entries")
                    val out = File(target, entry.name)
                    if (entry.name.isEmpty() || entry.name.startsWith('/') ||
                        entry.name.contains('\\') || !out.canonicalPath.startsWith(root)) {
                        throw IOException("Unsafe archive entry")
                    }
                    if (entry.isDirectory) {
                        if (!out.isDirectory && !out.mkdirs()) throw IOException("Cannot create directory")
                    } else {
                        val parent = out.parentFile ?: throw IOException("Invalid archive path")
                        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create directory")
                        // Duplicate paths are rejected rather than silently overwriting data.
                        if (!out.createNewFile()) throw IOException("Duplicate archive entry")
                        out.outputStream().buffered().use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                total += n
                                if (total > maxBytes) throw IOException("Archive exceeds extraction limit")
                                output.write(buffer, 0, n)
                            }
                        }
                    }
                    input.closeEntry()
                }
            }
            true
        } catch (_: Exception) {
            target.deleteRecursively()
            false
        }
    }
}
