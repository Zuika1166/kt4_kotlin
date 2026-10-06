package com.example.plugins

import com.example.config.JwtConfig
import com.example.dto.ErrorResponse
import com.example.error.ForbiddenException
import com.example.error.UnauthorizedException
import com.example.model.UserRole
import com.example.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun Application.configureSecurity(
    jwtConfig: JwtConfig,
    userRepository: UserRepository
) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(jwtConfig.verifier)

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asLong()

                if (userId != null && userRepository.findById(userId) != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid or missing token")
                )
            }
        }
    }
}

fun ApplicationCall.currentUserId(): Long =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("userId")
        ?.asLong()
        ?: throw UnauthorizedException("Invalid or missing token")

fun ApplicationCall.currentRole(): UserRole {
    val role = principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("role")
        ?.asString()
        ?: throw UnauthorizedException("Invalid or missing token")

    return runCatching {
        UserRole.valueOf(role.uppercase())
    }.getOrElse {
        throw UnauthorizedException("Invalid token role")
    }
}

fun ApplicationCall.requireRole(role: UserRole) {
    if (currentRole() != role) {
        throw ForbiddenException("Admin role is required")
    }
}
