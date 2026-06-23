package com.tao0524.tickat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.ui.screen.expanded.ExpandedViewModel
import com.tao0524.tickat.ui.screen.taskedit.TaskEditViewModel
import com.tao0524.tickat.ui.screen.tasklist.TaskListViewModel

import android.content.Context
import com.tao0524.tickat.ui.screen.settings.SettingsViewModel

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
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(context.applicationContext, repository, appWidgetId = 0) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

class SettingsViewModelFactory(
    private val context: Context,
    private val repository: TaskRepository?,
    private val appWidgetId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(context.applicationContext, repository, appWidgetId) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}