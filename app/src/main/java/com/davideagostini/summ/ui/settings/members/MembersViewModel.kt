package com.davideagostini.summ.ui.settings.members

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Invite
import com.davideagostini.summ.data.entity.Member
import com.davideagostini.summ.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembersViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repository: MemberRepository,
) : ViewModel() {
    private val membersLoaded = MutableStateFlow(false)
    private val invitesLoaded = MutableStateFlow(false)

    val members: StateFlow<List<Member>> = repository.allMembers
        .onEach { membersLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val invites: StateFlow<List<Invite>> = repository.allInvites
        .onEach { invitesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(membersLoaded, invitesLoaded) { membersReady, invitesReady ->
        !membersReady || !invitesReady
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    fun updateInviteEmail(value: String) {
        _uiState.update { it.copy(inviteEmail = value, emailError = null) }
    }

    fun updateInviteRole(value: String) {
        _uiState.update { it.copy(inviteRole = value) }
    }

    fun saveInvite() {
        val email = _uiState.value.inviteEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = appContext.getString(R.string.members_invite_email_required)) }
            return
        }
        viewModelScope.launch {
            repository.createInvite(email = email, role = _uiState.value.inviteRole)
            _uiState.update { MembersUiState() }
        }
    }
}
