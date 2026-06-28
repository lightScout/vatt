package org.lightscout.vatt.data.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Clock
import org.lightscout.vatt.core.auth.SessionEvents
import org.lightscout.vatt.core.auth.TokenStore
import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.core.result.map
import org.lightscout.vatt.data.mapper.ManifestMapper
import org.lightscout.vatt.data.mapper.toDomain
import org.lightscout.vatt.data.mapper.toDomainOrNull
import org.lightscout.vatt.data.remote.api.VirginActiveApi
import org.lightscout.vatt.data.remote.toSession
import org.lightscout.vatt.domain.error.AppError
import org.lightscout.vatt.domain.model.BookingResult
import org.lightscout.vatt.domain.model.HomeManifest
import org.lightscout.vatt.domain.model.Timetable
import org.lightscout.vatt.domain.model.User
import org.lightscout.vatt.domain.repository.AuthRepository
import org.lightscout.vatt.domain.repository.BookingRepository
import org.lightscout.vatt.domain.repository.HomeRepository
import org.lightscout.vatt.domain.repository.ProfileRepository
import org.lightscout.vatt.domain.repository.TimetableRepository

class AuthRepositoryImpl(
    private val api: VirginActiveApi,
    private val tokenStore: TokenStore,
    private val sessionEvents: SessionEvents,
) : AuthRepository {

    override suspend fun login(username: String, password: String): ApiResult<User> {
        return when (val result = api.login(username, password)) {
            is ApiResult.Success -> {
                val dto = result.data
                tokenStore.save(dto.toSession(Clock.System.now()))
                val user = dto.user?.toDomain()
                if (user != null) {
                    ApiResult.Success(user)
                } else {
                    // Login didn't embed the profile — fall back to /me.
                    api.me().map { it.toDomain() }
                }
            }
            is ApiResult.Failure -> result
        }
    }

    override fun isLoggedIn(): Boolean = tokenStore.accessToken() != null
    override fun logout() = tokenStore.clear()
    override val sessionExpired: SharedFlow<Unit> get() = sessionEvents.expired
}

class ProfileRepositoryImpl(private val api: VirginActiveApi) : ProfileRepository {
    override suspend fun getProfile(): ApiResult<User> = api.me().map { it.toDomain() }
}

class HomeRepositoryImpl(
    private val api: VirginActiveApi,
    private val manifestMapper: ManifestMapper,
) : HomeRepository {
    override suspend fun getManifest(): ApiResult<HomeManifest> =
        api.homeManifest().map { manifestMapper.toDomain(it) }
}

class TimetableRepositoryImpl(private val api: VirginActiveApi) : TimetableRepository {
    override suspend fun getTimetable(clubId: String): ApiResult<Timetable> =
        api.timetable(clubId).map { it.toDomain() }
}

class BookingRepositoryImpl(private val api: VirginActiveApi) : BookingRepository {
    override suspend fun book(clubId: String, classId: String): ApiResult<BookingResult> =
        when (val r = api.book(clubId, classId)) {
            is ApiResult.Success ->
                r.data.toDomainOrNull()?.let { ApiResult.Success(it) }
                    ?: ApiResult.Failure(AppError.Serialization("Unparseable booking response"))
            is ApiResult.Failure -> r
        }

    override suspend fun cancel(clubId: String, classId: String): ApiResult<Unit> =
        api.cancel(clubId, classId)
}
