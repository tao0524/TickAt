package com.tao0524.tickat.ui.screen.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── イラスト専用カラー（OnboardingScreenと統一） ───
private val IllustSurface     = Color(0xFF2B2930)
private val IllustPrimaryText = Color(0xFFFFFFFF)
private val IllustSecondText  = Color(0xFF9E9E9E)

private val Feature1Color = Color(0xFFBB86FC)
private val Feature2Color = Color(0xFF81D4FA)
private val Feature3Color = Color(0xFFA5D6A7)

// ─── データ定義 ───

private data class FeatureInfo(
    val title: String,
    val color: Color,
    val description: String
)

private val features = listOf(
    FeatureInfo(
        title = "スマートな時計表示",
        color = Feature1Color,
        description = "今のスケジュールと、次の予定までのカウントダウンをホーム画面に表示します。"
    ),
    FeatureInfo(
        title = "タイミングを逃さない通知",
        color = Feature2Color,
        description = "スケジュールの開始と終了の時間に、メモと一緒に通知でお知らせします。"
    ),
    FeatureInfo(
        title = "1日の流れをチェック",
        color = Feature3Color,
        description = "タップすると、今日のスケジュール一覧と進み具合をチェックボックスで確認できます。"
    )
)

private data class HelpItem(val title: String, val body: String)

// ─── カスタマイズガイド ───

private data class GuideItem(
    val title: String,
    val emoji: String,
    val steps: List<String>
)

private val guideItems = listOf(
    GuideItem(
        title = "テーマを変える",
        emoji = "🎨",
        steps = listOf(
            "「表示設定」→「テーマ」を開く",
            "上部のタブでダーク・ライト・グラデーション・画像・カスタムを切り替え",
            "好みのテーマカードをタップして選択",
            "「カスタム」タブでは自分だけのオリジナルテーマを作成できます"
        )
    ),
    GuideItem(
        title = "背景をカスタマイズ",
        emoji = "🖼️",
        steps = listOf(
            "「表示設定」→「背景の種類」を開く",
            "透明・単色・線形/放射状グラデーション・画像から選択",
            "「画像」を選ぶと、端末の写真やギャラリーから好きな画像を背景に設定できます",
            "「カラー」セクションで背景色や透明度を調整"
        )
    ),
    GuideItem(
        title = "フォントを変える",
        emoji = "🔤",
        steps = listOf(
            "「表示設定」→「フォント」を開く",
            "フォントファミリー（Roboto, Thin, Condensedなど8種）を選択",
            "フォントウェイト（標準/太め）・斜体・テキストシャドウを設定",
            "「全体の大きさ」スライダーでウィジェット全体のサイズ感を調整",
            "時刻・日付・メッセージのサイズを個別に変えると、配置のバランスが変わり印象が大きく変化します",
            "12時間制ではAM/PMのサイズも調整可能。時刻との大きさの比率で左右のバランスが変わります",
            "ここはウィジェットの「顔」になる部分です。プレビューを見ながらいろいろ試してみてください！"
        )
    ),
    GuideItem(
        title = "色を変える",
        emoji = "🌈",
        steps = listOf(
            "「表示設定」→「カラー」を開く",
            "時刻のテキスト色・日付テキスト色・メッセージテキスト色を個別に設定",
            "12時間制の場合はAM/PMの色も設定可能"
        )
    ),
    GuideItem(
        title = "時刻表示を調整",
        emoji = "🕐",
        steps = listOf(
            "「表示設定」→「時刻」を開く",
            "24時間制/12時間制を選択",
            "秒数の表示ON/OFFを切り替え",
            "時刻にズレがある場合は「時刻の微調整」で補正"
        )
    ),
    GuideItem(
        title = "日付の表示形式",
        emoji = "📅",
        steps = listOf(
            "「表示設定」→「日付」を開く",
            "日付のフォーマット（例: 6月17日, 2026/06/17 など）を選択",
            "曜日のフォーマット（火, 火曜日 など）を選択"
        )
    ),
    GuideItem(
        title = "レイアウトを調整",
        emoji = "📐",
        steps = listOf(
            "「表示設定」→「レイアウト」を開く",
            "角丸スタイル：Pill（全丸）・Rounded・Soft・Square（四角）の4種から選択",
            "スライダーで角の丸みを細かく微調整できます",
            "「スケジュール名を表示」「カウントダウンを表示」「次のアラームを表示」で表示項目を制御",
            "「コンパクトな背景」でウィジェットの縦幅を詰めることもできます"
        )
    ),
    GuideItem(
        title = "通知の設定",
        emoji = "🔔",
        steps = listOf(
            "「表示設定」→「通知」を開く",
            "フルスクリーン：開始時に全画面表示",
            "通知：開始時にバナー通知のみ",
            "OFF：通知を表示しない",
            "カウントダウン通知音を端末の着信音・通知音から選択可能",
            "再生時間は3秒・5秒・10秒・15秒・30秒から選べます"
        )
    ),
    GuideItem(
        title = "複数ウィジェットの管理",
        emoji = "🔧",
        steps = listOf(
            "ホーム画面にTickAtウィジェットを複数配置できます",
            "トップ画面の⚙アイコン → ウィジェット一覧を表示",
            "各ウィジェットをタップすると個別の表示設定画面が開きます",
            "ウィジェットごとに異なるテーマ・色・フォントを設定可能"
        )
    )
)

