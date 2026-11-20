package com.rafinad.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d MMMM, EEEE", RU)

@Composable
fun DayScreen(store: Store, date: LocalDate, tip: Quote, onNextTip: () -> Unit, onProfile: () -> Unit, onToday: () -> Unit) {
    var day by remember(date) { mutableStateOf(store.day(date)) }
    fun update(d: Day) { day = d; store.saveDay(date, d) }
    val isToday = date == LocalDate.now()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (isToday) "Сегодня" else "${date.year}", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date.format(DAY_FORMAT), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            if (!isToday) TextButton(onToday) { Text("К сегодня") }
            IconButton(onProfile) { Icon(Icons.Default.Person, "Профиль") }
        }

        if (isToday) nudgeKind(LocalTime.now().hour)?.let { NudgeBlock(it, day.goal) }

        QuoteCard(tip, onNextTip)

        Section("Главная цель дня") {
            PlainField(day.goal, { update(day.copy(goal = it)) }, "Одна вещь, которая сделает день удачным")
        }
        Section("Постулат дня") {
            PlainField(day.postulate, { update(day.copy(postulate = it)) }, "Принцип, которому следую сегодня")
        }
        Section("Рабочие дела", progress(day.work)) {
            TaskList(day.work, "Добавить рабочее дело") { update(day.copy(work = it)) }
        }
        Section("Домашние дела", progress(day.home)) {
            TaskList(day.home, "Добавить домашнее дело") { update(day.copy(home = it)) }
        }
    }
}

private fun progress(tasks: List<Task>) = if (tasks.isEmpty()) null else "${tasks.count { it.done }}/${tasks.size}"

@Composable
private fun QuoteCard(q: Quote, onNext: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.primary, contentColor = c.onPrimary) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("МЫСЛЬ ДНЯ", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp, color = c.onPrimary.copy(alpha = 0.6f))
                IconButton(onNext) { Icon(Icons.Default.Refresh, "Другая мысль") }
            }
            Column(Modifier.padding(end = 12.dp)) {
                Text("«${q.text}»", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                Text("— ${q.author}", style = MaterialTheme.typography.bodySmall, color = c.onPrimary.copy(alpha = 0.7f))
            }
        }
    }
}
