package com.example.realtime

import com.example.dto.BookNotification
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class NotificationHub {
    private val sessions =
        ConcurrentHashMap.newKeySet<DefaultWebSocketServerSession>()

    fun add(session: DefaultWebSocketServerSession) {
        sessions.add(session)
    }

    fun remove(session: DefaultWebSocketServerSession) {
        sessions.remove(session)
    }

    suspend fun broadcast(notification: BookNotification) {
        val payload = Json.encodeToString(notification)

        for (session in sessions.toList()) {
            try {
                session.send(Frame.Text(payload))
            } catch (_: Throwable) {
                sessions.remove(session)
            }
        }
    }
}