// ─── よくある質問 ───

private val faqItems = listOf(
    HelpItem(
        title = "ウィジェットの追加方法",
        body  = """
ホーム画面を長押しする
→「ウィジェット」をタップする
→「TickAt」を選んで配置する

一部の機種では「Android ウィジェット」の中に表示される場合があります。
        """.trimIndent()
    ),
    HelpItem(
        title = "複数ウィジェットを配置したい",
        body  = """
ホーム画面にTickAtウィジェットを何個でも配置できます。

それぞれのウィジェットに異なるテーマや色、フォントを設定できるので、用途や気分に合わせてカスタマイズを楽しめます。

設定はトップ画面の⚙アイコンからウィジェット一覧を開き、変更したいウィジェットをタップしてください。
        """.trimIndent()
    ),
    HelpItem(
        title = "タイムブロックとリマインダーの違い",
        body  = """
「タイムブロック」は開始〜終了時間があり、ウィジェットに現在のスケジュール名と時間帯が表示されます。

「リマインダー」は指定時刻に通知のみを行います。ウィジェットには表示されません。
        """.trimIndent()
    ),
    HelpItem(
        title = "アラーム時刻が表示されない",
        body  = """
以下を確認してください:

① 「表示設定」で「次のアラームを表示」がONになっているか
② 端末の時計アプリでアラームがセットされているか
③ スケジュールが進行中の場合はスケジュール名が優先されます

一部の機種では、アラーム情報が正しく取得できない場合があります。
        """.trimIndent()
    ),
    HelpItem(
        title = "フォントサイズを変えたい",
        body  = """
「表示設定」→「フォント」セクションの下部にサイズ調整があります。

時刻・日付・メッセージ・AM/PMのサイズをそれぞれ個別に＋−ボタンで調整できます。プレビューで変更を確認してから「保存」してください。
        """.trimIndent()
    ),
    HelpItem(
        title = "「編集前に戻す」と「デフォルトに戻す」の違い",
        body  = """
「編集前に戻す」は、今の編集内容を取り消して、最後に保存した状態に戻します。

「デフォルトに戻す」は、右上の︙メニューから選べます。すべての設定を工場出荷時の初期状態にリセットします。
        """.trimIndent()
    ),
    HelpItem(
        title = "背景が透明にならない",
        body  = """
「表示設定」→「背景の種類」で「透明」を選択してください。

テーマで選んだ背景色は「背景の種類」の設定で上書きされます。透明にしたい場合は「テーマ」ではなく「背景の種類」で設定します。
        """.trimIndent()
    )
)

