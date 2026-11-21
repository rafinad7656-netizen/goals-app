package com.rafinad.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val SHORT = DateTimeFormatter.ofPattern("d MMM", RU)
private val DAY_HEADER = DateTimeFormatter.ofPattern("d MMMM, EEEE", RU)

/** Оттенки акцента для диаграммы — палитра остаётся трёхцветной. */
private fun shade(i: Int) = Accent.copy(alpha = (1f - i * 0.16f).coerceAtLeast(0.15f))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoneyScreen(store: Store) {
    var entries by remember { mutableStateOf(store.money()) }
    var cats by remember { mutableStateOf(store.categories()) }
    fun update(new: List<Money>) { entries = new; store.saveMoney(new) }
    fun updateCats(new: List<Category>) { cats = new; store.saveCategories(new) }

    var start by remember { mutableLongStateOf(store.startBalance()) }
    var editingBalance by remember { mutableStateOf(false) }
    var chartIncome by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    // форма новой записи
    var adding by remember { mutableStateOf(false) }
    var income by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf(LocalDate.now()) }
    var pickingDate by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }

    val c = MaterialTheme.colorScheme
    val total = start + balance(entries)
    val month = entries.filter { YearMonth.from(LocalDate.parse(it.date)) == YearMonth.now() }
    val totals = totalsByCategory(month, chartIncome)
    val byDay = entries.groupBy { it.date }.toSortedMap(compareByDescending { it })

    fun add() {
        val a = amount.toLongOrNull()?.takeIf { it > 0 } ?: return
        val cat = selected
        update(listOf(Money(a, income, note.trim(), entryDate.toString(), cat?.name ?: "", cat?.icon ?: "")) + entries)
        amount = ""; note = ""; adding = false
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenTitle("Финансы") }
            item {
                Surface({ editingBalance = true }, Modifier.fillMaxWidth(), shape = CardShape, color = c.primary, contentColor = c.onPrimary) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Баланс", Modifier.weight(1f), color = c.onPrimary.copy(alpha = 0.6f))
                            Icon(Icons.Outlined.Edit, "Указать баланс", Modifier.size(18.dp), tint = c.onPrimary.copy(alpha = 0.6f))
                        }
                        Text(money(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                        if (start == 0L && entries.isEmpty()) Text("Нажмите, чтобы указать, сколько у вас денег сейчас",
                            style = MaterialTheme.typography.bodySmall, color = c.onPrimary.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Stat("Доходы за месяц", "+" + money(month.filter { it.income }.sumOf { it.amount }), Modifier.weight(1f))
                            Stat("Расходы за месяц", "−" + money(month.filter { !it.income }.sumOf { it.amount }), Modifier.weight(1f))
                        }
                    }
                }
            }
            if (month.isNotEmpty()) item {
                Section("Этот месяц") {
                    Spacer(Modifier.height(12.dp))
                    Segments(chartIncome) { chartIncome = it }
                    if (totals.isEmpty()) {
                        Text(if (chartIncome) "Доходов в этом месяце нет" else "Расходов в этом месяце нет",
                            color = c.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        val sum = totals.sumOf { it.total }
                        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Donut(totals, money(sum))
                        }
                        totals.forEachIndexed { i, t ->
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(shade(i)))
                                Text(t.name.ifBlank { "Без категории" }, Modifier.weight(1f).padding(start = 10.dp))
                                Text("${t.total * 100 / sum}%", color = c.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                                Text(money(t.total), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            if (entries.isEmpty()) item {
                Text("Записей пока нет. Нажмите «Добавить» внизу.", Modifier.padding(8.dp), color = c.onSurfaceVariant)
            }
            byDay.forEach { (date, list) ->
                item(key = "h$date") { DayHeader(LocalDate.parse(date), list.filter { !it.income }.sumOf { it.amount }) }
                items(list) { m -> EntryRow(m) { update(entries - m) } }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Accent, contentColor = Paper,
            icon = { Icon(Icons.Default.Add, null) }, text = { Text("Добавить") },
        )
    }

    if (adding) ModalBottomSheet(
        onDismissRequest = { adding = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Новая запись", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Segments(income) { income = it; selected = null }
            PlainField(amount, { v -> amount = v.filter { it.isDigit() }.take(12) }, "Сумма, ₸",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Text("Категория", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.filter { it.income == income }.forEach { cat ->
                    Chip(cat.name, selected == cat, categoryIcon(cat.icon),
                        onClick = { selected = if (selected == cat) null else cat },
                        onLongClick = { deleting = cat })
                }
                Chip("Своя", false, Icons.Default.Add, { creating = true })
            }
            Text("Удерживайте категорию, чтобы удалить", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            PlainField(note, { note = it }, "Комментарий (необязательно)", singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Дата", color = c.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                Chip(if (entryDate == LocalDate.now()) "Сегодня" else entryDate.format(SHORT), false, Icons.Outlined.DateRange,
                    { pickingDate = true })
            }
            Button(
                { add() }, Modifier.fillMaxWidth().height(52.dp),
                enabled = (amount.toLongOrNull() ?: 0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (income) Accent else c.onSurface,
                    contentColor = if (income) Paper else c.background,
                ),
            ) { Text(if (income) "Добавить доход" else "Добавить расход") }
        }
    }

    if (creating) NewCategoryDialog(income, { creating = false }) { cat ->
        val existing = cats.find { it.income == cat.income && it.name.equals(cat.name, ignoreCase = true) }
        if (existing == null) updateCats(cats + cat)
        selected = existing ?: cat
        creating = false
    }

    if (pickingDate) DatePick(entryDate, { pickingDate = false }) { entryDate = it }

    if (editingBalance) {
        var v by remember { mutableStateOf(if (total > 0) "$total" else "") }
        AlertDialog(
            onDismissRequest = { editingBalance = false },
            confirmButton = {
                TextButton({
                    v.toLongOrNull()?.let { start = it - balance(entries); store.saveStartBalance(start) }
                    editingBalance = false
                }, enabled = v.toLongOrNull() != null) { Text("Сохранить") }
            },
            dismissButton = { TextButton({ editingBalance = false }) { Text("Отмена") } },
            title = { Text("Сколько у вас сейчас?") },
            text = {
                Column {
                    Text("Укажите текущую сумму. Дальше баланс будет меняться от доходов и расходов.",
                        color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(v, { s -> v = s.filter { it.isDigit() }.take(12) }, singleLine = true,
                        suffix = { Text("₸") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            shape = CardShape,
        )
    }

    deleting?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            confirmButton = {
                TextButton({
                    updateCats(cats - cat)
                    if (selected == cat) selected = null
                    deleting = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton({ deleting = null }) { Text("Отмена") } },
            title = { Text("Удалить категорию «${cat.name}»?") },
            text = { Text("Уже добавленные записи останутся.") },
            shape = CardShape,
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, spent: Long) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> date.format(DAY_HEADER)
    }
    Row(Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (spent > 0) Text("−" + money(spent), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EntryRow(m: Money, onDelete: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.surface) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(m.icon, m.income)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(m.category.ifBlank { m.note.ifBlank { if (m.income) "Доход" else "Расход" } })
                if (m.category.isNotBlank() && m.note.isNotBlank())
                    Text(m.note, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            Text((if (m.income) "+" else "−") + money(m.amount), color = if (m.income) Income else Expense,
                fontWeight = FontWeight.Medium)
            IconButton(onDelete) { Icon(Icons.Default.Close, "Удалить запись", tint = c.onSurfaceVariant) }
        }
    }
}

/** Переключатель Расход / Доход. */
@Composable
private fun Segments(income: Boolean, onChange: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(c.surfaceVariant).padding(4.dp)) {
        listOf(false to "Расход", true to "Доход").forEach { (value, label) ->
            val on = income == value
            Box(
                Modifier.weight(1f).clip(CircleShape)
                    .background(if (!on) Color.Transparent else if (value) Accent else c.onSurface)
                    .clickable { onChange(value) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontWeight = FontWeight.Medium,
                    color = if (!on) c.onSurfaceVariant else if (value) Paper else c.background)
            }
        }
    }
}

@Composable
private fun Donut(totals: List<CategoryTotal>, center: String) {
    val sum = totals.sumOf { it.total }.toFloat()
    Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            var angle = -90f
            totals.forEachIndexed { i, t ->
                val sweep = 360f * t.total / sum
                drawArc(shade(i), angle, sweep, useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke))
                angle += sweep
            }
        }
        Text(center, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Chip(text: String, selected: Boolean, icon: ImageVector?, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    Surface(
        Modifier.clip(CircleShape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = CircleShape,
        color = if (selected) c.onSurface else c.surfaceVariant,
        contentColor = if (selected) c.background else c.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, Modifier.padding(end = 6.dp).size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun NewCategoryDialog(income: Boolean, onDismiss: () -> Unit, onCreate: (Category) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(CATEGORY_ICONS.keys.first()) }
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({ onCreate(Category(name.trim(), icon, income)) }, enabled = name.isNotBlank()) { Text("Создать") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } },
        title = { Text(if (income) "Категория дохода" else "Категория расхода") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it.take(24) }, label = { Text("Название") }, singleLine = true)
                Text("Иконка", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                CATEGORY_ICONS.entries.chunked(6).forEach { row ->
                    Row {
                        row.forEach { (key, vector) ->
                            val on = key == icon
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (on) Accent else Color.Transparent)
                                    .clickable { icon = key },
                                contentAlignment = Alignment.Center,
                            ) { Icon(vector, null, Modifier.size(22.dp), tint = if (on) Paper else c.onSurface) }
                        }
                    }
                }
            }
        },
        shape = CardShape,
    )
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
