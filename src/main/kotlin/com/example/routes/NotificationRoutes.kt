package com.example.routes

import com.example.config.JwtConfig
import com.example.realtime.NotificationHub
import com.example.repository.UserRepository
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close

fun Route.notificationRoutes(
    jwtConfig: JwtConfig,
    userRepository: UserRepository,
    notificationHub: NotificationHub
) {
    webSocket("/ws/notifications") {
        val token = call.request.queryParameters["token"]

        if (token == null) {
            close(
                CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Token is required"
                )
            )
            return@webSocket
        }

        val decoded = runCatching {
            jwtConfig.verifier.verify(token)
        }.getOrNull()

        val userId = decoded
            ?.getClaim("userId")
            ?.asLong()

        if (userId == null || userRepository.findById(userId) == null) {
            close(
                CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Invalid token"
                )
            )
            return@webSocket
        }

        notificationHub.add(this)

        try {
            for (frame in incoming) {
                if (frame is Frame.Close) {
                    break
                }
            }
        } finally {
            notificationHub.remove(this)
        }
    }
}
