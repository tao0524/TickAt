package com.tao0524.tickat.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.domain.model.needsRepeatDays
import com.tao0524.tickat.domain.model.repeatsOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TaskAlertScheduler {

    fun scheduleAll(context: Context, tasks: List<Task>) {
        tasks.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, task: Task) {
        if (!task.isEnabled) return
        // 曜日がまだ決まっていない「曜日を選ぶ」の予定は予約しない（前の版の予約も取り消す）
        if (task.needsRepeatDays()) {
            cancel(context, task.id)
            return
        }

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        var nextStart = today.atTime(task.startTime)

        // TIMEBLOCK: 現在進行中なら今日の終了アラームを先に登録（ONCE早期returnの前に実行）。今日が繰り返す曜日のときだけ
        var endScheduled = false
        if (task.taskType == TaskType.TIMEBLOCK && task.endTime > task.startTime && task.repeatsOn(today.dayOfWeek)) {
            val todayEnd = LocalDate.now().atTime(task.endTime)
            if (!now.isBefore(LocalDate.now().atTime(task.startTime)) && now.isBefore(todayEnd)) {
                scheduleEndAlarm(context, task, todayEnd)
                endScheduled = true
            }
        }

        if (task.repeat == RepeatType.ONCE) {
            // 今日の開始時刻が過ぎていれば翌日にスケジュール（発火後に自動オフ）
            if (!nextStart.isAfter(now)) nextStart = nextStart.plusDays(1)
        } else if (!(nextStart.isAfter(now) && task.repeatsOn(today.dayOfWeek))) {
            // 今日の開始時刻が過ぎたか、今日が繰り返す曜日でない → 繰り返す曜日のうち、次の日にする
            var days = 1L
            while (days <= 7L && !task.repeatsOn(today.plusDays(days).dayOfWeek)) days++
            nextStart = today.plusDays(days).atTime(task.startTime)
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

