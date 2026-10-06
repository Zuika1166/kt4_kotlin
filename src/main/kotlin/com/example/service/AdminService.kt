package com.example.service

import com.example.dto.UserResponse
import com.example.repository.UserRepository

class AdminService(
    private val userRepository: UserRepository
) {
    fun getUsers(): List<UserResponse> =
        userRepository.findAll().map { user ->
            UserResponse(
                id = user.id,
                login = user.login,
                role = user.role
            )
        }
}
