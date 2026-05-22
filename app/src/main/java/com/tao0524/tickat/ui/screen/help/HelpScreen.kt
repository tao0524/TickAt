package com.tao0524.tickat.ui.screen.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class HelpItem(val title: String, val body: String)

private val helpItems = listOf(
    HelpItem(
        title = "ウィジェットの追加方法",
        body  = """
ホーム画面を長押しする
→「ウィジェット」をタップする
→「TickAt」を選んで配置する

HyperOS（Xiaomi）の場合は
「Android ウィジェット」の中に
TickAt があります。
        """.trimIndent()
    ),
    HelpItem(
        title = "タスクとは",
        body  = """
タスクとは「この時間帯にこの情報を表示する」という設定です。

・開始〜終了時刻でウィジェットに表示される時間帯を設定します
・表示機能で何を見せるかを選びます
・繰り返しで毎日・平日・週1回・1回のみを選べます

現在時刻に該当するタスクが自動で選ばれて表示されます。
        """.trimIndent()
    ),
    HelpItem(
        title = "5つの表示機能",
        body  = """
時計
現在時刻と終了時刻を大きく表示します。

日付
今日の日付・曜日と今年の残り日数を表示します。

カウントダウン
指定した期日までの残り日数・時間・分を表示します。

次の予定
タスクの開始時刻と「あと○分で開始」などの状態を表示します。

メモ
自由に入力した一言メモを全画面で大きく表示します。
        """.trimIndent()
    ),
    HelpItem(
        title = "ウィジェットが更新されない",
        body  = """
バッテリー最適化が有効になっていると、時刻の更新が止まることがあります。

設定 → アプリ → TickAt
→ バッテリー（またはバッテリー詳細）
→「制限なし」に変更してください。

HyperOS の場合は「省エネ」ではなく「制限なし」を選択してください。
        """.trimIndent()
    ),
    HelpItem(
        title = "タスクが表示されない",
        body  = """
以下をご確認ください。

・タスクの時間帯が現在時刻と一致しているか
・タスクが1件も登録されていないか

タスク未設定・時間外の場合は「今は自由な時間」と表示されます。＋ボタンからタスクを追加してください。
        """.trimIndent()
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val expandedStates = remember { mutableStateListOf(*Array(helpItems.size) { false }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ヘルプ",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            helpItems.forEachIndexed { index, item ->
                HelpSection(
                    title      = item.title,
                    body       = item.body,
                    isExpanded = expandedStates[index],
                    onToggle   = { expandedStates[index] = !expandedStates[index] }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    body: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (isExpanded) "▲" else "▼",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(250)) +
                    fadeIn(animationSpec = tween(250)),
            exit  = shrinkVertically(animationSpec = tween(200)) +
                    fadeOut(animationSpec = tween(150))
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

