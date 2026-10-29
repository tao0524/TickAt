package com.tao0524.tickat.ui.screen.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.repeatDayMask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.domain.model.WidgetDisplayMode
import android.content.Context
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
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val repeat: RepeatType = RepeatType.DAILY,
    val memoText: String = "",
    val taskType: TaskType = TaskType.TIMEBLOCK,
    val displayMode: WidgetDisplayMode = WidgetDisplayMode.COUNTDOWN,
    // オフの予定を編集して保存しても、オフのまま残す
    val isEnabled: Boolean = true,
    // 光っている曜日（月=1、火=2、水=4、木=8、金=16、土=32、日=64 の足し算）。「毎日」は全部、「平日」は月〜金
    val repeatDays: Int = 0x7F
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
                        startTime = task.startTime,
                        endTime = task.endTime,
                        repeat = task.repeat,
                        memoText = task.memoText,
                        taskType = task.taskType,
                        displayMode = task.displayMode,
                        isEnabled = task.isEnabled,
                        repeatDays = task.repeatDayMask()
                    )
                }
            }
        }
    }

    fun onNameChange(v: String)                   = _state.update { it.copy(name = v) }
    fun onStartTimeChange(v: LocalTime)           = _state.update { it.copy(startTime = v) }
    fun onEndTimeChange(v: LocalTime)             = _state.update { it.copy(endTime = v) }
    // 「毎日」は7つ全部、「平日」は月〜金を光らせる。ほかの種類から「曜日を選ぶ」に変えたときは、曜日を全部消して選んでもらう
    fun onRepeatChange(v: RepeatType) = _state.update { state ->
        when (v) {
            RepeatType.DAILY   -> state.copy(repeat = v, repeatDays = 0x7F)
            RepeatType.WEEKDAY -> state.copy(repeat = v, repeatDays = 0x1F)
            RepeatType.WEEKLY  ->
                if (state.repeat == RepeatType.WEEKLY) state
                else state.copy(repeat = v, repeatDays = 0)
            RepeatType.ONCE    -> state.copy(repeat = v)
        }
    }
    // 曜日を押すと、光ったり消えたりする。全部なら「毎日」、月〜金なら「平日」、それ以外は「曜日を選ぶ」にする
    fun onRepeatDayToggle(day: DayOfWeek) = _state.update { state ->
        val days = state.repeatDays xor (1 shl (day.value - 1))
        val repeat = when (days) {
            0x7F -> RepeatType.DAILY
            0x1F -> RepeatType.WEEKDAY
            else -> RepeatType.WEEKLY
        }
        state.copy(repeat = repeat, repeatDays = days)
    }
    fun onMemoChange(v: String)                   = _state.update { it.copy(memoText = v) }
    fun onTaskTypeChange(v: TaskType) = _state.update { state ->
        val newMode = if (v == TaskType.REMINDER && state.displayMode == WidgetDisplayMode.COUNTDOWN) {
            WidgetDisplayMode.SIMPLE
        } else if (v == TaskType.TIMEBLOCK && state.displayMode == WidgetDisplayMode.SIMPLE) {
            WidgetDisplayMode.COUNTDOWN
        } else {
            state.displayMode
        }
        state.copy(taskType = v, displayMode = newMode)
    }
    fun onDisplayModeChange(v: WidgetDisplayMode)  = _state.update { it.copy(displayMode = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) return
        // 「曜日を選ぶ」で曜日が1つもないときは保存しない（画面の「保存」も押せない）
        if (s.repeat == RepeatType.WEEKLY && s.repeatDays == 0) return
        viewModelScope.launch {
            val taskId = s.id ?: java.util.UUID.randomUUID().toString()
            val savedTask = Task(
                id = taskId,
                name = s.name,
                startTime = s.startTime,
                endTime = s.endTime,
                repeat = s.repeat,
                memoText = s.memoText,
                taskType = s.taskType,
                displayMode = s.displayMode,
                isEnabled = s.isEnabled,
                // 曜日を書くのは「曜日を選ぶ」だけ。「毎日」「平日」は種類で決まる（Task.repeatDayMask）
                repeatDays = if (s.repeat == RepeatType.WEEKLY) s.repeatDays else 0
            )
            repository.save(savedTask)
            TaskAlertScheduler.schedule(context, savedTask)
            onDone()
        }
    }
}