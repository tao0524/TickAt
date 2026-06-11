package com.tao0524.tickat.ui.screen.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── イラスト専用カラー（OnboardingScreenと統一） ───
private val IllustSurface     = Color(0xFF2B2930)
private val IllustPrimaryText = Color(0xFFFFFFFF)
private val IllustSecondText  = Color(0xFF9E9E9E)

// 各モードのテーマカラー（OnboardingScreen Page3と統一）
private val ClockColor     = Color(0xFFBB86FC)
private val DateColor      = Color(0xFF81D4FA)
private val CountdownColor = Color(0xFFEF9A9A)
private val NextEventColor = Color(0xFFA5D6A7)
private val MemoColor      = Color(0xFFFFE082)

// ─── データ定義 ───

private data class ModeInfo(
    val name: String,
    val color: Color,
    val scene: String,
    val description: String
)

private val modes = listOf(
    ModeInfo(
        name = "時計",
        color = ClockColor,
        scene = "仕事中・勉強中に",
        description = "現在時刻と終了時刻を大きく表示。今の時間帯があとどれくらいか一目でわかります。"
    ),
    ModeInfo(
        name = "日付",
        color = DateColor,
        scene = "朝の確認に",
        description = "今日の日付・曜日と、今年の残り日数を表示します。"
    ),
    ModeInfo(
        name = "カウントダウン",
        color = CountdownColor,
        scene = "試験・イベント前に",
        description = "指定した期日までの残り日数・時間・分をリアルタイムで表示します。"
    ),
    ModeInfo(
        name = "次の予定",
        color = NextEventColor,
        scene = "移動前・休憩中に",
        description = "次のシーンの開始時刻と「あと○分」を表示。切り替えのタイミングを逃しません。"
    ),
    ModeInfo(
        name = "メモ",
        color = MemoColor,
        scene = "リマインダーに",
        description = "自由に入力した一言メモを全画面で大きく表示します。"
    )
)

private data class HelpItem(val title: String, val body: String)

private val faqItems = listOf(
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
        title = "シーンとは",
        body  = """
シーンとは「この時間帯にこの情報を表示する」という設定です。

・開始〜終了時刻でウィジェットに表示される時間帯を設定します
・表示機能で何を見せるかを選びます
・繰り返しで毎日・平日・週1回・1回のみを選べます

現在時刻に該当するシーンが自動で選ばれて表示されます。
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
        title = "シーンが表示されない",
        body  = """
以下をご確認ください。

・シーンの時間帯が現在時刻と一致しているか
・シーンが1件も登録されていないか

シーン未設定・時間外の場合は「今は自由な時間」と表示されます。＋ボタンからシーンを追加してください。
        """.trimIndent()
    )
)

// ─── メイン画面 ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val expandedStates = remember { mutableStateListOf(*Array(faqItems.size) { false }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "使い方ガイド",
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

            // ── セクション1：TickAtでできること ──
            Text(
                text = "5つの表示モード",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "時間帯ごとに必要な情報を、ウィジェットに表示できます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(16.dp))

            modes.forEachIndexed { index, mode ->
                ModeCard(mode = mode)
                if (index < modes.size - 1) {
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── セクション2：よくある質問 ──
            Text(
                text = "よくある質問",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            faqItems.forEachIndexed { index, item ->
                FaqSection(
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

// ─── モードカード ───

@Composable
private fun ModeCard(mode: ModeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ミニイラスト
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IllustSurface),
                contentAlignment = Alignment.Center
            ) {
                when (mode.name) {
                    "時計"         -> ClockIllustration()
                    "日付"         -> DateIllustration()
                    "カウントダウン" -> CountdownIllustration()
                    "次の予定"     -> NextEventIllustration()
                    "メモ"         -> MemoIllustration()
                }
            }

            Spacer(Modifier.height(12.dp))

            // モード名 + シーン
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = mode.color)
                }
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "—",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = mode.scene,
                    style = MaterialTheme.typography.bodySmall,
                    color = mode.color,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(6.dp))

            // 説明
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

// ─── 各モードのミニイラスト ───

@Composable
private fun ClockIllustration() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "14:32",
            color = IllustPrimaryText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "終了  15:00",
            color = IllustSecondText.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DateIllustration() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "6月11日（水）",
            color = IllustPrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "今年の残り  203日",
            color = IllustSecondText.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun CountdownIllustration() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "あと 12日 3:42",
            color = IllustPrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "夏休みまで",
            color = IllustSecondText.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun NextEventIllustration() {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "15:00〜",
                color = IllustPrimaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "ミーティング",
                color = IllustSecondText.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NextEventColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = "あと 28分",
                color = NextEventColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MemoIllustration() {
    Text(
        text = "牛乳を買う",
        color = IllustPrimaryText,
        fontSize = 26.sp,
        fontWeight = FontWeight.Light,
        textAlign = TextAlign.Center
    )
}

// ─── FAQアコーディオン ───

@Composable
private fun FaqSection(
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