package org.lightscout.vatt.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.data.remote.dto.BookingResponseDto
import org.lightscout.vatt.data.remote.dto.HomeManifestDto
import org.lightscout.vatt.data.remote.dto.LoginRequestDto
import org.lightscout.vatt.data.remote.dto.TimetableDto
import org.lightscout.vatt.data.remote.dto.TokenPairDto
import org.lightscout.vatt.data.remote.dto.UserDto
import org.lightscout.vatt.data.remote.safeApiCall

/**
 * Thin HTTP layer — one function per endpoint, each returning a typed [ApiResult] of its DTO. No business
 * logic; repositories handle mapping and orchestration.
 */
class VirginActiveApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun login(username: String, password: String): ApiResult<TokenPairDto> =
        safeApiCall {
            client.post("$baseUrl/auth/login") { setBody(LoginRequestDto(username, password)) }
        }

    suspend fun me(): ApiResult<UserDto> =
        safeApiCall { client.get("$baseUrl/me") }

    suspend fun homeManifest(): ApiResult<HomeManifestDto> =
        safeApiCall { client.get("$baseUrl/home/manifest") }

    suspend fun timetable(clubId: String): ApiResult<TimetableDto> =
        safeApiCall {
            client.get {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("clubs", clubId, "classes", "timetable")
                }
            }
        }

    suspend fun book(clubId: String, classId: String): ApiResult<BookingResponseDto> =
        safeApiCall {
            client.post {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("clubs", clubId, "classes", classId, "bookings")
                }
                setBody("{}")
            }
        }

    suspend fun cancel(clubId: String, classId: String): ApiResult<Unit> =
        safeApiCall {
            client.delete {
                url {
                    takeFrom(baseUrl)
                    appendPathSegments("clubs", clubId, "classes", classId, "bookings")
                }
            }
        }
}
