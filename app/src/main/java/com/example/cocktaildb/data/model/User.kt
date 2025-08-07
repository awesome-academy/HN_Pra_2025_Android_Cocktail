package com.example.cocktaildb.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val loginMethod: LoginMethod = LoginMethod.EMAIL_PASSWORD,
    val profileImage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)