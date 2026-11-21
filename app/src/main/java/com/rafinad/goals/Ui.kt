package com.rafinad.goals

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

val RU: Locale = Locale.forLanguageTag("ru")
val CardShape = RoundedCornerShape(20.dp)
val Income = Color(0xFF2E7D5B)
val Expense = Color(0xFFC0392B)

private val Light = lightColorScheme(
    primary = Color(0xFF1C1C1E), onPrimary = Color.White,
    background = Color(0xFFF5F5F3), surface = Color.White, onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFECECEA), onSurfaceVariant = Color(0xFF8A8A8E),
    secondaryContainer = Color(0xFFE6E6E3),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFF2F2F2), onPrimary = Color(0xFF111111),
    background = Color(0xFF0F0F10), surface = Color(0xFF1A1A1C), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF26262A), onSurfaceVariant = Color(0xFF8E8E93),
    secondaryContainer = Color(0xFF2C2C30),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)

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
                Text(title.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium,
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
            })
            Text(task.title, Modifier.weight(1f),
                color = if (task.done) muted else Color.Unspecified,
                textDecoration = if (task.done) TextDecoration.LineThrough else null)
            IconButton(onClick = { onChange(tasks.without(i)) }) { Icon(Icons.Default.Close, "Удалить", tint = muted) }
        }
    }
    AddRow(hint) { onChange(tasks + Task(it)) }
}
