package org.lightscout.vatt.domain.usecase

import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.domain.model.BookingResult
import org.lightscout.vatt.domain.model.HomeManifest
import org.lightscout.vatt.domain.model.Timetable
import org.lightscout.vatt.domain.model.User
import org.lightscout.vatt.domain.repository.AuthRepository
import org.lightscout.vatt.domain.repository.BookingRepository
import org.lightscout.vatt.domain.repository.HomeRepository
import org.lightscout.vatt.domain.repository.TimetableRepository

/**
 * Use cases are the application's verbs. Most here are thin delegations to a repository — deliberately so:
 * the seam exists for testability and so that business rules (e.g. [IsWithinCancellationWindowUseCase],
 * and the book-vs-waitlist interpretation) have an obvious home, even where there's no rule *yet*.
 */

class LoginUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): ApiResult<User> {
        val u = username.trim()
        return auth.login(u, password)
    }
}

class GetHomeManifestUseCase(private val home: HomeRepository) {
    suspend operator fun invoke(): ApiResult<HomeManifest> = home.getManifest()
}

class GetTimetableUseCase(private val timetable: TimetableRepository) {
    suspend operator fun invoke(clubId: String): ApiResult<Timetable> = timetable.getTimetable(clubId)
}

class BookClassUseCase(private val booking: BookingRepository) {
    suspend operator fun invoke(clubId: String, classId: String): ApiResult<BookingResult> =
        booking.book(clubId, classId)
}

class CancelBookingUseCase(private val booking: BookingRepository) {
    suspend operator fun invoke(clubId: String, classId: String): ApiResult<Unit> =
        booking.cancel(clubId, classId)
}
