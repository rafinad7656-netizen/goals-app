package com.rafinad.goals

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

data class Task(val title: String, val done: Boolean = false)

data class Goal(val title: String, val tasks: List<Task> = emptyList()) {
    val progress: Float
        get() = if (tasks.isEmpty()) 0f else tasks.count { it.done }.toFloat() / tasks.size
}

data class Day(
    val goal: String = "",
    val postulate: String = "",
    val work: List<Task> = emptyList(),
    val home: List<Task> = emptyList(),
)

/** amount — целые тенге; date — ISO yyyy-MM-dd. */
data class Money(val amount: Long, val income: Boolean, val note: String, val date: String)

fun balance(items: List<Money>): Long = items.sumOf { if (it.income) it.amount else -it.amount }

fun <T> List<T>.replace(i: Int, v: T) = toMutableList().also { it[i] = v }
fun <T> List<T>.without(i: Int) = filterIndexed { j, _ -> j != i }

/** Всё хранится в SharedPreferences JSON-строками: цели, каждый день отдельно, финансы. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("goals", Context.MODE_PRIVATE)

    fun goals(): List<Goal> = read(GOALS) { raw ->
        objects(JSONArray(raw)).map { Goal(it.getString("title"), tasks(it.optJSONArray("tasks"))) }
    } ?: emptyList()

    fun saveGoals(goals: List<Goal>) = write(GOALS, JSONArray(goals.map {
        JSONObject().put("title", it.title).put("tasks", tasksJson(it.tasks))
    }))

    fun day(date: LocalDate): Day = read(dayKey(date)) { raw ->
        val o = JSONObject(raw)
        Day(o.optString("goal"), o.optString("postulate"), tasks(o.optJSONArray("work")), tasks(o.optJSONArray("home")))
    } ?: Day()

    fun saveDay(date: LocalDate, d: Day) = write(dayKey(date), JSONObject()
        .put("goal", d.goal).put("postulate", d.postulate)
        .put("work", tasksJson(d.work)).put("home", tasksJson(d.home)))

    /** Дни, в которых есть хотя бы одно дело — для точек в календаре. */
    fun busyDays(): Set<LocalDate> = prefs.all.keys
        .filter { DAY_KEY.matches(it) }
        .map { LocalDate.parse(it.removePrefix("day_")) }
        .filter { day(it).let { d -> d.work.isNotEmpty() || d.home.isNotEmpty() } }
        .toSet()

    fun money(): List<Money> = read(MONEY) { raw ->
        objects(JSONArray(raw)).map {
            Money(it.getLong("amount"), it.getBoolean("income"), it.optString("note"), it.getString("date"))
        }
    } ?: emptyList()

    fun saveMoney(items: List<Money>) = write(MONEY, JSONArray(items.map {
        JSONObject().put("amount", it.amount).put("income", it.income).put("note", it.note).put("date", it.date)
    }))

    private fun <T> read(key: String, parse: (String) -> T): T? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            parse(raw)
        } catch (e: JSONException) {
            // keep the broken data so the next save doesn't wipe it for good
            prefs.edit().putString("$key.corrupt", raw).apply()
            null
        }
    }

    private fun write(key: String, value: Any) = prefs.edit().putString(key, value.toString()).apply()

    private fun objects(arr: JSONArray) = (0 until arr.length()).map { arr.getJSONObject(it) }

    private fun tasks(arr: JSONArray?) =
        if (arr == null) emptyList() else objects(arr).map { Task(it.getString("title"), it.getBoolean("done")) }

    private fun tasksJson(tasks: List<Task>) =
        JSONArray(tasks.map { JSONObject().put("title", it.title).put("done", it.done) })

    private fun dayKey(date: LocalDate) = "day_$date"

    private companion object {
        const val GOALS = "data" // старый ключ, чтобы сохранённые цели не пропали
        const val MONEY = "money"
        val DAY_KEY = Regex("day_\\d{4}-\\d{2}-\\d{2}")
    }
}
