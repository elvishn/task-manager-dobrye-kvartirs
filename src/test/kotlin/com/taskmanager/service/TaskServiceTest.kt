package com.taskmanager.service

import com.taskmanager.dto.CreateTaskRequest
import com.taskmanager.exception.TaskNotFoundException
import com.taskmanager.model.Task
import com.taskmanager.model.TaskStatus
import com.taskmanager.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.time.LocalDateTime

// ─────────────────────────────────────────────────────────────────────────────
// Как тестировать реактивный код: StepVerifier
// ─────────────────────────────────────────────────────────────────────────────
//
// Обычный JUnit-тест не умеет ждать асинхронных результатов.
// StepVerifier из библиотеки reactor-test решает эту проблему:
// он подписывается на Mono/Flux и проверяет каждый сигнал по шагам.
//
// Три вида сигналов в Reactor:
//   onNext(value)  — пришло очередное значение
//   onComplete()   — поток завершился нормально (все значения отданы)
//   onError(e)     — поток завершился с ошибкой
//
// Шаблон теста:
//   StepVerifier.create(mono)   ← подписываемся
//     .assertNext { }           ← проверяем onNext
//     .verifyComplete()         ← ожидаем onComplete + запускаем весь сценарий
//
// Важно: без вызова verify*/expect* в конце тест не выполнится совсем!
// ─────────────────────────────────────────────────────────────────────────────

class TaskServiceTest {

    private val taskRepository: TaskRepository = mockk()
    private val taskService = TaskService(taskRepository)

    private val now = LocalDateTime.now()

    private fun task(
        id: Long = 1L,
        title: String = "Test task",
        status: TaskStatus = TaskStatus.NEW
    ) = Task(id = id, title = title, description = null, status = status, createdAt = now, updatedAt = now)

    @Test
    fun `createTask returns TaskResponse`() {
        val request = CreateTaskRequest(title = "Test task", description = null)
        every { taskRepository.save("Test task", null) } returns task()

        StepVerifier.create(taskService.createTask(request))
            // assertNext проверяет onNext-сигнал: значение пришло и соответствует условию.
            .assertNext { response ->
                assert(response.id == 1L)
                assert(response.title == "Test task")
                assert(response.status == TaskStatus.NEW)
            }
            // verifyComplete = «жду onComplete» + «запускаю подписку и блокирую тест до завершения».
            // Без этого вызова лямбда выше никогда не выполнится.
            .verifyComplete()
    }

    @Test
    fun `getTaskById returns TaskResponse when task exists`() {
        every { taskRepository.findById(1L) } returns task()

        StepVerifier.create(taskService.getTaskById(1L))
            .assertNext { response ->
                assert(response.id == 1L)
                assert(response.title == "Test task")
            }
            .verifyComplete()
    }

    @Test
    fun `getTaskById throws TaskNotFoundException when task does not exist`() {
        every { taskRepository.findById(99L) } returns null

        StepVerifier.create(taskService.getTaskById(99L))
            // expectError проверяет onError-сигнал: поток завершился ошибкой нужного типа.
            // .verify() в конце обязателен — именно он запускает подписку.
            .expectError(TaskNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `updateStatus updates task status`() {
        val updated = task(status = TaskStatus.DONE)
        every { taskRepository.updateStatus(1L, TaskStatus.DONE) } returns updated

        StepVerifier.create(taskService.updateStatus(1L, TaskStatus.DONE))
            .assertNext { response ->
                assert(response.status == TaskStatus.DONE)
            }
            .verifyComplete()
    }

    @Test
    fun `updateStatus throws TaskNotFoundException when task does not exist`() {
        every { taskRepository.updateStatus(99L, TaskStatus.DONE) } returns null

        StepVerifier.create(taskService.updateStatus(99L, TaskStatus.DONE))
            .expectError(TaskNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `deleteTask completes when task exists`() {
        every { taskRepository.deleteById(1L) } returns true

        StepVerifier.create(taskService.deleteTask(1L))
            // verifyComplete без assertNext: Mono<Void> не несёт значений,
            // только сигнал «успешно завершено».
            .verifyComplete()

        verify { taskRepository.deleteById(1L) }
    }

    @Test
    fun `deleteTask throws TaskNotFoundException when task does not exist`() {
        every { taskRepository.deleteById(99L) } returns false

        StepVerifier.create(taskService.deleteTask(99L))
            .expectError(TaskNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `getTasks returns paged response with status filter`() {
        val tasks = listOf(task(id = 1L), task(id = 2L))
        every { taskRepository.findAll(0, 10, TaskStatus.NEW) } returns tasks
        every { taskRepository.count(TaskStatus.NEW) } returns 2L

        StepVerifier.create(taskService.getTasks(0, 10, TaskStatus.NEW))
            .assertNext { page ->
                assert(page.content.size == 2)
                assert(page.totalElements == 2L)
                assert(page.totalPages == 1)
                assert(page.page == 0)
                assert(page.size == 10)
            }
            .verifyComplete()
    }

    @Test
    fun `getTasks returns paged response without filter`() {
        val tasks = listOf(task(id = 1L, status = TaskStatus.NEW), task(id = 2L, status = TaskStatus.DONE))
        every { taskRepository.findAll(0, 10, null) } returns tasks
        every { taskRepository.count(null) } returns 2L

        StepVerifier.create(taskService.getTasks(0, 10, null))
            .assertNext { page ->
                assert(page.content.size == 2)
                assert(page.totalElements == 2L)
            }
            .verifyComplete()
    }

    @Test
    fun `getTasks returns correct totalPages for partial last page`() {
        val tasks = (1..5L).map { task(id = it) }
        every { taskRepository.findAll(0, 3, null) } returns tasks.take(3)
        every { taskRepository.count(null) } returns 5L

        StepVerifier.create(taskService.getTasks(0, 3, null))
            .assertNext { page ->
                assert(page.totalPages == 2)
            }
            .verifyComplete()
    }
}
