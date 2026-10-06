package com.example.routes

import com.example.plugins.currentUserId
import com.example.service.AuthService
import com.example.dto.LoginRequest
import com.example.dto.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            call.respond(HttpStatusCode.Created, authService.register(request))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            call.respond(HttpStatusCode.OK, authService.login(request))
        }

        authenticate("auth-jwt") {
            get("/me") {
                call.respond(
                    HttpStatusCode.OK,
                    authService.me(call.currentUserId())
                )
            }
        }
    }
}
