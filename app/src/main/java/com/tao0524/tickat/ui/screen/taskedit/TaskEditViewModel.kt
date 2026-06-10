package com.tao0524.tickat.ui.screen.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import android.content.Context
import com.tao0524.tickat.widget.CountdownAlarmReceiver
import com.tao0524.tickat.widget.TaskAlertScheduler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TaskEditState(
    val id: String? = null,
    val name: String = "",
    val feature: TaskFeature = TaskFeature.CLOCK,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val repeat: RepeatType = RepeatType.DAILY,
    val memoText: String = "",
    val targetDateTime: LocalDateTime? = null
)

private val KEY_HINT_TASKEDIT = booleanPreferencesKey("hint_taskedit")

class TaskEditViewModel(
    private val repository: TaskRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditState())
    val state = _state.asStateFlow()

    val hintTaskEditShown = context.displaySettingsDataStore.data
        .map { prefs -> prefs[KEY_HINT_TASKEDIT] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun dismissHintTaskEdit() {
        viewModelScope.launch {
            context.displaySettingsDataStore.edit { prefs ->
                prefs[KEY_HINT_TASKEDIT] = true
            }
        }
    }

    fun load(id: String) {
        viewModelScope.launch {
            repository.getById(id)?.let { task ->
                _state.update {
                    TaskEditState(
                        id = task.id,
                        name = task.name,
                        feature = task.feature,
                        startTime = task.startTime,
                        endTime = task.endTime,
                        repeat = task.repeat,
                        memoText = task.memoText,
                        targetDateTime = task.targetDateTime
                    )
                }
            }
        }
    }

    fun onNameChange(v: String)                   = _state.update { it.copy(name = v) }
    fun onFeatureChange(v: TaskFeature)           = _state.update { it.copy(feature = v) }
    fun onStartTimeChange(v: LocalTime)           = _state.update { it.copy(startTime = v) }
    fun onEndTimeChange(v: LocalTime)             = _state.update { it.copy(endTime = v) }
    fun onRepeatChange(v: RepeatType)             = _state.update { it.copy(repeat = v) }
    fun onMemoChange(v: String)                   = _state.update { it.copy(memoText = v) }
    fun onTargetDateTimeChange(v: LocalDateTime?) = _state.update { it.copy(targetDateTime = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            val taskId = s.id ?: java.util.UUID.randomUUID().toString()
            repository.save(
                Task(
                    id = taskId,
                    name = s.name,
                    feature = s.feature,
                    startTime = s.startTime,
                    endTime = s.endTime,
                    repeat = s.repeat,
                    memoText = s.memoText,
                    targetDateTime = s.targetDateTime
                )
            )
            val savedTask = Task(
                id = taskId,
                name = s.name,
                feature = s.feature,
                startTime = s.startTime,
                endTime = s.endTime,
                repeat = s.repeat,
                memoText = s.memoText,
                targetDateTime = s.targetDateTime
            )
            if (s.feature == TaskFeature.COUNTDOWN && s.targetDateTime != null) {
                CountdownAlarmReceiver.schedule(
                    context       = context,
                    taskId        = taskId,
                    taskName      = s.name,
                    targetDateTime = s.targetDateTime
                )
            } else {
                CountdownAlarmReceiver.cancel(context, taskId)
            }
            TaskAlertScheduler.schedule(context, savedTask)
            onDone()
        }
    }
}