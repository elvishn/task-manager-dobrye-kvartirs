package com.taskmanager.controller

import com.ninjasquad.springmockk.MockkBean
import com.taskmanager.dto.CreateTaskRequest
import com.taskmanager.dto.PageResponse
import com.taskmanager.dto.TaskResponse
import com.taskmanager.exception.TaskNotFoundException
import com.taskmanager.model.TaskStatus
import com.taskmanager.service.TaskService
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@WebFluxTest(TaskController::class)
class TaskControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var taskService: TaskService

    private val now = LocalDateTime.of(2026, 3, 26, 12, 0, 0)

    private fun taskResponse(
        id: Long = 1L,
        title: String = "Prepare report",
        status: TaskStatus = TaskStatus.NEW
    ) = TaskResponse(
        id = id,
        title = title,
        description = "Monthly financial report",
        status = status,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `POST api-tasks returns 201 with created task`() {
        val response = taskResponse()
        every { taskService.createTask(any()) } returns Mono.just(response)

        webTestClient.post().uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"title":"Prepare report","description":"Monthly financial report"}""")
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.status").isEqualTo("NEW")
            .jsonPath("$.title").isEqualTo("Prepare report")
    }

    @Test
    fun `POST api-tasks returns 400 when title is blank`() {
        webTestClient.post().uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"title":"","description":"desc"}""")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `POST api-tasks returns 400 when title is too short`() {
        webTestClient.post().uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"title":"ab","description":"desc"}""")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `GET api-tasks returns 200 with paged list`() {
        val page = PageResponse(
            content = listOf(taskResponse()),
            page = 0,
            size = 10,
            totalElements = 1L,
            totalPages = 1
        )
        every { taskService.getTasks(0, 10, null) } returns Mono.just(page)

        webTestClient.get().uri("/api/tasks?page=0&size=10")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content[0].id").isEqualTo(1)
            .jsonPath("$.totalElements").isEqualTo(1)
            .jsonPath("$.totalPages").isEqualTo(1)
    }

    @Test
    fun `GET api-tasks with status filter passes status to service`() {
        val page = PageResponse(
            content = listOf(taskResponse()),
            page = 0,
            size = 10,
            totalElements = 1L,
            totalPages = 1
        )
        every { taskService.getTasks(0, 10, TaskStatus.NEW) } returns Mono.just(page)

        webTestClient.get().uri("/api/tasks?page=0&size=10&status=NEW")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content[0].status").isEqualTo("NEW")
    }

    @Test
    fun `GET api-tasks-id returns 200 when task found`() {
        every { taskService.getTaskById(1L) } returns Mono.just(taskResponse())

        webTestClient.get().uri("/api/tasks/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.title").isEqualTo("Prepare report")
    }

    @Test
    fun `GET api-tasks-id returns 404 when task not found`() {
        every { taskService.getTaskById(99L) } returns Mono.error(TaskNotFoundException(99L))

        webTestClient.get().uri("/api/tasks/99")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `PATCH api-tasks-id-status returns 200 with updated task`() {
        val updated = taskResponse(status = TaskStatus.DONE)
        every { taskService.updateStatus(1L, TaskStatus.DONE) } returns Mono.just(updated)

        webTestClient.patch().uri("/api/tasks/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"status":"DONE"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("DONE")
    }

    @Test
    fun `PATCH api-tasks-id-status returns 404 when task not found`() {
        every { taskService.updateStatus(99L, TaskStatus.DONE) } returns Mono.error(TaskNotFoundException(99L))

        webTestClient.patch().uri("/api/tasks/99/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"status":"DONE"}""")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `DELETE api-tasks-id returns 204 when task deleted`() {
        every { taskService.deleteTask(1L) } returns Mono.empty()

        webTestClient.delete().uri("/api/tasks/1")
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    fun `DELETE api-tasks-id returns 404 when task not found`() {
        every { taskService.deleteTask(99L) } returns Mono.error(TaskNotFoundException(99L))

        webTestClient.delete().uri("/api/tasks/99")
            .exchange()
            .expectStatus().isNotFound
    }
}
