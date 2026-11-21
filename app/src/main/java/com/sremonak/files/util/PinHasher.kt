package com.sremonak.files.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Versioned, per-PIN salted password hashing; never stores the PIN itself. */
object PinHasher {
    private const val ITERATIONS = 210_000
    private const val PREFIX = "pbkdf2-sha256"

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val derived = derive(pin, salt, ITERATIONS)
        val encoder = Base64.getEncoder()
        return "$PREFIX\$$ITERATIONS\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(derived)}"
    }

    fun verify(pin: String, stored: String): Boolean = try {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != PREFIX) false else {
            val iterations = parts[1].toInt()
            // Reject malformed or attacker-controlled expensive hashes.
            if (iterations !in 100_000..1_000_000) false else {
                val decoder = Base64.getDecoder()
                val salt = decoder.decode(parts[2])
                val expected = decoder.decode(parts[3])
                salt.size == 16 && expected.size == 32 &&
                    MessageDigest.isEqual(expected, derive(pin, salt, iterations))
            }
        }
    } catch (_: Exception) { false }

    fun isModern(stored: String): Boolean = stored.startsWith("$PREFIX$")

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
