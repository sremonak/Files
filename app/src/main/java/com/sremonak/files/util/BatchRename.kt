package com.sremonak.files.util

import java.io.File

/** Creates a conservative, non-overwriting numbered rename plan for selected regular files. */
object BatchRename {
    fun plan(files: List<File>, prefix: String): List<Pair<File, File>> {
        val clean = prefix.trim()
        require(clean.isNotEmpty() && clean != "." && clean != ".." &&
            clean.none { it == '/' || it == '\\' || it == '\n' || it == '\r' || it == '\u0000' }) {
            "Enter a valid name without path separators."
        }
        require(files.isNotEmpty()) { "Select files first." }
        val sorted = files.distinctBy { it.absolutePath }.sortedBy { it.absolutePath.lowercase() }
        require(sorted.size == files.size && sorted.all { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) }) {
            "Select existing files only; folders and symbolic links are not supported."
        }
        val width = maxOf(2, sorted.size.toString().length)
        val planned = sorted.mapIndexed { index, file ->
            val dot = file.name.lastIndexOf('.')
            val extension = if (dot > 0 && dot < file.name.lastIndex) file.name.substring(dot) else ""
            val name = "$clean ${String.format(java.util.Locale.ROOT, "%0${width}d", index + 1)}$extension"
            file to File(file.parentFile ?: error("Missing parent folder"), name)
        }
        require(planned.map { it.second.absolutePath }.toSet().size == planned.size) { "Duplicate destination names." }
        require(planned.none { (source, target) -> target.exists() && source.absolutePath != target.absolutePath }) {
            "A destination name already exists. Choose a different prefix."
        }
        return planned
    }
}
