package com.chainlock.app

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager

class BlockScreenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val lockedApp = intent.getStringExtra("locked_app") ?: "This App"
        val unlockApp = intent.getStringExtra("unlock_app") ?: "required app"
        val remainingMins = intent.getIntExtra("remaining_minutes", 0)
        val requiredMins = intent.getIntExtra("required_minutes", 1)
        val usedMs = intent.getLongExtra("used_ms", 0L)
        val usedMins = (usedMs / 60000).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0a0a0f"))
            setPadding(60, 60, 60, 60)
        }

        val lockIcon = TextView(this).apply {
            text = "🔒"
            textSize = 72f
            gravity = Gravity.CENTER
        }

        val appName = TextView(this).apply {
            text = lockedApp
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val lockedLabel = TextView(this).apply {
            text = "LOCKED"
            textSize = 13f
            setTextColor(Color.parseColor("#ef4444"))
            gravity = Gravity.CENTER
            letterSpacing = 0.2f
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER
        }

        val infoText = TextView(this).apply {
            text = "Use $unlockApp first"
            textSize = 16f
            setTextColor(Color.parseColor("#a0a0c0"))
            gravity = Gravity.CENTER
        }

        val progressText = TextView(this).apply {
            text = "$usedMins / $requiredMins minutes"
            textSize = 22f
            setTextColor(Color.parseColor("#ff6b35"))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val remainText = TextView(this).apply {
            text = "~$remainingMins min remaining"
            textSize = 14f
            setTextColor(Color.parseColor("#6b6b8a"))
            gravity = Gravity.CENTER
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = requiredMins
            progress = usedMins.coerceAtMost(requiredMins)
            minimumHeight = 16
            progressDrawable.setColorFilter(
                Color.parseColor("#7c3aed"),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        card.addView(infoText)
        card.addView(Space(this).apply { minimumHeight = 16 })
        card.addView(progressText)
        card.addView(remainText)
        card.addView(Space(this).apply { minimumHeight = 20 })
        card.addView(progressBar)

        val backBtn = Button(this).apply {
            text = "← Go Back"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2a2a45"))
            textSize = 15f
            setPadding(60, 24, 60, 24)
            setOnClickListener {
                val home = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(home)
                finish()
            }
        }

        root.addView(lockIcon)
        root.addView(Space(this).apply { minimumHeight = 16 })
        root.addView(appName)
        root.addView(lockedLabel)
        root.addView(Space(this).apply { minimumHeight = 40 })
        root.addView(card)
        root.addView(Space(this).apply { minimumHeight = 40 })
        root.addView(backBtn)

        setContentView(root)
    }

    override fun onBackPressed() {
        val home = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }
}
