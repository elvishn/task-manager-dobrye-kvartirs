package com.taskmanager.service

import com.taskmanager.dto.CreateTaskRequest
import com.taskmanager.dto.PageResponse
import com.taskmanager.dto.TaskResponse
import com.taskmanager.exception.TaskNotFoundException
import com.taskmanager.model.TaskStatus
import com.taskmanager.repository.TaskRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.math.ceil

@Service
class TaskService(private val taskRepository: TaskRepository) {

    fun createTask(request: CreateTaskRequest): Mono<TaskResponse> =
        Mono
            .fromCallable { taskRepository.save(request.title, request.description) }
            .map { TaskResponse.from(it) }
            .subscribeOn(Schedulers.boundedElastic())

    fun getTaskById(id: Long): Mono<TaskResponse> =
        Mono
            .fromCallable { taskRepository.findById(id) ?: throw TaskNotFoundException(id) }
            .map { TaskResponse.from(it) }
            .subscribeOn(Schedulers.boundedElastic())

    fun getTasks(page: Int, size: Int, status: TaskStatus?): Mono<PageResponse<TaskResponse>> =
        Mono
            .fromCallable {
                val tasks = taskRepository.findAll(page, size, status)
                val total = taskRepository.count(status)
                val totalPages = if (total == 0L) {
                    0
                } else {
                    ceil(total.toDouble() / size).toInt()
                }
                PageResponse(
                    content = tasks.map { TaskResponse.from(it) },
                    page = page,
                    size = size,
                    totalElements = total,
                    totalPages = totalPages
                )
            }
            .subscribeOn(Schedulers.boundedElastic())

    fun updateStatus(id: Long, status: TaskStatus): Mono<TaskResponse> =
        Mono
            .fromCallable {
                taskRepository.updateStatus(id, status) ?: throw TaskNotFoundException(id)
            }
            .map { TaskResponse.from(it) }
            .subscribeOn(Schedulers.boundedElastic())

    fun deleteTask(id: Long): Mono<Void> =
        Mono
            .fromCallable {
                if (!taskRepository.deleteById(id)) throw TaskNotFoundException(id)
            }
            .subscribeOn(Schedulers.boundedElastic())
            .then()
}
