package com.sremonak.files.data.model

import java.io.File

enum class FileCategory {
    IMAGE, VIDEO, AUDIO, DOCUMENT, PDF, APK, ARCHIVE, CODE, FONT, OTHER
}

enum class SortOption { NAME, SIZE, DATE_MODIFIED, DATE_CREATED, TYPE }

enum class ViewMode { LIST, GRID }

data class FileInfo(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val createdAt: Long,
    val mimeType: String?,
    val extension: String,
    val category: FileCategory,
    val isHidden: Boolean,
    val isFavorite: Boolean = false,
    val childCount: Int = 0
) {
    val path: String get() = file.absolutePath
}

data class MediaInfo(
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val bitrateKbps: Long = 0L
)

data class FileDetails(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val extension: String,
    val mimeType: String?,
    val createdAt: Long,
    val modifiedAt: Long,
    val accessedAt: Long,
    val permissions: String,
    val owner: String,
    val isHidden: Boolean,
    val isSymlink: Boolean,
    val linkTarget: String?,
    val volumeLabel: String,
    val media: MediaInfo = MediaInfo(),
    val exif: Map<String, String> = emptyMap()
)

data class TrashEntry(
    val id: String,
    val originalPath: String,
    val name: String,
    val size: Long,
    val deletedAt: Long,
    val isDirectory: Boolean
)

data class DuplicateGroup(
    val hash: String,
    val files: List<File>,
    val size: Long
) {
    val wastedBytes: Long get() = size * (files.size - 1)
}

data class CategoryStats(
    val category: FileCategory,
    val count: Long = 0,
    val totalSize: Long = 0
)

data class StorageStats(
    val totalBytes: Long,
    val availableBytes: Long,
    val label: String
) {
    val usedBytes: Long get() = totalBytes - availableBytes
    val usedFraction: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
}

data class ScanResult(
    val categories: Map<FileCategory, CategoryStats> = emptyMap(),
    val totalFiles: Long = 0,
    val totalSize: Long = 0,
    val largestFiles: List<File> = emptyList(),
    val filesBySize: Map<Long, List<File>> = emptyMap()
)

data class ClipboardState(
    val files: List<File> = emptyList(),
    val move: Boolean = false
) {
    val isActive: Boolean get() = files.isNotEmpty()
}
