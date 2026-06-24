package com.tao0524.tickat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.ui.screen.expanded.ExpandedViewModel
import com.tao0524.tickat.ui.screen.taskedit.TaskEditViewModel
import com.tao0524.tickat.ui.screen.tasklist.TaskListViewModel

import android.content.Context

class ViewModelFactory(
    private val repository: TaskRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(TaskListViewModel::class.java) ->
            TaskListViewModel(repository, context) as T
        modelClass.isAssignableFrom(TaskEditViewModel::class.java) ->
            TaskEditViewModel(repository, context) as T
        modelClass.isAssignableFrom(ExpandedViewModel::class.java) ->
            ExpandedViewModel(repository, context) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}