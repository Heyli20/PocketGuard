package com.example.pocketguard.models

data class User(
    val name: String,
    val phone: String,
    val email: String,
    val password: String?,
    val currency: String = "USD",
    val imageUri: String? = null
) 
