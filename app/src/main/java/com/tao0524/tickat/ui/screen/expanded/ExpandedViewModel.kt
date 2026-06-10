package com.tao0524.tickat.ui.screen.expanded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.domain.model.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class ExpandedUiState(
    val currentTask: Task? = null,
    val nextTask: Task? = null,
    val now: LocalTime = LocalTime.now(),
    val isLoading: Boolean = true
)

class ExpandedViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _now = MutableStateFlow(LocalTime.now())
    private val _targetTaskId = MutableStateFlow<String?>(null)

    fun setTargetTaskId(id: String?) {
        _targetTaskId.value = id
    }

    val uiState = combine(repository.allTasks, _now, _targetTaskId) { tasks, now, targetId ->
        val targetTask = targetId?.let { id -> tasks.find { it.id == id } }
        val active   = tasks.filter { now >= it.startTime && now < it.endTime }
        val upcoming = tasks.filter { it.startTime > now }.minByOrNull { it.startTime }
        val current  = targetTask
            ?: active.minByOrNull { it.startTime }
            ?: upcoming
            ?: tasks.minByOrNull { it.startTime }
        val next = upcoming.takeIf { it != current }
        ExpandedUiState(
            currentTask = current,
            nextTask = next,
            now = now,
            isLoading = false
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpandedUiState()
        )

    init {
        // 毎秒 _now を更新 → uiState がリコンポーズ → 表示が自動更新
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                _now.value = LocalTime.now()
            }
        }
    }
}

