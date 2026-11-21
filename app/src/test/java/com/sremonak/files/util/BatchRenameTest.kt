package com.sremonak.files.util

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BatchRenameTest {
    @get:Rule val dir = TemporaryFolder()

    @Test fun numberedPlanPreservesExtensionsAndDoesNotRenameYet() {
        val a = dir.newFile("a.JPG")
        val b = dir.newFile("b.txt")
        val plan = BatchRename.plan(listOf(b, a), "Holiday")
        assertEquals(listOf("Holiday 01.JPG", "Holiday 02.txt"), plan.map { it.second.name })
        assertTrue(a.exists())
        assertTrue(b.exists())
    }

    @Test fun refusesCollisionWithoutTouchingFiles() {
        val a = dir.newFile("a.txt")
        val existing = dir.newFile("Report 01.txt").apply { writeText("important") }
        assertThrows(IllegalArgumentException::class.java) { BatchRename.plan(listOf(a), "Report") }
        assertTrue(a.exists())
        assertEquals("important", existing.readText())
    }

    @Test fun refusesPathTraversalAndFolders() {
        val a = dir.newFile("a.txt")
        assertThrows(IllegalArgumentException::class.java) { BatchRename.plan(listOf(a), "../escape") }
        assertThrows(IllegalArgumentException::class.java) { BatchRename.plan(listOf(File(dir.root, ".")), "name") }
    }
}
