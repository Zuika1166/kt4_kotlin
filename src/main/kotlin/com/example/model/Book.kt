package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: Long,
    val title: String,
    val author: String,
    val year: Int
)
