package com.tao0524.tickat.widget

import android.content.Context
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.domain.model.WidgetDisplayMode
import com.tao0524.tickat.domain.model.repeatsOn
import com.tao0524.tickat.ui.screen.settings.AppSettings
import java.time.DayOfWeek
import java.time.LocalTime

// ウィジェットの時計の下に出す行（メッセージ）を決める。
// ホーム画面のウィジェット（WidgetUpdateService）と、表示設定のプレビュー（SettingsScreen）の両方がこの関数を使う。
// 出すものがないときは "" を返す。
internal fun buildMessageText(
    context: Context,
    settings: AppSettings,
    tasks: List<Task>,
    now: LocalTime,
    todayDow: DayOfWeek
): String {
    // 今日が繰り返す曜日でない予定は出さない（通知と同じ曜日だけ。「1回のみ」はいつでも出す）
    val enabledTasks = tasks.filter { it.isEnabled && it.repeatsOn(todayDow) }
    val timeblocks = enabledTasks.filter { it.taskType == TaskType.TIMEBLOCK }
    // 「非表示」を選んだ予定は、やっている最中もウィジェットに出さない
    val activeBlock = timeblocks.firstOrNull {
        it.displayMode != WidgetDisplayMode.HIDDEN && now >= it.startTime && now < it.endTime
    }

    return if (activeBlock != null && settings.showTaskName) {
        // 進行中のタイムブロックは最優先
        val fmt = if (settings.use24Hour) "H:mm" else "h:mm"
        val sf = java.time.format.DateTimeFormatter.ofPattern(fmt)
        val start = activeBlock.startTime.format(sf)
        val end = activeBlock.endTime.format(sf)
        // 12時間制では、同じ行の「時刻表示」の予定と同じく「午前」「午後」を付ける。
        // 始まりと終わりが同じ午前（午後）なら最初だけ、またがるときは両方に付ける（Unicode CLDR の日本語の時間の範囲の書き方）
        val range = if (settings.use24Hour) {
            "$start〜$end"
        } else {
            val startAmPm = if (activeBlock.startTime.hour < 12) "午前" else "午後"
            val endAmPm = if (activeBlock.endTime.hour < 12) "午前" else "午後"
            if (startAmPm == endAmPm) "$startAmPm$start〜$end" else "$startAmPm$start〜$endAmPm$end"
        }
        "${activeBlock.name} $range"
    } else {
        // 全タスクからdisplayModeに応じて候補を収集
        data class Candidate(val minutesUntil: Long, val text: String)
        val candidates = mutableListOf<Candidate>()

        for (task in enabledTasks) {
            if (task.displayMode == WidgetDisplayMode.HIDDEN) continue
            // 進行中のタイムブロックはスキップ
            if (task.taskType == TaskType.TIMEBLOCK && now >= task.startTime && now < task.endTime) continue

            val rawMinutes = java.time.Duration.between(now, task.startTime).toMinutes()
            if (rawMinutes < 0 && task.repeat != RepeatType.ONCE) continue
            val minutesUntil = if (rawMinutes < 0) rawMinutes + 1440 else rawMinutes

            when (task.displayMode) {
                WidgetDisplayMode.COUNTDOWN -> {
                    val text = when {
                        minutesUntil >= 60 -> "${task.name}まで あと${minutesUntil / 60}時間${minutesUntil % 60}分"
                        minutesUntil >= 1  -> "${task.name}まで あと${minutesUntil}分"
                        else               -> "${task.name}まで あと1分未満"
                    }
                    candidates.add(Candidate(minutesUntil, text))
                }
                WidgetDisplayMode.SIMPLE -> {
                    val timeStr = if (settings.use24Hour) {
                        task.startTime.format(java.time.format.DateTimeFormatter.ofPattern("H:mm"))
                    } else {
                        val amPm = if (task.startTime.hour < 12) "午前" else "午後"
                        "$amPm${task.startTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm"))}"
                    }
                    val label = if (task.repeat == RepeatType.ONCE) {
                        if (task.startTime > now) "今日" else "明日"
                    } else ""
                    val prefix = if (label.isNotEmpty()) "$label " else ""
                    candidates.add(Candidate(minutesUntil, "$prefix$timeStr ${task.name}"))
                }
                else -> { /* HIDDEN — already filtered */ }
            }
        }

        // 「次の予定を表示」がオンなら直近の予定。出す予定がないときは、「次のアラームを表示」がオンならシステムアラーム
        // （「次の予定を表示」がオフでも、「次のアラームを表示」がオンならアラームを出す）
        val nearest = if (settings.showCountdown) candidates.minByOrNull { it.minutesUntil } else null
        if (nearest != null) {
            nearest.text
        } else if (settings.showNextAlarm) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val nextAlarm = alarmManager.nextAlarmClock
            val creatorPkg = nextAlarm?.showIntent?.creatorPackage
            val isXiaomiCalendar = creatorPkg == "com.xiaomi.calendar"
            if (nextAlarm?.showIntent != null && !isXiaomiCalendar) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }
                val isSystemAlarm = cal.get(java.util.Calendar.HOUR_OF_DAY) == 0
                        && cal.get(java.util.Calendar.MINUTE) == 0
                        && cal.get(java.util.Calendar.SECOND) == 0
                        && cal.get(java.util.Calendar.MILLISECOND) == 0
                if (!isSystemAlarm) {
                    if (settings.use24Hour) {
                        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val m = cal.get(java.util.Calendar.MINUTE)
                        "⏰ %d:%02d".format(h, m)
                    } else {
                        val h = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
                        val m = cal.get(java.util.Calendar.MINUTE)
                        val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "午前" else "午後"
                        "⏰ $amPm$h:%02d".format(m)
                    }
                } else ""
            } else ""
        } else ""
    }
}
