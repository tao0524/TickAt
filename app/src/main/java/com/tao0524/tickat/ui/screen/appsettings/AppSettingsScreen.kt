package com.tao0524.tickat.ui.screen.appsettings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tao0524.tickat.ui.component.FULL_SCREEN_SELECTED_MESSAGE
import com.tao0524.tickat.ui.component.FULL_SCREEN_SELECTED_TITLE
import com.tao0524.tickat.ui.component.FirstTimeHint
import com.tao0524.tickat.ui.component.FullScreenPermissionDialog
import com.tao0524.tickat.ui.component.needsFullScreenPermission
import com.tao0524.tickat.ui.component.openFullScreenPermissionSettings
import com.tao0524.tickat.ui.screen.settings.SectionHeader
import com.tao0524.tickat.ui.screen.settings.SelectOption
import com.tao0524.tickat.ui.screen.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    onOpenWidgetList: () -> Unit
) {
    val isLoaded by viewModel.isLoaded.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val alertMode by viewModel.alertMode.collectAsState()
    val savedSoundUri by viewModel.notificationSoundUri.collectAsState()
    val savedSoundName by viewModel.notificationSoundName.collectAsState()
    val notificationDuration by viewModel.notificationDuration.collectAsState()
    val endAlertMode by viewModel.endAlertMode.collectAsState()
    val endSoundUri by viewModel.endSoundUri.collectAsState()
    val endSoundName by viewModel.endSoundName.collectAsState()
    val endDuration by viewModel.endDuration.collectAsState()
    val hintShown by viewModel.hintShown.collectAsState()
    val context = LocalContext.current

    if (!isLoaded) return

    var currentSoundUri by remember { mutableStateOf(savedSoundUri) }
    LaunchedEffect(savedSoundUri) { currentSoundUri = savedSoundUri }

    val soundName = remember(currentSoundUri, savedSoundName) {
        soundDisplayName(context, currentSoundUri, savedSoundName)
    }
    // 終了の音は、まだ選んでいないときは「開始と同じ」と出す
    val endSoundLabel = remember(endSoundUri, endSoundName) {
        endSoundUri?.let { soundDisplayName(context, it, endSoundName) } ?: "開始と同じ"
    }

    // 開始の音の選択画面（選んだ音は notification_sounds/custom_sound にコピーする）
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        savePickedSound(context, result, "custom_sound") { uri, name ->
            currentSoundUri = uri
            viewModel.saveNotificationSound(uri, name)
        }
    }

    // 終了の音の選択画面（開始とは別の notification_sounds/custom_sound_end にコピーする。
    // 同じ場所だと、開始の音を選び直したときに終了の音も変わってしまうため）
    val endRingtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        savePickedSound(context, result, "custom_sound_end") { uri, name ->
            viewModel.saveEndNotificationSound(uri, name)
        }
    }

    // どちらの音を選ぼうとしているか（ストレージの許可をもらったあとに、同じほうの選択画面を開くため）
    var soundPickerTarget by rememberSaveable { mutableStateOf("start") }
    val launchSoundPicker: (String) -> Unit = { target ->
        if (target == "end") {
            // 終了の音をまだ選んでいないときは、開始の音を選んだ形で開く
            endRingtonePickerLauncher.launch(soundPickerIntent(endSoundUri ?: savedSoundUri))
        } else {
            ringtonePickerLauncher.launch(soundPickerIntent(savedSoundUri))
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSoundPicker(soundPickerTarget)
        } else {
            Toast.makeText(context, "ストレージへのアクセスが許可されていません", Toast.LENGTH_SHORT).show()
        }
    }

    val openSoundPicker: (String) -> Unit = { target ->
        if (Build.VERSION.SDK_INT <= 32 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            soundPickerTarget = target
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            launchSoundPicker(target)
        }
    }

    var showFullScreenPermissionDialog by remember { mutableStateOf(false) }

    // --- 全画面通知の許可のお願い（「全画面」を押した直後） ---
    if (showFullScreenPermissionDialog) {
        FullScreenPermissionDialog(
            title          = FULL_SCREEN_SELECTED_TITLE,
            message        = FULL_SCREEN_SELECTED_MESSAGE,
            onLater        = { showFullScreenPermissionDialog = false },
            onOpenSettings = {
                showFullScreenPermissionDialog = false
                openFullScreenPermissionSettings(context)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "設定",
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
            if (!hintShown) {
                FirstTimeHint(
                    message = "アプリテーマと通知はこの画面で、ウィジェットの色やフォントは「ウィジェット設定」で変えられます",
                    onDismiss = { viewModel.dismissHint() },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // --- ウィジェット設定（ウィジェットの一覧へ） ---
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenWidgetList),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "ウィジェット設定",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- アプリテーマ（ダーク/ライト） ---
            SectionHeader("アプリテーマ")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ThemeMode.SYSTEM to "システム",
                        ThemeMode.DARK   to "ダーク",
                        ThemeMode.LIGHT  to "ライト"
                    ).forEach { (mode, label) ->
                        val isSelected = currentThemeMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.saveThemeMode(mode) }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // --- 通知（1枚目は開始、2枚目はタイムブロックの終了。どれも押した瞬間に保存する） ---
            SectionHeader("通知")
            Card(
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    AlertModeSelector(
                        title       = "スケジュール開始時の表示",
                        selected    = alertMode,
                        description = when (alertMode) {
                            "FULLSCREEN"   -> "開始時に全画面で表示します（スマホを使っているときは、画面のいちばん上の通知になります）"
                            "NOTIFICATION" -> "開始時にバナー通知を表示します"
                            else           -> "開始時の通知を表示しません"
                        },
                        onSelect    = { value ->
                            viewModel.saveAlertMode(value)
                            // 許可がないまま「全画面」を押したら知らせる（選んだままにする）
                            if (value == "FULLSCREEN" && needsFullScreenPermission(context)) {
                                showFullScreenPermissionDialog = true
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    SoundSelector(
                        title      = "開始の通知音",
                        soundLabel = soundName,
                        resetLabel = if (currentSoundUri.isNotEmpty()) "デフォルト\nに戻す" else null,
                        onPick     = { openSoundPicker("start") },
                        onReset    = {
                            currentSoundUri = ""
                            viewModel.saveNotificationSound("")
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    DurationSelector(
                        selected = notificationDuration,
                        onSelect = { viewModel.saveNotificationDuration(it) }
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
                    AlertModeSelector(
                        title       = "スケジュール終了時の表示",
                        selected    = endAlertMode,
                        description = when (endAlertMode) {
                            "FULLSCREEN"   -> "タイムブロックの終了時に全画面で表示します（スマホを使っているときは、画面のいちばん上の通知になります）"
                            "NOTIFICATION" -> "タイムブロックの終了時にバナー通知を表示します"
                            else           -> "終了時の通知を表示しません"
                        },
                        onSelect    = { value ->
                            viewModel.saveEndAlertMode(value)
                            // 開始と同じく、許可がないまま「全画面」を押したら知らせる（選んだままにする）
                            if (value == "FULLSCREEN" && needsFullScreenPermission(context)) {
                                showFullScreenPermissionDialog = true
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    SoundSelector(
                        title      = "終了の通知音",
                        soundLabel = endSoundLabel,
                        resetLabel = if (endSoundUri != null) "開始と同じ\nに戻す" else null,
                        onPick     = { openSoundPicker("end") },
                        onReset    = { viewModel.resetEndNotificationSound() }
                    )
                    Spacer(Modifier.height(12.dp))
                    // 終了の再生時間をまだ選んでいないときは、開始と同じ秒数のボタンが選ばれた形で出す
                    DurationSelector(
                        selected = endDuration ?: notificationDuration,
                        onSelect = { viewModel.saveEndNotificationDuration(it) }
                    )
                }
            }
            // 2つの注意書きは、開始と終了の両方の音に当てはまるので、2枚のカードの下に1回だけ出す
            Spacer(Modifier.height(8.dp))
            Text(
                "タップして端末の着信音・通知音から選択できます",
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "※ この設定はTickAtの通知にのみ適用されます。端末の時計アプリなど他のアプリのアラーム音には影響しません。",
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// 「全画面」「通知」「OFF」の3つのボタンと、選んでいるものの説明（開始と終了で同じ形）
@Composable
private fun AlertModeSelector(
    title: String,
    selected: String,
    description: String,
    onSelect: (String) -> Unit
) {
    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("FULLSCREEN" to "全画面", "NOTIFICATION" to "通知", "OFF" to "OFF").forEach { (value, label) ->
            SelectOption(
                label      = label,
                isSelected = selected == value,
                modifier   = Modifier.weight(1f),
                onSelect   = { onSelect(value) }
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        text = description,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
    )
}

// 通知音の欄（押すと選択画面が開く）と、その横の戻すボタン（resetLabel が null のときは出さない）
@Composable
private fun SoundSelector(
    title: String,
    soundLabel: String,
    resetLabel: String?,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape    = RoundedCornerShape(8.dp),
            color    = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).clickable(onClick = onPick)
        ) {
            Text(soundLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
        }
        if (resetLabel != null) {
            TextButton(onClick = onReset) {
                Text(resetLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

// 「再生時間」の5つのボタン
@Composable
private fun DurationSelector(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Text("再生時間", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(3, 5, 10, 15, 30).forEach { sec ->
            SelectOption(
                label      = "${sec}秒",
                isSelected = selected == sec,
                modifier   = Modifier.weight(1f),
                onSelect   = { onSelect(sec) }
            )
        }
    }
}

// 音の欄に出す名前。"" は端末の既定の通知音、"/" で始まるのは TickAt の中にコピーした音（選んだときの名前）、それ以外はスマホの音の名前
private fun soundDisplayName(context: Context, uri: String, savedName: String): String = when {
    uri.isEmpty() -> "既定の通知音"
    uri.startsWith("/") -> savedName.ifEmpty { "カスタム音" }
    else -> runCatching {
        RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) ?: "カスタム音"
    }.getOrElse { "カスタム音" }
}

// 着信音・通知音の選択画面を開く Intent。前に選んだスマホの音があれば、それを選んだ形で開く
private fun soundPickerIntent(existingUri: String): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "通知音を選択")
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        if (existingUri.isNotEmpty() && !existingUri.startsWith("/")) {
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUri))
        }
    }

// 選択画面で選んだ音を保存する（開始と終了で同じ処理。コピーするファイルの名前だけ違う）
private fun savePickedSound(
    context: Context,
    result: ActivityResult,
    fileName: String,
    save: (uri: String, name: String?) -> Unit
) {
    if (result.resultCode != Activity.RESULT_OK) return
    @Suppress("DEPRECATION")
    val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    if (uri == null) {
        save("", null)
        return
    }

    val uriStr = uri.toString()

    if (uriStr.startsWith("content://settings/")) {
        save(uriStr, null)
        return
    }

    try {
        val soundDir = java.io.File(context.filesDir, "notification_sounds")
        if (!soundDir.exists()) soundDir.mkdirs()
        val destFile = java.io.File(soundDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val displayName = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.replace(Regex("\\.[^.]+$"), "") else null
            }
        }.getOrNull() ?: "カスタム音"
        save(destFile.absolutePath, displayName)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "通知音のコピーに失敗しました", Toast.LENGTH_SHORT).show()
    }
}
