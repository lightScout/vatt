package org.lightscout.vatt.data.remote

import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds
import org.lightscout.vatt.core.auth.Session
import org.lightscout.vatt.data.remote.dto.TokenPairDto

/** Small skew so we treat the token as expired slightly early and refresh proactively. */
private val EXPIRY_SKEW = 30.seconds

fun TokenPairDto.toSession(now: Instant): Session {
    val expiry = expiresIn?.let { secs ->
        now + (secs.seconds - EXPIRY_SKEW).coerceAtLeast(0.seconds)
    }
    return Session(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiry = expiry,
    )
}
