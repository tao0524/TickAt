package com.tao0524.tickat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskType
import java.time.DayOfWeek

// ─── 前の版の「週1回」の予定に、繰り返す曜日を選んでもらう小さな画面 ───
// 曜日のない「曜日を選ぶ」の予定（Task.needsRepeatDays）がある間、1つずつ出す。オフの予定にも出す。

const val REPEAT_DAYS_PROMPT_TITLE = "繰り返す曜日を選んでください"

const val REPEAT_DAYS_PROMPT_MESSAGE =
    "新しい版で、曜日を選べるようになりました。選んだ曜日に通知し、ウィジェットにも表示します。いくつでも選べます。"

private val promptDayLabels = listOf("月", "火", "水", "木", "金", "土", "日")

// 選んだ曜日で予定を作り直す。予定を作る画面と同じく、7つ全部は「毎日」、月〜金は「平日」にし、曜日を書くのは「曜日を選ぶ」だけ
fun Task.withChosenRepeatDays(days: Int): Task = when (days and 0x7F) {
    0x7F -> copy(repeat = RepeatType.DAILY, repeatDays = 0)
    0x1F -> copy(repeat = RepeatType.WEEKDAY, repeatDays = 0)
    else -> copy(repeat = RepeatType.WEEKLY, repeatDays = days and 0x7F)
}

/** 曜日を1つ以上選んで「OK」を押すまで閉じない（◁ や画面の外を押しても何もしない） */
@Composable
fun RepeatDaysDialog(
    task: Task,
    onConfirm: (Int) -> Unit
) {
    var days by rememberSaveable(task.id) { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(REPEAT_DAYS_PROMPT_TITLE) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(REPEAT_DAYS_PROMPT_MESSAGE)
                Column {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 時刻の書き方は予定の一覧と同じ（リマインダーは開始だけ、タイムブロックは開始 – 終了）
                    Text(
                        text = if (task.taskType == TaskType.REMINDER) task.startTime.toString()
                        else "${task.startTime} – ${task.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 7つの曜日のボタン。予定を作る画面（TaskEditScreen）と同じ形・色
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DayOfWeek.values().forEach { day ->
                        val bit = 1 shl (day.value - 1)
                        val selected = (days and bit) != 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .toggleable(
                                    value = selected,
                                    role = Role.Checkbox,
                                    onValueChange = { days = days xor bit }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = promptDayLabels[day.value - 1],
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(days) },
                enabled = days != 0
            ) { Text("OK") }
        }
    )
}
