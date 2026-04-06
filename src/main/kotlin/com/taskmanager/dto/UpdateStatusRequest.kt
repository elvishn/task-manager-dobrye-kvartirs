package com.taskmanager.dto

import com.taskmanager.model.TaskStatus
import jakarta.validation.constraints.NotNull

data class UpdateStatusRequest(
    @field:NotNull(message = "Status must not be null")
    val status: TaskStatus
)
