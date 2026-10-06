package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("user")
    USER,

    @SerialName("admin")
    ADMIN
}

data class User(
    val id: Long,
    val login: String,
    val passwordHash: String,
    val role: UserRole
)
