package com.example.routes

import com.example.config.JwtConfig
import com.example.realtime.NotificationHub
import com.example.repository.UserRepository
import com.example.service.AdminService
import com.example.service.AuthService
import com.example.service.BookService
import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    bookService: BookService,
    adminService: AdminService,
    userRepository: UserRepository,
    jwtConfig: JwtConfig,
    notificationHub: NotificationHub
) {
    routing {
        swaggerUI(
            path = "swagger",
            swaggerFile = "openapi/documentation.yaml"
        )

        openAPI(
            path = "openapi",
            swaggerFile = "openapi/documentation.yaml"
        )

        healthRoutes()
        authRoutes(authService)
        bookRoutes(bookService, notificationHub)
        adminRoutes(adminService)
        notificationRoutes(jwtConfig, userRepository, notificationHub)
    }
}
