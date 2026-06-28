package org.lightscout.vatt.domain.repository

import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.domain.model.BookingResult
import org.lightscout.vatt.domain.model.HomeManifest
import org.lightscout.vatt.domain.model.Timetable
import org.lightscout.vatt.domain.model.User

/**
 * Domain-facing contracts. Implementations live in the data layer; the domain (and therefore the use
 * cases and ViewModels) depend only on these interfaces, never on Ktor or DTOs.
 */

interface AuthRepository {
    /** Logs in and stores the resulting session tokens. */
    suspend fun login(username: String, password: String): ApiResult<User>

    /** Whether a session is currently held (tokens present). */
    fun isLoggedIn(): Boolean

    /** Clears the session (local only). */
    fun logout()

    /** Emits when the session ends unexpectedly (refresh failed) so the UI can route to login. */
    val sessionExpired: kotlinx.coroutines.flow.SharedFlow<Unit>
}

interface ProfileRepository {
    suspend fun getProfile(): ApiResult<User>
}

interface HomeRepository {
    suspend fun getManifest(): ApiResult<HomeManifest>
}

interface TimetableRepository {
    suspend fun getTimetable(clubId: String): ApiResult<Timetable>
}

interface BookingRepository {
    /** Book or (if full) join the waitlist for a class. Single-shot — never auto-retried. */
    suspend fun book(clubId: String, classId: String): ApiResult<BookingResult>

    /** Cancel a booking or leave a waitlist. Single-shot — never auto-retried. */
    suspend fun cancel(clubId: String, classId: String): ApiResult<Unit>
}
