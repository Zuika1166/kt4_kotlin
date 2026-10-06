package com.example.service

import com.example.dto.CreateBookRequest
import com.example.dto.UpdateBookRequest
import com.example.error.ResourceNotFoundException
import com.example.error.ValidationException
import com.example.model.Book
import com.example.repository.BookRepository

class BookService(
    private val repository: BookRepository
) {
    fun getAll(): List<Book> = repository.findAll()

    fun getById(id: Long): Book =
        repository.findById(id)
            ?: throw ResourceNotFoundException("Book not found")

    fun create(request: CreateBookRequest): Book {
        validate(request.title, request.author, request.year)
        return repository.create(request)
    }

    fun update(id: Long, request: UpdateBookRequest): Book {
        validate(request.title, request.author, request.year)

        return repository.update(id, request)
            ?: throw ResourceNotFoundException("Book not found")
    }

    fun delete(id: Long): Book {
        val book = getById(id)

        if (!repository.delete(id)) {
            throw ResourceNotFoundException("Book not found")
        }

        return book
    }

    private fun validate(title: String, author: String, year: Int) {
        if (title.isBlank()) {
            throw ValidationException("Title is required")
        }

        if (author.isBlank()) {
            throw ValidationException("Author is required")
        }

        if (year !in 1..2100) {
            throw ValidationException("Year must be between 1 and 2100")
        }
    }
}
