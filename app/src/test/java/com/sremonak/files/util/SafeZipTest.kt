package com.sremonak.files.util

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeZipTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private fun zip(name: String, entry: String, data: String): File {
        val file = temporaryFolder.newFile(name)
        ZipOutputStream(file.outputStream()).use { out ->
            out.putNextEntry(ZipEntry(entry))
            out.write(data.toByteArray())
            out.closeEntry()
        }
        return file
    }

    @Test fun extractsValidFile() {
        val file = zip("ok.zip", "folder/file.txt", "hello")
        val target = File(temporaryFolder.root, "output")
        assertTrue(SafeZip.extract(file, target))
        assertEquals("hello", File(target, "folder/file.txt").readText())
    }

    @Test fun rejectsTraversalAndDeletesPartialOutput() {
        val file = zip("bad.zip", "../escape.txt", "bad")
        val target = File(temporaryFolder.root, "output")
        assertFalse(SafeZip.extract(file, target))
        assertFalse(File(temporaryFolder.root, "escape.txt").exists())
        assertFalse(target.exists())
    }

    @Test fun rejectsOversizedArchive() {
        val file = zip("large.zip", "file.txt", "123456789")
        val target = File(temporaryFolder.root, "output")
        assertFalse(SafeZip.extract(file, target, maxBytes = 4))
        assertFalse(target.exists())
    }

    @Test fun neverOverwritesExistingDirectory() {
        val file = zip("ok.zip", "file.txt", "hello")
        val target = temporaryFolder.newFolder("existing")
        File(target, "keep").writeText("untouched")
        assertFalse(SafeZip.extract(file, target))
        assertEquals("untouched", File(target, "keep").readText())
    }
}
