package com.taskmanager.repository

import com.taskmanager.model.TaskStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TaskRepository::class)
class TaskRepositoryTest {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Test
    fun `save creates task with NEW status`() {
        val task = taskRepository.save("My task", "description")

        assertNotNull(task.id)
        assertEquals("My task", task.title)
        assertEquals("description", task.description)
        assertEquals(TaskStatus.NEW, task.status)
        assertNotNull(task.createdAt)
        assertNotNull(task.updatedAt)
    }

    @Test
    fun `findById returns task when exists`() {
        val saved = taskRepository.save("Find me", null)
        val found = taskRepository.findById(saved.id)

        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals("Find me", found.title)
    }

    @Test
    fun `findById returns null when task does not exist`() {
        val result = taskRepository.findById(Long.MAX_VALUE)
        assertNull(result)
    }

    @Test
    fun `findAll returns tasks ordered by createdAt DESC`() {
        taskRepository.save("Task A", null)
        taskRepository.save("Task B", null)

        val tasks = taskRepository.findAll(0, 10, null)

        assertTrue(tasks.isNotEmpty())
        // Verify ordering — created_at DESC means later tasks first
        for (i in 0 until tasks.size - 1) {
            assertTrue(!tasks[i].createdAt.isBefore(tasks[i + 1].createdAt))
        }
    }

    @Test
    fun `findAll with status filter returns only matching tasks`() {
        val saved = taskRepository.save("Filter task", null)
        taskRepository.updateStatus(saved.id, TaskStatus.DONE)

        val newTasks = taskRepository.findAll(0, 10, TaskStatus.NEW)
        val doneTasks = taskRepository.findAll(0, 10, TaskStatus.DONE)

        assertTrue(newTasks.none { it.id == saved.id })
        assertTrue(doneTasks.any { it.id == saved.id })
    }

    @Test
    fun `findAll respects pagination`() {
        repeat(5) { taskRepository.save("Paginated task $it", null) }

        val page0 = taskRepository.findAll(0, 2, null)
        val page1 = taskRepository.findAll(1, 2, null)

        assertEquals(2, page0.size)
        assertEquals(2, page1.size)
        assertTrue(page0.map { it.id }.intersect(page1.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun `count returns total number of tasks`() {
        val before = taskRepository.count(null)
        taskRepository.save("Count task", null)
        val after = taskRepository.count(null)

        assertEquals(before + 1, after)
    }

    @Test
    fun `count with status filter counts correctly`() {
        val task = taskRepository.save("Status count", null)
        taskRepository.updateStatus(task.id, TaskStatus.IN_PROGRESS)

        val inProgress = taskRepository.count(TaskStatus.IN_PROGRESS)
        assertTrue(inProgress >= 1)
    }

    @Test
    fun `updateStatus changes task status and updatedAt`() {
        val saved = taskRepository.save("Update me", null)
        val updated = taskRepository.updateStatus(saved.id, TaskStatus.IN_PROGRESS)

        assertNotNull(updated)
        assertEquals(TaskStatus.IN_PROGRESS, updated!!.status)
        assertTrue(!updated.updatedAt.isBefore(saved.updatedAt))
    }

    @Test
    fun `updateStatus returns null when task does not exist`() {
        val result = taskRepository.updateStatus(Long.MAX_VALUE, TaskStatus.DONE)
        assertNull(result)
    }

    @Test
    fun `deleteById removes task and returns true`() {
        val saved = taskRepository.save("Delete me", null)
        val deleted = taskRepository.deleteById(saved.id)

        assertTrue(deleted)
        assertNull(taskRepository.findById(saved.id))
    }

    @Test
    fun `deleteById returns false when task does not exist`() {
        val result = taskRepository.deleteById(Long.MAX_VALUE)
        assertFalse(result)
    }
}
