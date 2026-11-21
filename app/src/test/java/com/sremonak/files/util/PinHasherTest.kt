package com.sremonak.files.util

import org.junit.Assert.*
import org.junit.Test

class PinHasherTest {
    @Test fun pinHashIsSaltedAndVerifies() {
        val first = PinHasher.hash("123456")
        val second = PinHasher.hash("123456")
        assertNotEquals(first, second)
        assertTrue(PinHasher.verify("123456", first))
        assertFalse(PinHasher.verify("123457", first))
    }

    @Test fun malformedHashesAreRejected() {
        assertFalse(PinHasher.verify("1234", "pbkdf2-sha256\$999999999\$bad\$bad"))
        assertFalse(PinHasher.verify("1234", "anything"))
    }
}
