package com.example.service

import com.example.config.AdminCredentials
import com.example.config.JwtConfig
import com.example.dto.LoginRequest
import com.example.dto.RegisterRequest
import com.example.dto.TokenResponse
import com.example.dto.UserResponse
import com.example.error.ConflictException
import com.example.error.UnauthorizedException
import com.example.error.ValidationException
import com.example.model.UserRole
import com.example.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthService(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) {
    fun register(request: RegisterRequest): UserResponse {
        val login = request.login.trim()

        if (login.length < 3) {
            throw ValidationException("Login must contain at least 3 characters")
        }

        if (request.password.length < 6) {
            throw ValidationException("Password must contain at least 6 characters")
        }

        if (userRepository.findByLogin(login) != null) {
            throw ConflictException("Login already exists")
        }

        val user = userRepository.create(
            login = login,
            passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt()),
            role = UserRole.USER
        )

        return UserResponse(user.id, user.login, user.role)
    }

    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByLogin(request.login.trim())
            ?: throw UnauthorizedException("Invalid login or password")

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid login or password")
        }

        return TokenResponse(
            token = jwtConfig.createToken(user),
            expiresInSeconds = jwtConfig.expiresInSeconds,
            role = user.role
        )
    }

    fun me(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            ?: throw UnauthorizedException("User not found")

        return UserResponse(user.id, user.login, user.role)
    }

    fun ensureAdmin(credentials: AdminCredentials) {
        if (userRepository.findByLogin(credentials.login) != null) {
            return
        }

        userRepository.create(
            login = credentials.login,
            passwordHash = BCrypt.hashpw(credentials.password, BCrypt.gensalt()),
            role = UserRole.ADMIN
        )
    }
}
