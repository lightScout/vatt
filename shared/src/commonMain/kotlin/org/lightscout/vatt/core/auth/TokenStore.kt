package org.lightscout.vatt.core.auth

import kotlin.time.Clock
import kotlin.time.Instant

/** The session tokens plus the computed absolute expiry of the access token. */
data class Session(
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiry: Instant?,
)

/**
 * Holds the current session. Kept behind an interface so the storage strategy is swappable: this
 * assessment uses an in-memory implementation (the mock wipes all state on restart, so persistence has
 * little payoff), but a DataStore/Keychain-backed version could drop in without touching callers.
 */
interface TokenStore {
    fun save(session: Session)
    fun current(): Session?
    fun accessToken(): String?
    fun refreshToken(): String?
    fun clear()

    /** True when there is no access token, or it has expired (with a small safety skew). */
    fun isAccessTokenExpired(now: Instant = Clock.System.now()): Boolean
}

class InMemoryTokenStore : TokenStore {
    private var session: Session? = null

    override fun save(session: Session) { this.session = session }
    override fun current(): Session? = session
    override fun accessToken(): String? = session?.accessToken
    override fun refreshToken(): String? = session?.refreshToken
    override fun clear() { session = null }

    override fun isAccessTokenExpired(now: Instant): Boolean {
        val expiry = session?.accessTokenExpiry ?: return session?.accessToken == null
        return now >= expiry
    }
}
