package com.taskmanager.controller


import com.taskmanager.service.TaskService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/api/tasks")
class TaskController(private val taskService: TaskService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(): Mono<String> = Mono.just("OK")

    @GetMapping
    fun getTasks(): Mono<String> = Mono.just("OK")

    @GetMapping("/{id}")
    fun getTaskById(@PathVariable id: Long): Mono<String> = Mono.just("OK")

    @PatchMapping("/{id}/status")
    fun updateStatus(@PathVariable id: Long): Mono<String> = Mono.just("OK")

    @DeleteMapping("/{id}")
    fun deleteTask(@PathVariable id: Long): Mono<String> = Mono.just("OK")
}
