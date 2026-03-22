package com.davideagostini.summ.data.entity

data class Invite(
    val id: String = "",
    val email: String = "",
    val role: String = "member",
    val status: String = "pending",
)
