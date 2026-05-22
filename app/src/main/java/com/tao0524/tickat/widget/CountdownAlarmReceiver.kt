package com.tao0524.tickat.widget

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tao0524.tickat.R
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_DURATION
import com.tao0524.tickat.ui.screen.settings.KEY_NOTIFICATION_SOUND
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class CountdownAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId   = intent.getStringExtra(EXTRA_TASK_ID)   ?: return
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs    = context.displaySettingsDataStore.data.first()
                val soundUri = prefs[KEY_NOTIFICATION_SOUND]    ?: ""
                val durationMs = (prefs[KEY_NOTIFICATION_DURATION] ?: 5) * 1000L

                val ringtoneUri = if (soundUri.isEmpty()) {
                    android.media.RingtoneManager.getDefaultUri(
                        android.media.RingtoneManager.TYPE_NOTIFICATION
                    )
                } else {
                    android.net.Uri.parse(soundUri)
                }

                val ringtone = android.media.RingtoneManager.getRingtone(context, ringtoneUri)
                // API 28以上はloopを明示的に無効化
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.isLooping = false
                }
                ringtone?.play()

                // 通知バナーを表示
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                createChannelIfNeeded(manager)
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(taskName)
                    .setContentText(context.getString(R.string.notification_countdown_done))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                manager.notify(taskId.hashCode(), notification)

                // 設定した再生時間後に停止
                delay(durationMs)
                ringtone?.stop()

            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "tickat_countdown_3"
        private const val EXTRA_TASK_ID      = "task_id"
        private const val EXTRA_TASK_NAME    = "task_name"

        fun createChannelIfNeeded(manager: NotificationManager) {
            manager.deleteNotificationChannel("tickat_countdown")
            manager.deleteNotificationChannel("tickat_countdown_1")
            manager.deleteNotificationChannel("tickat_countdown_2")
            manager.deleteNotificationChannel(CHANNEL_ID)

            val channel = NotificationChannel(
                CHANNEL_ID,
                "カウントダウン通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "カウントダウン終了時に通知します"
                enableVibration(true)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }

        fun schedule(
            context: Context,
            taskId: String,
            taskName: String,
            targetDateTime: LocalDateTime
        ) {
            val triggerMillis = targetDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            if (triggerMillis <= System.currentTimeMillis()) return

            val intent = Intent(context, CountdownAlarmReceiver::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_NAME, taskName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
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
            val intent = Intent(context, CountdownAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}