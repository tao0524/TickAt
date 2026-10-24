package com.tao0524.tickat.ui.component

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ─── 全画面通知（USE_FULL_SCREEN_INTENT）の許可 ───
// Android 14 以上では、Google Play から入れたアプリには最初は許可がない。
// Android 13 以下では許可が自動で与えられる。

const val FULL_SCREEN_PROMPT_TITLE = "全画面でのお知らせ"

const val FULL_SCREEN_PROMPT_MESSAGE =
    "TickAt は、予定の時刻に、画面全体でお知らせできます（右上の⚙ →「通知」→「全画面」）。\n\n" +
    "画面が真っ暗なときやロック画面のときにも全画面で出すには、スマホの設定で許可が必要です。\n\n" +
    "許可しない場合は、次のようにお知らせします。\n" +
    "スマホを使っているときは、画面のいちばん上に、通知が出ます（「止める」ボタン付き）。\n" +
    "画面が真っ暗なときは、全画面では出ず、音だけでお知らせします。\n\n" +
    "あとから許可したいときは、ヘルプに手順があります。"

const val FULL_SCREEN_SELECTED_TITLE = "許可が必要です"

const val FULL_SCREEN_SELECTED_MESSAGE =
    "画面が真っ暗なときやロック画面のときにも全画面で出すには、スマホの設定で許可が必要です。\n\n" +
    "許可しない場合、画面が真っ暗なときやロック画面のときは、全画面では出ません。"

/** 全画面通知の許可を利用者にお願いできる版か（Android 14 以上） */
fun isFullScreenPermissionRequestable(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/** Android 14 以上で、全画面通知の許可がないとき true */
fun needsFullScreenPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    return !manager.canUseFullScreenIntent()
}

/** 「全画面通知」の画面を開く。その画面がない機種では、TickAt の「アプリ情報」の画面を開く */
fun openFullScreenPermissionSettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.startActivity(appDetails)
        return
    }
    try {
        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(appDetails)
    }
}

/** 全画面通知の許可のお願い。左が「今はしない」、右が「設定を開く」 */
@Composable
fun FullScreenPermissionDialog(
    title: String,
    message: String,
    onLater: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(title) },
        text = {
            Text(
                text = message,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("設定を開く") }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("今はしない") }
        }
    )
}

