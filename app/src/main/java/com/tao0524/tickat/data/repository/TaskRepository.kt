package com.tao0524.tickat.data.repository

import com.tao0524.tickat.data.local.TaskDao
import com.tao0524.tickat.domain.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    val allTasks: Flow<List<Task>> = dao.getAllTasks()

    suspend fun save(task: Task) = dao.upsertTask(task)
    suspend fun delete(task: Task) = dao.deleteTask(task)
    suspend fun deleteById(id: String) = dao.deleteTaskById(id)
    suspend fun getById(id: String): Task? = dao.getTaskById(id)
    suspend fun setEnabled(id: String, isEnabled: Boolean) = dao.updateTaskEnabled(id, isEnabled)
}

