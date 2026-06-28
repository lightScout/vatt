package org.lightscout.vatt.domain.model

/**
 * A single bookable class instance. Mirrors the server's `ClassInstance` but in pure-domain terms:
 * enums instead of raw strings, [ZonedTime] instead of ISO strings.
 */
data class ClassSession(
    val classId: String,
    val clubId: String,
    val title: String,
    val trainer: String,
    val type: ClassType,
    val startsAt: ZonedTime,
    val endsAt: ZonedTime,
    val spots: Int,
    val available: Int,
    val waitlistCount: Int,
    val status: ClassStatus,
    val userBookingStatus: UserBookingStatus,
) {
    val isFull: Boolean get() = status == ClassStatus.FULL || available <= 0
    val isBookedByUser: Boolean get() = userBookingStatus == UserBookingStatus.BOOKED
    val isWaitlistedByUser: Boolean get() = userBookingStatus == UserBookingStatus.WAITLISTED
    val hasReservation: Boolean get() = isBookedByUser || isWaitlistedByUser
}

/** Known class categories. [UNKNOWN] keeps the timetable rendering when the server adds a new type. */
enum class ClassType(val imageRef: String) {
    GROUP_WORKOUT("groupWorkout"),
    YOGA("yoga"),
    SPIN("spin"),
    PILATES("pilates"),
    HIIT("hiit"),
    SWIMMING("swimming"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(raw: String?): ClassType = when (raw?.lowercase()) {
            "groupworkout" -> GROUP_WORKOUT
            "yoga" -> YOGA
            "spin" -> SPIN
            "pilates" -> PILATES
            "hiit" -> HIIT
            "swimming" -> SWIMMING
            else -> UNKNOWN
        }
    }
}

enum class ClassStatus {
    AVAILABLE, FULL, CANCELLED, UNKNOWN;

    companion object {
        fun fromWire(raw: String?): ClassStatus = when (raw?.lowercase()) {
            "available" -> AVAILABLE
            "full" -> FULL
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

enum class UserBookingStatus {
    NONE, BOOKED, WAITLISTED, UNKNOWN;

    companion object {
        fun fromWire(raw: String?): UserBookingStatus = when (raw?.lowercase()) {
            "none" -> NONE
            "booked" -> BOOKED
            "waitlisted" -> WAITLISTED
            else -> UNKNOWN
        }
    }
}
