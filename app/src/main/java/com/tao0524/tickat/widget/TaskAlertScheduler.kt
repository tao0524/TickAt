package com.tao0524.tickat.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TaskAlertScheduler {

    fun scheduleAll(context: Context, tasks: List<Task>) {
        tasks.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, task: Task) {
        val now = LocalDateTime.now()
        var nextStart = LocalDate.now().atTime(task.startTime)

        if (nextStart.isAfter(now)) {
            // 今日の開始時刻がまだ来ていない
            when (task.repeat) {
                RepeatType.WEEKDAY -> {
                    val dow = nextStart.dayOfWeek.value
                    if (dow > 5) {
                        nextStart = nextStart.plusDays((8 - dow).toLong())
                    }
                }
                else -> { /* 今日のまま */ }
            }
        } else {
            // 今日の開始時刻は既に過ぎた → 次回を計算
            when (task.repeat) {
                RepeatType.DAILY -> nextStart = nextStart.plusDays(1)
                RepeatType.WEEKDAY -> {
                    nextStart = nextStart.plusDays(1)
                    while (nextStart.dayOfWeek.value > 5) {
                        nextStart = nextStart.plusDays(1)
                    }
                }
                RepeatType.WEEKLY -> nextStart = nextStart.plusWeeks(1)
                RepeatType.ONCE -> return // 1回限りで既に過ぎた
            }
        }

        val triggerMillis = nextStart
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, TaskStartAlertReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_name", task.name)
            putExtra("task_feature", task.feature.name)
            putExtra("task_start", task.startTime.toString())
            putExtra("task_end", task.endTime.toString())
            putExtra("task_memo", task.memoText)
            putExtra("task_repeat", task.repeat.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("start_${task.id}").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
            )
        }
    }

    fun cancel(context: Context, taskId: String) {
        val intent = Intent(context, TaskStartAlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("start_${taskId}").hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}

