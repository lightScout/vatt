package org.lightscout.vatt.domain.error

/**
 * Domain-level error taxonomy. The data layer translates HTTP failures and the mock's error envelope
 * (`{ error, message, code?, requestId }`) into these so the presentation layer can react with tailored
 * UX without ever knowing about Ktor or HTTP status codes.
 *
 * The [requestId] is carried through where available — it's the single most useful thing to quote when
 * reporting a flaky call to the backend team.
 */
sealed class AppError(open val requestId: String? = null) {

    /** No connectivity / host unreachable / socket timeout. Safe to retry idempotent reads. */
    data class Network(val cause: String? = null) : AppError()

    /** The mock's injected `ChaosFailure` (deliberate transient failure). Retryable for reads. */
    data class Transient(override val requestId: String? = null, val message: String? = null) :
        AppError(requestId)

    /** 401 on a normal call — access token rejected. Handled by the auth refresh flow first. */
    data object Unauthorized : AppError()

    /** Refresh token itself is invalid/expired → the session is over; force logout. */
    data object SessionExpired : AppError()

    /** Bad username/password at login. */
    data object InvalidCredentials : AppError()

    /** Booking a class that has already started/finished (`ClassInPast`, 422). */
    data class ClassInPast(override val requestId: String? = null) : AppError(requestId)

    /** User already holds a booking/waitlist spot for this class (`AlreadyBooked`/`AlreadyWaitlisted`). */
    data class AlreadyBooked(val waitlisted: Boolean, override val requestId: String? = null) :
        AppError(requestId)

    /** Trying to cancel a booking the server doesn't know about (`BookingNotFound`, 404). */
    data class BookingNotFound(override val requestId: String? = null) : AppError(requestId)

    /** A resource (club/class/user) wasn't found, or a date was out of range. */
    data class NotFound(val what: String? = null, override val requestId: String? = null) :
        AppError(requestId)

    /** A 4xx we recognise by envelope but don't special-case, carrying the server's message. */
    data class Api(
        val code: String?,
        val message: String?,
        override val requestId: String? = null,
    ) : AppError(requestId)

    /** Response couldn't be parsed — shape drift beyond what defensive decoding tolerated. */
    data class Serialization(val cause: String? = null) : AppError()

    /** Anything not otherwise classified. */
    data class Unknown(val cause: String? = null) : AppError()
}
