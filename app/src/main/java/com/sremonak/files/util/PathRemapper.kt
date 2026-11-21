package com.sremonak.files.util

import java.io.File

object PathRemapper {
    /** Rewrite the path itself or a descendant, but never similarly-prefixed siblings. */
    fun relocate(path: String, oldPath: String, newPath: String): String {
        val old = oldPath.trimEnd(File.separatorChar)
        return when {
            path == old -> newPath
            path.startsWith(old + File.separator) -> newPath + path.substring(old.length)
            else -> path
        }
    }
}
