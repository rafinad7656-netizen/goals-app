package com.rafinad.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun GoalsScreen(store: Store) {
    var goals by remember { mutableStateOf(store.goals()) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    fun update(new: List<Goal>) { goals = new; store.saveGoals(new) }
    val c = MaterialTheme.colorScheme

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Цели") }
        item { Section("Новая цель") { AddRow("Чего хочу достичь") { update(goals + Goal(it)) } } }
        itemsIndexed(goals) { i, goal ->
            Surface(
                onClick = { expanded = if (expanded == i) null else i },
                modifier = Modifier.fillMaxWidth(), shape = CardShape, color = c.surface,
            ) {
                Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text("${goal.tasks.count { it.done }}/${goal.tasks.size}", color = c.onSurfaceVariant)
                        IconButton({ update(goals.without(i)); expanded = null }) {
                            Icon(Icons.Default.Close, "Удалить цель", tint = c.onSurfaceVariant)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { goal.progress },
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp).height(4.dp).clip(CircleShape),
                        color = c.primary, trackColor = c.surfaceVariant,
                    )
                    if (expanded == i) {
                        Spacer(Modifier.height(8.dp))
                        TaskList(goal.tasks, "Добавить шаг") { update(goals.replace(i, goal.copy(tasks = it))) }
                    }
                }
            }
        }
    }
}