// ─── 困ったときは ───

private val troubleshootItems = listOf(
    HelpItem(
        title = "ウィジェットが更新されない",
        body  = """
バッテリー最適化が有効になっていると、時刻の更新が止まることがあります。

設定 → アプリ → TickAt → バッテリー（またはバッテリー詳細）→「制限なし」に変更してください。

機種によって表示名が異なる場合があります。「制限なし」に相当する設定を選んでください。
        """.trimIndent()
    ),
    HelpItem(
        title = "スケジュールが表示されない",
        body  = """
以下を確認してください:

① スケジュールが登録されているか
② 「スケジュール名を表示」がONになっているか
③ 「カウントダウンを表示」がONになっているか

ウィジェットには現在進行中の「タイムブロック」か、次の予定へのカウントダウンが表示されます。
        """.trimIndent()
    ),
    HelpItem(
        title = "再起動後にウィジェットが真っ黒になる",
        body  = """
端末のバッテリーセーバー（省エネ設定）が有効になっていると、再起動後にウィジェットが表示されないことがあります。

特に Xiaomi / POCO / Redmi 端末（HyperOS / MIUI）では、再起動のたびにバッテリー制限が自動的にオンに戻る場合があります。

以下の設定を確認してください:

① 設定 → アプリ → TickAt → バッテリー →「制限なし」に変更
② 下の「バッテリー最適化の設定を開く」ボタンから直接設定できます

※ アプリを一度タップして開くだけでも復帰します。
        """.trimIndent()
    ),
    HelpItem(
        title = "時刻がずれている",
        body  = """
「表示設定」→「時刻」→「時刻の微調整」で補正できます。

インターネット上の正確な時刻（協定世界時：UTC）を基準にして調整してください。
        """.trimIndent()
    )
)

// ─── 端末固有の問題 ───

private val deviceIssueItems = listOf(
    HelpItem(
        title = "アラーム時刻が正しく表示されない",
        body  = """
お使いの端末にプリインストールされている時計アプリが、アラーム情報を正しく提供しないことがあります。

この場合、Google 時計アプリ（Google Play から無料でインストール可能）に切り替えてアラームを設定すると、正しく表示されるようになります。

※ これはアプリの不具合ではなく、端末メーカーの時計アプリの仕様によるものです。
        """.trimIndent()
    ),
    HelpItem(
        title = "ウィジェットがバックグラウンドで停止する",
        body  = """
一部のメーカー（Xiaomi、Samsung、OPPO など）は、バッテリー節約のためにアプリのバックグラウンド動作を強制的に停止させることがあります。

以下の設定を確認してください:

① 設定 → アプリ → TickAt → バッテリー →「制限なし」に変更
② 自動起動（オートスタート）の許可を有効にする
③ タスク一覧で TickAt を「ロック」する（スワイプで消えないようにする）

※ 設定の名称や場所は機種によって異なります。
        """.trimIndent()
    ),
    HelpItem(
        title = "OS アップデート後に設定がリセットされる",
        body  = """
一部の端末では、OS のアップデート後にバッテリー最適化の設定が初期状態に戻ることがあります。

アップデート後にウィジェットの動作がおかしくなった場合は、バッテリーや自動起動の設定をもう一度確認してください。
        """.trimIndent()
    )
)

