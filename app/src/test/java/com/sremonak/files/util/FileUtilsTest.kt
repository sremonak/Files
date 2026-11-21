package com.sremonak.files.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileUtilsTest {
    @Test fun formatsStorageUnitsCorrectly() {
        assertEquals("0 B", FileUtils.formatBytes(0))
        assertEquals("1.0 KB", FileUtils.formatBytes(1024))
        assertEquals("1.0 MB", FileUtils.formatBytes(1024 * 1024))
        assertEquals("1.0 GB", FileUtils.formatBytes(1024L * 1024 * 1024))
    }
}
