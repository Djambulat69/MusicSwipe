package com.isaev.musicswipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.random.Random

object Pkce {
    private const val DEFAULT_CODE_VERIFIER_LENGTH = 100
    private const val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    suspend fun generateCodeVerifier(length: Int = DEFAULT_CODE_VERIFIER_LENGTH): String =
        withContext(Dispatchers.Default) {
            buildString(length) {
                repeat(length) {
                    ensureActive()
                    append(possible.random(SecureRandomKotlin()))
                }
            }
        }

    fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(codeVerifier.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
            .dropLast(1)
            .replace('+', '-')
            .replace('/', '_')
    }

    private class SecureRandomKotlin() : Random() {
        private val secureRandom = SecureRandom.getInstanceStrong()

        override fun nextBits(bitCount: Int): Int {
            return secureRandom.nextInt()
        }
    }
}
