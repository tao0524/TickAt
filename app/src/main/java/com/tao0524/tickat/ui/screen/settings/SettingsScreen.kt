package com.tao0524.tickat.ui.screen.settings

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.sqrt
import android.view.LayoutInflater
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tao0524.tickat.R
import com.tao0524.tickat.widget.buildBackgroundBitmap
import com.tao0524.tickat.widget.calcFontSizes
import com.tao0524.tickat.widget.buildTextBitmap
import com.tao0524.tickat.widget.buildTimeWithAmPmBitmap
import androidx.compose.foundation.text.KeyboardActions

// ────────────────────────────────────────────────────────────────────────────
// 定数
// ────────────────────────────────────────────────────────────────────────────


private data class ThemePreset(
    val name: String,
    val bgColor: Long,
    val bgGradientEnd: Long = 0L,
    val textColor: Long,
    val gradientDirection: GradientDirection? = null
)


private val darkThemes = listOf(
    ThemePreset("モノクローム", 0xFF1A1A1AL, 0L, 0xFFFFFFFFL),
    ThemePreset("セピア",       0xFF6B4226L, 0L, 0xFFFFF3E0L),
    ThemePreset("ダークブルー", 0xFF0D2137L, 0L, 0xFFE8F4FDL),
)

private val lightThemes = listOf(
    ThemePreset("イエロー", 0xFFFFFDE7L, 0L, 0xFF4E3B00L),
    ThemePreset("グリーン", 0xFFE8F5E9L, 0L, 0xFF1B5E20L),
    ThemePreset("パープル", 0xFFF3E5F5L, 0L, 0xFF4A148CL),
    ThemePreset("ピンク",   0xFFFCE4ECL, 0L, 0xFF880E4FL),
)

private val gradientThemes = listOf(
    ThemePreset("横",   0xFF7B2FF7L, 0xFF2BC0E4L, 0xFFFFFFFFL, GradientDirection.HORIZONTAL),
    ThemePreset("斜め", 0xFFFC4A1AL, 0xFFF7B733L, 0xFFFFFFFFL, GradientDirection.DIAGONAL),
    ThemePreset("縦",   0xFF1A237EL, 0xFF4DD0E1L, 0xFFE8F5E9L, GradientDirection.VERTICAL),
    ThemePreset("円系", 0xFFAD1457L, 0xFFFF6F00L, 0xFFFFFFFFL, GradientDirection.RADIAL),
)

// MCW準拠・統一20色プリセット（bg / text / accent 共用）
private val colorPresets = listOf(
    0xFF000000L, // 黒
    0xFFE53935L, // 赤
    0xFFD81B60L, // ディープローズ
    0xFFEC407AL, // ピンク
    0xFF8E24AAL, // パープル
    0xFF5E35B1L, // ディープパープル
    0xFF3949ABL, // インディゴ
    0xFF1E88E5L, // ブルー
    0xFF039BE5L, // ライトブルー
    0xFF00ACC1L, // シアン
    0xFF00897BL, // ティール
    0xFF43A047L, // グリーン
    0xFF7CB342L, // ライトグリーン
    0xFFC0CA33L, // ライム
    0xFFFDD835L, // イエロー
    0xFFFFB300L, // アンバー
    0xFFFB8C00L, // オレンジ
    0xFF6D4C41L, // ブラウン
    0xFF546E7AL, // ブルーグレー
    0xFFFFFFFFL  // 白
)

// 色相バー（縦グラデーション用・0°〜360°）
private val hueBarColors = listOf(
    Color(0xFFFF0000L),
    Color(0xFFFFFF00L),
    Color(0xFF00FF00L),
    Color(0xFF00FFFFL),
    Color(0xFF0000FFL),
    Color(0xFFFF00FFL),
    Color(0xFFFF0000L)
)

private fun blendToWhite(base: Color, ratio: Float): Color = Color(
    red   = (base.red   + (1f - base.red)   * ratio).coerceIn(0f, 1f),
    green = (base.green + (1f - base.green) * ratio).coerceIn(0f, 1f),
    blue  = (base.blue  + (1f - base.blue)  * ratio).coerceIn(0f, 1f),
    alpha = 1f
)

