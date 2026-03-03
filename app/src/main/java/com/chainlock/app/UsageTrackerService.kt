package com.chainlock.app

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class UsageTrackerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val INTERVAL_MS = 30_000L
    private val CHANNEL_ID = "chainlock_channel"

    private val tracker = object : Runnable {
        override fun run() {
            updateUsageProgress()
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        handler.post(tracker)
    }

    private fun updateUsageProgress() {
        val usageManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()

        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        val stats = usageManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startOfDay, now
        )

        val progress = ChainStorage.loadProgress(this).toMutableMap()

        for (stat in stats) {
            if (stat.totalTimeInForeground > 0) {
                progress[stat.packageName] = stat.totalTimeInForeground
            }
        }

        ChainStorage.saveProgress(this, progress)
        checkAndUnlockSteps(progress)
    }

    private fun checkAndUnlockSteps(progress: Map<String, Long>) {
        val chains = ChainStorage.loadChains(this)
        for (chain in chains) {
            if (!chain.isActive) continue
            val currentUnlocked = ChainStorage.getUnlockedStep(this, chain.id)

            if (currentUnlocked < chain.steps.size - 1) {
                val currentStep = chain.steps[currentUnlocked]
                val usedMs = progress[currentStep.packageName] ?: 0L
                val requiredMs = currentStep.requiredMinutes * 60 * 1000L

                if (usedMs >= requiredMs) {
                    val newUnlocked = currentUnlocked + 1
                    ChainStorage.setUnlockedStep(this, chain.id, newUnlocked)
                    showUnlockNotification(chain.steps[newUnlocked].appName)
                }
            }
        }
    }

    private fun showUnlockNotification(appName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("🔓 $appName Unlocked!")
            .setContentText("You can now open $appName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("ChainLock Active")
            .setContentText("Monitoring app usage...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "ChainLock", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(tracker)
        super.onDestroy()
    }
}
