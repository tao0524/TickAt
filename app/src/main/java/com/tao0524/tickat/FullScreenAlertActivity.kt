package com.tao0524.tickat

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tao0524.tickat.ui.screen.settings.ThemeMode
import com.tao0524.tickat.ui.theme.TickAtTheme
import com.tao0524.tickat.widget.TaskAlertService

class FullScreenAlertActivity : ComponentActivity() {

    // 画面に出す予定の情報（画面を出したまま別の予定が鳴ったときに書き換える）
    private var taskName      by mutableStateOf("")
    private var taskTypeLabel by mutableStateOf("")
    private var timeRange     by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 通知を押して開いたときは、通知音を止める（画面の作り直しのときは何もしない）
        if (savedInstanceState == null) {
            TaskAlertService.stopFromNotificationTap(this, intent)
        }

        // ロック画面上での表示 + 画面点灯
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Intentからタスク情報を取得
        readTaskInfo(intent)

        setContent {
            TickAtTheme(themeMode = ThemeMode.DARK) {
                // 音が鳴っている間だけ、画面を暗くさせない
                val isAlerting by TaskAlertService.isAlerting.collectAsState()
                LaunchedEffect(isAlerting) {
                    if (isAlerting) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                // 鳴っている間は ◁（戻る）で閉じない（止めるのは「止める」ボタンだけ）
                BackHandler(enabled = isAlerting) { }
                FullScreenAlertContent(
                    taskName      = taskName,
                    taskTypeLabel = taskTypeLabel,
                    timeRange    = timeRange,
                    isAlerting   = isAlerting,
                    onButtonClick = {
                        // 鳴っている間は音を止めてから閉じる（鳴り終わったあとは閉じるだけ）
                        stopService(Intent(this@FullScreenAlertActivity, TaskAlertService::class.java))
                        finish()
                    }
                )
            }
        }
    }

    // すでに開いているときに、通知を押した場合・別の予定が鳴った場合
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readTaskInfo(intent)
        TaskAlertService.stopFromNotificationTap(this, intent)
    }

    private fun readTaskInfo(intent: Intent) {
        taskName      = intent.getStringExtra("task_name") ?: ""
        taskTypeLabel = intent.getStringExtra("task_type_label") ?: ""
        timeRange     = intent.getStringExtra("time_range") ?: ""
    }
}

// ─── フルスクリーンUI ───

private val BgColor      = Color(0xFF1C1B1F)
private val AccentColor  = Color(0xFFBB86FC)
private val TextPrimary  = Color(0xFFE6E1E5)
private val TextSecond   = Color(0xFF9E9E9E)

@Composable
private fun FullScreenAlertContent(
    taskName: String,
    taskTypeLabel: String,
    timeRange: String,
    isAlerting: Boolean,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            // タスクタイプ
            Text(
                text = taskTypeLabel,
                color = AccentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(12.dp))

            // タスク名
            Text(
                text = taskName,
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // 時間帯
            Text(
                text = timeRange,
                color = TextSecond,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(48.dp))

            // 鳴っている間は「止める」、鳴り終わったあとは「閉じる」
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentColor,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
            ) {
                Text(
                    text = if (isAlerting) "止める" else "閉じる",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}