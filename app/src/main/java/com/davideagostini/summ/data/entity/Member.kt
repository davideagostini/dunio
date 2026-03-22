package com.davideagostini.summ.data.entity

data class Member(
    val userId: String = "",
    val role: String = "member",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
)
