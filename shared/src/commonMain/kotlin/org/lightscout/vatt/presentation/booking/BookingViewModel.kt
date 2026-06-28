package org.lightscout.vatt.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lightscout.vatt.core.cache.ClassCache
import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.domain.model.BookingResult
import org.lightscout.vatt.domain.model.ClassSession
import org.lightscout.vatt.domain.reminder.ReminderOutcome
import org.lightscout.vatt.domain.usecase.BookClassUseCase
import org.lightscout.vatt.domain.usecase.CancelBookingUseCase
import org.lightscout.vatt.domain.usecase.IsWithinCancellationWindowUseCase
import org.lightscout.vatt.domain.usecase.SetReminderUseCase
import org.lightscout.vatt.presentation.common.toUserMessage

/**
 * Drives the class detail + booking flow. The state machine is explicit and lives entirely here so the
 * Composables stay dumb:
 *  - book a class, or (if it's full) confirm joining the waitlist;
 *  - show a confirmation with the booking id / waitlist position;
 *  - cancel, with a 12-hour forfeit warning when applicable;
 *  - set a local reminder / add to calendar.
 */
data class BookingUiState(
    val classSession: ClassSession? = null,
    val notFound: Boolean = false,
    val isProcessing: Boolean = false,
    val result: BookingResult? = null,
    val cancelled: Boolean = false,
    val error: String? = null,
    /** Showing the "class is full — join the waitlist?" confirmation. */
    val waitlistPrompt: Boolean = false,
    /** Showing the within-12h forfeit warning before cancelling. */
    val cancelForfeitPrompt: Boolean = false,
    val reminderFeedback: String? = null,
) {
    /** The class as currently known (post-booking result takes precedence over the cached session). */
    val current: ClassSession? get() = result?.classSession ?: classSession
    val hasReservation: Boolean
        get() = result != null || current?.hasReservation == true
}

class BookingViewModel(
    private val classCache: ClassCache,
    private val bookClass: BookClassUseCase,
    private val cancelBooking: CancelBookingUseCase,
    private val isWithinCancellationWindow: IsWithinCancellationWindowUseCase,
    private val setReminder: SetReminderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    fun bind(classId: String) {
        val session = classCache.get(classId)
        _state.update { it.copy(classSession = session, notFound = session == null) }
    }

    // --- Booking -----------------------------------------------------------------------------------

    fun onBookClicked() {
        val session = _state.value.current ?: return
        if (session.isFull && !session.hasReservation) {
            // Full class → confirm the waitlist intent before calling the API.
            _state.update { it.copy(waitlistPrompt = true) }
        } else {
            performBook(session)
        }
    }

    fun confirmWaitlist() {
        _state.update { it.copy(waitlistPrompt = false) }
        _state.value.current?.let { performBook(it) }
    }

    fun dismissWaitlist() = _state.update { it.copy(waitlistPrompt = false) }

    private fun performBook(session: ClassSession) {
        _state.update { it.copy(isProcessing = true, error = null) }
        viewModelScope.launch {
            // Single-shot: booking writes are never auto-retried (see HttpClient retry policy).
            when (val r = bookClass(session.clubId, session.classId)) {
                is ApiResult.Success ->
                    _state.update { it.copy(isProcessing = false, result = r.data, error = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(isProcessing = false, error = r.error.toUserMessage()) }
            }
        }
    }

    // --- Cancellation ------------------------------------------------------------------------------

    fun onCancelClicked() {
        val session = _state.value.current ?: return
        if (isWithinCancellationWindow(session)) {
            _state.update { it.copy(cancelForfeitPrompt = true) }
        } else {
            performCancel(session)
        }
    }

    fun confirmCancel() {
        _state.update { it.copy(cancelForfeitPrompt = false) }
        _state.value.current?.let { performCancel(it) }
    }

    fun dismissCancelForfeit() = _state.update { it.copy(cancelForfeitPrompt = false) }

    private fun performCancel(session: ClassSession) {
        _state.update { it.copy(isProcessing = true, error = null) }
        viewModelScope.launch {
            when (val r = cancelBooking(session.clubId, session.classId)) {
                is ApiResult.Success ->
                    _state.update {
                        it.copy(isProcessing = false, cancelled = true, result = null, error = null)
                    }
                is ApiResult.Failure ->
                    _state.update { it.copy(isProcessing = false, error = r.error.toUserMessage()) }
            }
        }
    }

    /** Whether cancelling the current class would fall inside the 12h forfeit window (for inline hints). */
    fun isWithinForfeitWindow(): Boolean =
        _state.value.current?.let { isWithinCancellationWindow(it) } ?: false

    // --- Reminders ---------------------------------------------------------------------------------

    fun setLocalReminder() = runReminder { booking -> setReminder.reminder(booking) }
    fun addToCalendar() = runReminder { booking -> setReminder.calendar(booking) }

    private fun runReminder(action: suspend (BookingResult) -> ReminderOutcome) {
        val booking = _state.value.result ?: return
        viewModelScope.launch {
            val message = when (action(booking)) {
                ReminderOutcome.Scheduled -> "Reminder set."
                ReminderOutcome.NotSupported -> "Reminders aren't available on this platform yet."
                ReminderOutcome.PermissionDenied -> "Permission needed to set a reminder."
                is ReminderOutcome.Failed -> "Couldn't set the reminder."
            }
            _state.update { it.copy(reminderFeedback = message) }
        }
    }

    fun clearReminderFeedback() = _state.update { it.copy(reminderFeedback = null) }
    fun clearError() = _state.update { it.copy(error = null) }
}
