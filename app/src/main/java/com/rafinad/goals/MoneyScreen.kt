package com.rafinad.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val SHORT = DateTimeFormatter.ofPattern("d MMM yyyy", RU)

@Composable
fun MoneyScreen(store: Store) {
    var items by remember { mutableStateOf(store.money()) }
    fun update(new: List<Money>) { items = new; store.saveMoney(new) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val c = MaterialTheme.colorScheme

    val month = items.filter { YearMonth.from(LocalDate.parse(it.date)) == YearMonth.now() }
    fun add(income: Boolean) {
        val a = amount.toLongOrNull()?.takeIf { it > 0 } ?: return
        update(listOf(Money(a, income, note.trim(), LocalDate.now().toString())) + items)
        amount = ""; note = ""
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Финансы") }
        item {
            Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.primary, contentColor = c.onPrimary) {
                Column(Modifier.padding(20.dp)) {
                    Text("Баланс", color = c.onPrimary.copy(alpha = 0.6f))
                    Text(money(balance(items)), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
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
                PlainField(amount, { v -> amount = v.filter { it.isDigit() }.take(12) }, "Сумма, ₸",
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                PlainField(note, { note = it }, "Комментарий", singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ add(true) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(Income, Color.White)) { Text("+ Доход") }
                    Button({ add(false) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(Expense, Color.White)) { Text("− Расход") }
                }
            }
        }
        if (items.isEmpty()) item {
            Text("Записей пока нет", Modifier.padding(8.dp), color = c.onSurfaceVariant)
        }
        itemsIndexed(items) { i, m ->
            Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.surface) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.note.ifBlank { if (m.income) "Доход" else "Расход" })
                        Text(LocalDate.parse(m.date).format(SHORT), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                    Text((if (m.income) "+" else "−") + money(m.amount), color = if (m.income) Income else Expense,
                        fontWeight = FontWeight.Medium)
                    IconButton({ update(items.without(i)) }) { Icon(Icons.Default.Close, "Удалить", tint = c.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Long, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
        Text(money(value), style = MaterialTheme.typography.titleMedium)
    }
}
