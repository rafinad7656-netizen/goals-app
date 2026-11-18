package com.rafinad.goals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = GoalStore(this)
        setContent { MaterialTheme { GoalsScreen(store) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(store: GoalStore) {
    var goals by remember { mutableStateOf(store.load()) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    fun update(new: List<Goal>) { goals = new; store.save(new) }

    Scaffold(topBar = { TopAppBar(title = { Text("Цели и задачи") }) }) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            AddRow("Новая цель") { update(goals + Goal(it)) }
            LazyColumn {
                itemsIndexed(goals) { i, goal ->
                    GoalCard(
                        goal = goal,
                        expanded = expanded == i,
                        onClick = { expanded = if (expanded == i) null else i },
                        onChange = { g -> update(goals.toMutableList().also { it[i] = g }) },
                        onDelete = { update(goals.filterIndexed { j, _ -> j != i }); expanded = null },
                    )
                }
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal, expanded: Boolean, onClick: () -> Unit, onChange: (Goal) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("${goal.tasks.count { it.done }}/${goal.tasks.size}")
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Удалить цель") }
            }
            LinearProgressIndicator(progress = { goal.progress }, modifier = Modifier.fillMaxWidth())
            if (expanded) {
                goal.tasks.forEachIndexed { j, task ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(task.done, { done ->
                            onChange(goal.copy(tasks = goal.tasks.toMutableList().also { it[j] = task.copy(done = done) }))
                        })
                        Text(task.title, Modifier.weight(1f))
                        IconButton(onClick = { onChange(goal.copy(tasks = goal.tasks.filterIndexed { k, _ -> k != j })) }) {
                            Icon(Icons.Default.Delete, "Удалить задачу")
                        }
                    }
                }
                AddRow("Новая задача") { onChange(goal.copy(tasks = goal.tasks + Task(it))) }
            }
        }
    }
}

@Composable
fun AddRow(hint: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(text, { text = it }, Modifier.weight(1f), label = { Text(hint) }, singleLine = true)
        IconButton(onClick = { if (text.isNotBlank()) { onAdd(text.trim()); text = "" } }) {
            Icon(Icons.Default.Add, "Добавить")
        }
    }
}
