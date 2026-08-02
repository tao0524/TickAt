package com.tao0524.tickat.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.tao0524.tickat.FullScreenAlertActivity
import com.tao0524.tickat.MainActivity
import com.tao0524.tickat.R
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.ui.screen.settings.KEY_ALERT_MODE
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_SOUND
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_DURATION
import com.tao0524.tickat.data.local.AppDatabase
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

class TaskStartAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskId    = intent.getStringExtra("task_id")    ?: return@launch
                val taskName  = intent.getStringExtra("task_name")  ?: return@launch
                val start     = intent.getStringExtra("task_start") ?: ""
                val end       = intent.getStringExtra("task_end")   ?: ""
                val memo      = intent.getStringExtra("task_memo")  ?: ""
                val repeat    = intent.getStringExtra("task_repeat") ?: ""
                val taskType  = intent.getStringExtra("task_type")  ?: "TIMEBLOCK"
                val alertType = intent.getStringExtra("alert_type") ?: "START"

                val featureLabel = if (taskType == "TIMEBLOCK") "タイムブロック" else "リマインダー"

                // DataStoreからアラートモード・通知音を読み込む
                val prefs = try {
                    context.displaySettingsDataStore.data.first()
                } catch (_: Exception) {
                    null
                }
                val alertMode = prefs?.get(KEY_ALERT_MODE) ?: "NOTIFICATION"
                val soundUriStr = prefs?.get(KEY_NOTIFICATION_SOUND) ?: ""

                // 終了通知の場合：再スケジュール不要、常にNOTIFICATION
                if (alertType == "END") {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val activeChannelId = createChannelIfNeeded(manager, soundUriStr)

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingOpen = PendingIntent.getActivity(
                        context, taskId.hashCode(), openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, activeChannelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("$taskName 終了")
                        .setContentText("$start〜$end")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingOpen)
                        .setAutoCancel(true)
                        .build()
                    manager.notify(("end_$taskId").hashCode(), notification)
                    return@launch
                }

                // 繰り返しスケジュールは次回分を再スケジュール（モードに関係なく常に実行）
                rescheduleIfNeeded(context, taskId, taskName, start, end, memo, repeat, taskType)

                // ONCEタスクの自動オフ（アラーム発火後にタスクを無効化）
                if (repeat == RepeatType.ONCE.name) {
                    try {
                        AppDatabase.getInstance(context).taskDao().updateTaskEnabled(taskId, false)
                    } catch (_: Exception) {
                        // DB更新失敗時はタスクが有効のまま残る（安全側に倒す）
                    }
                }

                // OFFの場合は通知を出さない
                if (alertMode == "OFF") return@launch

                // 再生時間を読み込む
                val duration = prefs?.get(KEY_NOTIFICATION_DURATION) ?: 5

                // TaskAlertService を起動（通知表示 + 音の再生はサービスが担当）
                val serviceIntent = Intent(context, TaskAlertService::class.java).apply {
                    putExtra("task_id", taskId)
                    putExtra("task_name", taskName)
                    putExtra("task_start", start)
                    putExtra("task_end", end)
                    putExtra("task_memo", memo)
                    putExtra("task_type", taskType)
                    putExtra("alert_mode", alertMode)
                    putExtra("sound_uri", soundUriStr)
                    putExtra("duration", duration)
                }
                context.startForegroundService(serviceIntent)

            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun rescheduleIfNeeded(
        context: Context,
        taskId: String, taskName: String,
        start: String, end: String, memo: String, repeat: String,
        taskType: String
    ) {
        if (repeat.isEmpty() || repeat == RepeatType.ONCE.name) return
        try {
            val task = Task(
                id        = taskId,
                name      = taskName,
                startTime = LocalTime.parse(start),
                endTime   = LocalTime.parse(end),
                repeat    = RepeatType.valueOf(repeat),
                memoText  = memo,
                taskType  = TaskType.valueOf(taskType)
            )
            TaskAlertScheduler.schedule(context, task)
        } catch (_: Exception) {
            // パース失敗時は再スケジュールしない
        }
    }

    companion object {
        private const val BASE_CHANNEL_ID = "tickat_task_start"

        fun createChannelIfNeeded(manager: NotificationManager, soundUriStr: String): String {
            val currentChannelId = if (soundUriStr.isEmpty()) {
                BASE_CHANNEL_ID
            } else {
                "${BASE_CHANNEL_ID}_${System.currentTimeMillis()}"
            }

            // 同じ通知音のチャンネルが既に存在すればスキップ
            if (manager.getNotificationChannel(currentChannelId) != null) {
                return currentChannelId
            }

            // 古いTickAtチャンネルを削除
            for (channel in manager.notificationChannels) {
                if (channel.id.startsWith(BASE_CHANNEL_ID) && channel.id != currentChannelId) {
                    manager.deleteNotificationChannel(channel.id)
                }
            }

            // 新しいチャンネルを作成
            val channel = NotificationChannel(
                currentChannelId,
                "スケジュール開始通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "スケジュールの開始時刻に通知します"
                enableVibration(true)
                if (soundUriStr.isNotEmpty()) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                    setSound(Uri.parse(soundUriStr), audioAttributes)
                }
            }
            manager.createNotificationChannel(channel)

            return currentChannelId
        }
    }
}