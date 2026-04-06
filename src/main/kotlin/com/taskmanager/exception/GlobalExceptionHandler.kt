package com.taskmanager.exception

import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleTaskNotFound(ex: TaskNotFoundException): ErrorResponse =
        ErrorResponse(status = HttpStatus.NOT_FOUND.value(), message = ex.message ?: "Not found")

    @ExceptionHandler(WebExchangeBindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: WebExchangeBindException): ErrorResponse {
        val message = ex.bindingResult.allErrors
            .joinToString("; ") { error ->
                if (error is FieldError) "${error.field}: ${error.defaultMessage}"
                else error.defaultMessage ?: "Validation error"
            }
        return ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), message = message)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneric(ex: Exception): ErrorResponse =
        ErrorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), message = "Internal server error")
}

data class ErrorResponse(val status: Int, val message: String)
