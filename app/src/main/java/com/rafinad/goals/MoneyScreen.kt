package com.rafinad.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val SHORT = DateTimeFormatter.ofPattern("d MMM yyyy", RU)

private val EMOJIS = listOf(
    "💼", "💻", "🏠", "🏦", "💰", "📈",
    "👨‍👩‍👧", "👧", "👦", "❤️", "🎁", "🐶",
    "🛒", "🍔", "☕", "👕", "💊", "🏥",
    "🚗", "⛽", "🚌", "✈️", "📱", "💡",
    "🎓", "📚", "⚽", "🎮", "🔧", "🧾",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyScreen(store: Store) {
    var entries by remember { mutableStateOf(store.money()) }
    var cats by remember { mutableStateOf(store.categories()) }
    fun update(new: List<Money>) { entries = new; store.saveMoney(new) }
    fun updateCats(new: List<Category>) { cats = new; store.saveCategories(new) }

    var income by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    var start by remember { mutableLongStateOf(store.startBalance()) }
    var editingBalance by remember { mutableStateOf(false) }
    var entryDate by remember { mutableStateOf(LocalDate.now()) }
    var pickingDate by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme

    val total = start + balance(entries)
    val month = entries.filter { YearMonth.from(LocalDate.parse(it.date)) == YearMonth.now() }
    val totals = totalsByCategory(month, income)
    fun add() {
        val a = amount.toLongOrNull()?.takeIf { it > 0 } ?: return
        val cat = selected
        update(listOf(Money(a, income, note.trim(), entryDate.toString(), cat?.name ?: "", cat?.emoji ?: "")) + entries)
        amount = ""; note = ""
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Финансы") }
        item {
            Surface({ editingBalance = true }, Modifier.fillMaxWidth(), shape = CardShape, color = c.primary, contentColor = c.onPrimary) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Баланс", Modifier.weight(1f), color = c.onPrimary.copy(alpha = 0.6f))
                        Icon(Icons.Default.Edit, "Указать баланс", Modifier.size(18.dp), tint = c.onPrimary.copy(alpha = 0.6f))
                    }
                    Text(money(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    if (start == 0L && entries.isEmpty()) Text("Нажмите, чтобы указать, сколько у вас денег сейчас",
                        style = MaterialTheme.typography.bodySmall, color = c.onPrimary.copy(alpha = 0.6f))
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Stat("Доходы за месяц", month.filter { it.income }.sumOf { it.amount }, Modifier.weight(1f))
                        Stat("Расходы за месяц", month.filter { !it.income }.sumOf { it.amount }, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Section("Новая запись") {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(CircleShape).background(c.surfaceVariant).padding(4.dp)) {
                    Segment("− Расход", !income, Expense, Modifier.weight(1f)) { income = false; selected = null }
                    Segment("+ Доход", income, Income, Modifier.weight(1f)) { income = true; selected = null }
                }
                Text(if (income) "Категории доходов" else "Категории расходов",
                    style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.filter { it.income == income }.forEach { cat ->
                        Chip("${cat.emoji} ${cat.name}", selected == cat,
                            onClick = { selected = if (selected == cat) null else cat },
                            onLongClick = { deleting = cat })
                    }
                    Chip("+ Своя", false, { creating = true })
                }
                Text("Удерживайте категорию, чтобы удалить", style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                PlainField(amount, { v -> amount = v.filter { it.isDigit() }.take(12) }, "Сумма, ₸",
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                PlainField(note, { note = it }, "Комментарий", singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Дата:", color = c.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                    Chip(if (entryDate == LocalDate.now()) "Сегодня" else entryDate.format(SHORT), true, { pickingDate = true })
                    if (entryDate != LocalDate.now()) TextButton({ entryDate = LocalDate.now() }) { Text("Сегодня") }
                }
                Button({ add() }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(if (income) Income else Expense, Color.White)) {
                    Text(if (income) "+ Добавить доход" else "− Добавить расход")
                }
            }
        }
        if (totals.isNotEmpty()) item {
            Section(if (income) "Доходы за месяц" else "Расходы за месяц") {
                val sum = totals.sumOf { it.total }
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Donut(totals, money(sum))
                }
                totals.forEachIndexed { i, t ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(PALETTE[i % PALETTE.size]))
                        Text("${t.emoji.ifBlank { "•" }} ${t.name.ifBlank { "Без категории" }}",
                            Modifier.weight(1f).padding(start = 8.dp))
                        Text("${t.total * 100 / sum}%", color = c.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                        Text(money(t.total), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (entries.isEmpty()) item {
            Text("Записей пока нет", Modifier.padding(8.dp), color = c.onSurfaceVariant)
        }
        items(entries.sortedByDescending { it.date }) { m ->
            Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.surface) {
                Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(c.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(m.emoji.ifBlank { if (m.income) "+" else "−" }, fontSize = 20.sp)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(m.category.ifBlank { m.note.ifBlank { if (m.income) "Доход" else "Расход" } })
                        val sub = listOfNotNull(m.note.takeIf { m.category.isNotBlank() && it.isNotBlank() },
                            LocalDate.parse(m.date).format(SHORT))
                        Text(sub.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                    Text((if (m.income) "+" else "−") + money(m.amount), color = if (m.income) Income else Expense,
                        fontWeight = FontWeight.Medium)
                    IconButton({ update(entries - m) }) { Icon(Icons.Default.Close, "Удалить", tint = c.onSurfaceVariant) }
                }
            }
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
            shape = CardShape, containerColor = c.surface,
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
                }) { Text("Удалить", color = Expense) }
            },
            dismissButton = { TextButton({ deleting = null }) { Text("Отмена") } },
            title = { Text("Удалить «${cat.emoji} ${cat.name}»?") },
            text = { Text("Уже добавленные записи останутся.") },
            shape = CardShape, containerColor = c.surface,
        )
    }
}

private val PALETTE = listOf(
    Color(0xFF5B8DEF), Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFF81C784),
    Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFF06292), Color(0xFF90A4AE),
)

@Composable
private fun Donut(totals: List<CategoryTotal>, center: String) {
    val sum = totals.sumOf { it.total }.toFloat()
    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            var angle = -90f
            totals.forEachIndexed { i, t ->
                val sweep = 360f * t.total / sum
                drawArc(PALETTE[i % PALETTE.size], angle, sweep, useCenter = false,
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
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val c = MaterialTheme.colorScheme
    Surface(
        Modifier.clip(CircleShape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = CircleShape,
        color = if (selected) c.primary else c.surfaceVariant,
        contentColor = if (selected) c.onPrimary else c.onSurface,
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Segment(text: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(CircleShape).background(if (selected) color else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NewCategoryDialog(income: Boolean, onDismiss: () -> Unit, onCreate: (Category) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(EMOJIS.first()) }
    val c = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({ if (name.isNotBlank()) onCreate(Category(name.trim(), emoji, income)) }, enabled = name.isNotBlank()) {
                Text("Создать")
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } },
        title = { Text(if (income) "Категория дохода" else "Категория расхода") },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(24) }, label = { Text("Название") }, singleLine = true)
                Text("Иконка", style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                EMOJIS.chunked(6).forEach { row ->
                    Row {
                        row.forEach { e ->
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (e == emoji) c.surfaceVariant else Color.Transparent)
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 22.sp) }
                        }
                    }
                }
            }
        },
        shape = CardShape, containerColor = c.surface,
    )
}

@Composable
private fun Stat(label: String, value: Long, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
        Text(money(value), style = MaterialTheme.typography.titleMedium)
    }
}
