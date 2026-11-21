package com.rafinad.goals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
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
    DAY("День", Icons.Outlined.WbSunny),
    CALENDAR("Календарь", Icons.Outlined.DateRange),
    GOALS("Цели", Icons.Outlined.Flag),
    MONEY("Финансы", Icons.Outlined.AccountBalanceWallet),
}

@Composable
fun App(store: Store) {
    var profile by remember { mutableStateOf(store.profile()) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    val p = profile
    if (p == null || editingProfile) {
        ProfileScreen(p, if (p == null) null else ({ editingProfile = false })) { new ->
            store.saveProfile(new); profile = new; editingProfile = false
        }
        return
    }

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
                Tab.DAY -> DayScreen(store, date, QUOTES[tip], nextTip, { editingProfile = true }) { date = LocalDate.now() }
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
            title = {
                Text(if (isBirthday(p.birthday, LocalDate.now())) "С днём рождения, ${p.name}!"
                     else "${greeting(LocalTime.now().hour)}, ${p.name}!")
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    nudgeKind(LocalTime.now().hour)?.let {
                        NudgeBlock(it, store.day(LocalDate.now()).goal)
                        Spacer(Modifier.height(16.dp))
                    }
                    if (p.reason.isNotBlank()) {
                        Text("Помните, зачем вы здесь: ${p.reason}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                    }
                    Text("СОВЕТ ДНЯ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
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
