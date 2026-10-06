package com.example.routes

import com.example.dto.BookNotification
import com.example.dto.CreateBookRequest
import com.example.dto.NotificationType
import com.example.dto.UpdateBookRequest
import com.example.error.ValidationException
import com.example.model.UserRole
import com.example.plugins.requireRole
import com.example.realtime.NotificationHub
import com.example.service.BookService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.bookRoutes(
    bookService: BookService,
    notificationHub: NotificationHub
) {
    route("/books") {
        get {
            call.respond(HttpStatusCode.OK, bookService.getAll())
        }

        get("/{id}") {
            call.respond(
                HttpStatusCode.OK,
                bookService.getById(call.bookId())
            )
        }

        authenticate("auth-jwt") {
            post {
                val book = bookService.create(
                    call.receive<CreateBookRequest>()
                )

                notificationHub.broadcast(
                    BookNotification(NotificationType.CREATED, book)
                )

                call.respond(HttpStatusCode.Created, book)
            }

            put("/{id}") {
                val book = bookService.update(
                    call.bookId(),
                    call.receive<UpdateBookRequest>()
                )

                notificationHub.broadcast(
                    BookNotification(NotificationType.UPDATED, book)
                )

                call.respond(HttpStatusCode.OK, book)
            }

            delete("/{id}") {
                call.requireRole(UserRole.ADMIN)

                val book = bookService.delete(call.bookId())

                notificationHub.broadcast(
                    BookNotification(NotificationType.DELETED, book)
                )

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ApplicationCall.bookId(): Long {
    val id = parameters["id"]?.toLongOrNull()

    if (id == null || id <= 0) {
        throw ValidationException("Book id must be a positive number")
    }

    return id
}
