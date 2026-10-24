package com.tao0524.tickat.ui.screen.appsettings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
    val hintShown by viewModel.hintShown.collectAsState()
    val context = LocalContext.current

    if (!isLoaded) return

    var currentSoundUri by remember { mutableStateOf(savedSoundUri) }
    LaunchedEffect(savedSoundUri) { currentSoundUri = savedSoundUri }

    val soundName = remember(currentSoundUri, savedSoundName) {
        when {
            currentSoundUri.isEmpty() -> "既定の通知音"
            currentSoundUri.startsWith("/") -> savedSoundName.ifEmpty { "カスタム音" }
            else -> runCatching {
                RingtoneManager.getRingtone(context, Uri.parse(currentSoundUri))?.getTitle(context) ?: "カスタム音"
            }.getOrElse { "カスタム音" }
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri == null) {
                currentSoundUri = ""
                viewModel.saveNotificationSound("")
                return@rememberLauncherForActivityResult
            }

            val uriStr = uri.toString()

            if (uriStr.startsWith("content://settings/")) {
                currentSoundUri = uriStr
                viewModel.saveNotificationSound(uriStr)
                return@rememberLauncherForActivityResult
            }

            try {
                val soundDir = java.io.File(context.filesDir, "notification_sounds")
                if (!soundDir.exists()) soundDir.mkdirs()
                val destFile = java.io.File(soundDir, "custom_sound")
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
                val internalUri = destFile.absolutePath
                currentSoundUri = internalUri
                viewModel.saveNotificationSound(internalUri, displayName)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "通知音のコピーに失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "通知音を選択")
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                if (savedSoundUri.isNotEmpty() && !savedSoundUri.startsWith("/")) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(savedSoundUri))
                }
            }
            ringtonePickerLauncher.launch(intent)
        } else {
            Toast.makeText(context, "ストレージへのアクセスが許可されていません", Toast.LENGTH_SHORT).show()
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

            // --- 通知 ---
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
                        listOf("FULLSCREEN" to "全画面", "NOTIFICATION" to "通知", "OFF" to "OFF").forEach { (value, label) ->
                            SelectOption(
                                label      = label,
                                isSelected = alertMode == value,
                                modifier   = Modifier.weight(1f),
                                onSelect   = {
                                    viewModel.saveAlertMode(value)
                                    // 許可がないまま「全画面」を押したら知らせる（選んだままにする）
                                    if (value == "FULLSCREEN" && needsFullScreenPermission(context)) {
                                        showFullScreenPermissionDialog = true
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (alertMode) {
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
                                if (Build.VERSION.SDK_INT <= 32 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED
                                ) {
                                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                } else {
                                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "通知音を選択")
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        if (savedSoundUri.isNotEmpty() && !savedSoundUri.startsWith("/")) {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(savedSoundUri))
                                        }
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                }
                            }
                        ) {
                            Text(soundName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                        }
                        if (currentSoundUri.isNotEmpty()) {
                            TextButton(onClick = {
                                currentSoundUri = ""
                                viewModel.saveNotificationSound("")
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
                                isSelected = notificationDuration == sec,
                                modifier   = Modifier.weight(1f),
                                onSelect   = { viewModel.saveNotificationDuration(sec) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("タップして端末の着信音・通知音から選択できます", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("※ この設定はTickAtの通知にのみ適用されます。端末の時計アプリなど他のアプリのアラーム音には影響しません。", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
