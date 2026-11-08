package com.tao0524.tickat.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tao0524.tickat.FullScreenAlertActivity
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.domain.model.needsRepeatDays
import com.tao0524.tickat.ui.screen.settings.KEY_ALERT_MODE
import com.tao0524.tickat.ui.screen.settings.KEY_END_ALERT_MODE
import com.tao0524.tickat.ui.screen.settings.KEY_END_NOTIFICATION_DURATION
import com.tao0524.tickat.ui.screen.settings.KEY_END_NOTIFICATION_SOUND
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

                // データベースの今の予定を読む。曜日がまだ決まっていない「曜日を選ぶ」の予定は、鳴らさず次も予約しない
                // （前の版で予約した「週1回」の通知が、アップデートのあとに届いたとき）
                val dbTask = try {
                    AppDatabase.getInstance(context).taskDao().getTaskById(taskId)
                } catch (_: Exception) {
                    null
                }
                if (dbTask != null && dbTask.needsRepeatDays()) return@launch

                val featureLabel = if (taskType == "TIMEBLOCK") "タイムブロック" else "リマインダー"

                // 「1回のみ」のタイムブロックは、やっている最中もウィジェットに名前と時間を出すため、終了の時刻にオフにする。
                // リマインダーと、終了が開始より後でないタイムブロック（終了の通知を予約しない）は、今までどおり開始の時刻にオフにする
                val endsLater = runCatching { LocalTime.parse(end).isAfter(LocalTime.parse(start)) }.getOrDefault(false)
                val offAtEnd = repeat == RepeatType.ONCE.name && taskType == "TIMEBLOCK" && endsLater

                // DataStoreからアラートモード・通知音を読み込む
                val prefs = try {
                    context.displaySettingsDataStore.data.first()
                } catch (_: Exception) {
                    null
                }
                val alertMode = prefs?.get(KEY_ALERT_MODE) ?: "NOTIFICATION"
                val soundUriStr = prefs?.get(KEY_NOTIFICATION_SOUND) ?: ""

                // 終了通知の場合：再スケジュール不要
                if (alertType == "END") {
                    if (offAtEnd) {
                        try {
                            AppDatabase.getInstance(context).taskDao().updateTaskEnabled(taskId, false)
                        } catch (_: Exception) {
                            // DB更新失敗時はタスクが有効のまま残る（安全側に倒す）
                        }
                        // 最中に TickAt を開いたり再起動したりすると、明日の開始が予約し直されているので取り消す
                        TaskAlertScheduler.cancel(context, taskId)
                    }
                    // 終了の通知は「スケジュール終了時の表示」「終了の通知音」と終了の「再生時間」で出す。
                    // 表示をまだ選んでいないときは「通知」（前の版と同じ動き）。音と再生時間は、選ぶまでは開始と同じ
                    val endAlertMode = prefs?.get(KEY_END_ALERT_MODE) ?: "NOTIFICATION"
                    if (endAlertMode == "OFF") return@launch
                    val endSoundUriStr = prefs?.get(KEY_END_NOTIFICATION_SOUND) ?: soundUriStr
                    // 開始と同じく TaskAlertService で出し、TickAt が音を鳴らす
                    // （スマホに鳴らしてもらうと、TickAt の中にコピーした音は読めずに鳴らないため）
                    val endDuration = prefs?.get(KEY_END_NOTIFICATION_DURATION)
                        ?: prefs?.get(KEY_NOTIFICATION_DURATION) ?: 5
                    val endIntent = Intent(context, TaskAlertService::class.java).apply {
                        putExtra("task_id", taskId)
                        putExtra("task_name", taskName)
                        putExtra("task_start", start)
                        putExtra("task_end", end)
                        putExtra("task_type", taskType)
                        putExtra("alert_mode", endAlertMode)
                        putExtra("alert_type", "END")
                        putExtra("sound_uri", endSoundUriStr)
                        putExtra("duration", endDuration)
                    }
                    context.startForegroundService(endIntent)
                    return@launch
                }

                // 繰り返しスケジュールは次回分を再スケジュール（モードに関係なく常に実行）
                rescheduleIfNeeded(context, dbTask, taskId, taskName, start, end, memo, repeat, taskType)

                // ONCEタスクの自動オフ（アラーム発火後にタスクを無効化）。終了でオフにするタイムブロックは除く
                if (repeat == RepeatType.ONCE.name && !offAtEnd) {
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
        dbTask: Task?,
        taskId: String, taskName: String,
        start: String, end: String, memo: String, repeat: String,
        taskType: String
    ) {
        if (repeat.isEmpty() || repeat == RepeatType.ONCE.name) return
        // データベースの予定が読めたときは、それで次を予約する（選んだ曜日を使うため）
        if (dbTask != null) {
            TaskAlertScheduler.schedule(context, dbTask)
            return
        }
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
}