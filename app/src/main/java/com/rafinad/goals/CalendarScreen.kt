package com.rafinad.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", RU)
private val DAY_TITLE = DateTimeFormatter.ofPattern("d MMMM, EEEE", RU)
private val WEEK = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/** Нажатие на день показывает его деньги; кнопка внизу открывает дела этого дня. */
@Composable
fun CalendarScreen(store: Store, selected: LocalDate, onOpenDay: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.from(selected)) }
    var picked by remember { mutableStateOf(selected) }
    val busy = remember { store.busyDays() }
    val entries = remember { store.money() }
    val spent = remember(entries) { expensesByDay(entries) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Календарь")
        Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад") }
                    Text(month.format(MONTH_FORMAT).replaceFirstChar { it.uppercase() }, Modifier.weight(1f),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton({ month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вперёд") }
                }
                Row {
                    WEEK.forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, color = muted)
                    }
                }
                val blanks = month.atDay(1).dayOfWeek.value - 1
                val cells = List<LocalDate?>(blanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
                cells.chunked(7).forEach { week ->
                    Row {
                        week.forEach { d ->
                            if (d == null) Spacer(Modifier.weight(1f))
                            else DayCell(d, d == picked, d in busy, spent[d.toString()]) { picked = d }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Text("Под числом — сколько потрачено. Точка — есть дела.", style = MaterialTheme.typography.bodySmall, color = muted)

        val dayEntries = entries.filter { it.date == picked.toString() }
        Section(picked.format(DAY_TITLE)) {
            if (dayEntries.isEmpty()) {
                Text("Нет записей о деньгах", color = muted, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                Row(Modifier.padding(top = 8.dp)) {
                    Text("Расходы", Modifier.weight(1f))
                    Text("−" + money(dayEntries.filter { !it.income }.sumOf { it.amount }), color = Expense, fontWeight = FontWeight.Medium)
                }
                Row(Modifier.padding(bottom = 8.dp)) {
                    Text("Доходы", Modifier.weight(1f))
                    Text("+" + money(dayEntries.filter { it.income }.sumOf { it.amount }), color = Income, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                dayEntries.forEach { m ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(m.icon, m.income, 32.dp)
                        Text(m.category.ifBlank { m.note.ifBlank { if (m.income) "Доход" else "Расход" } },
                            Modifier.weight(1f).padding(start = 10.dp))
                        Text((if (m.income) "+" else "−") + money(m.amount), color = if (m.income) Income else Expense)
                    }
                }
            }
            TextButton({ onOpenDay(picked) }) { Text("Открыть дела этого дня →") }
        }
    }
}

@Composable
private fun RowScope.DayCell(d: LocalDate, selected: Boolean, busy: Boolean, spent: Long?, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val today = d == LocalDate.now()
    Box(
        Modifier.weight(1f).aspectRatio(0.8f).padding(2.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.primary else if (today) c.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${d.dayOfMonth}", color = if (selected) c.onPrimary else c.onSurface,
                fontWeight = if (today) FontWeight.Bold else FontWeight.Normal)
            Text(spent?.let { "−" + compact(it) } ?: "", fontSize = 9.sp, maxLines = 1,
                color = if (selected) c.onPrimary else c.onSurfaceVariant)
            Box(Modifier.size(4.dp).clip(CircleShape).background(
                if (!busy) Color.Transparent else if (selected) c.onPrimary else Accent))
        }
    }
}
