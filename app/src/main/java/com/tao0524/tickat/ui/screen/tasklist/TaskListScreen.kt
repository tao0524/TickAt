package com.tao0524.tickat.ui.screen.tasklist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tao0524.tickat.domain.model.RepeatType
import com.tao0524.tickat.domain.model.Task
import com.tao0524.tickat.domain.model.TaskFeature

private fun TaskFeature.accentColor() = when (this) {
    TaskFeature.CLOCK      -> Color(0xFFBB86FC)
    TaskFeature.DATE       -> Color(0xFF81D4FA)
    TaskFeature.COUNTDOWN  -> Color(0xFFEF9A9A)
    TaskFeature.NEXT_EVENT -> Color(0xFFA5D6A7)
    TaskFeature.MEMO       -> Color(0xFFFFE082)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val guideStep by viewModel.guideStep.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TickAt",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Text(
                            text = "設定",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onOpenHelp) {
                        Text(
                            text = "？",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "タスクを追加")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyState(
                onApplyTemplate = { viewModel.applyTemplate() },
                onAddTask = onAddTask,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (guideStep in 1..3) {
                    item(key = "guide") {
                        GuideCard(
                            step = guideStep,
                            onNext = {
                                if (guideStep < 3) viewModel.advanceGuide()
                                else viewModel.completeGuide()
                            },
                            onDismiss = { viewModel.completeGuide() }
                        )
                    }
                }
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onEditTask(task.id) },
                        onDelete = { viewModel.delete(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onApplyTemplate: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clockColor = Color(0xFFBB86FC)
    val dateColor = Color(0xFF81D4FA)
    val memoColor = Color(0xFFFFE082)
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "時間割を作りましょう",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "時間帯ごとにウィジェットの表示内容を切り替えられます",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            val barH = size.height
            val hourW = size.width / 24f
            val r = 6.dp.toPx()

            drawRoundRect(
                color = emptyBarColor,
                size = Size(size.width, barH),
                cornerRadius = CornerRadius(r)
            )
            drawRect(
                color = clockColor,
                topLeft = Offset(hourW * 7, 0f),
                size = Size(hourW * 2, barH)
            )
            drawRect(
                color = dateColor,
                topLeft = Offset(hourW * 9, 0f),
                size = Size(hourW * 9, barH)
            )
            drawRect(
                color = memoColor,
                topLeft = Offset(hourW * 18, 0f),
                size = Size(hourW * 5, barH)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "朝の時計" to clockColor,
                "今日の日付" to dateColor,
                "おつかれさま" to memoColor
            ).forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = color)
                    }
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onApplyTemplate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "おすすめを使ってみる",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onAddTask) {
            Text(
                "自分で作る",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val accentColor = task.feature.accentColor()

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    modifier = Modifier
                        .width(3.dp)
                        .height(64.dp)
                ) {
                    drawRoundRect(
                        color = accentColor,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(size.width / 2)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${task.startTime} – ${task.endTime}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TaskBadge(text = task.feature.label())
                        TaskBadge(text = task.repeat.label())
                    }
                }
                Spacer(Modifier.width(16.dp))
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "削除",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun TaskBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun TaskFeature.label() = when (this) {
    TaskFeature.CLOCK      -> "時計"
    TaskFeature.DATE       -> "日付"
    TaskFeature.COUNTDOWN  -> "カウントダウン"
    TaskFeature.NEXT_EVENT -> "次の予定"
    TaskFeature.MEMO       -> "メモ"
}

private fun RepeatType.label() = when (this) {
    RepeatType.DAILY   -> "毎日"
    RepeatType.WEEKDAY -> "平日"
    RepeatType.WEEKLY  -> "週1回"
    RepeatType.ONCE    -> "1回のみ"
}

@Composable
private fun GuideCard(
    step: Int,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, body) = when (step) {
        1 -> "ウィジェットを配置しよう" to
                "ホーム画面を長押し →「ウィジェット」→ TickAtを選んで配置してください"
        2 -> "タップして確認しよう" to
                "配置したウィジェットをタップすると、今の時間帯の情報が全画面で表示されます"
        else -> "自分好みにカスタマイズ" to
                "タスクをタップすると名前・時間帯・表示内容を自由に変更できます"
    }
    val accentColor = Color(0xFFBB86FC)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(
                                color = if (i < step) accentColor
                                else accentColor.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "×",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (step < 3) "次へ" else "完了",
                    modifier = Modifier.padding(vertical = 2.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}