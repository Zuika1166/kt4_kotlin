package com.example

import com.example.config.AdminCredentials
import com.example.config.DatabaseSettings
import com.example.config.JwtConfig
import com.example.database.DatabaseFactory
import com.example.plugins.configureMonitoring
import com.example.plugins.configureSecurity
import com.example.plugins.configureSerialization
import com.example.plugins.configureStatusPages
import com.example.plugins.configureWebSockets
import com.example.realtime.NotificationHub
import com.example.repository.JdbcBookRepository
import com.example.repository.JdbcUserRepository
import com.example.routes.configureRouting
import com.example.service.AdminService
import com.example.service.AuthService
import com.example.service.BookService
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = 8080
    ) {
        module()
    }.start(wait = true)
}

fun Application.module(
    databaseSettings: DatabaseSettings = DatabaseSettings.fromEnvironment(),
    jwtConfig: JwtConfig = JwtConfig(),
    adminCredentials: AdminCredentials = AdminCredentials.fromEnvironment()
) {
    val database = DatabaseFactory(databaseSettings)
    database.initialize()

    val userRepository = JdbcUserRepository(database)
    val bookRepository = JdbcBookRepository(database)
    val authService = AuthService(userRepository, jwtConfig)
    val bookService = BookService(bookRepository)
    val adminService = AdminService(userRepository)
    val notificationHub = NotificationHub()

    authService.ensureAdmin(adminCredentials)

    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureWebSockets()
    configureSecurity(jwtConfig, userRepository)
    configureRouting(
        authService = authService,
        bookService = bookService,
        adminService = adminService,
        userRepository = userRepository,
        jwtConfig = jwtConfig,
        notificationHub = notificationHub
    )

    environment.monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        database.close()
    }
}
