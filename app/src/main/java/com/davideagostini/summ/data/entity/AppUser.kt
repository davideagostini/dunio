package com.davideagostini.summ.data.entity

data class AppUser(
    val uid: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val householdId: String? = null,
)
