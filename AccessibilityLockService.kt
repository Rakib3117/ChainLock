package com.chainlock.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AccessibilityLockService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Ignore system UI and our own app
        if (packageName == "com.chainlock.app" ||
            packageName == "com.android.systemui" ||
            packageName == "android") return

        val chains = ChainStorage.loadChains(this)
        val progress = ChainStorage.loadProgress(this)

        for (chain in chains) {
            if (!chain.isActive) continue

            val unlockedStep = ChainStorage.getUnlockedStep(this, chain.id)

            // Check each step after the first unlocked one
            for (stepIndex in unlockedStep + 1 until chain.steps.size) {
                val step = chain.steps[stepIndex]

                if (step.packageName == packageName) {
                    // This app is locked — check if previous step is completed
                    val prevStep = chain.steps[stepIndex - 1]
                    val usedMs = progress[prevStep.packageName] ?: 0L
                    val requiredMs = prevStep.requiredMinutes * 60 * 1000L

                    if (usedMs < requiredMs) {
                        // BLOCK IT — show lock screen
                        val remaining = ((requiredMs - usedMs) / 60000).toInt() + 1
                        val intent = Intent(this, BlockScreenActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("locked_app", step.appName)
                            putExtra("unlock_app", prevStep.appName)
                            putExtra("remaining_minutes", remaining)
                            putExtra("required_minutes", prevStep.requiredMinutes)
                            putExtra("used_ms", usedMs)
                        }
                        startActivity(intent)
                        return
                    } else {
                        // Unlock this step
                        ChainStorage.setUnlockedStep(this, chain.id, stepIndex)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service started
    }
}
