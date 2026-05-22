package com.tao0524.tickat.ui.screen.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import com.tao0524.tickat.widget.CountdownAlarmReceiver

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
            repository.delete(task)
        }
    }
}

