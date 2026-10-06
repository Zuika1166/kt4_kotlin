package com.example.dto

import com.example.model.Book
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    @SerialName("created")
    CREATED,

    @SerialName("updated")
    UPDATED,

    @SerialName("deleted")
    DELETED
}

@Serializable
data class BookNotification(
    val type: NotificationType,
    val book: Book
)
