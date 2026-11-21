package com.sremonak.files.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PathRemapperTest {
    @Test fun relocatesOnlyTheTargetAndItsDescendants() {
        assertEquals("/storage/new", PathRemapper.relocate("/storage/old", "/storage/old", "/storage/new"))
        assertEquals("/storage/new/a.jpg", PathRemapper.relocate("/storage/old/a.jpg", "/storage/old", "/storage/new"))
        assertEquals("/storage/older/a.jpg", PathRemapper.relocate("/storage/older/a.jpg", "/storage/old", "/storage/new"))
    }
}
