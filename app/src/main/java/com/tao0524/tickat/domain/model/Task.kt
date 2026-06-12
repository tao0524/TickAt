package com.tao0524.tickat.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val feature: TaskFeature,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val repeat: RepeatType,
    val memoText: String = "",
    val targetDateTime: LocalDateTime? = null,
    val sortOrder: Int = 0,
    val taskType: TaskType = TaskType.TIMEBLOCK
)

