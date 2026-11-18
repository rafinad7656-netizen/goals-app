package com.rafinad.goals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(this)
        setContent { AppTheme { App(store) } }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    DAY("День", Icons.Default.Home),
    CALENDAR("Календарь", Icons.Default.DateRange),
    GOALS("Цели", Icons.Default.Star),
    MONEY("Финансы", Icons.Default.ShoppingCart),
}

@Composable
fun App(store: Store) {
    var tab by rememberSaveable { mutableStateOf(Tab.DAY) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var tip by rememberSaveable { mutableIntStateOf(Random.nextInt(QUOTES.size)) }
    var showTip by rememberSaveable { mutableStateOf(true) }
    val nextTip = { tip = (tip + 1 + Random.nextInt(QUOTES.size - 1)) % QUOTES.size }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(tab == t, { tab = t }, icon = { Icon(t.icon, null) }, label = { Text(t.label) })
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                Tab.DAY -> DayScreen(store, date, QUOTES[tip], nextTip) { date = LocalDate.now() }
                Tab.CALENDAR -> CalendarScreen(store, date) { date = it; tab = Tab.DAY }
                Tab.GOALS -> GoalsScreen(store)
                Tab.MONEY -> MoneyScreen(store)
            }
        }
    }

    if (showTip) {
        val q = QUOTES[tip]
        AlertDialog(
            onDismissRequest = { showTip = false },
            confirmButton = { TextButton({ showTip = false }) { Text("Спасибо") } },
            dismissButton = { TextButton(nextTip) { Text("Ещё совет") } },
            title = { Text("Совет дня") },
            text = {
                Column {
                    Text("«${q.text}»", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("— ${q.author}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = CardShape,
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}
