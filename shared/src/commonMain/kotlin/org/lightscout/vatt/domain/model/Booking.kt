package org.lightscout.vatt.domain.model

/**
 * Result of a booking request. The mock uses a **single** endpoint that either books or — when the class
 * is full — places the user on the waitlist; the branch is signalled by [status] plus a non-null
 * [waitlistPosition].
 */
data class BookingResult(
    val bookingId: String,
    val status: BookingStatus,
    val waitlistPosition: Int?,
    val classSession: ClassSession,
) {
    val isWaitlisted: Boolean get() = status == BookingStatus.WAITLISTED
}

enum class BookingStatus {
    BOOKED, WAITLISTED, UNKNOWN;

    companion object {
        fun fromWire(raw: String?): BookingStatus = when (raw?.lowercase()) {
            "booked" -> BOOKED
            "waitlisted" -> WAITLISTED
            else -> UNKNOWN
        }
    }
}
