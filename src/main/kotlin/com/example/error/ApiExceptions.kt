package com.example.error

class ValidationException(message: String) : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)

class UnauthorizedException(message: String) : RuntimeException(message)

class ForbiddenException(message: String) : RuntimeException(message)
