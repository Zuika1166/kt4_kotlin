package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateBookRequest(
    val title: String,
    val author: String,
    val year: Int
)

@Serializable
data class UpdateBookRequest(
    val title: String,
    val author: String,
    val year: Int
)
