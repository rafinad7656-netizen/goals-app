package com.rafinad.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private val DAY_TITLE = DateTimeFormatter.ofPattern("d MMMM, EEEE", RU)
private val MONTH_NAME = DateTimeFormatter.ofPattern("LLLL", RU)
private val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private fun fmt(v: Float) = String.format(RU, "%.1f", v)

private fun schedule(h: Habit): String {
    val days = if (h.days.isEmpty()) "Каждый день" else h.days.sorted().joinToString(", ") { WEEKDAYS[it - 1] }
    return if (h.reminder.isBlank()) days else "$days · напоминание ${h.reminder}"
}

@Composable
fun HabitsScreen(store: Store) {
    val ctx = LocalContext.current
    var habits by remember { mutableStateOf(store.habits()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var marks by remember(date) { mutableStateOf(store.marks(date)) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    fun saveHabits(new: List<Habit>) { Reminders.sync(ctx, habits, new); habits = new; store.saveHabits(new) }
    fun mark(id: String, m: HabitMark) { marks = marks + (id to m); store.saveMarks(date, marks) }

    val planned = habits.filter { it.plannedOn(date) }
    val bonus = habits.filter { !it.plannedOn(date) }
    val dayLevel = averageLevel(
        planned.map { marks[it.id]?.level ?: 0 } + bonus.mapNotNull { marks[it.id]?.level?.takeIf { l -> l > 0 } })
    val today = LocalDate.now()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Привычки") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ date = date.minusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Предыдущий день") }
                Text(if (date == today) "Сегодня" else date.format(DAY_TITLE), Modifier.weight(1f),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                IconButton({ date = date.plusDays(1) }, enabled = date < today) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Следующий день")
                }
            }
        }
        item { LevelCard(dayLevel) }
        items(planned, key = { it.id }) { h ->
            HabitCard(store, h, marks[h.id] ?: HabitMark(), date, { mark(h.id, it) }) { editing = h }
        }
        if (bonus.isNotEmpty()) {
            item {
                Text("Бонус — сегодня не по плану, но можно сделать", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            }
            items(bonus, key = { "b" + it.id }) { h ->
                HabitCard(store, h, marks[h.id] ?: HabitMark(), date, { mark(h.id, it) }) { editing = h }
            }
        }
        item {
            OutlinedButton({ editing = Habit("", "", "check") }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Новая привычка")
            }
        }
    }

    editing?.let { h ->
        HabitEditor(
            h,
            onDismiss = { editing = null },
            onDelete = if (h.id.isBlank()) null else ({ saveHabits(habits.filter { it.id != h.id }); editing = null }),
        ) { new ->
            saveHabits(if (h.id.isBlank()) habits + new.copy(id = "h${System.currentTimeMillis()}")
                       else habits.map { if (it.id == h.id) new else it })
            editing = null
        }
    }
}

@Composable
private fun LevelCard(level: Float?) {
    val c = MaterialTheme.colorScheme
    val word = when {
        level == null || level == 0f -> "Отметьте, что сделали"
        level < 3 -> "Начало положено"
        level < 6 -> "Хороший день"
        level < 8.5 -> "Отличный день"
        else -> "Максимум!"
    }
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.primary, contentColor = c.onPrimary) {
        Column(Modifier.padding(20.dp)) {
            Text("Уровень дня", color = c.onPrimary.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(level?.let { fmt(it) } ?: "0", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(" / 10", color = c.onPrimary.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(word, color = c.onPrimary.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun HabitCard(store: Store, habit: Habit, mark: HabitMark, date: LocalDate, onMark: (HabitMark) -> Unit, onEdit: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.surface) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(habit.icon, income = true)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(schedule(habit), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                Text("${mark.level}/10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = if (mark.level > 0) Accent else c.onSurfaceVariant)
                IconButton(onEdit) { Icon(Icons.Outlined.Edit, "Изменить привычку", tint = c.onSurfaceVariant) }
            }
            Slider(
                value = mark.level.toFloat(),
                onValueChange = {
                    val l = it.roundToInt()
                    if (l == 10 && mark.level < 10) playDone()
                    if (l != mark.level) onMark(mark.copy(level = l))
                },
                valueRange = 0f..10f, steps = 9,
                modifier = Modifier.padding(end = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Accent, activeTrackColor = Accent, activeTickColor = Paper.copy(alpha = 0.5f),
                    inactiveTrackColor = c.surfaceVariant, inactiveTickColor = c.onSurfaceVariant.copy(alpha = 0.4f),
                ),
            )
            val hint = if (habit.id == READING) "≈ ${mark.level * 6} стр. · ${habit.hint}" else habit.hint
            if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            if (habit.id == READING) BooksRow(store, YearMonth.from(date))
            if (habit.steps.isNotEmpty()) {
                TextButton({ open = !open }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (open) "Скрыть пункты" else "Пункты: ${mark.steps.size} из ${habit.steps.size}", color = Accent)
                }
                if (open) habit.steps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(i in mark.steps, { on ->
                            if (on) playDone()
                            val done = if (on) mark.steps + i else mark.steps - i
                            // отмеченные пункты подтягивают уровень, но не снижают выбранный вручную
                            onMark(HabitMark(if (on) max(mark.level, levelFromSteps(done.size, habit.steps.size)) else mark.level, done))
                        }, colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Paper))
                        Text(step)
                    }
                }
            }
        }
    }
}

