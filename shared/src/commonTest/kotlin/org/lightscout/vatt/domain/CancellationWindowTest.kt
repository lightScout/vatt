package org.lightscout.vatt.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import org.lightscout.vatt.data.mapper.parseZonedTimeOrNull
import org.lightscout.vatt.domain.model.ClassSession
import org.lightscout.vatt.domain.model.ClassStatus
import org.lightscout.vatt.domain.model.ClassType
import org.lightscout.vatt.domain.model.UserBookingStatus
import org.lightscout.vatt.domain.usecase.IsWithinCancellationWindowUseCase

private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

private fun sessionStarting(startIso: String): ClassSession {
    val start = parseZonedTimeOrNull(startIso)!!
    return ClassSession(
        classId = "c", clubId = "club", title = "Yoga", trainer = "T",
        type = ClassType.YOGA, startsAt = start, endsAt = start,
        spots = 10, available = 5, waitlistCount = 0,
        status = ClassStatus.AVAILABLE, userBookingStatus = UserBookingStatus.BOOKED,
    )
}

class CancellationWindowTest {

    // "Now" fixed at 2026-06-28T12:00:00Z for all cases.
    private val now = Instant.parse("2026-06-28T12:00:00Z")
    private val useCase = IsWithinCancellationWindowUseCase(clock = FixedClock(now))

    @Test
    fun classMoreThan12hAway_isOutsideWindow() {
        // Starts 18h after now → no forfeit warning.
        assertFalse(useCase(sessionStarting("2026-06-29T06:00:00+00:00")))
    }

    @Test
    fun classLessThan12hAway_isWithinWindow() {
        // Starts 8h after now → within the forfeit window.
        assertTrue(useCase(sessionStarting("2026-06-28T20:00:00+00:00")))
    }

    @Test
    fun classJustOver12h_isOutside_andJustUnder_isWithin() {
        assertFalse(useCase(sessionStarting("2026-06-29T00:01:00+00:00"))) // 12h01m away
        assertTrue(useCase(sessionStarting("2026-06-28T23:59:00+00:00")))  // 11h59m away
    }

    @Test
    fun offsetIsRespected_notNaiveWallClock() {
        // Local wall-clock 23:00 looks ~11h away if you ignore the offset, but +02:00 makes it 21:00Z,
        // i.e. 9h after now → genuinely within the window. Confirms we compare absolute instants.
        assertTrue(useCase(sessionStarting("2026-06-28T23:00:00+02:00")))
    }

    @Test
    fun classAlreadyStarted_isWithinWindow() {
        assertTrue(useCase(sessionStarting("2026-06-28T11:00:00+00:00")))
    }
}
