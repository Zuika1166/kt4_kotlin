package com.example.repository

import com.example.database.DatabaseFactory
import com.example.dto.CreateBookRequest
import com.example.dto.UpdateBookRequest
import com.example.model.Book
import java.sql.Statement

interface BookRepository {
    fun findAll(): List<Book>
    fun findById(id: Long): Book?
    fun create(request: CreateBookRequest): Book
    fun update(id: Long, request: UpdateBookRequest): Book?
    fun delete(id: Long): Boolean
}

class JdbcBookRepository(
    private val database: DatabaseFactory
) : BookRepository {
    override fun findAll(): List<Book> =
        database.connection { connection ->
            connection.prepareStatement(
                "SELECT id, title, author, publication_year FROM books ORDER BY id"
            ).use { statement ->
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) {
                            add(result.toBook())
                        }
                    }
                }
            }
        }

    override fun findById(id: Long): Book? =
        database.connection { connection ->
            connection.prepareStatement(
                "SELECT id, title, author, publication_year FROM books WHERE id = ?"
            ).use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        result.toBook()
                    } else {
                        null
                    }
                }
            }
        }

    override fun create(request: CreateBookRequest): Book =
        database.connection { connection ->
            connection.prepareStatement(
                "INSERT INTO books(title, author, publication_year) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { statement ->
                statement.setString(1, request.title.trim())
                statement.setString(2, request.author.trim())
                statement.setInt(3, request.year)
                statement.executeUpdate()

                statement.generatedKeys.use { keys ->
                    keys.next()
                    Book(
                        id = keys.getLong(1),
                        title = request.title.trim(),
                        author = request.author.trim(),
                        year = request.year
                    )
                }
            }
        }

    override fun update(
        id: Long,
        request: UpdateBookRequest
    ): Book? =
        database.connection { connection ->
            connection.prepareStatement(
                "UPDATE books SET title = ?, author = ?, publication_year = ? WHERE id = ?"
            ).use { statement ->
                statement.setString(1, request.title.trim())
                statement.setString(2, request.author.trim())
                statement.setInt(3, request.year)
                statement.setLong(4, id)

                if (statement.executeUpdate() == 0) {
                    null
                } else {
                    Book(
                        id = id,
                        title = request.title.trim(),
                        author = request.author.trim(),
                        year = request.year
                    )
                }
            }
        }

    override fun delete(id: Long): Boolean =
        database.connection { connection ->
            connection.prepareStatement(
                "DELETE FROM books WHERE id = ?"
            ).use { statement ->
                statement.setLong(1, id)
                statement.executeUpdate() > 0
            }
        }

    private fun java.sql.ResultSet.toBook(): Book =
        Book(
            id = getLong("id"),
            title = getString("title"),
            author = getString("author"),
            year = getInt("publication_year")
        )
}
