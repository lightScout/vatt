package org.lightscout.vatt.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Clock
import org.lightscout.vatt.core.auth.TokenStore
import org.lightscout.vatt.data.remote.dto.RefreshRequestDto
import org.lightscout.vatt.data.remote.dto.TokenPairDto

/**
 * Builds the single shared [HttpClient]. The engine is chosen automatically per platform (OkHttp on
 * Android, Darwin on iOS) since exactly one is on each target's classpath.
 *
 * Key behaviours:
 *  - **Bearer auth + refresh:** the access token is attached automatically; on 401 Ktor calls
 *    `refreshTokens`, which hits `/auth/refresh`. If refresh fails, the session is cleared and
 *    [onSessionExpired] fires so the app can route back to login.
 *  - **Retry is GET-only.** Idempotent reads are retried on transient 5xx / IO errors (the mock injects
 *    `ChaosFailure` constantly). Writes — booking POST and cancel DELETE — are NEVER auto-retried, to
 *    avoid double-booking / double-cancelling.
 */
fun createHttpClient(
    baseUrl: String,
    tokenStore: TokenStore,
    onSessionExpired: () -> Unit,
    enableLogging: Boolean = true,
): HttpClient = HttpClient {
    expectSuccess = false

    install(ContentNegotiation) { json(AppJson) }

    if (enableLogging) {
        install(Logging) { logger = SimpleLogger; level = LogLevel.INFO }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }

    install(HttpRequestRetry) {
        maxRetries = 3
        // Only retry idempotent GETs. Booking/cancel writes must never be silently repeated.
        retryIf { request, response ->
            request.method == HttpMethod.Get && response.status.value in 500..599
        }
        retryOnExceptionIf { request, _ ->
            request.method == HttpMethod.Get
        }
        exponentialDelay(base = 2.0, maxDelayMs = 4_000L)
    }

    install(Auth) {
        bearer {
            loadTokens {
                tokenStore.current()?.let { BearerTokens(it.accessToken, it.refreshToken ?: "") }
            }
            refreshTokens {
                val refresh = tokenStore.refreshToken()
                if (refresh.isNullOrBlank()) {
                    tokenStore.clear(); onSessionExpired(); return@refreshTokens null
                }
                val response = client.post("$baseUrl/auth/refresh") {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequestDto(refresh))
                }
                if (response.status.isSuccess()) {
                    val session = response.body<TokenPairDto>().toSession(Clock.System.now())
                    tokenStore.save(session)
                    BearerTokens(session.accessToken, session.refreshToken ?: refresh)
                } else {
                    tokenStore.clear(); onSessionExpired(); null
                }
            }
            // Attach the access token preemptively on every request (avoids an extra 401 round-trip).
            // Login has no token yet, and the refresh call is tagged separately, so this is safe.
            sendWithoutRequest { true }
        }
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}
