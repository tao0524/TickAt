package com.tao0524.tickat.ui.screen.expanded

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.ui.screen.settings.KEY_SHOW_CHECKBOXES
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

private val KEY_CHECKED_TASKS = stringSetPreferencesKey("checked_task_ids")

data class ScheduleItem(
    val task: Task,
    val isChecked: Boolean,
    val isActive: Boolean
)

data class ExpandedUiState(
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val showCheckboxes: Boolean = true,
    val isLoading: Boolean = true,
    val targetTaskId: String? = null
)

class ExpandedViewModel(
    private val repository: TaskRepository,
    private val context: Context
) : ViewModel() {

    private val _targetTaskId = MutableStateFlow<String?>(null)

    fun setTargetTaskId(id: String?) {
        _targetTaskId.value = id
    }

    private val checkedTaskIds = context.displaySettingsDataStore.data
        .map { prefs -> prefs[KEY_CHECKED_TASKS] ?: emptySet() }

    private val showCheckboxes = context.displaySettingsDataStore.data
        .map { prefs -> prefs[KEY_SHOW_CHECKBOXES] ?: true }

    val uiState = combine(
        repository.allTasks,
        checkedTaskIds,
        showCheckboxes,
        _targetTaskId
    ) { tasks, checkedIds, showCb, targetId ->
        val now = LocalTime.now()
        val sortedTasks = tasks.filter { it.isEnabled }.sortedBy { it.startTime }
        val items = sortedTasks.map { task ->
            ScheduleItem(
                task = task,
                isChecked = task.id in checkedIds,
                isActive = task.taskType == TaskType.TIMEBLOCK &&
                        !now.isBefore(task.startTime) && now.isBefore(task.endTime)
            )
        }
        ExpandedUiState(
            scheduleItems = items,
            showCheckboxes = showCb,
            isLoading = false,
            targetTaskId = targetId
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpandedUiState()
        )

    fun toggleCheck(taskId: String) {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                val current = prefs[KEY_CHECKED_TASKS] ?: emptySet()
                prefs[KEY_CHECKED_TASKS] = if (taskId in current) {
                    current - taskId
                } else {
                    current + taskId
                }
            }
        }
    }
}