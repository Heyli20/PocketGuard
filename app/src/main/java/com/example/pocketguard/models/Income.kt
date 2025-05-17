package com.example.pocketguard.models

data class Income(
    val title: String,
    val amount: Double,
    val date: String,
    val description: String? = null,
    val category: String? = null
)

