package com.example.repository

import com.example.database.DatabaseFactory
import com.example.model.User
import com.example.model.UserRole
import java.sql.Statement

interface UserRepository {
    fun findById(id: Long): User?
    fun findByLogin(login: String): User?
    fun findAll(): List<User>
    fun create(login: String, passwordHash: String, role: UserRole): User
}

class JdbcUserRepository(
    private val database: DatabaseFactory
) : UserRepository {
    override fun findById(id: Long): User? =
        database.connection { connection ->
            connection.prepareStatement(
                "SELECT id, login, password_hash, role FROM users WHERE id = ?"
            ).use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        result.toUser()
                    } else {
                        null
                    }
                }
            }
        }

    override fun findByLogin(login: String): User? =
        database.connection { connection ->
            connection.prepareStatement(
                "SELECT id, login, password_hash, role FROM users WHERE login = ?"
            ).use { statement ->
                statement.setString(1, login)
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        result.toUser()
                    } else {
                        null
                    }
                }
            }
        }

    override fun findAll(): List<User> =
        database.connection { connection ->
            connection.prepareStatement(
                "SELECT id, login, password_hash, role FROM users ORDER BY id"
            ).use { statement ->
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) {
                            add(result.toUser())
                        }
                    }
                }
            }
        }

    override fun create(
        login: String,
        passwordHash: String,
        role: UserRole
    ): User =
        database.connection { connection ->
            connection.prepareStatement(
                "INSERT INTO users(login, password_hash, role) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { statement ->
                statement.setString(1, login)
                statement.setString(2, passwordHash)
                statement.setString(3, role.name)
                statement.executeUpdate()

                statement.generatedKeys.use { keys ->
                    keys.next()
                    User(
                        id = keys.getLong(1),
                        login = login,
                        passwordHash = passwordHash,
                        role = role
                    )
                }
            }
        }

    private fun java.sql.ResultSet.toUser(): User =
        User(
            id = getLong("id"),
            login = getString("login"),
            passwordHash = getString("password_hash"),
            role = UserRole.valueOf(getString("role"))
        )
}
