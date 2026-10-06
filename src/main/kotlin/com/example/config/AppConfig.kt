package com.example.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.example.model.User
import java.util.Date

data class DatabaseSettings(
    val url: String,
    val user: String,
    val password: String,
    val driver: String,
    val maximumPoolSize: Int = 10
) {
    companion object {
        fun fromEnvironment(): DatabaseSettings = DatabaseSettings(
            url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/kt4",
            user = System.getenv("DB_USER") ?: "kt4",
            password = System.getenv("DB_PASSWORD") ?: "kt4",
            driver = System.getenv("DB_DRIVER") ?: "org.postgresql.Driver"
        )
    }
}

data class AdminCredentials(
    val login: String,
    val password: String
) {
    companion object {
        fun fromEnvironment(): AdminCredentials = AdminCredentials(
            login = System.getenv("ADMIN_LOGIN") ?: "admin",
            password = System.getenv("ADMIN_PASSWORD") ?: "admin123"
        )
    }
}

data class JwtConfig(
    val secret: String = System.getenv("JWT_SECRET") ?: "local-development-secret-change-me",
    val issuer: String = System.getenv("JWT_ISSUER") ?: "kt4-kotlin",
    val audience: String = System.getenv("JWT_AUDIENCE") ?: "kt4-users",
    val realm: String = System.getenv("JWT_REALM") ?: "KT4 API",
    val expiresInSeconds: Long = 3600
) {
    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun createToken(user: User): String {
        val expiresAt = Date(System.currentTimeMillis() + expiresInSeconds * 1000)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", user.id)
            .withClaim("login", user.login)
            .withClaim("role", user.role.name.lowercase())
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }
}
