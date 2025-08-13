package com.example.cocktaildb.data.model

enum class LoginMethod(val value: String) {
    EMAIL_PASSWORD("email_password"),
    GOOGLE("google");

    companion object {
        fun fromString(value: String): LoginMethod {
            return values().find { it.value == value } ?: EMAIL_PASSWORD
        }
    }
}
