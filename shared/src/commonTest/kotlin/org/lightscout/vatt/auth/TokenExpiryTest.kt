package org.lightscout.vatt.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.lightscout.vatt.core.auth.InMemoryTokenStore
import org.lightscout.vatt.data.remote.dto.TokenPairDto
import org.lightscout.vatt.data.remote.toSession

class TokenExpiryTest {

    private val now = Instant.parse("2026-06-28T12:00:00Z")

    @Test
    fun expiryIsComputedWithSafetySkew() {
        // expiresIn 300s, minus the 30s safety skew → token treated as expiring at now+270s.
        val session = TokenPairDto(accessToken = "a", refreshToken = "r", expiresIn = 300).toSession(now)
        assertEquals(now + 270.seconds, session.accessTokenExpiry)
    }

    @Test
    fun tokenIsValidBeforeExpiry_andExpiredAfter() {
        val store = InMemoryTokenStore()
        store.save(TokenPairDto(accessToken = "a", refreshToken = "r", expiresIn = 300).toSession(now))

        assertFalse(store.isAccessTokenExpired(now + 200.seconds)) // before the 270s skewed expiry
        assertTrue(store.isAccessTokenExpired(now + 280.seconds))  // after it → should refresh
    }

    @Test
    fun noSession_isConsideredExpired() {
        val store = InMemoryTokenStore()
        assertTrue(store.isAccessTokenExpired(now))
    }

    @Test
    fun clearRemovesSession() {
        val store = InMemoryTokenStore()
        store.save(TokenPairDto(accessToken = "a", refreshToken = "r", expiresIn = 300).toSession(now))
        assertTrue(store.accessToken() != null)
        store.clear()
        assertTrue(store.accessToken() == null)
        assertTrue(store.refreshToken() == null)
    }
}
