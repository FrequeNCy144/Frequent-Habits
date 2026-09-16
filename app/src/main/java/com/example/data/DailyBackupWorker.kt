package com.example.data

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class DailyBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("habits_settings", Context.MODE_PRIVATE)
        val backupUriStr = sharedPrefs.getString("backup_folder_uri", "") ?: ""
        if (backupUriStr.isEmpty()) {
            return Result.failure()
        }

        val success = BackupManager.performBackup(applicationContext, backupUriStr)
        return if (success) {
            Result.success()
        } else {
            // Retry if fails
            Result.retry()
        }
    }

    companion object {
        fun scheduleDailyBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .build()

            val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DailyBackupWork",
                ExistingPeriodicWorkPolicy.KEEP, // Avoid resetting timer on every launch
                backupRequest
            )
        }
    }
}
