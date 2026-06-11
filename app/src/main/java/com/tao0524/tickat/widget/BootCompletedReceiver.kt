package com.tao0524.tickat.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tao0524.tickat.data.local.AppDatabase
import com.tao0524.tickat.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val repository = TaskRepository(db.taskDao())
                val tasks = repository.allTasks.first()
                TaskAlertScheduler.scheduleAll(context, tasks)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

