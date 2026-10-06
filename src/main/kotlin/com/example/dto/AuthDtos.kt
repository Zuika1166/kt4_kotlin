package com.example.dto

import com.example.model.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class UserResponse(
    val id: Long,
    val login: String,
    val role: UserRole
)

@Serializable
data class TokenResponse(
    val token: String,
    val expiresInSeconds: Long,
    val role: UserRole
)

@Serializable
data class ErrorResponse(
    val message: String
)
