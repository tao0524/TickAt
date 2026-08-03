package com.tao0524.tickat.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tao0524.tickat.FullScreenAlertActivity
import com.tao0524.tickat.MainActivity
import com.tao0524.tickat.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TaskAlertService : Service() {

    companion object {
        const val ACTION_TAP = "com.tao0524.tickat.ACTION_ALERT_TAP"
        const val ACTION_DISMISS = "com.tao0524.tickat.ACTION_ALERT_DISMISS"
        private const val SILENT_CHANNEL_ID = "tickat_alert_silent"
        private const val STATIC_CHANNEL_ID = "tickat_alert_static"
    }

    // 内部コピーファイル → MediaPlayer、それ以外 → Ringtone
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var soundJob: Job? = null
    private var currentNotificationId: Int = 0

    // タイマー終了後の静的通知で再利用するタスク情報
    private var taskName: String = ""
    private var contentText: String = ""
    private var alertMode: String = "NOTIFICATION"
    private var taskStartTime: String = ""
    private var taskEndTime: String = ""
    private var featureLabel: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TAP -> {
                stopPlayback(StopReason.TAP)
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                stopPlayback(StopReason.DISMISS)
                return START_NOT_STICKY
            }
        }

        // --- タスク情報の取得 ---
        val taskId = intent?.getStringExtra("task_id") ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        taskName = intent.getStringExtra("task_name") ?: ""
        taskStartTime = intent.getStringExtra("task_start") ?: ""
        taskEndTime = intent.getStringExtra("task_end") ?: ""
        val memo = intent.getStringExtra("task_memo") ?: ""
        val taskType = intent.getStringExtra("task_type") ?: "TIMEBLOCK"
        alertMode = intent.getStringExtra("alert_mode") ?: "NOTIFICATION"
        val soundUriStr = intent.getStringExtra("sound_uri") ?: ""
        val duration = intent.getIntExtra("duration", 5)

        featureLabel = if (taskType == "TIMEBLOCK") "タイムブロック" else "リマインダー"

        contentText = when (taskType) {
            "TIMEBLOCK" -> if (memo.isNotEmpty()) "$taskStartTime〜$taskEndTime\n💬 $memo"
            else "$taskStartTime〜$taskEndTime"
            "REMINDER"  -> if (memo.isNotEmpty()) "💬 $memo" else ""
            else        -> "$taskStartTime〜$taskEndTime"
        }

        currentNotificationId = ("start_$taskId").hashCode()

        // --- サイレント通知チャンネル作成 ---
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(manager)

        // --- フォアグラウンド通知を構築 ---
        val tapPending = PendingIntent.getService(
            this, currentNotificationId,
            Intent(this, TaskAlertService::class.java).apply { action = ACTION_TAP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPending = PendingIntent.getService(
            this, currentNotificationId + 1000,
            Intent(this, TaskAlertService::class.java).apply { action = ACTION_DISMISS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, SILENT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(taskName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPending)
            .setDeleteIntent(dismissPending)
            .setOngoing(false)
            .setAutoCancel(false)

        if (memo.isNotEmpty()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        }

        if (alertMode == "FULLSCREEN") {
            val fullScreenIntent = Intent(this, FullScreenAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("task_name", taskName)
                putExtra("task_type_label", featureLabel)
                putExtra("time_range", "$taskStartTime〜$taskEndTime")
            }
            val fullScreenPending = PendingIntent.getActivity(
                this, currentNotificationId + 2000, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPending, true)
        }

        // --- startForeground ---
        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                currentNotificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
        } else {
            startForeground(currentNotificationId, notification)
        }

        // --- 音声再生 ---
        soundJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                playSound(soundUriStr)
                delay(duration * 1000L)
                stopPlayback(StopReason.TIMER)
            } catch (_: Exception) {
                delay(duration * 1000L)
                stopPlayback(StopReason.TIMER)
            }
        }

        return START_NOT_STICKY
    }

    // ---------------------------------------------------------------
    //  音声再生：内部ファイル → MediaPlayer / それ以外 → Ringtone
    // ---------------------------------------------------------------

    private fun playSound(soundUriStr: String) {
        val audioAttrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        if (soundUriStr.startsWith("/")) {
            // 内部ストレージにコピー済みのファイル → MediaPlayer（パーミッション不要）
            val file = java.io.File(soundUriStr)
            if (!file.exists()) return
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttrs)
                setDataSource(soundUriStr)
                isLooping = true
                prepare()
                start()
            }
        } else {
            // デフォルト音 or content://settings/... → Ringtone API（安全）
            val uri = if (soundUriStr.isNotEmpty()) {
                Uri.parse(soundUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val r = RingtoneManager.getRingtone(this, uri) ?: return
            r.audioAttributes = audioAttrs

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                r.isLooping = true
                r.play()
            } else {
                r.play()
                CoroutineScope(Dispatchers.Main).launch {
                    while (isActive) {
                        delay(500)
                        if (!r.isPlaying) r.play()
                    }
                }
            }
            ringtone = r
        }
    }

    // ---------------------------------------------------------------
    //  通知チャンネル
    // ---------------------------------------------------------------

    private fun ensureChannels(manager: NotificationManager) {
        if (manager.getNotificationChannel(SILENT_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    SILENT_CHANNEL_ID,
                    "タスクアラート",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "タスク開始時のアラート通知（音はアプリ側で制御）"
                    setSound(null, null)
                    enableVibration(true)
                }
            )
        }
        if (manager.getNotificationChannel(STATIC_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    STATIC_CHANNEL_ID,
                    "タスク通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "タスク開始通知（音なし・見逃し防止用）"
                    setSound(null, null)
                }
            )
        }
    }

    // ---------------------------------------------------------------
    //  再生停止
    // ---------------------------------------------------------------

    private enum class StopReason { TIMER, TAP, DISMISS }

    private fun stopPlayback(reason: StopReason) {
        releasePlayer()
        soundJob?.cancel()

        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}

        when (reason) {
            StopReason.TIMER -> {
                postStaticNotification()
            }
            StopReason.TAP -> {
                val target = if (alertMode == "FULLSCREEN") {
                    Intent(this, FullScreenAlertActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("task_name", taskName)
                        putExtra("task_type_label", featureLabel)
                        putExtra("time_range", "$taskStartTime〜$taskEndTime")
                    }
                } else {
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                }
                startActivity(target)
            }
            StopReason.DISMISS -> { }
        }
        stopSelf()
    }

    private fun postStaticNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingOpen = PendingIntent.getActivity(
            this, currentNotificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, STATIC_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(taskName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(currentNotificationId, notification)
    }

    // ---------------------------------------------------------------
    //  リソース解放
    // ---------------------------------------------------------------

    private fun releasePlayer() {
        mediaPlayer?.let { mp ->
            try { if (mp.isPlaying) mp.stop() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
        }
        mediaPlayer = null

        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
    }

    override fun onDestroy() {
        releasePlayer()
        soundJob?.cancel()
        super.onDestroy()
    }
}