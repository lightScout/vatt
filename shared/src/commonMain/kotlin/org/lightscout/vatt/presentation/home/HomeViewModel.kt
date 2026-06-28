package org.lightscout.vatt.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lightscout.vatt.core.result.ApiResult
import org.lightscout.vatt.core.session.UserSession
import org.lightscout.vatt.domain.model.HomeManifest
import org.lightscout.vatt.domain.usecase.GetHomeManifestUseCase
import org.lightscout.vatt.presentation.common.toUserMessage

data class HomeUiState(
    val isLoading: Boolean = true,
    val manifest: HomeManifest? = null,
    val error: String? = null,
    val clubId: String? = null,
    val firstName: String? = null,
)

class HomeViewModel(
    private val getManifest: GetHomeManifestUseCase,
    private val userSession: UserSession,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(clubId = userSession.homeClubId, firstName = userSession.user?.firstName)
        }
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getManifest()) {
                is ApiResult.Success ->
                    _state.update { it.copy(isLoading = false, manifest = result.data, error = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(isLoading = false, error = result.error.toUserMessage()) }
            }
        }
    }
}
