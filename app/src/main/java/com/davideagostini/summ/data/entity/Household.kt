package com.davideagostini.summ.data.entity

data class Household(
    val id: String,
    val name: String,
    val ownerId: String,
    val currency: String = "EUR",
)
