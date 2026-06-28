package org.lightscout.vatt.data.mapper

import kotlinx.datetime.LocalDate
import org.lightscout.vatt.data.remote.dto.BookingResponseDto
import org.lightscout.vatt.data.remote.dto.ClassInstanceDto
import org.lightscout.vatt.data.remote.dto.ClubDto
import org.lightscout.vatt.data.remote.dto.TimetableDto
import org.lightscout.vatt.data.remote.dto.UserDto
import org.lightscout.vatt.domain.model.BookingResult
import org.lightscout.vatt.domain.model.BookingStatus
import org.lightscout.vatt.domain.model.ClassSession
import org.lightscout.vatt.domain.model.ClassStatus
import org.lightscout.vatt.domain.model.ClassType
import org.lightscout.vatt.domain.model.Club
import org.lightscout.vatt.domain.model.MembershipTier
import org.lightscout.vatt.domain.model.Timetable
import org.lightscout.vatt.domain.model.TimetableDay
import org.lightscout.vatt.domain.model.User
import org.lightscout.vatt.domain.model.UserBookingStatus

fun UserDto.toDomain(): User = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    membershipTier = MembershipTier.fromWire(membershipTier),
    homeClub = homeClub?.toDomain() ?: Club(id = "", name = ""),
)

fun ClubDto.toDomain(): Club = Club(id = id, name = name)

/**
 * Maps a class instance, returning null when the start/end times are unparseable — the caller drops it
 * rather than rendering a broken row. Times are non-negotiable for a bookable class.
 */
fun ClassInstanceDto.toDomainOrNull(): ClassSession? {
    val start = parseZonedTimeOrNull(startsAt) ?: return null
    val end = parseZonedTimeOrNull(endsAt) ?: return null
    return ClassSession(
        classId = classId,
        clubId = clubId,
        title = title,
        trainer = trainer,
        type = ClassType.fromWire(type),
        startsAt = start,
        endsAt = end,
        spots = spots,
        available = available,
        waitlistCount = waitlistCount,
        status = ClassStatus.fromWire(status),
        userBookingStatus = UserBookingStatus.fromWire(userBookingStatus),
    )
}

fun TimetableDto.toDomain(): Timetable = Timetable(
    clubId = clubId,
    weekStart = parseLocalDateOrNull(weekStart) ?: parseLocalDateOrNull(selectedDate) ?: epochFallback(),
    weekEnd = parseLocalDateOrNull(weekEnd) ?: parseLocalDateOrNull(selectedDate) ?: epochFallback(),
    selectedDate = parseLocalDateOrNull(selectedDate) ?: epochFallback(),
    days = days.mapNotNull { day ->
        val date = parseLocalDateOrNull(day.date) ?: return@mapNotNull null
        TimetableDay(
            date = date,
            classes = day.classes.mapNotNull { it.toDomainOrNull() }
                .sortedBy { it.startsAt.instant },
        )
    },
)

fun BookingResponseDto.toDomainOrNull(): BookingResult? {
    val session = classInstance.toDomainOrNull() ?: return null
    return BookingResult(
        bookingId = bookingId,
        status = BookingStatus.fromWire(status),
        waitlistPosition = waitlistPosition,
        classSession = session,
    )
}

private fun parseLocalDateOrNull(raw: String?): LocalDate? =
    raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun epochFallback(): LocalDate = LocalDate(1970, 1, 1)
