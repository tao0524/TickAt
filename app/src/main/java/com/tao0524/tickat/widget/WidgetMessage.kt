package com.tao0524.tickat.widget

import android.content.Context
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
import com.tao0524.tickat.domain.model.WidgetDisplayMode
import com.tao0524.tickat.domain.model.repeatsOn
import com.tao0524.tickat.ui.screen.settings.AmPmLabel
import com.tao0524.tickat.ui.screen.settings.AppSettings
import java.time.DayOfWeek
import java.time.LocalTime

// 12時間制の時刻に、表示設定の「AM/PM表記」に合わせて午前・午後（AM・PM）を付ける。hour は 0〜23、text は「8:00」の形。
// 日本語は前に付けて「午前8:00」、英語は後ろに空白を入れて「8:00 AM」（Unicode CLDR の日本語・英語の時刻の書き方）
private fun withAmPm(hour: Int, text: String, label: AmPmLabel): String =
    if (label == AmPmLabel.ENGLISH) "$text ${if (hour < 12) "AM" else "PM"}"
    else "${if (hour < 12) "午前" else "午後"}$text"

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
        // 12時間制では、同じ行の「時刻表示」の予定と同じく、「AM/PM表記」に合わせて午前・午後（AM・PM）を付ける。
        // 始まりと終わりが同じ午前（午後）なら、日本語は最初だけ「午前6:00〜6:30」、英語は最後だけ「6:00〜6:30 AM」。
        // またがるときは両方に付ける（Unicode CLDR の日本語・英語の時間の範囲の書き方。区切りはアプリのほかの所と同じ「〜」）
        val range = if (settings.use24Hour) {
            "$start〜$end"
        } else {
            val startHour = activeBlock.startTime.hour
            val endHour = activeBlock.endTime.hour
            when {
                (startHour < 12) != (endHour < 12) ->
                    "${withAmPm(startHour, start, settings.amPmLabel)}〜${withAmPm(endHour, end, settings.amPmLabel)}"
                settings.amPmLabel == AmPmLabel.ENGLISH -> "$start〜${withAmPm(endHour, end, settings.amPmLabel)}"
                else -> "${withAmPm(startHour, start, settings.amPmLabel)}〜$end"
            }
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
                        withAmPm(task.startTime.hour, task.startTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm")), settings.amPmLabel)
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
                        "⏰ " + withAmPm(cal.get(java.util.Calendar.HOUR_OF_DAY), "%d:%02d".format(h, m), settings.amPmLabel)
                    }
                } else ""
            } else ""
        } else ""
    }
}
