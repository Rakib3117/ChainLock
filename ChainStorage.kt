package com.chainlock.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// ── Data Models ───────────────────────────────────────────────

data class ChainStep(
    val packageName: String,
    val appName: String,
    val requiredMinutes: Int
)

data class AppChain(
    val id: String,
    val name: String,
    val steps: List<ChainStep>,
    var isActive: Boolean = true
)

// ── Persistent Storage ────────────────────────────────────────

object ChainStorage {

    private const val PREF_NAME = "chainlock_prefs"
    private const val KEY_CHAINS = "chains"
    private const val KEY_PROGRESS = "progress"

    fun saveChains(context: Context, chains: List<AppChain>) {
        val arr = JSONArray()
        for (chain in chains) {
            val obj = JSONObject()
            obj.put("id", chain.id)
            obj.put("name", chain.name)
            obj.put("isActive", chain.isActive)
            val steps = JSONArray()
            for (step in chain.steps) {
                val s = JSONObject()
                s.put("packageName", step.packageName)
                s.put("appName", step.appName)
                s.put("requiredMinutes", step.requiredMinutes)
                steps.put(s)
            }
            obj.put("steps", steps)
            arr.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CHAINS, arr.toString()).apply()
    }

    fun loadChains(context: Context): MutableList<AppChain> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CHAINS, "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<AppChain>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val stepsArr = obj.getJSONArray("steps")
            val steps = mutableListOf<ChainStep>()
            for (j in 0 until stepsArr.length()) {
                val s = stepsArr.getJSONObject(j)
                steps.add(ChainStep(
                    packageName = s.getString("packageName"),
                    appName = s.getString("appName"),
                    requiredMinutes = s.getInt("requiredMinutes")
                ))
            }
            list.add(AppChain(
                id = obj.getString("id"),
                name = obj.getString("name"),
                steps = steps,
                isActive = obj.optBoolean("isActive", true)
            ))
        }
        return list
    }

    // Track usage progress: packageName -> milliseconds used today
    fun saveProgress(context: Context, progress: Map<String, Long>) {
        val obj = JSONObject()
        for ((pkg, ms) in progress) obj.put(pkg, ms)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PROGRESS, obj.toString()).apply()
    }

    fun loadProgress(context: Context): MutableMap<String, Long> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROGRESS, "{}") ?: "{}"
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Long>()
        for (key in obj.keys()) map[key] = obj.getLong(key)
        return map
    }

    fun resetDailyProgress(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_PROGRESS).apply()
    }

    // Which step is currently unlocked per chain
    fun getUnlockedStep(context: Context, chainId: String): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt("unlocked_$chainId", 0)
    }

    fun setUnlockedStep(context: Context, chainId: String, step: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt("unlocked_$chainId", step).apply()
    }
}
