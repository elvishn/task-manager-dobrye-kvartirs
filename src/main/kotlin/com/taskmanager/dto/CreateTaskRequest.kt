package com.taskmanager.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateTaskRequest(
    @field:NotBlank(message = "Title must not be blank")
    @field:Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    val title: String,
    val description: String?
)
