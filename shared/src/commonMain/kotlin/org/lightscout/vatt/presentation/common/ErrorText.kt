package org.lightscout.vatt.presentation.common

import org.lightscout.vatt.domain.error.AppError

/** Maps a domain [AppError] to a concise, user-facing message. Centralised so wording is consistent. */
fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network -> "Couldn't reach the server. Check your connection and try again."
    is AppError.Transient -> "The service is busy right now. Please try again."
    AppError.Unauthorized -> "Your session needs refreshing. Please try again."
    AppError.SessionExpired -> "Your session has expired. Please sign in again."
    AppError.InvalidCredentials -> "Incorrect email or password."
    is AppError.ClassInPast -> "This class has already started and can't be booked."
    is AppError.AlreadyBooked ->
        if (waitlisted) "You're already on the waitlist for this class."
        else "You're already booked for this class."
    is AppError.BookingNotFound -> "We couldn't find that booking — it may already be cancelled."
    is AppError.NotFound -> "We couldn't find what you were looking for."
    is AppError.Api -> message ?: "Something went wrong. Please try again."
    is AppError.Serialization -> "We received an unexpected response. Please try again."
    is AppError.Unknown -> "Something went wrong. Please try again."
}
