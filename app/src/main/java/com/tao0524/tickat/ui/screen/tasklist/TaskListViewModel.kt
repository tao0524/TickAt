package com.tao0524.tickat.ui.screen.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskFeature
import com.tao0524.tickat.domain.model.RepeatType
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import com.tao0524.tickat.widget.CountdownAlarmReceiver
import com.tao0524.tickat.widget.TaskAlertScheduler

class TaskListViewModel(
    private val repository: TaskRepository,
    private val context: Context
) : ViewModel() {

    val tasks = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun delete(task: Task) {
        viewModelScope.launch {
            CountdownAlarmReceiver.cancel(context, task.id)
            TaskAlertScheduler.cancel(context, task.id)
            repository.delete(task)
        }
    }

    fun applyTemplate() {
        viewModelScope.launch {
            val templates = listOf(
                Task(
                    name = "朝の時計",
                    feature = TaskFeature.CLOCK,
                    startTime = LocalTime.of(7, 0),
                    endTime = LocalTime.of(9, 0),
                    repeat = RepeatType.DAILY,
                    sortOrder = 0
                ),
                Task(
                    name = "今日の日付",
                    feature = TaskFeature.DATE,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(18, 0),
                    repeat = RepeatType.DAILY,
                    sortOrder = 1
                ),
                Task(
                    name = "おつかれさま",
                    feature = TaskFeature.MEMO,
                    startTime = LocalTime.of(18, 0),
                    endTime = LocalTime.of(23, 0),
                    repeat = RepeatType.DAILY,
                    memoText = "今日もおつかれさま",
                    sortOrder = 2
                )
            )
            templates.forEach {
                repository.save(it)
                TaskAlertScheduler.schedule(context, it)
            }
        }
    }
}

