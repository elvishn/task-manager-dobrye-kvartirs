package com.taskmanager.repository

import com.taskmanager.model.Task
import com.taskmanager.model.TaskStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class TaskRepository(private val jdbcClient: JdbcClient) {

    fun save(title: String, description: String?): Task {
        val now = LocalDateTime.now()
        val keyHolder = GeneratedKeyHolder()
        jdbcClient
            .sql(
                """
                INSERT INTO tasks (title, description, status, created_at, updated_at)
                VALUES (:title, :description, :status, :createdAt, :updatedAt)
            """
            )
            .param("title", title)
            .param("description", description)
            .param("status", TaskStatus.NEW.name)
            .param("createdAt", now)
            .param("updatedAt", now)
            .update(keyHolder)
        val id = keyHolder.key!!.toLong()
        return findById(id)!!
    }

    fun findById(id: Long): Task? =
        jdbcClient
            .sql("SELECT * FROM tasks WHERE id = :id")
            .param("id", id)
            .query { rs, _ -> rs.toTask() }
            .optional()
            .orElse(null)

    fun findAll(page: Int, size: Int, status: TaskStatus?): List<Task> {
        val offset = page * size
        return if (status != null) {
            jdbcClient.sql(
                "SELECT * FROM tasks WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
            )
                .param("status", status.name)
                .param("limit", size)
                .param("offset", offset)
                .query { rs, _ -> rs.toTask() }
                .list()
        } else {
            jdbcClient.sql(
                "SELECT * FROM tasks ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
            )
                .param("limit", size)
                .param("offset", offset)
                .query { rs, _ -> rs.toTask() }
                .list()
        }
    }

    fun count(status: TaskStatus?): Long =
        if (status != null) {
            jdbcClient
                .sql("SELECT COUNT(*) FROM tasks WHERE status = :status")
                .param("status", status.name)
                .query(Long::class.java)
                .single()
        } else {
            jdbcClient
                .sql("SELECT COUNT(*) FROM tasks")
                .query(Long::class.java)
                .single()
        }

    fun updateStatus(id: Long, status: TaskStatus): Task? {
        val updatedAt = LocalDateTime.now()
        val rows = jdbcClient
            .sql(
                "UPDATE tasks SET status = :status, updated_at = :updatedAt WHERE id = :id"
            )
            .param("status", status.name)
            .param("updatedAt", updatedAt)
            .param("id", id)
            .update()
        return if (rows > 0) findById(id) else null
    }

    fun deleteById(id: Long): Boolean {
        val rows = jdbcClient
            .sql("DELETE FROM tasks WHERE id = :id")
            .param("id", id)
            .update()
        return rows > 0
    }

    private fun ResultSet.toTask() = Task(
        id = getLong("id"),
        title = getString("title"),
        description = getString("description"),
        status = TaskStatus.valueOf(getString("status")),
        createdAt = getObject("created_at", LocalDateTime::class.java),
        updatedAt = getObject("updated_at", LocalDateTime::class.java)
    )
}
