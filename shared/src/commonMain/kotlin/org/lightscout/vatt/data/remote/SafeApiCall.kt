package org.lightscout.vatt.data.remote

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.data.remote.dto.ApiErrorDto
import org.lightscout.vatt.domain.error.AppError

/**
 * Wraps an HTTP call so every outcome becomes a typed [ApiResult]: a decoded body on success, or a mapped
 * [AppError] on a non-2xx response or thrown exception. Keeps try/catch and status handling out of the
 * repositories.
 */
suspend inline fun <reified T> safeApiCall(crossinline block: suspend () -> HttpResponse): ApiResult<T> {
    return try {
        val response = block()
        if (response.status.isSuccess()) {
            ApiResult.Success(response.body<T>())
        } else {
            ApiResult.Failure(mapErrorResponse(response))
        }
    } catch (e: CancellationException) {
        throw e // never swallow coroutine cancellation
    } catch (e: SerializationException) {
        ApiResult.Failure(AppError.Serialization(e.message))
    } catch (e: Throwable) {
        // Most remaining failures at this layer are connectivity/timeout related.
        ApiResult.Failure(AppError.Network(e.message))
    }
}

/** Maps the mock's error envelope + HTTP status to a domain [AppError]. */
suspend fun mapErrorResponse(response: HttpResponse): AppError {
    val dto: ApiErrorDto? = runCatching { response.body<ApiErrorDto>() }.getOrNull()
    val rid = dto?.requestId
    return when (dto?.error) {
        "ChaosFailure" -> AppError.Transient(rid, dto.message)
        "InvalidCredentials" -> AppError.InvalidCredentials
        "InvalidRefreshToken" -> AppError.SessionExpired
        "ClassInPast" -> AppError.ClassInPast(rid)
        "AlreadyBooked" -> AppError.AlreadyBooked(waitlisted = false, requestId = rid)
        "AlreadyWaitlisted" -> AppError.AlreadyBooked(waitlisted = true, requestId = rid)
        "BookingNotFound" -> AppError.BookingNotFound(rid)
        "ClassNotFound", "ClubNotFound", "UserNotFound" -> AppError.NotFound(dto.error, rid)
        "DateOutOfRange", "InvalidDate" -> AppError.Api(dto.error, dto.message, rid)
        else -> when (response.status.value) {
            401 -> AppError.Unauthorized
            in 500..599 -> AppError.Transient(rid, dto?.message)
            else -> AppError.Api(dto?.code ?: dto?.error, dto?.message, rid)
        }
    }
}
