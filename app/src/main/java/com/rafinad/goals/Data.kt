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

/** amount — целые тенге; date — ISO yyyy-MM-dd; category/emoji копируются, чтобы запись пережила удаление категории. */
data class Money(
    val amount: Long,
    val income: Boolean,
    val note: String,
    val date: String,
    val category: String = "",
    val emoji: String = "",
)

data class Category(val name: String, val emoji: String, val income: Boolean)

val DEFAULT_CATEGORIES = listOf(
    Category("Зарплата", "💼", true),
    Category("Аренда", "🏠", true),
    Category("Семья", "👨‍👩‍👧", false),
    Category("Работа", "💻", false),
    Category("Продукты", "🛒", false),
)

/** birthday — ISO yyyy-MM-dd или пусто. */
data class Profile(val name: String, val birthday: String, val occupation: String, val reason: String)

fun greeting(hour: Int) = when (hour) {
    in 5..11 -> "Доброе утро"
    in 12..16 -> "Добрый день"
    in 17..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

// shortcut: рождённые 29 февраля не получат поздравление в невисокосный год
fun isBirthday(birthday: String, today: LocalDate): Boolean {
    if (birthday.isBlank()) return false
    val b = LocalDate.parse(birthday)
    return b.month == today.month && b.dayOfMonth == today.dayOfMonth
}

data class CategoryTotal(val emoji: String, val name: String, val total: Long)

fun balance(items: List<Money>): Long = items.sumOf { if (it.income) it.amount else -it.amount }

/** Сумма расходов по дням: ключ — ISO-дата. */
fun expensesByDay(items: List<Money>): Map<String, Long> =
    items.filter { !it.income }.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }

/** Короткая запись суммы для клетки календаря: 450, 12к, 1,2м. */
fun compact(v: Long): String = when {
    v < 1_000 -> "$v"
    v < 1_000_000 -> "${v / 1_000}к"
    else -> "${v / 1_000_000},${v % 1_000_000 / 100_000}м"
}

fun totalsByCategory(items: List<Money>, income: Boolean): List<CategoryTotal> = items
    .filter { it.income == income }
    .groupBy { it.emoji to it.category }
    .map { (k, v) -> CategoryTotal(k.first, k.second, v.sumOf { it.amount }) }
    .sortedByDescending { it.total }

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
            Money(it.getLong("amount"), it.getBoolean("income"), it.optString("note"), it.getString("date"),
                it.optString("category"), it.optString("emoji"))
        }
    } ?: emptyList()

    fun saveMoney(items: List<Money>) = write(MONEY, JSONArray(items.map {
        JSONObject().put("amount", it.amount).put("income", it.income).put("note", it.note).put("date", it.date)
            .put("category", it.category).put("emoji", it.emoji)
    }))

    /** Баланс = startBalance + все доходы − все расходы. */
    fun startBalance(): Long = prefs.getLong(START_BALANCE, 0L)

    fun saveStartBalance(v: Long) = prefs.edit().putLong(START_BALANCE, v).apply()

    fun profile(): Profile? = read(PROFILE) { raw ->
        val o = JSONObject(raw)
        Profile(o.getString("name"), o.optString("birthday"), o.optString("occupation"), o.optString("reason"))
    }

    fun saveProfile(p: Profile) = write(PROFILE, JSONObject()
        .put("name", p.name).put("birthday", p.birthday).put("occupation", p.occupation).put("reason", p.reason))

    fun categories(): List<Category> = read(CATEGORIES) { raw ->
        objects(JSONArray(raw)).map { Category(it.getString("name"), it.getString("emoji"), it.getBoolean("income")) }
    } ?: DEFAULT_CATEGORIES

    fun saveCategories(cats: List<Category>) = write(CATEGORIES, JSONArray(cats.map {
        JSONObject().put("name", it.name).put("emoji", it.emoji).put("income", it.income)
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
        const val CATEGORIES = "categories"
        const val PROFILE = "profile"
        const val START_BALANCE = "start_balance"
        val DAY_KEY = Regex("day_\\d{4}-\\d{2}-\\d{2}")
    }
}
