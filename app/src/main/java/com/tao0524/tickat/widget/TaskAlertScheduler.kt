package com.tao0524.tickat.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
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

        // TIMEBLOCK: 現在進行中なら今日の終了アラームを先に登録（ONCE早期returnの前に実行）
        var endScheduled = false
        if (task.taskType == TaskType.TIMEBLOCK && task.endTime > task.startTime) {
            val todayEnd = LocalDate.now().atTime(task.endTime)
            if (!now.isBefore(LocalDate.now().atTime(task.startTime)) && now.isBefore(todayEnd)) {
                scheduleEndAlarm(context, task, todayEnd)
                endScheduled = true
            }
        }

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
            putExtra("task_start", task.startTime.toString())
            putExtra("task_end", task.endTime.toString())
            putExtra("task_memo", task.memoText)
            putExtra("task_repeat", task.repeat.name)
            putExtra("task_type", task.taskType.name)
            putExtra("alert_type", "START")
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

        // TIMEBLOCK: 次回の終了アラームも登録（今日分を登録済みならスキップ）
        if (!endScheduled && task.taskType == TaskType.TIMEBLOCK && task.endTime > task.startTime) {
            scheduleEndAlarm(context, task, nextStart.toLocalDate().atTime(task.endTime))
        }
    }

    private fun scheduleEndAlarm(context: Context, task: Task, endDateTime: LocalDateTime) {
        val triggerMillis = endDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, TaskStartAlertReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_name", task.name)
            putExtra("task_start", task.startTime.toString())
            putExtra("task_end", task.endTime.toString())
            putExtra("task_memo", task.memoText)
            putExtra("task_repeat", task.repeat.name)
            putExtra("task_type", task.taskType.name)
            putExtra("alert_type", "END")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("end_${task.id}").hashCode(),
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
        // 開始アラームのキャンセル
        val startIntent = Intent(context, TaskStartAlertReceiver::class.java)
        val startPending = PendingIntent.getBroadcast(
            context,
            ("start_${taskId}").hashCode(),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPending != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(startPending)
            startPending.cancel()
        }

        // 終了アラームのキャンセル
        val endIntent = Intent(context, TaskStartAlertReceiver::class.java)
        val endPending = PendingIntent.getBroadcast(
            context,
            ("end_${taskId}").hashCode(),
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPending != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(endPending)
            endPending.cancel()
        }
    }
}

