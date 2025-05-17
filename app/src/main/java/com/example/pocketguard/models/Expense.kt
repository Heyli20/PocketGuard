package com.example.pocketguard.models

data class Expense(
    val title: String,
    val amount: Double,
    val date: String,
    val category: String,
    val description: String? = null
)


