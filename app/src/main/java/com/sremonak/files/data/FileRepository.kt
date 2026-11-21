package com.sremonak.files.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.StatFs
import androidx.exifinterface.media.ExifInterface
import com.sremonak.files.data.model.CategoryStats
import com.sremonak.files.data.model.ClipboardState
import com.sremonak.files.data.model.DuplicateGroup
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.data.model.FileDetails
import com.sremonak.files.data.model.FileInfo
import com.sremonak.files.data.model.MediaInfo
import com.sremonak.files.data.model.ScanResult
import com.sremonak.files.data.model.StorageStats
import com.sremonak.files.data.model.TrashEntry
import com.sremonak.files.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileRepository(private val context: Context) {

    private val trashDir: File = File(context.filesDir, "trash").apply { mkdirs() }

    /** In-memory clipboard shared across screens (copy / move / paste). */
    var clipboard: ClipboardState = ClipboardState()
        private set

    fun setClipboard(files: List<File>, move: Boolean) {
        clipboard = ClipboardState(files.toList(), move)
    }

    fun clearClipboard() {
        clipboard = ClipboardState()
    }

    // ---------- Storage roots ----------
    fun internalRoot(): File = Environment.getExternalStorageDirectory()

    fun sdCardRoot(): File? {
        val external = context.getExternalFilesDir(null) ?: return null
        val rootPath = external.absolutePath.substringBefore("/Android/data")
        val root = File(rootPath)
        return if (root.exists() && root.canRead()) root else null
    }

    fun downloadsDir(): File = File(internalRoot(), "Download")
    fun picturesDir(): File = File(internalRoot(), "Pictures")
    fun dcimDir(): File = File(internalRoot(), "DCIM")
    fun musicDir(): File = File(internalRoot(), "Music")
    fun moviesDir(): File = File(internalRoot(), "Movies")
    fun documentsDir(): File = File(internalRoot(), "Documents")

    fun volumeLabelFor(file: File): String {
        val path = file.absolutePath
        val internal = internalRoot().absolutePath
        if (path.startsWith(internal)) return "Internal storage"
        val sd = sdCardRoot()?.absolutePath
        if (sd != null && path.startsWith(sd)) return "SD card"
        return "—"
    }

    fun storageStats(): StorageStats {
        val root = internalRoot()
        val stat = StatFs(root.path)
        return StorageStats(
            totalBytes = stat.totalBytes,
            availableBytes = stat.availableBytes,
            label = "Internal storage"
        )
    }

    // ---------- Listing ----------
    suspend fun listFiles(dir: File): List<File> = withContext(Dispatchers.IO) {
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        dir.listFiles()?.toList() ?: emptyList()
    }

    fun toInfo(file: File, favorites: Set<String>): FileInfo {
        val isDir = file.isDirectory
        return FileInfo(
            file = file,
            name = file.name,
            isDirectory = isDir,
            size = if (isDir) 0L else file.length(),
            lastModified = file.lastModified(),
            createdAt = createdAtOf(file),
            mimeType = if (isDir) null else FileUtils.mimeFor(file),
            extension = FileUtils.extensionOf(file.name),
            category = FileUtils.categoryFor(file),
            isHidden = FileUtils.isHidden(file),
            isFavorite = favorites.contains(file.absolutePath),
            childCount = if (isDir) file.listFiles()?.size ?: 0 else 0
        )
    }

    private fun createdAtOf(file: File): Long {
        return try {
            val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            attrs.creationTime().toMillis()
        } catch (_: Exception) {
            file.lastModified()
        }
    }

    // ---------- Details ----------
    suspend fun getDetails(file: File): FileDetails = withContext(Dispatchers.IO) {
        val attrs = try {
            Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        } catch (_: Exception) {
            null
        }
        val isSymlink = try { Files.isSymbolicLink(file.toPath()) } catch (_: Exception) { false }
        val linkTarget = if (isSymlink) {
            try { Files.readSymbolicLink(file.toPath()).toString() } catch (_: Exception) { null }
        } else null

        FileDetails(
            name = file.name,
            path = file.absolutePath,
            size = if (file.isDirectory) dirSize(file) else file.length(),
            isDirectory = file.isDirectory,
            extension = FileUtils.extensionOf(file.name),
            mimeType = if (file.isDirectory) null else FileUtils.mimeFor(file),
            createdAt = attrs?.creationTime()?.toMillis() ?: file.lastModified(),
            modifiedAt = file.lastModified(),
            accessedAt = attrs?.lastAccessTime()?.toMillis() ?: file.lastModified(),
            permissions = permissionsOf(file),
            owner = ownerOf(file),
            isHidden = FileUtils.isHidden(file),
            isSymlink = isSymlink,
            linkTarget = linkTarget,
            volumeLabel = volumeLabelFor(file),
            media = if (file.isDirectory) MediaInfo() else getMediaInfo(file),
            exif = if (FileUtils.categoryFor(file) == FileCategory.IMAGE) getExif(file) else emptyMap()
        )
    }

    private fun permissionsOf(file: File): String {
        return try {
            val perms = Files.getPosixFilePermissions(file.toPath())
            val sb = StringBuilder()
            sb.append(if (file.isDirectory) "d" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_READ)) "r" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE)) "w" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE)) "x" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.GROUP_READ)) "r" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.GROUP_WRITE)) "w" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE)) "x" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_READ)) "r" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE)) "w" else "-")
            sb.append(if (perms.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE)) "x" else "-")
            sb.toString()
        } catch (_: Exception) {
            val r = if (file.canRead()) "r" else "-"
            val w = if (file.canWrite()) "w" else "-"
            val x = if (file.canExecute()) "x" else "-"
            "-$r$w$x------"
        }
    }

    private fun ownerOf(file: File): String {
        return try {
            Files.getOwner(file.toPath()).name
        } catch (_: Exception) {
            "—"
        }
    }

    suspend fun getMediaInfo(file: File): MediaInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = try {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            } catch (_: Exception) {
                0L
            }
            MediaInfo(width, height, duration, bitrate / 1000)
        } catch (_: Exception) {
            MediaInfo()
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }

    suspend fun getExif(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val result = linkedMapOf<String, String>()
        try {
            val exif = ExifInterface(file)
            fun put(label: String, tag: String) {
                val value = exif.getAttribute(tag)
                if (!value.isNullOrBlank()) result[label] = value
            }
            put("Date taken", ExifInterface.TAG_DATETIME_ORIGINAL)
            put("Modified", ExifInterface.TAG_DATETIME)
            put("Camera make", ExifInterface.TAG_MAKE)
            put("Camera model", ExifInterface.TAG_MODEL)
            put("Software", ExifInterface.TAG_SOFTWARE)
            put("ISO", ExifInterface.TAG_ISO)
            put("Focal length", ExifInterface.TAG_FOCAL_LENGTH)
            put("Exposure time", ExifInterface.TAG_EXPOSURE_TIME)
            put("Aperture (F-number)", ExifInterface.TAG_F_NUMBER)
            put("Flash", ExifInterface.TAG_FLASH)
            put("Orientation", ExifInterface.TAG_ORIENTATION)
            put("Image width", ExifInterface.TAG_IMAGE_WIDTH)
            put("Image height", ExifInterface.TAG_IMAGE_LENGTH)
            put("Resolution X", ExifInterface.TAG_RESOLUTION_X)
            put("Resolution Y", ExifInterface.TAG_RESOLUTION_Y)
            val lat = exif.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, 0.0)
            val lon = exif.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, 0.0)
            if (lat != 0.0 || lon != 0.0) {
                result["GPS coordinates"] = String.format("%.6f, %.6f", lat, lon)
            }
        } catch (_: Exception) {
            // EXIF unavailable for this file — return what we have.
        }
        result
    }

    suspend fun computeHash(file: File, algorithm: String): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun dirSize(file: File): Long = withContext(Dispatchers.IO) {
        if (!file.isDirectory) return@withContext file.length()
        var total = 0L
        try {
            file.walkTopDown().forEach { f -> if (f.isFile) total += f.length() }
        } catch (_: Exception) { }
        total
    }

    // ---------- Mutations ----------
    private fun validName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." &&
            !name.contains('/') && !name.contains('\\') && !name.contains('\n') && !name.contains('\r')

    private fun copyComplete(source: File, target: File): Boolean {
        return try {
            if (source.isDirectory) {
                source.copyRecursively(target, onError = { _, error -> throw error })
            } else {
                source.copyTo(target)
                true
            }
        } catch (_: Exception) {
            target.deleteRecursively()
            false
        }
    }

    private fun transfer(source: File, target: File): Boolean {
        if (source.renameTo(target)) return true
        if (!copyComplete(source, target)) return false
        // Keep the completed copy even if deleting the original fails.
        return source.deleteRecursively()
    }

    private fun destinationInsideSource(source: File, dest: File): Boolean {
        val src = source.canonicalPath
        val dst = dest.canonicalPath
        return source.isDirectory && (dst == src || dst.startsWith(src + File.separator))
    }

    suspend fun rename(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        val name = newName.trim()
        if (!validName(name)) return@withContext false
        val target = File(file.parentFile ?: return@withContext false, name)
        if (target.exists()) return@withContext false
        transfer(file, target)
    }

    suspend fun createFolder(dir: File, name: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (!validName(trimmed) || !dir.isDirectory) return@withContext false
        val target = File(dir, trimmed)
        !target.exists() && target.mkdir()
    }

    suspend fun createTextFile(dir: File, name: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (!validName(trimmed) || !dir.isDirectory) return@withContext false
        try { File(dir, trimmed).createNewFile() } catch (_: Exception) { false }
    }

    suspend fun copyTo(files: List<File>, destDir: File): Int = withContext(Dispatchers.IO) {
        if (!destDir.isDirectory) return@withContext 0
        var count = 0
        for (source in files) {
            try {
                if (!source.exists() || destinationInsideSource(source, destDir)) continue
                if (copyComplete(source, FileUtils.uniqueFileIn(destDir, source.name))) count++
            } catch (_: Exception) { }
        }
        count
    }

    suspend fun moveTo(files: List<File>, destDir: File): Int = withContext(Dispatchers.IO) {
        if (!destDir.isDirectory) return@withContext 0
        var count = 0
        for (source in files) {
            try {
                if (!source.exists() || destinationInsideSource(source, destDir) ||
                    source.parentFile?.canonicalFile == destDir.canonicalFile) continue
                if (transfer(source, FileUtils.uniqueFileIn(destDir, source.name))) count++
            } catch (_: Exception) { }
        }
        count
    }

    // ---------- Trash ----------
    suspend fun deleteToTrash(files: List<File>): Int = withContext(Dispatchers.IO) {
        trashDir.mkdirs()
        var count = 0
        for (source in files) {
            if (!source.exists() || source.name.contains('\n') || source.absolutePath.contains('\n')) continue
            val id = UUID.randomUUID().toString()
            val trashed = File(trashDir, id)
            val meta = File(trashDir, "$id.meta")
            try {
                val size = if (source.isDirectory) dirSizeSync(source) else source.length()
                // Save metadata first so a successful move is always recoverable.
                meta.writeText(buildString {
                    appendLine(source.absolutePath)
                    appendLine(System.currentTimeMillis())
                    appendLine(size)
                    appendLine(source.isDirectory)
                    appendLine(source.name)
                })
                if (transfer(source, trashed)) count++
                else if (!trashed.exists()) meta.delete()
            } catch (_: Exception) {
                if (!trashed.exists()) meta.delete()
            }
        }
        count
    }

    private fun dirSizeSync(file: File): Long {
        if (!file.isDirectory) return file.length()
        var total = 0L
        try { file.walkTopDown().forEach { f -> if (f.isFile) total += f.length() } } catch (_: Exception) { }
        return total
    }

    suspend fun listTrash(): List<TrashEntry> = withContext(Dispatchers.IO) {
        trashDir.mkdirs()
        val entries = mutableListOf<TrashEntry>()
        val metas = trashDir.listFiles { f -> f.extension == "meta" } ?: emptyArray()
        for (meta in metas) {
            try {
                val lines = meta.readLines()
                if (lines.size < 5) continue
                val id = meta.name.removeSuffix(".meta")
                val data = File(trashDir, id)
                if (!data.exists()) {
                    meta.delete()
                    continue
                }
                entries.add(
                    TrashEntry(
                        id = id,
                        originalPath = lines[0],
                        name = lines[4],
                        size = lines[2].toLongOrNull() ?: 0L,
                        deletedAt = lines[1].toLongOrNull() ?: 0L,
                        isDirectory = lines[3].toBoolean()
                    )
                )
            } catch (_: Exception) { }
        }
        entries.sortedByDescending { it.deletedAt }
    }

    suspend fun restore(entry: TrashEntry): Boolean = withContext(Dispatchers.IO) {
        val data = File(trashDir, entry.id)
        if (!data.exists()) return@withContext false
        val parent = File(entry.originalPath).parentFile ?: return@withContext false
        parent.mkdirs()
        val target = FileUtils.uniqueFileIn(parent, entry.name)
        try {
            if (!transfer(data, target)) return@withContext false
            File(trashDir, "${entry.id}.meta").delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    suspend fun purgeExpiredTrash(days: Int) {
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000
        for (entry in listTrash()) {
            if (entry.deletedAt < cutoff) {
                File(trashDir, entry.id).deleteRecursively()
                File(trashDir, "${entry.id}.meta").delete()
            }
        }
    }

    suspend fun trashSize(): Long = withContext(Dispatchers.IO) { dirSizeSync(trashDir) }

    suspend fun deleteTrashEntry(entry: TrashEntry) = withContext(Dispatchers.IO) {
        File(trashDir, entry.id).deleteRecursively()
        File(trashDir, "${entry.id}.meta").delete()
    }

    suspend fun searchFilesByCategory(category: FileCategory, maxResults: Int = 1000): List<File> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<File>()
            val stack = ArrayDeque<Pair<File, Int>>()
            stack.add(internalRoot() to 0)
            while (stack.isNotEmpty() && results.size < maxResults) {
                coroutineContext.ensureActive()
                val (dir, depth) = stack.removeLast()
                if (depth > 8) continue
                val children = try { dir.listFiles() } catch (_: Exception) { null } ?: continue
                for (child in children) {
                    if (child.isDirectory) {
                        if (child.name == "data" && dir.name == "Android") continue
                        stack.add(child to depth + 1)
                    } else if (FileUtils.categoryFor(child) == category) {
                        results.add(child)
                        if (results.size >= maxResults) break
                    }
                }
            }
            results
        }

    suspend fun searchFiles(query: String, maxResults: Int = 500): List<File> =
        withContext(Dispatchers.IO) {
            val needle = query.lowercase()
            val results = mutableListOf<File>()
            val stack = ArrayDeque<Pair<File, Int>>()
            stack.add(internalRoot() to 0)
            while (stack.isNotEmpty() && results.size < maxResults) {
                coroutineContext.ensureActive()
                val (dir, depth) = stack.removeLast()
                if (depth > 8) continue
                val children = try { dir.listFiles() } catch (_: Exception) { null } ?: continue
                for (child in children) {
                    if (child.name.lowercase().contains(needle)) {
                        results.add(child)
                        if (results.size >= maxResults) break
                    }
                    if (child.isDirectory) {
                        if (child.name == "data" && dir.name == "Android") continue
                        stack.add(child to depth + 1)
                    }
                }
            }
            results
        }

    // ---------- ZIP ----------
    suspend fun zipFiles(files: List<File>, destDir: File): File? = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext null
        val zipName = if (files.size == 1) {
            val base = files[0].name.substringBeforeLast(".")
            "$base.zip"
        } else {
            "archive_${System.currentTimeMillis()}.zip"
        }
        val zipFile = FileUtils.uniqueFileIn(destDir, zipName)
        try {
            ZipOutputStream(zipFile.outputStream()).use { zip ->
                for (source in files) {
                    if (source.isDirectory) {
                        source.walkTopDown().forEach { f ->
                            val rel = source.toPath().relativize(f.toPath()).toString()
                            val entryName = if (rel.isEmpty()) "${source.name}/" else if (f.isDirectory) "${source.name}/$rel/" else "${source.name}/$rel"
                            zip.putNextEntry(ZipEntry(entryName))
                            if (f.isFile) f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    } else {
                        zip.putNextEntry(ZipEntry(source.name))
                        source.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            zipFile
        } catch (_: Exception) {
            zipFile.delete()
            null
        }
    }

    suspend fun unzip(zip: File, destDir: File): File? = withContext(Dispatchers.IO) {
        val base = zip.nameWithoutExtension
        val targetDir = FileUtils.uniqueFileIn(destDir, base)
        targetDir.mkdirs()
        try {
            ZipInputStream(zip.inputStream()).use { input ->
                var entry = input.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)
                    val destCanonical = targetDir.canonicalPath
                    if (!outFile.canonicalPath.startsWith(destCanonical + File.separator)) {
                        throw java.io.IOException("Unsafe ZIP entry")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out -> input.copyTo(out) }
                    }
                    entry = input.nextEntry
                }
            }
            targetDir
        } catch (_: Exception) {
            targetDir.deleteRecursively()
            null
        }
    }

    // ---------- Storage scan / analyzer ----------
    suspend fun scanStorage(
        root: File,
        maxDepth: Int = 8,
        onProgress: (scanned: Long) -> Unit = {}
    ): ScanResult = withContext(Dispatchers.IO) {
        val categories = mutableMapOf<FileCategory, CategoryStats>()
        val allFiles = mutableListOf<File>()
        var totalFiles = 0L
        var totalSize = 0L
        var scanned = 0L

        val stack = ArrayDeque<Pair<File, Int>>()
        stack.add(root to 0)
        while (stack.isNotEmpty()) {
            val (dir, depth) = stack.removeLast()
            if (depth > maxDepth) continue
            val children = try { dir.listFiles() } catch (_: Exception) { null } ?: continue
            for (child in children) {
                scanned++
                if (scanned % 200 == 0L) onProgress(scanned)
                if (child.isDirectory) {
                    // Skip app-private data subtree for speed — not user content.
                    if (child.name == "data" && dir.name == "Android") continue
                    stack.add(child to depth + 1)
                } else if (child.isFile) {
                    totalFiles++
                    val len = child.length()
                    totalSize += len
                    allFiles.add(child)
                    val cat = FileUtils.categoryFor(child)
                    val prev = categories[cat] ?: CategoryStats(cat)
                    categories[cat] = prev.copy(count = prev.count + 1, totalSize = prev.totalSize + len)
                }
            }
        }

        val largest = allFiles.sortedByDescending { it.length() }.take(50)
        val bySize = allFiles.filter { it.length() > 0 }.groupBy { it.length() }
            .filterValues { it.size > 1 }
        ScanResult(categories, totalFiles, totalSize, largest, bySize)
    }

    suspend fun findDuplicates(filesBySize: Map<Long, List<File>>): List<DuplicateGroup> =
        withContext(Dispatchers.IO) {
            val groups = mutableListOf<DuplicateGroup>()
            for ((size, files) in filesBySize) {
                if (files.size < 2) continue
                val byHash = files.groupBy { file ->
                    try {
                        computeHashBlocking(file, "MD5")
                    } catch (_: Exception) {
                        ""
                    }
                }
                for ((hash, dupes) in byHash) {
                    if (hash.isNotEmpty() && dupes.size > 1) {
                        groups.add(DuplicateGroup(hash, dupes, size))
                    }
                }
            }
            groups.sortedByDescending { it.wastedBytes }
        }

    private fun computeHashBlocking(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
