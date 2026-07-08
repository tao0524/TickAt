package com.tao0524.tickat.ui.screen.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.TaskType
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import com.tao0524.tickat.widget.TaskAlertScheduler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.flow.map

private val KEY_GUIDE_STEP = intPreferencesKey("guide_step")
private val KEY_HINT_TASKLIST = booleanPreferencesKey("hint_tasklist")

class TaskListViewModel(
    private val repository: TaskRepository,
    private val context: Context
) : ViewModel() {

    val tasks = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val guideStep = context.displaySettingsDataStore.data
        .map { prefs -> prefs[KEY_GUIDE_STEP] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun advanceGuide() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                val current = prefs[KEY_GUIDE_STEP] ?: 0
                if (current in 1..2) prefs[KEY_GUIDE_STEP] = current + 1
            }
        }
    }

    fun completeGuide() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_GUIDE_STEP] = 4
            }
        }
    }

    val hintTaskListShown = context.displaySettingsDataStore.data
        .map { prefs -> prefs[KEY_HINT_TASKLIST] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun dismissHintTaskList() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_HINT_TASKLIST] = true
            }
        }
    }

    fun toggleEnabled(task: Task, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(task.id, enabled)
            if (enabled) {
                val updated = task.copy(isEnabled = true)
                TaskAlertScheduler.schedule(context, updated)
            } else {
                TaskAlertScheduler.cancel(context, task.id)
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            TaskAlertScheduler.cancel(context, task.id)
            repository.delete(task)
        }
    }

    fun applyTemplate() {
        viewModelScope.launch {
            val templates = listOf(
                Task(
                    name = "朝のルーティン",
                    startTime = LocalTime.of(6, 0),
                    endTime = LocalTime.of(6, 30),
                    repeat = RepeatType.DAILY,
                    sortOrder = 0,
                    taskType = TaskType.TIMEBLOCK
                ),
                Task(
                    name = "昼休み",
                    startTime = LocalTime.of(12, 0),
                    endTime = LocalTime.of(13, 0),
                    repeat = RepeatType.WEEKDAY,
                    sortOrder = 1,
                    taskType = TaskType.TIMEBLOCK
                ),
                Task(
                    name = "食事会",
                    startTime = LocalTime.of(19, 0),
                    endTime = LocalTime.of(19, 0),
                    repeat = RepeatType.ONCE,
                    sortOrder = 2,
                    taskType = TaskType.REMINDER
                )
            )
            templates.forEach {
                repository.save(it)
                TaskAlertScheduler.schedule(context, it)
            }
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_GUIDE_STEP] = 1
            }
        }
    }
}