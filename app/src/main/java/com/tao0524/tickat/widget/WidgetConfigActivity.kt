package com.tao0524.tickat.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tao0524.tickat.ui.theme.TickAtTheme

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // キャンセルをデフォルトに設定（戻るボタン対策）
        setResult(RESULT_CANCELED)

        // appWidgetIdを取得
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // IDが無効なら終了
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TickAtTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("ウィジェット設定（準備中）")
                    }
                }
            }
        }

        // 設定完了としてRESULT_OKを返す
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

