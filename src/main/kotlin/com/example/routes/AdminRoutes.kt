package com.example.routes

import com.example.model.UserRole
import com.example.plugins.requireRole
import com.example.service.AdminService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.adminRoutes(adminService: AdminService) {
    authenticate("auth-jwt") {
        route("/admin") {
            get("/users") {
                call.requireRole(UserRole.ADMIN)
                call.respond(HttpStatusCode.OK, adminService.getUsers())
            }
        }
    }
}
