package com.tao0524.tickat.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val repeat: RepeatType,
    val memoText: String = "",
    val sortOrder: Int = 0,
    val taskType: TaskType = TaskType.TIMEBLOCK,
    val isEnabled: Boolean = true,
    val displayMode: WidgetDisplayMode = WidgetDisplayMode.COUNTDOWN,
    // 繰り返す曜日（月=1、火=2、水=4、木=8、金=16、土=32、日=64 の足し算）。「曜日を選ぶ」（WEEKLY）のときだけ使う。0 は曜日がまだ決まっていない
    val repeatDays: Int = 0
)

// この予定が繰り返す曜日。「毎日」「平日」は種類で決まり、「曜日を選ぶ」は repeatDays。「1回のみ」は 0
fun Task.repeatDayMask(): Int = when (repeat) {
    RepeatType.DAILY   -> 0x7F
    RepeatType.WEEKDAY -> 0x1F
    RepeatType.WEEKLY  -> repeatDays and 0x7F
    RepeatType.ONCE    -> 0
}

// その曜日に鳴る（ウィジェットに出す）予定か。「1回のみ」はいつでも true
fun Task.repeatsOn(day: DayOfWeek): Boolean =
    repeat == RepeatType.ONCE || (repeatDayMask() and (1 shl (day.value - 1))) != 0

// 「曜日を選ぶ」なのに曜日がまだ決まっていない予定（前の版の「週1回」）
fun Task.needsRepeatDays(): Boolean = repeat == RepeatType.WEEKLY && repeatDayMask() == 0