package com.tao0524.tickat.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tao0524.tickat.MainActivity
import com.tao0524.tickat.R
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskFeature
import java.time.LocalTime

class TaskStartAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId   = intent.getStringExtra("task_id")   ?: return
        val taskName = intent.getStringExtra("task_name")  ?: return
        val feature  = intent.getStringExtra("task_feature") ?: ""
        val start    = intent.getStringExtra("task_start") ?: ""
        val end      = intent.getStringExtra("task_end")   ?: ""
        val memo     = intent.getStringExtra("task_memo")  ?: ""
        val repeat   = intent.getStringExtra("task_repeat") ?: ""

        val featureLabel = when (feature) {
            "CLOCK"      -> "時計"
            "DATE"       -> "日付"
            "COUNTDOWN"  -> "カウントダウン"
            "NEXT_EVENT" -> "次の予定"
            "MEMO"       -> "メモ"
            else         -> ""
        }

        // 通知を表示
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createChannelIfNeeded(manager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_expanded", true)
            putExtra("expanded_task_id", taskId)
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "$start〜$end — $featureLabel"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(taskName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        manager.notify(("start_$taskId").hashCode(), notification)

        // 繰り返しタスクは次回分を再スケジュール
        if (repeat.isNotEmpty() && repeat != RepeatType.ONCE.name) {
            try {
                val task = Task(
                    id        = taskId,
                    name      = taskName,
                    feature   = TaskFeature.valueOf(feature),
                    startTime = LocalTime.parse(start),
                    endTime   = LocalTime.parse(end),
                    repeat    = RepeatType.valueOf(repeat),
                    memoText  = memo
                )
                TaskAlertScheduler.schedule(context, task)
            } catch (_: Exception) {
                // パース失敗時は再スケジュールしない
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "tickat_task_start"

        fun createChannelIfNeeded(manager: NotificationManager) {
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "タスク開始通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "タスクの開始時刻に通知します"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}

