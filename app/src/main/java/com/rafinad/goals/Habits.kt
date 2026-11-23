package com.rafinad.goals

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * days — дни недели по плану (1 = пн … 7 = вс), пусто — каждый день.
 * В остальные дни привычку можно выполнить как бонус.
 * reminder — время уведомления "HH:mm" или пусто.
 */
data class Habit(
    val id: String,
    val name: String,
    val icon: String,
    val days: Set<Int> = emptySet(),
    val steps: List<String> = emptyList(),
    val hint: String = "",
    val reminder: String = "",
)

/** level — оценка дня 0–10; steps — индексы выполненных пунктов. */
data class HabitMark(val level: Int = 0, val steps: Set<Int> = emptySet())

const val READING = "reading"

val DEFAULT_HABITS = listOf(
    Habit("sport", "Спорт", "sport", setOf(1, 4),
        listOf("Жим лёжа", "Турник", "Брусья", "Жим ногами", "Квадрицепсы", "Жим в наклоне", "Бицепс", "Икры", "Плечи"),
        "10 — вся программа"),
    Habit("hygiene", "Гигиена перед сном", "night", emptySet(),
        listOf("Почистить зубы", "Снять линзы"), "10 — всё сделано", reminder = "21:30"),
    Habit("it", "Прокачка в IT", "laptop", emptySet(),
        listOf("Прочитать новости", "Посмотреть GitHub", "Изучить новый термин или возможность", "Записать идею"),
        "10 — всё из списка и новая идея"),
    Habit(READING, "Чтение книг", "book", emptySet(), emptyList(), "10 — 60 страниц и больше"),
    Habit("mind", "Ментальное и духовное развитие", "spa", emptySet(),
        listOf("Тренинг или обучение", "Работа над поведением в коллективе", "Навыки лидера"),
        "10 — день прожит осознанно"),
)

fun Habit.plannedOn(date: LocalDate) = days.isEmpty() || date.dayOfWeek.value in days

/** Уровень по доле выполненных пунктов. */
fun levelFromSteps(done: Int, total: Int) = if (total == 0) 0 else (done * 10 + total - 1) / total

/** Средний уровень (0–10) по отметкам; null — нечего считать. */
fun averageLevel(levels: List<Int>): Float? = if (levels.isEmpty()) null else levels.average().toFloat()

/** Уведомления о привычках с указанным временем. Переустанавливаются каждый день и после перезагрузки. */
object Reminders {
    private const val CHANNEL = "habits"

    fun sync(ctx: Context, old: List<Habit>, new: List<Habit>) {
        old.forEach { cancel(ctx, it.id) }
        new.filter { it.reminder.isNotBlank() }.forEach { schedule(ctx, it) }
    }

    fun scheduleAll(ctx: Context) = Store(ctx).habits().filter { it.reminder.isNotBlank() }.forEach { schedule(ctx, it) }

    fun schedule(ctx: Context, habit: Habit) {
        val time = runCatching { LocalTime.parse(habit.reminder) }.getOrNull() ?: return
        var at = LocalDate.now().atTime(time)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // inexact window: exact alarms need a special permission that Play restricts
        ctx.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, ms, 5 * 60_000L, pending(ctx, habit.id))
    }

    private fun cancel(ctx: Context, id: String) = ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, id))

    private fun pending(ctx: Context, id: String) = PendingIntent.getBroadcast(
        ctx, id.hashCode(),
        Intent(ctx, ReminderReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun show(ctx: Context, habit: Habit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Привычки", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(habit.id.hashCode(), Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habit.name)
            .setContentText(habit.steps.joinToString(" · ").ifBlank { "Время для привычки" })
            .setContentIntent(open)
            .setAutoCancel(true)
            .build())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) return Reminders.scheduleAll(ctx)
        val habit = Store(ctx).habits().find { it.id == intent.getStringExtra("id") } ?: return
        if (habit.reminder.isBlank()) return
        Reminders.show(ctx, habit)
        Reminders.schedule(ctx, habit) // next day
    }
}