private fun generateTintShadeColors(baseColor: Long): List<Long> {
    val base  = Color(baseColor)
    val white = Color(0xFFFFFFFFL)
    val black = Color(0xFF000000L)
    return listOf(
        lerp(white, base, 0.10f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(white, base, 0.22f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(white, base, 0.36f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(white, base, 0.52f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(white, base, 0.70f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(white, base, 0.88f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(base, black, 0.15f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(base, black, 0.30f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(base, black, 0.46f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(base, black, 0.62f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(base, black, 0.78f).toArgb().toLong() and 0xFFFFFFFFL,
        lerp(base, black, 0.92f).toArgb().toLong() and 0xFFFFFFFFL
    )
}

// ────────────────────────────────────────────────────────────────────────────
// DrawScope ヘルパー
// ────────────────────────────────────────────────────────────────────────────

private fun contentColorFor(bgColor: Long): Color {
    val r = ((bgColor shr 16) and 0xFF) / 255f
    val g = ((bgColor shr 8)  and 0xFF) / 255f
    val b = ( bgColor         and 0xFF) / 255f
    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text          = title,
        color         = MaterialTheme.colorScheme.onBackground,
        fontSize      = 13.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        modifier      = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp)
    )
}

private fun DrawScope.drawCheckerboard(cellSizeDp: Float = 6f) {
    val cellPx = cellSizeDp.dp.toPx()
    val cols = (size.width  / cellPx).toInt() + 1
    val rows = (size.height / cellPx).toInt() + 1
    for (row in 0..rows) {
        for (col in 0..cols) {
            drawRect(
                color   = if ((row + col) % 2 == 0) Color(0xFFCCCCCC) else Color.White,
                topLeft = Offset(col * cellPx, row * cellPx),
                size    = Size(cellPx, cellPx)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// SettingsScreen
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val saved   by viewModel.settings.collectAsState()
    val hintShown by viewModel.hintSettingsShown.collectAsState()
    val context = LocalContext.current
    var draft   by remember(saved) { mutableStateOf(saved) }

    var currentSoundUri by remember { mutableStateOf(saved.notificationSoundUri) }
    LaunchedEffect(saved.notificationSoundUri) { currentSoundUri = saved.notificationSoundUri }

    val soundName = remember(currentSoundUri) {
        if (currentSoundUri.isEmpty()) "既定の通知音"
        else runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(currentSoundUri))?.getTitle(context) ?: "カスタム音"
        }.getOrElse { "カスタム音" }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val newUri = uri?.toString() ?: ""
            currentSoundUri = newUri
            draft = draft.copy(notificationSoundUri = newUri)
            viewModel.save(draft.copy(notificationSoundUri = newUri))
        }
    }

    val bgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { draft = draft.copy(bgImageUri = it.toString()) }
    }

    var formatExpanded by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var selectedThemeTab by remember { mutableStateOf(0) }
    val pickerTarget = remember { mutableStateOf<String?>(null) }

    val target = pickerTarget.value
    if (target != null) {
        val currentColor = when (target) {
            "bg"       -> draft.bgColor
            "text"     -> draft.textColor
            "date"     -> draft.dateTextColor
            "gradEnd"  -> if (draft.bgGradientEnd != 0L) draft.bgGradientEnd else 0xFF808080L
            "amPm"     -> if (draft.amPmColor != 0L) draft.amPmColor else draft.textColor
            else       -> if (draft.bgColor2 != 0L) draft.bgColor2 else 0xFF808080L
        }
        val dialogTitle = when (target) {
            "bg"       -> "背景色"
            "text"     -> "テキスト色"
            "date"     -> "日付・秒数テキスト色"
            "gradEnd"  -> "グラデーション終了色"
            "amPm"     -> "AM/PMテキスト色"
            else       -> "背景色2"
        }
        ColorPickerDialog(
            title        = dialogTitle,
            presets      = colorPresets,
            initialColor = currentColor,
            initialAlpha = when (target) {
                "bg"       -> draft.bgAlpha
                "bgColor2" -> draft.bgColor2Alpha
                "gradEnd"  -> draft.bgGradientEndAlpha
                else       -> 100
            },
            showAlpha    = target == "bg" || target == "date" || target == "bgColor2" || target == "gradEnd",
            onDismiss    = { pickerTarget.value = null },
            onConfirm    = { color, alpha ->
                draft = when (target) {
                    "bg"       -> draft.copy(bgColor = color, bgAlpha = alpha, bgGradientEnd = 0L)
                    "text"     -> draft.copy(textColor = color)
                    "date"     -> draft.copy(dateTextColor = color)
                    "gradEnd"  -> draft.copy(bgGradientEnd = color, bgGradientEndAlpha = alpha)
                    "amPm"     -> draft.copy(amPmColor = color)
                    else       -> draft.copy(bgColor2 = color, bgColor2Alpha = alpha)
                }
                pickerTarget.value = null
            }
        )
    }

    if (showFontSizeDialog) {
        FontSizePickerDialog(
            currentBalance = draft.clockDateBalance,
            currentScale   = draft.fontScale,
            draft          = draft,
            onDismiss      = { showFontSizeDialog = false },
            onConfirm      = { balance, scale ->
                draft = draft.copy(clockDateBalance = balance, fontScale = scale)
                showFontSizeDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("表示設定", color = MaterialTheme.colorScheme.onBackground) },
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
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(
                    color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick  = {
                        viewModel.save(draft)
                        Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存", modifier = Modifier.padding(vertical = 4.dp), fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = { viewModel.reset(); draft = AppSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("デフォルトに戻す", modifier = Modifier.padding(vertical = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                WidgetPreview(draft)
            }
            if (!hintShown) {
                FirstTimeHint(
                    message = "ウィジェットの色やフォントを変更できます",
                    onDismiss = { viewModel.dismissHintSettings() },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                SectionHeader("テーマ")
                val tabThemes = when (selectedThemeTab) {
                    0    -> darkThemes
                    1    -> lightThemes
                    2    -> gradientThemes
                    else -> emptyList()
                }
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        TabRow(
                            selectedTabIndex = selectedThemeTab,
                            containerColor   = MaterialTheme.colorScheme.surface,
                            contentColor     = MaterialTheme.colorScheme.primary
                        ) {
                            listOf("ダーク", "ライト", "グラデーション", "画像").forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedThemeTab == index,
                                    onClick  = { selectedThemeTab = index },
                                    text     = { Text(title, fontSize = 12.sp) }
                                )
                            }
                        }
                        if (selectedThemeTab == 3) {
                            val imageLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri ->
                                uri?.let { draft = draft.copy(bgImageUri = it.toString()) }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (draft.bgImageUri.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        AsyncImage(
                                            model = draft.bgImageUri,
                                            contentDescription = "背景画像",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Button(
                                    onClick = { imageLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = if (draft.bgImageUri.isEmpty()) "＋ 画像を選ぶ" else "画像を変更する",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tabThemes.chunked(2).forEach { rowThemes ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowThemes.forEach { theme ->
                                            ThemeCard(
                                                theme      = theme,
                                                isSelected = draft.bgColor == theme.bgColor &&
                                                        draft.bgGradientEnd == theme.bgGradientEnd &&
                                                        draft.textColor == theme.textColor,
                                                modifier   = Modifier.weight(1f),
                                                onSelect   = {
                                                    val newBgType =
                                                        if (selectedThemeTab == 2 && theme == gradientThemes.last()) BackgroundType.RADIAL
                                                        else if (selectedThemeTab == 2) BackgroundType.LINEAR
                                                        else BackgroundType.SOLID
                                                    val newGradDir = theme.gradientDirection ?: GradientDirection.DIAGONAL
                                                    draft = draft.copy(
                                                        bgColor           = theme.bgColor,
                                                        bgGradientEnd     = theme.bgGradientEnd,
                                                        textColor         = theme.textColor,
                                                        bgType            = newBgType,
                                                        gradientDirection = newGradDir
                                                    )
                                                }
                                            )
                                        }
                                        if (rowThemes.size < 2) {
                                            if (selectedThemeTab == 0) {
                                                val isTransparentSelected = draft.bgAlpha == 0
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(96.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(Color(0xFF888888), Color(0xFF444444))
                                                            )
                                                        )
                                                        .border(
                                                            width = if (isTransparentSelected) 2.5.dp else 1.dp,
                                                            color = if (isTransparentSelected) MaterialTheme.colorScheme.primary else Color(0xFF3A383F),
                                                            shape = RoundedCornerShape(14.dp)
                                                        )
                                                        .clickable { draft = draft.copy(bgAlpha = 0) }
                                                        .padding(12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text("完全透明", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text("bgAlpha = 0", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                                                    }
                                                    if (isTransparentSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(6.dp)
                                                                .size(18.dp)
                                                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SectionHeader("背景の種類")
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        BackgroundType.TRANSPARENT to "透明",
                        BackgroundType.SOLID        to "単色",
                        BackgroundType.LINEAR       to "線形グラデーション",
                        BackgroundType.RADIAL       to "放射状グラデーション",
                        BackgroundType.IMAGE        to "画像"
                    ).forEachIndexed { index, (type, label) ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { draft = draft.copy(bgType = type) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = draft.bgType == type,
                                onClick  = { draft = draft.copy(bgType = type) }
                            )
                            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    }
                }
                AnimatedVisibility(visible = draft.bgType == BackgroundType.IMAGE) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (draft.bgImageUri.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model              = draft.bgImageUri,
                                    contentDescription = "背景画像",
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Button(
                            onClick  = { bgImageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                if (draft.bgImageUri.isEmpty()) "＋ 画像を選ぶ" else "画像を変更する",
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = draft.bgType == BackgroundType.LINEAR) {
                    Column(modifier = Modifier.padding(top = 0.dp)) {
                        SectionHeader("グラデーションの始点")
                        LinearStartPicker(
                            selected           = draft.linearStartPoint,
                            bgColor            = draft.bgColor,
                            endColor           = if (draft.bgGradientEnd != 0L) draft.bgGradientEnd else draft.bgColor,
                            bgColor2           = draft.bgColor2,
                            gradientColorCount = draft.gradientColorCount,
                            onSelect           = { draft = draft.copy(linearStartPoint = it) }
                        )
                    }
                }

                AnimatedVisibility(visible = draft.bgType == BackgroundType.RADIAL) {
                    Column(modifier = Modifier.padding(top = 0.dp)) {
                        SectionHeader("グラデーションの中心")
                        RadialCenterPicker(
                            selected           = draft.gradientCenter,
                            bgColor            = draft.bgColor,
                            endColor           = if (draft.bgGradientEnd != 0L) draft.bgGradientEnd else draft.bgColor,
                            bgColor2           = draft.bgColor2,
                            gradientColorCount = draft.gradientColorCount,
                            onSelect           = { draft = draft.copy(gradientCenter = it) }
                        )
                    }
                }

                SectionHeader("カラー")
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedVisibility(visible = draft.bgType == BackgroundType.LINEAR || draft.bgType == BackgroundType.RADIAL) {
                        Column {
                            Row(
                                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    text     = "グラデーション色数",
                                    color    = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                SelectOption(
                                    label      = "2色",
                                    isSelected = draft.gradientColorCount == 2,
                                    modifier   = Modifier.width(56.dp),
                                    onSelect   = { draft = draft.copy(gradientColorCount = 2) }
                                )
                                SelectOption(
                                    label      = "3色",
                                    isSelected = draft.gradientColorCount == 3,
                                    modifier   = Modifier.width(56.dp),
                                    onSelect   = { draft = draft.copy(gradientColorCount = 3) }
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }
                    }
                    ColorRow(
                        label   = "背景色",
                        sub     = "アプリ全画面・ウィジェット",
                        color   = draft.bgColor,
                        onClick = { pickerTarget.value = "bg" }
                    )
                    AnimatedVisibility(visible = draft.bgType != BackgroundType.TRANSPARENT && draft.bgType != BackgroundType.IMAGE) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                AlphaSliderRow(
                                    alpha    = draft.bgAlpha,
                                    onChange = { draft = draft.copy(bgAlpha = it) }
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = (draft.bgType == BackgroundType.LINEAR || draft.bgType == BackgroundType.RADIAL) && draft.gradientColorCount == 3) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            val bgColor2Display = if (draft.bgColor2 != 0L) draft.bgColor2 else 0xFF808080L
                            ColorRow(
                                label   = "背景色2",
                                sub     = "グラデーション中間色",
                                color   = bgColor2Display,
                                onClick = { pickerTarget.value = "bgColor2" }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                AlphaSliderRow(
                                    alpha    = draft.bgColor2Alpha,
                                    onChange = { draft = draft.copy(bgColor2Alpha = it) }
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = draft.bgType == BackgroundType.LINEAR || draft.bgType == BackgroundType.RADIAL) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ColorRow(
                                label   = "グラデーション終了色",
                                sub     = if (draft.bgGradientEnd != 0L) "設定済み" else "未設定（タップして追加）",
                                color   = if (draft.bgGradientEnd != 0L) draft.bgGradientEnd else 0xFF808080L,
                                onClick = { pickerTarget.value = "gradEnd" }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                AlphaSliderRow(
                                    alpha    = draft.bgGradientEndAlpha,
                                    onChange = { draft = draft.copy(bgGradientEndAlpha = it) }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ColorRow(
                        label   = "テキスト色",
                        sub     = "時刻・スケジュール名・ラベル",
                        color   = draft.textColor,
                        onClick = { pickerTarget.value = "text" }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ColorRow(
                        label   = "日付・秒数テキスト色",
                        sub     = "日付・秒数の表示色",
                        color   = draft.dateTextColor,
                        onClick = { pickerTarget.value = "date" }
                    )
                }

                SectionHeader("フォント")
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("フォントファミリー", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                WidgetFont.ROBOTO    to "Roboto\n標準",
                                WidgetFont.SERIF     to "Serif\nセリフ",
                                WidgetFont.CONDENSED to "Cond.\n細身",
                                WidgetFont.MONO      to "Mono\n等幅"
                            ).forEach { (font, label) ->
                                SelectOption(
                                    label      = label,
                                    isSelected = draft.fontFamily == font,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(fontFamily = font) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("フォントウェイト", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                TextWeight.REGULAR to "Regular\n標準",
                                TextWeight.BOLD    to "Bold\n太め"
                            ).forEach { (weight, label) ->
                                SelectOption(
                                    label      = label,
                                    isSelected = draft.fontWeight == weight,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(fontWeight = weight) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "斜体",
                        sub      = "イタリック体で表示",
                        checked  = draft.isItalic,
                        onToggle = { draft = draft.copy(isItalic = it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "テキストシャドウ",
                        sub      = "文字に影をつける",
                        checked  = draft.showTextShadow,
                        onToggle = { draft = draft.copy(showTextShadow = it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFontSizeDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("フォントサイズ", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text(
                                text = "%.1f".format(draft.fontScale) + "×  " + when {
                                    draft.clockDateBalance == 0 -> "均等"
                                    draft.clockDateBalance < 0  -> "時刻寄り"
                                    else                        -> "日付寄り"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                            )
                        }
                        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                    }
                }

                SectionHeader("時刻")
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { formatExpanded = !formatExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("時間表示形式", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text(
                                when {
                                    draft.use24Hour -> "24時間制 — 14:32"
                                    draft.amPmPosition == AmPmPosition.AFTER && draft.amPmLabel == AmPmLabel.JAPANESE -> "12時間制 — 2:32 午後"
                                    draft.amPmPosition == AmPmPosition.AFTER -> "12時間制 — 2:32 PM"
                                    draft.amPmLabel == AmPmLabel.JAPANESE -> "12時間制 — 午後 2:32"
                                    else -> "12時間制 — PM 2:32"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                            )
                        }
                        Text(if (formatExpanded) "▲" else "▼", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    AnimatedVisibility(
                        visible = formatExpanded,
                        enter   = expandVertically(tween(250)) + fadeIn(tween(250)),
                        exit    = shrinkVertically(tween(200)) + fadeOut(tween(150))
                    ) {
                        Column {
                            Row(
                                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(true to "24時間制\n14:32", false to "12時間制\n2:32 PM").forEach { (is24, label) ->
                                    SelectOption(
                                        label      = label,
                                        isSelected = draft.use24Hour == is24,
                                        modifier   = Modifier.weight(1f),
                                        onSelect   = { draft = draft.copy(use24Hour = is24) }
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = !draft.use24Hour,
                                enter   = expandVertically(tween(200)) + fadeIn(tween(200)),
                                exit    = shrinkVertically(tween(150)) + fadeOut(tween(150))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
                                    Text("AM/PM位置", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(AmPmPosition.BEFORE to "前\n午後 3:52", AmPmPosition.AFTER to "後ろ\n3:52 午後").forEach { (pos, label) ->
                                            SelectOption(
                                                label      = label,
                                                isSelected = draft.amPmPosition == pos,
                                                modifier   = Modifier.weight(1f),
                                                onSelect   = { draft = draft.copy(amPmPosition = pos) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("AM/PM表記", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(AmPmLabel.JAPANESE to "日本語\n午前/午後", AmPmLabel.ENGLISH to "英語\nAM/PM").forEach { (lbl, label) ->
                                            SelectOption(
                                                label      = label,
                                                isSelected = draft.amPmLabel == lbl,
                                                modifier   = Modifier.weight(1f),
                                                onSelect   = { draft = draft.copy(amPmLabel = lbl) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text("AM/PMサイズ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        Text("${(draft.amPmScale * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                    Slider(
                                        value         = draft.amPmScale,
                                        onValueChange = { draft = draft.copy(amPmScale = it) },
                                        valueRange    = 0.3f..0.8f,
                                        steps         = 4,
                                        modifier      = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("AM/PMテキスト色", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                                    ColorRow(
                                        label   = if (draft.amPmColor != 0L) "カスタム" else "テキスト色と同じ",
                                        sub     = if (draft.amPmColor != 0L) "" else "テキスト色に連動します",
                                        color   = if (draft.amPmColor != 0L) draft.amPmColor else draft.textColor,
                                        onClick = { pickerTarget.value = "amPm" }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "時刻を表示",
                        sub      = "時刻の表示・非表示",
                        checked  = draft.showTime,
                        onToggle = { draft = draft.copy(showTime = it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "秒数を表示",
                        sub      = if (draft.showSeconds) "14:32:00" else "14:32",
                        checked  = draft.showSeconds,
                        onToggle = { draft = draft.copy(showSeconds = it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        val offsetSec = draft.timeOffset / 1000f
                        val offsetLabel = when {
                            draft.timeOffset > 0 -> "+%.2f秒".format(offsetSec)
                            draft.timeOffset < 0 -> "%.2f秒".format(offsetSec)
                            else                 -> "0秒（補正なし）"
                        }
                        var offsetInput by remember(draft.timeOffset) { mutableStateOf(draft.timeOffset.toString()) }
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            Text("時刻の微調整", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(offsetLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Slider(
                            value         = draft.timeOffset.toFloat().coerceIn(-1000f, 1000f),
                            onValueChange = { draft = draft.copy(timeOffset = it.toInt()) },
                            valueRange    = -1000f..1000f,
                            steps         = 19,
                            modifier      = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            listOf(-10, -5, -1, 1, 5, 10).forEach { sec ->
                                val label = if (sec > 0) "+${sec}s" else "${sec}s"
                                OutlinedButton(
                                    onClick  = { draft = draft.copy(timeOffset = draft.timeOffset + sec * 1000) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(label, fontSize = 11.sp)
                                }
                            }
                        }
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value         = offsetInput,
                                onValueChange = { offsetInput = it },
                                label           = { Text("ms", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    val v = offsetInput.toIntOrNull()
                                    if (v != null) draft = draft.copy(timeOffset = v)
                                    else offsetInput = draft.timeOffset.toString()
                                }),
                                singleLine      = true,
                                modifier        = Modifier.width(100.dp)
                            )
                        }
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "表示が世界時計より早い場合はマイナス方向、遅い場合はプラス方向に調整してください",
                                fontSize = 10.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { draft = draft.copy(timeOffset = 0) }) {
                                Text("リセット", fontSize = 12.sp)
                            }
                        }
                    }
                }

                SectionHeader("日付")
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("日付の形式", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("" to "なし", "M月d日" to "5月25日", "MM/dd" to "05/25", "M/d" to "5/25").forEach { (fmt, label) ->
                                SelectOption(
                                    label      = label,
                                    isSelected = draft.dateFormat == fmt,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(dateFormat = fmt) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("曜日の形式", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("" to "なし", "EEE" to "月", "(EEE)" to "(月)", "EEEE" to "月曜日").forEach { (fmt, label) ->
                                SelectOption(
                                    label      = label,
                                    isSelected = draft.weekdayFormat == fmt,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(weekdayFormat = fmt) }
                                )
                            }
                        }
                        AnimatedVisibility(visible = draft.dateFormat.isNotEmpty() && draft.weekdayFormat.isNotEmpty()) {
                            val weekdayExample = java.text.SimpleDateFormat(draft.weekdayFormat, java.util.Locale.getDefault()).format(java.util.Date())
                            val dateExample    = java.text.SimpleDateFormat(draft.dateFormat,    java.util.Locale.getDefault()).format(java.util.Date())
                            val preview = if (draft.dateWeekdayOrder == "DATE_FIRST") "$dateExample, $weekdayExample" else "$weekdayExample, $dateExample"
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Text(preview, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                TextButton(onClick = {
                                    draft = draft.copy(
                                        dateWeekdayOrder = if (draft.dateWeekdayOrder == "WEEKDAY_FIRST") "DATE_FIRST" else "WEEKDAY_FIRST"
                                    )
                                }) {
                                    Text("⇄ 入れ替え", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                SectionHeader("レイアウト")
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("ウィジェットサイズ", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WidgetSize.entries.forEach { size ->
                                SelectOption(
                                    label      = "${size.name}\n${when(size){ WidgetSize.S->"小"; WidgetSize.M->"標準"; WidgetSize.L->"大" }}",
                                    isSelected = draft.widgetSize == size,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(widgetSize = size) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("角丸スタイル", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                CornerStyle.PILL    to "Pill\n全丸",
                                CornerStyle.ROUNDED to "Rounded\n丸め",
                                CornerStyle.SOFT    to "Soft\n角小",
                                CornerStyle.SQUARE  to "Square\n四角"
                            ).forEach { (style, label) ->
                                SelectOption(
                                    label      = label,
                                    isSelected = draft.cornerStyle == style,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(cornerStyle = style) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "スケジュール名を表示",
                        sub      = "ウィジェットに現在のスケジュール名を表示",
                        checked  = draft.showTaskName,
                        onToggle = { draft = draft.copy(showTaskName = it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "カウントダウンを表示",
                        sub      = "空き時間に次のスケジュールまでの残り時間を表示",
                        checked  = draft.showCountdown,
                        onToggle = { draft = draft.copy(showCountdown = it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "チェックボックスを表示",
                        sub      = "スケジュール一覧にチェックボックスを表示します",
                        checked  = draft.showCheckboxes,
                        onToggle = { draft = draft.copy(showCheckboxes = it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    SwitchRow(
                        label    = "コンパクトな背景",
                        sub      = "ウィジェットの縦幅を詰める",
                        checked  = draft.compactBg,
                        onToggle = { draft = draft.copy(compactBg = it) }
                    )
                }

                SectionHeader("通知")
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("スケジュール開始時の表示", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FULLSCREEN" to "フルスクリーン", "NOTIFICATION" to "通知", "OFF" to "OFF").forEach { (value, label) ->
                                SelectOption(
                                    label      = label,
                                    isSelected = draft.alertMode == value,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(alertMode = value) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (draft.alertMode) {
                                "FULLSCREEN"   -> "開始時に全画面で表示します"
                                "NOTIFICATION" -> "開始時にバナー通知を表示します"
                                else           -> "通知を表示しません"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("カウントダウン通知音", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape    = RoundedCornerShape(8.dp),
                                color    = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickable {
                                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "通知音を選択")
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        if (draft.notificationSoundUri.isNotEmpty()) {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(draft.notificationSoundUri))
                                        }
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                }
                            ) {
                                Text(soundName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                            }
                            if (currentSoundUri.isNotEmpty()) {
                                TextButton(onClick = {
                                    currentSoundUri = ""
                                    draft = draft.copy(notificationSoundUri = "")
                                    viewModel.save(draft.copy(notificationSoundUri = ""))
                                }) {
                                    Text("デフォルト\nに戻す", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("再生時間", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 10, 15, 30).forEach { sec ->
                                SelectOption(
                                    label      = "${sec}秒",
                                    isSelected = draft.notificationDuration == sec,
                                    modifier   = Modifier.weight(1f),
                                    onSelect   = { draft = draft.copy(notificationDuration = sec) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("タップして端末の着信音・通知音から選択できます", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// ColorPickerDialog（Stage 1：プリセット / Stage 2：HSVカスタム）
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColorPickerDialog(
    title: String,
    presets: List<Long>,
    initialColor: Long,
    initialAlpha: Int,
    showAlpha: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (color: Long, alpha: Int) -> Unit
) {
    // ── 共有状態 ─────────────────────────────────────────────────────────────
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedAlpha by remember { mutableStateOf(initialAlpha) }
    var stage         by remember { mutableStateOf(1) }

    // Stage 1 HEX（selectedColor 変化でリセット）
    var hexInput1 by remember(selectedColor) {
        mutableStateOf(String.format("%06X", selectedColor and 0xFFFFFFL))
    }

    // ── Stage 2 HSV状態 ──────────────────────────────────────────────────────
    val initHsv = remember {
        FloatArray(3).also {
            android.graphics.Color.colorToHSV((initialColor and 0xFFFFFFFFL).toInt(), it)
        }
    }
    var hue by remember { mutableStateOf(initHsv[0]) }
    var sat by remember { mutableStateOf(initHsv[1]) }
    var bri by remember { mutableStateOf(initHsv[2]) }

    // HSV → Long色値
    val stage2Color = remember(hue, sat, bri) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri))
        argb.toLong() and 0xFFFFFFFFL
    }

    // Stage 2 HEX（ARGBまたはRGB、stage2Color/selectedAlpha変化でリセット）
    var hexInput2 by remember(stage2Color, selectedAlpha) {
        val str = if (showAlpha) {
            val a = (selectedAlpha * 255 / 100).coerceIn(0, 255)
            String.format("%02X%06X", a, stage2Color and 0xFFFFFFL)
        } else {
            String.format("%06X", stage2Color and 0xFFFFFFL)
        }
        mutableStateOf(str)
    }

    // SVキャンバス背景用の純色相色
    val hueColor = remember(hue) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        Color(argb.toLong() and 0xFFFFFFFFL)
    }

    // キャンバスサイズ追跡
    var svCanvasSize  by remember { mutableStateOf(Size.Zero) }
    var hueBarSize    by remember { mutableStateOf(Size.Zero) }
    var alphaBarSize  by remember { mutableStateOf(Size.Zero) }

    // Stage 1 グリッド dots の表示alpha
    val displayAlpha = if (showAlpha) selectedAlpha / 100f else 1f

    // プレビューバー用
    val afterColor = if (stage == 1) selectedColor else stage2Color
    val afterAlpha = if (showAlpha) selectedAlpha else 100
    val beforeAlpha = if (showAlpha) initialAlpha else 100

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape          = RoundedCornerShape(20.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier       = Modifier.fillMaxWidth()
        ) {
            if (stage == 1) {
                // ══════════════════════════════════════════════════════════
                // Stage 1：プリセット選択
                // ══════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(14.dp))

                    // 変更前 → 変更後 プレビュー
                    ColorPreviewBar(initialColor = initialColor, initialAlpha = beforeAlpha, afterColor = afterColor, afterAlpha = afterAlpha)
                    Spacer(Modifier.height(16.dp))

                    // ── カラーグリッド（4列 × 5行）───────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.chunked(4).forEach { rowColors ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowColors.forEach { value ->
                                    val isSelected = value == selectedColor
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.8.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = value },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCheckerboard()
                                            drawCircle(color = Color(value).copy(alpha = displayAlpha))
                                        }
                                        if (isSelected) {
                                            Text(
                                                text       = "✓",
                                                color      = Color.White,
                                                fontSize   = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 色調バリエーション行（tint/shade 10色・selectedColor 変化で自動更新）
                    val tintShadeColors = remember(selectedColor) {
                        generateTintShadeColors(selectedColor)
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tintShadeColors.forEach { value ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                    .clickable { selectedColor = value },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCheckerboard()
                                    drawCircle(color = Color(value).copy(alpha = displayAlpha))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // HEX入力（RGB 6桁）
                    HexInputRow(
                        previewColor = selectedColor,
                        previewAlpha = displayAlpha,
                        hexInput     = hexInput1,
                        onHexChange  = { input ->
                            val filtered = input.uppercase().filter { it.isDigit() || it in 'A'..'F' }
                            if (filtered.length <= 6) {
                                hexInput1 = filtered
                                if (filtered.length == 6) {
                                    selectedColor = 0xFF000000L or filtered.toLong(16)
                                }
                            }
                        }
                    )

                    // 不透明度スライダー（背景色のみ）
                    if (showAlpha) {
                        Spacer(Modifier.height(12.dp))
                        AlphaSliderRow(alpha = selectedAlpha, onChange = { selectedAlpha = it })
                    }

                    Spacer(Modifier.height(16.dp))

                    // 3ボタン行：カスタム / キャンセル / 選択
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(
                                    (selectedColor and 0xFFFFFFFFL).toInt(), hsv
                                )
                                hue = hsv[0]; sat = hsv[1]; bri = hsv[2]
                                stage = 2
                            }
                        ) {
                            Text("カスタム", color = MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("キャンセル", color = MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = { onConfirm(selectedColor, selectedAlpha) }) {
                            Text("選択", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

            } else {
                // ══════════════════════════════════════════════════════════
                // Stage 2：HSVカスタムピッカー
                // ══════════════════════════════════════════════════════════
                Column(modifier = Modifier.padding(20.dp)) {

                    Text("カスタム", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(14.dp))

                    // 変更前 → 変更後 プレビュー
                    ColorPreviewBar(initialColor = initialColor, initialAlpha = beforeAlpha, afterColor = afterColor, afterAlpha = afterAlpha)
                    Spacer(Modifier.height(14.dp))

                    // ── SV キャンバス + 縦 色相バー ──────────────────────
                    val canvasHeight = 200.dp
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 2D SV キャンバス
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(canvasHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (svCanvasSize.width > 0f) {
                                                sat = (offset.x / svCanvasSize.width).coerceIn(0f, 1f)
                                                bri = (1f - offset.y / svCanvasSize.height).coerceIn(0f, 1f)
                                            }
                                        }
                                    ) { change, _ ->
                                        if (svCanvasSize.width > 0f) {
                                            sat = (change.position.x / svCanvasSize.width).coerceIn(0f, 1f)
                                            bri = (1f - change.position.y / svCanvasSize.height).coerceIn(0f, 1f)
                                        }
                                    }
                                }
                        ) {
                            svCanvasSize = size
                            // 彩度グラデーション（白 → 純色相）
                            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                            // 明度グラデーション（透明 → 黒）
                            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                            // 選択インジケーター（リング）
                            val cx = sat * size.width
                            val cy = (1f - bri) * size.height
                            drawCircle(
                                color  = Color.Black,
                                radius = 11.dp.toPx(),
                                center = Offset(cx, cy),
                                style  = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color  = Color.White,
                                radius = 10.dp.toPx(),
                                center = Offset(cx, cy),
                                style  = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // 縦 色相バー
                        Canvas(
                            modifier = Modifier
                                .width(28.dp)
                                .height(canvasHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (hueBarSize.height > 0f) {
                                                hue = (offset.y / hueBarSize.height * 360f).coerceIn(0f, 360f)
                                            }
                                        }
                                    ) { change, _ ->
                                        if (hueBarSize.height > 0f) {
                                            hue = (change.position.y / hueBarSize.height * 360f).coerceIn(0f, 360f)
                                        }
                                    }
                                }
                        ) {
                            hueBarSize = size
                            // 虹グラデーション（縦）
                            drawRect(brush = Brush.verticalGradient(colors = hueBarColors))
                            // 選択インジケーター（横線）
                            val iy = (hue / 360f * size.height).coerceIn(0f, size.height - 1f)
                            drawLine(
                                color       = Color.White,
                                start       = Offset(0f, iy),
                                end         = Offset(size.width, iy),
                                strokeWidth = 3.dp.toPx()
                            )
                            drawLine(
                                color       = Color(0x88000000),
                                start       = Offset(0f, iy),
                                end         = Offset(size.width, iy),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // ── アルファバー（背景色のみ）────────────────────────
                    if (showAlpha) {
                        Spacer(Modifier.height(10.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (alphaBarSize.width > 0f) {
                                                selectedAlpha = ((1f - offset.x / alphaBarSize.width) * 100)
                                                    .roundToInt().coerceIn(0, 100)
                                            }
                                        }
                                    ) { change, _ ->
                                        if (alphaBarSize.width > 0f) {
                                            selectedAlpha = ((1f - change.position.x / alphaBarSize.width) * 100)
                                                .roundToInt().coerceIn(0, 100)
                                        }
                                    }
                                }
                        ) {
                            alphaBarSize = size
                            // チェッカーボード背景
                            drawCheckerboard()
                            // 現在色（不透明）→ 透明 グラデーション（左=不透明・右=透明）
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(stage2Color).copy(alpha = 1f), Color.Transparent)
                                )
                            )
                            // 選択インジケーター（縦線）
                            val ix = (1f - selectedAlpha / 100f) * size.width
                            drawLine(
                                color       = Color.White,
                                start       = Offset(ix, 0f),
                                end         = Offset(ix, size.height),
                                strokeWidth = 3.dp.toPx()
                            )
                            drawLine(
                                color       = Color(0x88000000),
                                start       = Offset(ix, 0f),
                                end         = Offset(ix, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // HEX入力（showAlpha=true → 8桁ARGB / false → 6桁RGB）
                    HexInputRow(
                        previewColor = stage2Color,
                        previewAlpha = selectedAlpha / 100f,
                        hexInput     = hexInput2,
                        onHexChange  = { input ->
                            val maxLen   = if (showAlpha) 8 else 6
                            val filtered = input.uppercase().filter { it.isDigit() || it in 'A'..'F' }
                            if (filtered.length <= maxLen) {
                                hexInput2 = filtered
                                if (filtered.length == maxLen) {
                                    val rgbStr = if (showAlpha) {
                                        val aInt = filtered.substring(0, 2).toInt(16)
                                        selectedAlpha = (aInt * 100f / 255f).roundToInt()
                                        filtered.substring(2)
                                    } else {
                                        filtered
                                    }
                                    val newColor = 0xFF000000L or rgbStr.toLong(16)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(
                                        (newColor and 0xFFFFFFFFL).toInt(), hsv
                                    )
                                    hue = hsv[0]; sat = hsv[1]; bri = hsv[2]
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // 3ボタン行：プリセット / キャンセル / 選択
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selectedColor = stage2Color
                                stage = 1
                            }
                        ) {
                            Text("プリセット", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("キャンセル", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { onConfirm(stage2Color, selectedAlpha) }) {
                            Text("選択", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// ダイアログ共通サブコンポーザブル
// ────────────────────────────────────────────────────────────────────────────

/** □ → □ 形式プレビューバー（チェッカーボード + alpha対応） */
@Composable
private fun ColorPreviewBar(
    initialColor: Long,
    initialAlpha: Int,
    afterColor: Long,
    afterAlpha: Int
) {
    val hasChanged = afterColor != initialColor || afterAlpha != initialAlpha
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCheckerboard()
                    drawRect(color = Color(initialColor).copy(alpha = initialAlpha / 100f))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("変更前", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (hasChanged) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCheckerboard()
                        drawRect(color = Color(afterColor).copy(alpha = afterAlpha / 100f))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "?",
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("変更後", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

/** HEX入力行（previewAlpha対応） */
@Composable
private fun HexInputRow(
    previewColor: Long,
    previewAlpha: Float = 1f,
    hexInput: String,
    onHexChange: (String) -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCheckerboard()
                drawCircle(color = Color(previewColor).copy(alpha = previewAlpha))
            }
        }
        Text(
            "#",
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize   = 16.sp,
            fontFamily = FontFamily.Monospace
        )
        OutlinedTextField(
            value         = hexInput,
            onValueChange = onHexChange,
            modifier      = Modifier.weight(1f),
            singleLine    = true,
            placeholder   = { Text("1C1B1F", fontSize = 14.sp, fontFamily = FontFamily.Monospace) },
            textStyle       = LocalTextStyle.current.copy(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor          = MaterialTheme.colorScheme.primary,
                focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor   = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

/** 不透明度スライダー行 */
@Composable
private fun AlphaSliderRow(alpha: Int, onChange: (Int) -> Unit) {
    var localValue by remember(alpha) { mutableFloatStateOf(alpha / 100f) }
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        Text("不透明度", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(52.dp))
        Slider(
            value         = localValue,
            onValueChange = { v ->
                localValue = v
                onChange((v * 100).roundToInt())
            },
            modifier      = Modifier.weight(1f)
        )
        Text(
            "${(localValue * 100).roundToInt()}%",
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize  = 12.sp,
            textAlign = TextAlign.End,
            modifier  = Modifier.width(36.dp)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 画面共通サブコンポーザブル
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun WidgetPreview(draft: AppSettings) {
    val context = LocalContext.current
    var bgBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(
        draft.bgColor, draft.bgAlpha, draft.bgType, draft.bgGradientEnd,
        draft.bgColor2, draft.gradientColorCount, draft.gradientDirection,
        draft.cornerStyle, draft.compactBg, draft.bgImageUri,
        draft.linearStartPoint, draft.gradientCenter,
        draft.bgColor2Alpha, draft.bgGradientEndAlpha
    ) {
        withContext(Dispatchers.IO) {
            bgBitmap = buildBackgroundBitmap(context, draft)
        }
    }

    val (actualW, actualH) = remember {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            android.content.ComponentName(
                context,
                com.tao0524.tickat.widget.TickAtWidgetReceiver::class.java
            )
        )
        if (ids.isNotEmpty()) {
            val opts = manager.getAppWidgetOptions(ids[0])
            val w = opts.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110).toFloat()
            val h = opts.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 44).toFloat()
            w to h
        } else {
            110f to 44f
        }
    }

    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()
    val scale = minOf(screenWidthDp * 0.90f / actualW, 110f / actualH)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFC0C0C0)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.widget_tickat_base, null, false).also { view ->
                    view.layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                val (clockSp, dateSp) = calcFontSizes(actualH.toInt(), draft.clockDateBalance, draft.fontScale)
                val scaledDensity = view.context.resources.displayMetrics.scaledDensity
                val clockPx = clockSp * scaledDensity
                val datePx  = dateSp  * scaledDensity
                val typefaceStyle = when {
                    draft.fontWeight == TextWeight.BOLD && draft.isItalic -> android.graphics.Typeface.BOLD_ITALIC
                    draft.fontWeight == TextWeight.BOLD                   -> android.graphics.Typeface.BOLD
                    draft.isItalic                                        -> android.graphics.Typeface.ITALIC
                    else                                                  -> android.graphics.Typeface.NORMAL
                }
                val typeface = when (draft.fontFamily) {
                    WidgetFont.SERIF     -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, typefaceStyle)
                    WidgetFont.CONDENSED -> android.graphics.Typeface.create("sans-serif-condensed", typefaceStyle)
                    WidgetFont.MONO      -> android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, typefaceStyle)
                    else                 -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, typefaceStyle)
                }
                val now      = java.util.Calendar.getInstance()
                val timeLocale = if (draft.amPmLabel == AmPmLabel.ENGLISH) java.util.Locale.ENGLISH else java.util.Locale.JAPANESE
                val timeBitmap = if (draft.use24Hour) {
                    val fmt = if (draft.showSeconds) "HH:mm:ss" else "HH:mm"
                    buildTextBitmap(java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault()).format(now.time), clockPx, draft.textColor.toInt(), typeface, draft.showTextShadow)
                } else {
                    val fmt      = if (draft.showSeconds) "h:mm:ss" else "h:mm"
                    val timeOnly = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault()).format(now.time)
                    val amPmStr  = java.text.SimpleDateFormat("a", timeLocale).format(now.time)
                    buildTimeWithAmPmBitmap(timeOnly, amPmStr, clockPx, draft.textColor.toInt(), typeface, draft.showTextShadow, draft.amPmPosition, draft.amPmScale, if (draft.amPmColor != 0L) draft.amPmColor.toInt() else draft.textColor.toInt())
                }
                val datePart    = if (draft.dateFormat.isNotEmpty())
                    java.text.SimpleDateFormat(draft.dateFormat, java.util.Locale.getDefault()).format(now.time)
                else ""
                val weekdayPart = if (draft.weekdayFormat.isNotEmpty())
                    java.text.SimpleDateFormat(draft.weekdayFormat, java.util.Locale.getDefault()).format(now.time)
                else ""
                val dateText = when {
                    weekdayPart.isNotEmpty() && datePart.isNotEmpty() ->
                        if (draft.dateWeekdayOrder == "DATE_FIRST") "$datePart, $weekdayPart"
                        else "$weekdayPart, $datePart"
                    weekdayPart.isNotEmpty() -> weekdayPart
                    datePart.isNotEmpty()    -> datePart
                    else                     -> ""
                }
                val fallbackDate = if (dateText.isEmpty() && !draft.showTime)
                    java.text.SimpleDateFormat("M/d", java.util.Locale.getDefault()).format(now.time) else ""
                val displayDate = dateText.ifEmpty { fallbackDate }
                view.findViewById<android.widget.ImageView>(R.id.widget_time_img)?.apply {
                    setImageBitmap(timeBitmap)
                    visibility = if (draft.showTime) android.view.View.VISIBLE else android.view.View.GONE
                }
                view.findViewById<android.widget.ImageView>(R.id.widget_date_img)?.apply {
                    if (displayDate.isNotEmpty()) setImageBitmap(buildTextBitmap(displayDate, datePx, draft.dateTextColor.toInt(), typeface, draft.showTextShadow))
                    visibility = if (displayDate.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
                bgBitmap?.let {
                    view.findViewById<android.widget.ImageView>(R.id.widget_bg)?.setImageBitmap(it)
                }
            },
            modifier = Modifier
                .width(actualW.dp)
                .height(actualH.dp)
                .scale(scale)
        )
    }
}

@Composable
private fun FontSizePickerDialog(
    currentBalance: Int,
    currentScale:   Float,
    draft:          AppSettings,
    onDismiss:      () -> Unit,
    onConfirm:      (Int, Float) -> Unit
) {
    var tempBalance by remember { mutableStateOf(currentBalance.toFloat()) }
    var tempScale   by remember { mutableStateOf(currentScale) }
    val balance = tempBalance.roundToInt()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "フォントサイズ",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                WidgetPreview(draft = draft.copy(clockDateBalance = balance, fontScale = tempScale, dateFormat = "M月d日"))
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("小さく", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.1f".format(tempScale) + "×", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("大きく", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value         = tempScale,
                    onValueChange = { tempScale = it },
                    valueRange    = 0.5f..1.5f,
                    steps         = 9,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("時刻優先", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("日付優先", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value         = tempBalance,
                    onValueChange = { tempBalance = it },
                    valueRange    = -10f..10f,
                    steps         = 19,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    TextButton(onClick = { onConfirm(balance, tempScale) }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: ThemePreset,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val isGradient = theme.bgGradientEnd != 0L
    val brush = if (isGradient) {
        when (theme.gradientDirection) {
            GradientDirection.HORIZONTAL -> Brush.horizontalGradient(
                colors = listOf(Color(theme.bgColor), Color(theme.bgGradientEnd))
            )
            GradientDirection.VERTICAL -> Brush.verticalGradient(
                colors = listOf(Color(theme.bgColor), Color(theme.bgGradientEnd))
            )
            GradientDirection.DIAGONAL -> Brush.linearGradient(
                colors = listOf(Color(theme.bgColor), Color(theme.bgGradientEnd))
            )
            GradientDirection.RADIAL -> Brush.radialGradient(
                colors = listOf(Color(theme.bgColor), Color(theme.bgGradientEnd))
            )
            null -> Brush.linearGradient(
                colors = listOf(Color(theme.bgColor), Color(theme.bgGradientEnd))
            )
        }
    } else null

    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (brush != null) Modifier.background(brush)
                else Modifier.background(Color(theme.bgColor))
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF3A383F),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(theme.textColor).copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "14:32",
                    color = Color(theme.textColor),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
            Text(
                text = theme.name,
                color = Color(theme.textColor).copy(alpha = 0.55f),
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = contentColorFor(theme.bgColor), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ColorRow(
    label: String,
    sub: String,
    color: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(sub,   color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(1.dp, Color(0xFF49454F), CircleShape)
            )
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    sub: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(sub,   color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SelectOption(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF49454F),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = if (isSelected) MaterialTheme.colorScheme.onSurface else Color(0xFF9E9E9E),
            fontSize   = 12.sp,
            textAlign  = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// LinearStartPicker
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun LinearStartPicker(
    selected:           GradientCenter,
    bgColor:            Long,
    endColor:           Long,
    bgColor2:           Long,
    gradientColorCount: Int,
    onSelect:           (GradientCenter) -> Unit
) {
    val positions = listOf(
        GradientCenter.TOP_LEFT,    GradientCenter.TOP_CENTER,    GradientCenter.TOP_RIGHT,
        GradientCenter.CENTER_LEFT, GradientCenter.CENTER,        GradientCenter.CENTER_RIGHT,
        GradientCenter.BOTTOM_LEFT, GradientCenter.BOTTOM_CENTER, GradientCenter.BOTTOM_RIGHT
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val (sx, sy) = when (selected) {
                GradientCenter.TOP_LEFT      -> 0f to 0f
                GradientCenter.TOP_CENTER    -> w / 2f to 0f
                GradientCenter.TOP_RIGHT     -> w to 0f
                GradientCenter.CENTER_LEFT   -> 0f to h / 2f
                GradientCenter.CENTER        -> 0f to h / 2f
                GradientCenter.CENTER_RIGHT  -> w to h / 2f
                GradientCenter.BOTTOM_LEFT   -> 0f to h
                GradientCenter.BOTTOM_CENTER -> w / 2f to h
                GradientCenter.BOTTOM_RIGHT  -> w to h
            }
            val (ex, ey) = when (selected) {
                GradientCenter.TOP_LEFT      -> w to h
                GradientCenter.TOP_CENTER    -> w / 2f to h
                GradientCenter.TOP_RIGHT     -> 0f to h
                GradientCenter.CENTER_LEFT   -> w to h / 2f
                GradientCenter.CENTER        -> w to h / 2f
                GradientCenter.CENTER_RIGHT  -> 0f to h / 2f
                GradientCenter.BOTTOM_LEFT   -> w to 0f
                GradientCenter.BOTTOM_CENTER -> w / 2f to 0f
                GradientCenter.BOTTOM_RIGHT  -> 0f to 0f
            }
            val use3  = gradientColorCount == 3 && bgColor2 != 0L
            val brush = if (use3) {
                Brush.linearGradient(
                    colors = listOf(Color(bgColor), Color(bgColor2), Color(endColor)),
                    start  = Offset(sx, sy),
                    end    = Offset(ex, ey)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(bgColor), Color(endColor)),
                    start  = Offset(sx, sy),
                    end    = Offset(ex, ey)
                )
            }
            drawRect(brush = brush)
        }
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            positions.chunked(3).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { pos ->
                        val isSelected = selected == pos
                        val isDisabled = pos == GradientCenter.CENTER
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDisabled -> Color.White.copy(alpha = 0.2f)
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else       -> Color.White.copy(alpha = 0.55f)
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = when {
                                        isDisabled -> Color.White.copy(alpha = 0.3f)
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else       -> Color.White.copy(alpha = 0.85f)
                                    },
                                    shape = CircleShape
                                )
                                .then(
                                    if (!isDisabled) Modifier.clickable { onSelect(pos) }
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    "✓",
                                    color      = Color.White,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstTimeHint(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "💡 $message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// RadialCenterPicker
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun RadialCenterPicker(
    selected:           GradientCenter,
    bgColor:            Long,
    endColor:           Long,
    bgColor2:           Long,
    gradientColorCount: Int,
    onSelect:           (GradientCenter) -> Unit
) {
    val positions = listOf(
        GradientCenter.TOP_LEFT,    GradientCenter.TOP_CENTER,    GradientCenter.TOP_RIGHT,
        GradientCenter.CENTER_LEFT, GradientCenter.CENTER,        GradientCenter.CENTER_RIGHT,
        GradientCenter.BOTTOM_LEFT, GradientCenter.BOTTOM_CENTER, GradientCenter.BOTTOM_RIGHT
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w  = size.width
            val h  = size.height
            val (rcx, rcy) = when (selected) {
                GradientCenter.TOP_LEFT      -> 0f to 0f
                GradientCenter.TOP_CENTER    -> w / 2f to 0f
                GradientCenter.TOP_RIGHT     -> w to 0f
                GradientCenter.CENTER_LEFT   -> 0f to h / 2f
                GradientCenter.CENTER        -> w / 2f to h / 2f
                GradientCenter.CENTER_RIGHT  -> w to h / 2f
                GradientCenter.BOTTOM_LEFT   -> 0f to h
                GradientCenter.BOTTOM_CENTER -> w / 2f to h
                GradientCenter.BOTTOM_RIGHT  -> w to h
            }
            val dx     = maxOf(rcx, w - rcx)
            val dy     = maxOf(rcy, h - rcy)
            val radius = sqrt(dx * dx + dy * dy)
            val use3   = gradientColorCount == 3 && bgColor2 != 0L
            val brush  = if (use3) {
                Brush.radialGradient(
                    colors = listOf(Color(bgColor), Color(bgColor2), Color(endColor)),
                    center = Offset(rcx, rcy),
                    radius = radius
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(bgColor), Color(endColor)),
                    center = Offset(rcx, rcy),
                    radius = radius
                )
            }
            drawRect(brush = brush)
        }
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            positions.chunked(3).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { pos ->
                        val isSelected = selected == pos
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.55f)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.85f),
                                    shape = CircleShape
                                )
                                .clickable { onSelect(pos) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    "✓",
                                    color      = Color.White,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}