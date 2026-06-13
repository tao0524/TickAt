package com.tao0524.tickat.data.local

import androidx.room.TypeConverter
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.TaskType
import java.time.LocalTime

class Converters {
    @TypeConverter fun fromLocalTime(value: String?): LocalTime? =
        value?.let { LocalTime.parse(it) }
    @TypeConverter fun toLocalTime(time: LocalTime?): String? =
        time?.toString()

    @TypeConverter fun fromRepeat(value: String?): RepeatType? =
        value?.let { RepeatType.valueOf(it) }
    @TypeConverter fun toRepeat(repeat: RepeatType?): String? =
        repeat?.name

    @TypeConverter fun fromTaskType(value: String?): TaskType? =
        value?.let { TaskType.valueOf(it) }
    @TypeConverter fun toTaskType(taskType: TaskType?): String? =
        taskType?.name
}