// ─── メイン画面 ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
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
        val faqExpandedStates = remember { mutableStateListOf(*Array(faqItems.size) { false }) }
        val troubleExpandedStates = remember { mutableStateListOf(*Array(troubleshootItems.size) { false }) }
        val deviceIssueExpandedStates = remember { mutableStateListOf(*Array(deviceIssueItems.size) { false }) }
        val guideExpandedStates = remember { mutableStateListOf(*Array(guideItems.size) { false }) }
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── セクション1：TickAtの3つの機能 ──
            Text(
                text = "TickAtの3つの機能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "毎日のスケジュールを、3つの機能でサポートします。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(16.dp))

            features.forEachIndexed { index, feature ->
                FeatureCard(feature = feature, index = index)
                if (index < features.size - 1) {
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── セクション2：カスタマイズガイド ──
            Text(
                text = "カスタマイズガイド",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ウィジェットの見た目を自分好みに設定できます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(12.dp))

            guideItems.forEachIndexed { index, guide ->
                GuideCard(
                    guide      = guide,
                    isExpanded = guideExpandedStates[index],
                    onToggle   = { guideExpandedStates[index] = !guideExpandedStates[index] }
                )
                if (index < guideItems.size - 1) {
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── セクション3：よくある質問 ──
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
                    isExpanded = faqExpandedStates[index],
                    onToggle   = { faqExpandedStates[index] = !faqExpandedStates[index] }
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── セクション4：困ったときは ──
            Text(
                text = "困ったときは",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            troubleshootItems.forEachIndexed { index, item ->
                FaqSection(
                    title      = item.title,
                    body       = item.body,
                    isExpanded = troubleExpandedStates[index],
                    onToggle   = { troubleExpandedStates[index] = !troubleExpandedStates[index] }
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── セクション5：端末固有の問題について ──
            Text(
                text = "端末固有の問題について",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "一部のスマートフォンメーカーは、バッテリー節約のために独自の制限を設けています。TickAtの動作に影響が出る場合は、以下を参考にしてください。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))

            deviceIssueItems.forEachIndexed { index, item ->
                FaqSection(
                    title      = item.title,
                    body       = item.body,
                    isExpanded = deviceIssueExpandedStates[index],
                    onToggle   = { deviceIssueExpandedStates[index] = !deviceIssueExpandedStates[index] }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── バッテリー最適化設定ボタン ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "⚡  バッテリー最適化の設定を開く",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "一覧から TickAt を探し、「最適化しない」に変更してください。再起動後もウィジェットが正常に表示されるようになります。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 外部リンクカード ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://dontkillmyapp.com") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "🔗  お使いの端末の詳しい設定方法",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "dontkillmyapp.com でメーカー名を選ぶと、機種ごとの具体的な設定手順が確認できます。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "※ 外部サイトです。TickAt開発元とは関係ありません。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── 機能カード ───

@Composable
private fun FeatureCard(feature: FeatureInfo, index: Int) {
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
                when (index) {
                    0 -> WidgetIllustration()
                    1 -> NotificationIllustration()
                    2 -> ChecklistIllustration()
                }
            }

            Spacer(Modifier.height(12.dp))

            // タイトル
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = feature.color)
                }
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(6.dp))

            // 説明
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

// ─── 各機能のミニイラスト ───

@Composable
private fun WidgetIllustration() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "14:32",
            color = IllustPrimaryText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "ミーティングまで",
                color = IllustSecondText,
                fontSize = 11.sp
            )
            Text(
                text = "あと 28分",
                color = IllustPrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NotificationIllustration() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF3A383F),
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(0.8f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "朝のルーティン 終了", color = IllustPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = "9:00", color = IllustSecondText, fontSize = 11.sp)
            }
            Text(
                text = "7:00〜9:00",
                color = IllustSecondText,
                fontSize = 12.sp
            )
            Text(
                text = "💬 ストレッチと朝食",
                color = IllustSecondText,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ChecklistIllustration() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(16.dp).background(Feature3Color, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Text("✓", color = IllustSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text("朝のルーティン", color = IllustSecondText, fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(16.dp).border(1.5.dp, IllustSecondText, RoundedCornerShape(4.dp)))
            Text("ミーティング", color = IllustPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── カスタマイズガイドカード ───

@Composable
private fun GuideCard(
    guide: GuideItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = guide.emoji,
                        fontSize = 20.sp
                    )
                    Text(
                        text = guide.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    guide.steps.forEachIndexed { index, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
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