@Composable
private fun BooksRow(store: Store, month: YearMonth) {
    var n by remember(month) { mutableIntStateOf(store.books(month)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Книг за ${month.format(MONTH_NAME)}: $n из 1")
            if (n >= 1) Text("Цель месяца выполнена", style = MaterialTheme.typography.bodySmall, color = Accent)
        }
        IconButton({ if (n > 0) { n--; store.saveBooks(month, n) } }) { Icon(Icons.Default.Remove, "Убрать книгу") }
        IconButton({ n++; store.saveBooks(month, n); playDone() }) { Icon(Icons.Default.Add, "Прочитал книгу") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitEditor(habit: Habit, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (Habit) -> Unit) {
    var name by remember { mutableStateOf(habit.name) }
    var icon by remember { mutableStateOf(habit.icon) }
    var days by remember { mutableStateOf(habit.days) }
    var steps by remember { mutableStateOf(habit.steps) }
    var hint by remember { mutableStateOf(habit.hint) }
    var reminder by remember { mutableStateOf(habit.reminder) }
    var pickingTime by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme
    val label: @Composable (String) -> Unit = { text ->
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({ onSave(habit.copy(name = name.trim(), icon = icon, days = days, steps = steps, hint = hint.trim(), reminder = reminder)) },
                enabled = name.isNotBlank()) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } },
        title = { Text(if (habit.id.isBlank()) "Новая привычка" else "Привычка") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Название") }, singleLine = true)
                label("Иконка")
                IconPicker(icon) { icon = it }
                label("Дни по плану (не выбрано — каждый день)")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WEEKDAYS.forEachIndexed { i, d ->
                        val on = (i + 1) in days
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(if (on) Accent else c.surfaceVariant)
                                .clickable { days = if (on) days - (i + 1) else days + (i + 1) },
                            contentAlignment = Alignment.Center,
                        ) { Text(d, style = MaterialTheme.typography.labelMedium, color = if (on) Paper else c.onSurface) }
                    }
                }
                label("Пункты")
                steps.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s, Modifier.weight(1f))
                        IconButton({ steps = steps.without(i) }) { Icon(Icons.Default.Close, "Убрать пункт", tint = c.onSurfaceVariant) }
                    }
                }
                AddRow("Добавить пункт") { steps = steps + it }
                label("Что значит уровень 10")
                OutlinedTextField(hint, { hint = it.take(60) }, placeholder = { Text("Например: 60 страниц") }, singleLine = true)
                label("Напоминание")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(reminder.isNotBlank(), { on -> reminder = if (on) "21:30" else "" },
                        colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Paper))
                    if (reminder.isNotBlank()) TextButton({ pickingTime = true }) { Text("в $reminder", color = Accent) }
                }
                if (onDelete != null) TextButton(onDelete, Modifier.padding(top = 16.dp)) { Text("Удалить привычку") }
            }
        },
        shape = CardShape,
    )

    if (pickingTime) {
        val t = runCatching { LocalTime.parse(reminder) }.getOrDefault(LocalTime.of(21, 30))
        val state = rememberTimePickerState(t.hour, t.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton({ reminder = String.format(RU, "%02d:%02d", state.hour, state.minute); pickingTime = false }) { Text("Готово") }
            },
            dismissButton = { TextButton({ pickingTime = false }) { Text("Отмена") } },
            text = { TimeInput(state) },
        )
    }
}

/** Характеристика привычек для главного экрана: средний уровень за 7 дней и напоминания, время которых пришло. */
@Composable
fun HabitsSummary(store: Store, onOpen: () -> Unit) {
    val habits = remember { store.habits() }
    val today = LocalDate.now()
    val week = remember { (0L..6L).map { today.minusDays(it) }.associateWith { store.marks(it) } }
    val todayMarks = week[today].orEmpty()
    val c = MaterialTheme.colorScheme

    val now = LocalTime.now()
    habits.filter { h ->
        h.plannedOn(today) && (todayMarks[h.id]?.level ?: 0) < 10 &&
            runCatching { !now.isBefore(LocalTime.parse(h.reminder)) }.getOrDefault(false)
    }.forEach { h ->
        Surface(onOpen, Modifier.fillMaxWidth(), shape = CardShape, color = Accent, contentColor = Paper) {
            Column(Modifier.padding(16.dp)) {
                Text("Пора: ${h.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (h.steps.isNotEmpty()) Text(h.steps.joinToString(" · "))
            }
        }
    }

    // по каждой привычке — только дни по плану и дни, когда её сделали как бонус
    val stats = habits.map { h ->
        h to averageLevel(week.filter { (d, m) -> h.plannedOn(d) || m[h.id] != null }.map { (_, m) -> m[h.id]?.level ?: 0 })
    }
    val avgs = stats.mapNotNull { it.second }
    Section("Мои привычки", if (avgs.isEmpty()) null else "неделя ${fmt(avgs.average().toFloat())} / 10") {
        stats.forEach { (h, avg) ->
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(h.icon, income = true, size = 32.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Row {
                        Text(h.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(todayMarks[h.id]?.level?.let { "сегодня $it" } ?: "", style = MaterialTheme.typography.labelMedium,
                            color = c.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { (avg ?: 0f) / 10f },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp).clip(CircleShape),
                        color = Accent, trackColor = c.surfaceVariant,
                    )
                }
            }
        }
        TextButton(onOpen, Modifier.padding(top = 4.dp)) { Text("Отметить привычки →", color = Accent) }
    }
}
