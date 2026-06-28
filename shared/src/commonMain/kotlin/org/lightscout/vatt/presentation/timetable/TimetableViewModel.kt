package org.lightscout.vatt.presentation.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.core.session.UserSession
import org.lightscout.vatt.domain.model.Timetable
import org.lightscout.vatt.domain.usecase.GetTimetableUseCase
import org.lightscout.vatt.presentation.common.toUserMessage

data class TimetableUiState(
    val isLoading: Boolean = true,
    val timetable: Timetable? = null,
    val error: String? = null,
    val selectedDate: LocalDate? = null,
)

class TimetableViewModel(
    private val getTimetable: GetTimetableUseCase,
    private val userSession: UserSession,
) : ViewModel() {

    private val _state = MutableStateFlow(TimetableUiState())
    val state: StateFlow<TimetableUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        val clubId = userSession.homeClubId
        if (clubId == null) {
            _state.update { it.copy(isLoading = false, error = "No club associated with your account.") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getTimetable(clubId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        timetable = result.data,
                        error = null,
                        selectedDate = it.selectedDate ?: result.data.selectedDate,
                    )
                }
                is ApiResult.Failure ->
                    _state.update { it.copy(isLoading = false, error = result.error.toUserMessage()) }
            }
        }
    }

    /** null = show all days. */
    fun selectDate(date: LocalDate?) = _state.update { it.copy(selectedDate = date) }
}
