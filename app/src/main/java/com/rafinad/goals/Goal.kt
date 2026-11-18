package com.rafinad.goals

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Task(val title: String, val done: Boolean = false)

data class Goal(val title: String, val tasks: List<Task> = emptyList()) {
    val progress: Float
        get() = if (tasks.isEmpty()) 0f else tasks.count { it.done }.toFloat() / tasks.size
}

/** Хранит цели в SharedPreferences одной JSON-строкой. */
class GoalStore(context: Context) {
    private val prefs = context.getSharedPreferences("goals", Context.MODE_PRIVATE)

    fun load(): List<Goal> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val g = arr.getJSONObject(i)
                val ts = g.getJSONArray("tasks")
                Goal(g.getString("title"), (0 until ts.length()).map { j ->
                    val t = ts.getJSONObject(j)
                    Task(t.getString("title"), t.getBoolean("done"))
                })
            }
        } catch (e: JSONException) {
            // keep the broken data so the next save doesn't wipe it for good
            prefs.edit().putString("$KEY.corrupt", raw).apply()
            emptyList()
        }
    }

    fun save(goals: List<Goal>) {
        val arr = JSONArray()
        goals.forEach { g ->
            val ts = JSONArray()
            g.tasks.forEach { ts.put(JSONObject().put("title", it.title).put("done", it.done)) }
            arr.put(JSONObject().put("title", g.title).put("tasks", ts))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object { const val KEY = "data" }
}
