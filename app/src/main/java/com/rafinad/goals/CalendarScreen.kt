package com.rafinad.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", RU)
private val WEEK = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun CalendarScreen(store: Store, selected: LocalDate, onPick: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.from(selected)) }
    val busy = remember { store.busyDays() }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            else DayCell(d, d == selected, d in busy) { onPick(d) }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Text("Нажмите на день, чтобы открыть его дела. Точка — есть дела.",
            style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

@Composable
private fun RowScope.DayCell(d: LocalDate, selected: Boolean, busy: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val today = d == LocalDate.now()
    Box(
        Modifier.weight(1f).aspectRatio(1f).padding(3.dp).clip(CircleShape)
            .background(if (selected) c.primary else if (today) c.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${d.dayOfMonth}", color = if (selected) c.onPrimary else c.onSurface,
                fontWeight = if (today) FontWeight.Bold else FontWeight.Normal)
            Box(Modifier.size(4.dp).clip(CircleShape).background(
                if (!busy) Color.Transparent else if (selected) c.onPrimary else c.primary))
        }
    }
}
