package com.davideagostini.summ.ui.settings.members

data class MembersUiState(
    val inviteEmail: String = "",
    val inviteRole: String = "member",
    val emailError: String? = null,
)
