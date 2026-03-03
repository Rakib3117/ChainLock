package com.chainlock.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.widget.*
import android.graphics.Color

class MainActivity : Activity() {

    private lateinit var chainListLayout: LinearLayout
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0a0f"))
            setPadding(24, 48, 24, 48)
        }
        scroll.addView(root)

        val header = TextView(this).apply {
            text = "🔗 ChainLock"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Serial App Unlock System"
            textSize = 13f
            setTextColor(Color.parseColor("#6b6b8a"))
            gravity = android.view.Gravity.CENTER
        }

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(32, 24, 32, 24)
        }

        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#a0a0c0"))
        }

        val permBtn = Button(this).apply {
            text = "⚙️ Grant Permissions"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#7c3aed"))
            textSize = 14f
            setOnClickListener { checkAndRequestPermissions() }
        }

        statusCard.addView(statusText)
        statusCard.addView(Space(this).apply { minimumHeight = 12 })
        statusCard.addView(permBtn)

        val addChainBtn = Button(this).apply {
            text = "+ New Chain"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#ff6b35"))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 32, 0, 32)
            setOnClickListener { showAddChainDialog() }
        }

        val chainsLabel = TextView(this).apply {
            text = "YOUR CHAINS"
            textSize = 11f
            setTextColor(Color.parseColor("#6b6b8a"))
            letterSpacing = 0.2f
        }

        chainListLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val resetBtn = Button(this).apply {
            text = "↺ Reset Daily Progress"
            setTextColor(Color.parseColor("#6b6b8a"))
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            textSize = 13f
            setOnClickListener {
                ChainStorage.resetDailyProgress(this@MainActivity)
                val chains = ChainStorage.loadChains(this@MainActivity)
                for (chain in chains) ChainStorage.setUnlockedStep(this@MainActivity, chain.id, 0)
                Toast.makeText(this@MainActivity, "Daily progress reset!", Toast.LENGTH_SHORT).show()
                refreshChainList()
            }
        }

        root.addView(header)
        root.addView(subtitle)
        root.addView(Space(this).apply { minimumHeight = 24 })
        root.addView(statusCard)
        root.addView(Space(this).apply { minimumHeight = 20 })
        root.addView(addChainBtn)
        root.addView(Space(this).apply { minimumHeight = 28 })
        root.addView(chainsLabel)
        root.addView(Space(this).apply { minimumHeight = 12 })
        root.addView(chainListLayout)
        root.addView(Space(this).apply { minimumHeight = 20 })
        root.addView(resetBtn)

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshChainList()
        startService(Intent(this, UsageTrackerService::class.java))
    }

    private fun updateStatus() {
        val accessibility = isAccessibilityEnabled()
        val usage = isUsagePermissionGranted()
        statusText.text = buildString {
            append("Accessibility Service: ${if (accessibility) "✅ ON" else "❌ OFF"}\n")
            append("Usage Stats: ${if (usage) "✅ ON" else "❌ OFF"}\n")
            if (accessibility && usage) append("\n🟢 ChainLock is ACTIVE")
            else append("\n🔴 Grant permissions above to activate")
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${AccessibilityLockService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
            .any { it.equals(service, ignoreCase = true) }
    }

    private fun isUsagePermissionGranted(): Boolean {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                info.applicationInfo.uid, packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    private fun checkAndRequestPermissions() {
        if (!isUsagePermissionGranted()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Enable ChainLock in Usage Access", Toast.LENGTH_LONG).show()
        } else if (!isAccessibilityEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Enable ChainLock Service in Accessibility", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "✅ All permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshChainList() {
        chainListLayout.removeAllViews()
        val chains = ChainStorage.loadChains(this)
        val progress = ChainStorage.loadProgress(this)

        if (chains.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No chains yet.\nTap '+ New Chain' to create one!"
                textSize = 14f
                setTextColor(Color.parseColor("#6b6b8a"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            chainListLayout.addView(empty)
            return
        }

        for (chain in chains) {
            chainListLayout.addView(buildChainCard(chain, progress))
            chainListLayout.addView(Space(this).apply { minimumHeight = 16 })
        }
    }

    private fun buildChainCard(chain: AppChain, progress: Map<String, Long>): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(28, 24, 28, 24)
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = chain.name
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val toggle = TextView(this).apply {
            text = if (chain.isActive) "ACTIVE" else "PAUSED"
            textSize = 11f
            setTextColor(if (chain.isActive) Color.parseColor("#22c55e") else Color.parseColor("#6b6b8a"))
            setPadding(16, 8, 16, 8)
            setBackgroundColor(if (chain.isActive) Color.parseColor("#22c55e22") else Color.parseColor("#2a2a45"))
        }

        titleRow.addView(title)
        titleRow.addView(toggle)
        card.addView(titleRow)
        card.addView(Space(this).apply { minimumHeight = 16 })

        val unlockedStep = ChainStorage.getUnlockedStep(this, chain.id)
        for ((i, step) in chain.steps.withIndex()) {
            val usedMs = progress[step.packageName] ?: 0L
            val usedMins = (usedMs / 60000).toInt()
            val isDone = i < unlockedStep
            val isCurrent = i == unlockedStep

            val stepRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 6, 0, 6)
            }

            val icon = TextView(this).apply {
                text = when { isDone -> "✅"; isCurrent -> "▶"; else -> "🔒" }
                textSize = 16f
                setPadding(0, 0, 16, 0)
            }

            val stepInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val appNameTv = TextView(this).apply {
                text = step.appName
                textSize = 14f
                setTextColor(when { isDone -> Color.parseColor("#22c55e"); isCurrent -> Color.WHITE; else -> Color.parseColor("#6b6b8a") })
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val timeTv = TextView(this).apply {
                text = if (isCurrent) "$usedMins / ${step.requiredMinutes} min used"
                       else "${step.requiredMinutes} min required"
                textSize = 12f
                setTextColor(Color.parseColor("#6b6b8a"))
            }

            stepInfo.addView(appNameTv)
            stepInfo.addView(timeTv)
            stepRow.addView(icon)
            stepRow.addView(stepInfo)
            card.addView(stepRow)

            if (i < chain.steps.size - 1) {
                card.addView(TextView(this).apply {
                    text = "    ↓ unlock next"
                    textSize = 11f
                    setTextColor(Color.parseColor("#ff6b3566"))
                })
            }
        }

        card.addView(Space(this).apply { minimumHeight = 20 })

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }

        val toggleBtn = Button(this).apply {
            text = if (chain.isActive) "⏸ Pause" else "▶ Activate"
            setTextColor(Color.parseColor("#a0a0c0"))
            setBackgroundColor(Color.parseColor("#2a2a45"))
            textSize = 12f
            setOnClickListener {
                val chains = ChainStorage.loadChains(this@MainActivity).toMutableList()
                val idx = chains.indexOfFirst { it.id == chain.id }
                if (idx >= 0) {
                    chains[idx] = chains[idx].copy(isActive = !chains[idx].isActive)
                    ChainStorage.saveChains(this@MainActivity, chains)
                    refreshChainList()
                }
            }
        }

        val deleteBtn = Button(this).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#ef4444"))
            setBackgroundColor(Color.parseColor("#ef444422"))
            textSize = 12f
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete Chain?")
                    .setMessage("Delete '${chain.name}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        val chains = ChainStorage.loadChains(this@MainActivity).toMutableList()
                        chains.removeAll { it.id == chain.id }
                        ChainStorage.saveChains(this@MainActivity, chains)
                        refreshChainList()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        btnRow.addView(toggleBtn)
        btnRow.addView(Space(this).apply { minimumWidth = 12 })
        btnRow.addView(deleteBtn)
        card.addView(btnRow)
        return card
    }

    private fun showAddChainDialog() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString() }

        val appNames = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val appPkgs = apps.map { it.packageName }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val chainNameInput = EditText(this).apply {
            hint = "Chain name (e.g. Study Mode)"
            textSize = 15f
        }
        layout.addView(TextView(this).apply { text = "Chain Name"; textSize = 12f; setTextColor(Color.GRAY) })
        layout.addView(chainNameInput)
        layout.addView(Space(this).apply { minimumHeight = 24 })
        layout.addView(TextView(this).apply {
            text = "Steps (in order):"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        layout.addView(Space(this).apply { minimumHeight = 12 })

        val stepsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(stepsContainer)

        fun addStepRow() {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val num = stepsContainer.childCount + 1
            row.addView(TextView(this).apply {
                text = "$num."
                textSize = 14f
                setTextColor(Color.parseColor("#7c3aed"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 12, 0)
            })
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, appNames)
                    .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val minInput = EditText(this).apply {
                hint = "min"
                setText("30")
                textSize = 13f
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(140, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            row.addView(spinner)
            row.addView(Space(this).apply { minimumWidth = 8 })
            row.addView(minInput)
            stepsContainer.addView(row)
        }

        addStepRow(); addStepRow()

        layout.addView(Button(this).apply {
            text = "+ Add Step"
            textSize = 13f
            setOnClickListener { addStepRow() }
        })

        AlertDialog.Builder(this)
            .setTitle("Create Chain")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = chainNameInput.text.toString().trim().ifEmpty { "Chain ${System.currentTimeMillis()}" }
                val steps = mutableListOf<ChainStep>()
                for (i in 0 until stepsContainer.childCount) {
                    val row = stepsContainer.getChildAt(i) as LinearLayout
                    val spinner = row.getChildAt(1) as Spinner
                    val minInput = row.getChildAt(3) as EditText
                    val idx = spinner.selectedItemPosition
                    val mins = minInput.text.toString().toIntOrNull() ?: 30
                    steps.add(ChainStep(appPkgs[idx], appNames[idx], mins))
                }
                if (steps.size < 2) {
                    Toast.makeText(this, "Need at least 2 steps!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val chains = ChainStorage.loadChains(this).toMutableList()
                chains.add(AppChain(System.currentTimeMillis().toString(), name, steps))
                ChainStorage.saveChains(this, chains)
                refreshChainList()
                Toast.makeText(this, "✅ '$name' saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
