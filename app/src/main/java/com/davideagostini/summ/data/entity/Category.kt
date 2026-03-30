package com.davideagostini.summ.data.entity

data class Category(
    val id: String = "",
    val name: String,
    val emoji: String,
    val type: String = "expense",
    val systemKey: String? = null,
    val usesDefaultTranslation: Boolean = false,
)
