package com.davideagostini.summ.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SessionUiState(
    val isSubmitting: Boolean = false,
    val isHouseholdTransitioning: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    @param:ApplicationContext
    private val appContext: Context,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun signInWithGoogle(context: Context) {
        launchAction(awaitSessionSettled = true) {
            sessionRepository.signInWithGoogle(context)
        }
    }

    fun createHousehold(name: String) {
        launchAction(awaitReady = true, householdTransition = true) {
            sessionRepository.createHousehold(name)
        }
    }

    fun joinHousehold(householdId: String) {
        launchAction(awaitReady = true, householdTransition = true) {
            sessionRepository.joinHousehold(householdId)
        }
    }

    fun signOut() {
        launchAction {
            sessionRepository.signOut()
        }
    }

    fun updateHouseholdCurrency(currency: String) {
        launchAction {
            sessionRepository.updateHouseholdCurrency(currency)
        }
    }

    private fun launchAction(
        awaitReady: Boolean = false,
        awaitSessionSettled: Boolean = false,
        householdTransition: Boolean = false,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    isHouseholdTransitioning = householdTransition,
                    errorMessage = null,
                )
            }
            try {
                action()
                if (awaitSessionSettled) {
                    withTimeoutOrNull(10_000) {
                        sessionState.firstOrNull { state ->
                            state is SessionState.Ready ||
                                state is SessionState.NeedsHousehold ||
                                state is SessionState.ConfigurationError
                        }
                    }
                }
                if (awaitReady) {
                    withTimeoutOrNull(10_000) {
                        sessionState.filterIsInstance<SessionState.Ready>().first()
                    }
                }
                _uiState.update { it.copy(isSubmitting = false, isHouseholdTransitioning = false) }
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isHouseholdTransitioning = false,
                        errorMessage = throwable.message ?: appContext.getString(R.string.session_generic_error),
                    )
                }
            }
        }
    }
}
