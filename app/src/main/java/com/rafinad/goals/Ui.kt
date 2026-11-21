package com.rafinad.goals

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

val RU: Locale = Locale.forLanguageTag("ru")
val CardShape = RoundedCornerShape(20.dp)

// Вся палитра — три цвета. Остальные оттенки — их прозрачность.
val Paper = Color(0xFFFAFAF7)
val Ink = Color(0xFF1C1C1E)
val Accent = Color(0xFF2E9E6B)

/** Доход — акцентом, расход — основным цветом текста. */
val Income = Accent
val Expense: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface

private fun scheme(dark: Boolean): ColorScheme {
    val bg = if (dark) Ink else Paper
    val fg = if (dark) Paper else Ink
    fun tint(a: Float) = fg.copy(alpha = a).compositeOver(bg)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = fg, onPrimary = bg, primaryContainer = tint(0.08f), onPrimaryContainer = fg,
        secondary = Accent, onSecondary = Paper, secondaryContainer = Accent.copy(alpha = 0.18f).compositeOver(bg),
        onSecondaryContainer = fg, tertiary = Accent, onTertiary = Paper,
        background = bg, onBackground = fg, surface = tint(0.04f), onSurface = fg,
        surfaceVariant = tint(0.08f), onSurfaceVariant = tint(0.55f),
        surfaceContainerLowest = bg, surfaceContainerLow = tint(0.03f), surfaceContainer = tint(0.04f),
        surfaceContainerHigh = tint(0.06f), surfaceContainerHighest = tint(0.08f),
        outline = tint(0.3f), outlineVariant = tint(0.12f),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme(isSystemInDarkTheme()), content = content)

/** Монохромные иконки категорий. Ключ хранится в записи. */
val CATEGORY_ICONS: Map<String, ImageVector> = linkedMapOf(
    "work" to Icons.Outlined.Work, "laptop" to Icons.Outlined.Laptop, "home" to Icons.Outlined.Home,
    "bank" to Icons.Outlined.AccountBalance, "wallet" to Icons.Outlined.AccountBalanceWallet,
    "trend" to Icons.AutoMirrored.Outlined.TrendingUp,
    "family" to Icons.Outlined.People, "child" to Icons.Outlined.ChildCare, "heart" to Icons.Outlined.FavoriteBorder,
    "gift" to Icons.Outlined.CardGiftcard, "pets" to Icons.Outlined.Pets, "person" to Icons.Outlined.Person,
    "cart" to Icons.Outlined.ShoppingCart, "food" to Icons.Outlined.Restaurant, "cafe" to Icons.Outlined.LocalCafe,
    "clothes" to Icons.Outlined.LocalMall, "meds" to Icons.Outlined.LocalPharmacy, "health" to Icons.Outlined.LocalHospital,
    "car" to Icons.Outlined.DirectionsCar, "fuel" to Icons.Outlined.LocalGasStation, "bus" to Icons.Outlined.DirectionsBus,
    "flight" to Icons.Outlined.Flight, "phone" to Icons.Outlined.Smartphone, "bulb" to Icons.Outlined.Lightbulb,
    "school" to Icons.Outlined.School, "book" to Icons.AutoMirrored.Outlined.MenuBook, "sport" to Icons.Outlined.FitnessCenter,
    "game" to Icons.Outlined.VideogameAsset, "tools" to Icons.Outlined.Build, "receipt" to Icons.Outlined.Receipt,
)

// Записи из прошлых версий хранили эмодзи.
private val LEGACY_EMOJI = mapOf(
    "💼" to "work", "💻" to "laptop", "🏠" to "home", "🏦" to "bank", "💰" to "wallet", "📈" to "trend",
    "👨‍👩‍👧" to "family", "👧" to "child", "👦" to "child", "❤️" to "heart", "🎁" to "gift", "🐶" to "pets",
    "🛒" to "cart", "🍔" to "food", "☕" to "cafe", "👕" to "clothes", "💊" to "meds", "🏥" to "health",
    "🚗" to "car", "⛽" to "fuel", "🚌" to "bus", "✈️" to "flight", "📱" to "phone", "💡" to "bulb",
    "🎓" to "school", "📚" to "book", "⚽" to "sport", "🎮" to "game", "🔧" to "tools", "🧾" to "receipt",
)

fun categoryIcon(key: String): ImageVector? = CATEGORY_ICONS[key] ?: CATEGORY_ICONS[LEGACY_EMOJI[key]]

/** Круглая подложка с иконкой категории; без категории — знак +/−. */
@Composable
fun CategoryBadge(icon: String, income: Boolean, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center) {
        val v = categoryIcon(icon)
        if (v != null) Icon(v, null, Modifier.size(size * 0.5f), tint = if (income) Income else Expense)
        else Text(if (income) "+" else "−", color = if (income) Income else Expense)
    }
}

// ToneGenerator can fail on some devices — then the app just stays silent
private val tone: ToneGenerator? by lazy { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull() }

fun playDone() {
    tone?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
}

fun money(v: Long) = String.format(RU, "%,d ₸", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePick(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    displayed: LocalDate? = initial,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    onPick: (LocalDate) -> Unit,
) {
    fun LocalDate.millis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.millis(),
        initialDisplayedMonthMillis = displayed?.millis(),
        yearRange = yearRange,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("Готово") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } },
    ) { DatePicker(state) }
}

@Composable
fun ScreenTitle(text: String) =
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

@Composable
fun Section(title: String, trailing: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
fun PlainField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    actions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
) {
    val clear = Color.Transparent
    TextField(
        value, onChange, modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = singleLine, keyboardOptions = keyboard, keyboardActions = actions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = clear, unfocusedContainerColor = clear,
            focusedIndicatorColor = clear, unfocusedIndicatorColor = clear,
        ),
    )
}

@Composable
fun AddRow(hint: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun submit() {
        if (text.isNotBlank()) { onAdd(text.trim()); text = "" }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        PlainField(text, { text = it }, hint, Modifier.weight(1f),
            KeyboardOptions(imeAction = ImeAction.Done), KeyboardActions(onDone = { submit() }), singleLine = true)
        IconButton(onClick = { submit() }) { Icon(Icons.Default.Add, "Добавить") }
    }
}

@Composable
fun TaskList(tasks: List<Task>, hint: String, onChange: (List<Task>) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    tasks.forEachIndexed { i, task ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(task.done, { done ->
                if (done) playDone()
                onChange(tasks.replace(i, task.copy(done = done)))
            }, colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Paper))
            Text(task.title, Modifier.weight(1f),
                color = if (task.done) muted else Color.Unspecified,
                textDecoration = if (task.done) TextDecoration.LineThrough else null)
            IconButton(onClick = { onChange(tasks.without(i)) }) { Icon(Icons.Default.Close, "Удалить", tint = muted) }
        }
    }
    AddRow(hint) { onChange(tasks + Task(it)) }
}
