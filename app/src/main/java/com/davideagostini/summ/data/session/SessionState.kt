package com.davideagostini.summ.data.session

import com.davideagostini.summ.data.entity.AppUser
import com.davideagostini.summ.data.entity.Household

sealed interface SessionState {
    data object Loading : SessionState
    data class ConfigurationError(val message: String) : SessionState
    data object SignedOut : SessionState
    data class NeedsHousehold(val user: AppUser) : SessionState
    data class Ready(val user: AppUser, val household: Household) : SessionState
}
