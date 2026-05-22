package com.tao0524.tickat.ui.screen.expanded

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ExpandedScreen(
    viewModel: ExpandedViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    fun dismiss() {
        scope.launch {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            visible = false
            delay(250)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { dismiss() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                initialScale = 0.85f
            ) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                targetScale = 0.92f
            ) + fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                } else {
                    FeatureContent(task = uiState.currentTask, now = uiState.now)
                }

                AnimatedVisibility(
                    visible = !uiState.isLoading && uiState.nextTask != null,
                    modifier = Modifier.align(Alignment.TopEnd),
                    enter = fadeIn(tween(300)) + slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { -it / 2 }
                    ),
                    exit = fadeOut(tween(200))
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "+1",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureContent(task: Task?, now: LocalTime) {
    if (task == null) {
        EmptyTaskContent(now = now)
        return
    }
    when (task.feature) {
        TaskFeature.CLOCK      -> ClockContent(now, task.name, task.endTime)
        TaskFeature.DATE       -> DateContent(task.name)
        TaskFeature.COUNTDOWN  -> CountdownContent(task)
        TaskFeature.NEXT_EVENT -> NextEventContent(task, now)
        TaskFeature.MEMO       -> MemoContent(task)
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SupplementText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun EmptyTaskContent(now: LocalTime) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubLabel(text = "今は自由な時間")
        Text(
            text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 80.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp
        )
        Spacer(Modifier.height(4.dp))
        SupplementText(text = "タスクが設定されていません")
    }
}

@Composable
private fun ClockContent(now: LocalTime, taskName: String, endTime: LocalTime?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubLabel(text = taskName)
        Text(
            text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 80.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp
        )
        if (endTime != null) {
            SupplementText(text = "終了  ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
        }
    }
}

@Composable
private fun DateContent(taskName: String) {
    val today = LocalDate.now()
    val dowLabel = when (today.dayOfWeek!!) {
        DayOfWeek.MONDAY    -> "月曜日"
        DayOfWeek.TUESDAY   -> "火曜日"
        DayOfWeek.WEDNESDAY -> "水曜日"
        DayOfWeek.THURSDAY  -> "木曜日"
        DayOfWeek.FRIDAY    -> "金曜日"
        DayOfWeek.SATURDAY  -> "土曜日"
        DayOfWeek.SUNDAY    -> "日曜日"
    }
    val endOfYear = LocalDate.of(today.year, 12, 31)
    val daysLeft = ChronoUnit.DAYS.between(today, endOfYear) + 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubLabel(text = dowLabel)
        Text(
            text = today.format(DateTimeFormatter.ofPattern("M月d日")),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light
        )
        SupplementText(text = "今年の残り ${daysLeft}日")
    }
}

@Composable
private fun CountdownContent(task: Task) {
    val nowDT = LocalDateTime.now()
    val targetDT = task.targetDateTime
        ?: LocalDateTime.of(LocalDate.now(), task.endTime)

    val totalMinutes = ChronoUnit.MINUTES.between(nowDT, targetDT).coerceAtLeast(0)
    val days    = totalMinutes / (60 * 24)
    val hours   = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubLabel(text = task.name)
        if (days > 0) {
            Text(
                text = "${days}日",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "${hours}時間 ${minutes}分",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 24.sp
            )
        } else {
            Text(
                text = "${hours}時間 ${minutes}分",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light
            )
        }
        if (task.targetDateTime != null) {
            SupplementText(text = targetDT.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")))
        }
    }
}

@Composable
private fun NextEventContent(task: Task, now: LocalTime) {
    val isOngoing = now >= task.startTime && now < task.endTime
    val minsUntil = ChronoUnit.MINUTES.between(now, task.startTime).coerceAtLeast(0)
    val statusText = when {
        isOngoing      -> "進行中"
        minsUntil < 60 -> "あと ${minsUntil}分で開始"
        else           -> "あと ${minsUntil / 60}時間で開始"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubLabel(text = "次の予定")
        Text(
            text = task.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isOngoing) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = if (isOngoing) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        SupplementText(text = task.startTime.format(DateTimeFormatter.ofPattern("HH:mm 開始")))
    }
}

@Composable
private fun MemoContent(task: Task) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubLabel(text = task.name)
        Text(
            text = task.memoText.ifBlank { "（メモなし）" },
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            lineHeight = 42.sp
        )
    }
}