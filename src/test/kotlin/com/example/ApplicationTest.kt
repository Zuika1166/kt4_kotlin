package com.example

import com.example.config.AdminCredentials
import com.example.config.DatabaseSettings
import com.example.config.JwtConfig
import com.example.dto.TokenResponse
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.contentType
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    private val jwtConfig = JwtConfig(
        secret = "test-secret",
        issuer = "kt4-test",
        audience = "kt4-test-users"
    )

    private val adminCredentials = AdminCredentials(
        login = "admin",
        password = "admin123"
    )

    @Test
    fun modulesRolesAndCrudWorkTogether() = testApplication {
        application {
            module(
                databaseSettings = testDatabase("modules"),
                jwtConfig = jwtConfig,
                adminCredentials = adminCredentials
            )
        }

        assertEquals(HttpStatusCode.OK, client.get("/health").status)

        val swaggerStatus = client.get("/swagger").status.value
        val openApiStatus = client.get("/openapi").status.value
        assertTrue(swaggerStatus in 200..399)
        assertTrue(openApiStatus in 200..399)

        val register = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"john","password":"secret123"}""")
        }
        assertEquals(HttpStatusCode.Created, register.status)

        val userToken = login("john", "secret123")
        val me = client.get("/auth/me") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.OK, me.status)
        assertTrue(me.bodyAsText().contains(""role": "user""))

        val create = client.post("/books") {
            bearerAuth(userToken)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Clean Code","author":"Robert C. Martin","year":2008}""")
        }
        assertEquals(HttpStatusCode.Created, create.status)

        assertEquals(HttpStatusCode.OK, client.get("/books").status)
        assertEquals(HttpStatusCode.OK, client.get("/books/1").status)

        val update = client.put("/books/1") {
            bearerAuth(userToken)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Clean Code","author":"Robert C. Martin","year":2009}""")
        }
        assertEquals(HttpStatusCode.OK, update.status)
        assertTrue(update.bodyAsText().contains("2009"))

        val forbiddenDelete = client.delete("/books/1") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenDelete.status)

        val forbiddenUsers = client.get("/admin/users") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenUsers.status)

        val adminToken = login("admin", "admin123")

        val users = client.get("/admin/users") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.OK, users.status)
        assertTrue(users.bodyAsText().contains(""role": "admin""))
        assertTrue(users.bodyAsText().contains(""role": "user""))

        val delete = client.delete("/books/1") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.NoContent, delete.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/books/1").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/books/abc").status)
    }

    @Test
    fun websocketBroadcastsBookEvents() = testApplication {
        application {
            module(
                databaseSettings = testDatabase("websocket"),
                jwtConfig = jwtConfig,
                adminCredentials = adminCredentials
            )
        }

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"socket-user","password":"secret123"}""")
        }

        val token = login("socket-user", "secret123")
        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/ws/notifications?token=$token") {
            val create = client.post("/books") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"title":"Domain-Driven Design","author":"Eric Evans","year":2003}""")
            }

            assertEquals(HttpStatusCode.Created, create.status)

            val frame = withTimeout(3000) {
                incoming.receive()
            }

            assertTrue(frame is Frame.Text)
            val payload = (frame as Frame.Text).readText()
            assertTrue(payload.contains(""type":"created""))
            assertTrue(payload.contains("Domain-Driven Design"))
        }
    }

    private suspend fun io.ktor.client.HttpClient.login(
        login: String,
        password: String
    ): String {
        val response = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"$login","password":"$password"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        return Json.decodeFromString<TokenResponse>(
            response.bodyAsText()
        ).token
    }

    private fun testDatabase(name: String): DatabaseSettings =
        DatabaseSettings(
            url = "jdbc:h2:mem:$name;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            user = "sa",
            password = "",
            driver = "org.h2.Driver",
            maximumPoolSize = 4
        )
}
