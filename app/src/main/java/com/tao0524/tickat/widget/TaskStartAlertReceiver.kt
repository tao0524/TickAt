package com.tao0524.tickat.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tao0524.tickat.FullScreenAlertActivity
import com.tao0524.tickat.MainActivity
import com.tao0524.tickat.R
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskFeature
import com.tao0524.tickat.ui.screen.settings.KEY_ALERT_MODE
import com.tao0524.tickat.ui.screen.settings.displaySettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

        // DataStoreからアラートモードを読み込む
        val alertMode = runBlocking {
            try {
                context.displaySettingsDataStore.data.first()[KEY_ALERT_MODE] ?: "NOTIFICATION"
            } catch (_: Exception) {
                "NOTIFICATION"
            }
        }

        // 繰り返しシーンは次回分を再スケジュール（モードに関係なく常に実行）
        rescheduleIfNeeded(context, taskId, taskName, feature, start, end, memo, repeat)

        // OFFの場合は通知を出さない
        if (alertMode == "OFF") return

        // 通知を表示
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createChannelIfNeeded(manager)

        val contentText = "$start〜$end — $featureLabel"

        // ExpandedScreen用PendingIntent（通知タップ時）
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(taskName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)

        // フルスクリーンモードの場合、setFullScreenIntentを追加
        if (alertMode == "FULLSCREEN") {
            val fullScreenIntent = Intent(context, FullScreenAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("scene_name", taskName)
                putExtra("feature_label", featureLabel)
                putExtra("time_range", "$start〜$end")
            }
            val fullScreenPending = PendingIntent.getActivity(
                context,
                taskId.hashCode() + 1,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPending, true)
            builder.setContentIntent(fullScreenPending)
        }

        manager.notify(("start_$taskId").hashCode(), builder.build())
    }

    private fun rescheduleIfNeeded(
        context: Context,
        taskId: String, taskName: String, feature: String,
        start: String, end: String, memo: String, repeat: String
    ) {
        if (repeat.isEmpty() || repeat == RepeatType.ONCE.name) return
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

    companion object {
        const val CHANNEL_ID = "tickat_task_start"

        fun createChannelIfNeeded(manager: NotificationManager) {
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "スケジュール開始通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "スケジュールの開始時刻に通知します"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}