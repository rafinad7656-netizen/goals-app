package com.rafinad.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FieldShape = RoundedCornerShape(16.dp)
private val BIRTHDAY_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", RU)
private val REASONS = listOf(
    "Хочу изменить жизнь", "Навести порядок в делах", "Контролировать деньги",
    "Достигать целей", "Стать продуктивнее",
)

/** Создание профиля при первом запуске; с onCancel — редактирование. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(initial: Profile?, onCancel: (() -> Unit)?, onSave: (Profile) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var birthday by rememberSaveable { mutableStateOf(initial?.birthday ?: "") }
    var occupation by rememberSaveable { mutableStateOf(initial?.occupation ?: "") }
    var reason by rememberSaveable { mutableStateOf(initial?.reason ?: "") }
    var picking by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = c.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (onCancel != null) TextButton(onCancel) { Text("← Назад") }
            Text(if (initial == null) "Давайте познакомимся" else "Профиль",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Чтобы приложение обращалось к вам лично. Данные остаются только на этом телефоне, интернет не нужен.",
                color = c.onSurfaceVariant)

            Field("Как вас зовут?", name, { name = it.take(40) }, "Имя")

            Column {
                Label("Дата рождения")
                Surface(onClick = { picking = true }, modifier = Modifier.fillMaxWidth(), shape = FieldShape, color = c.surface) {
                    Text(
                        if (birthday.isBlank()) "Выбрать дату" else LocalDate.parse(birthday).format(BIRTHDAY_FORMAT),
                        Modifier.padding(16.dp),
                        color = if (birthday.isBlank()) c.onSurfaceVariant else c.onSurface,
                    )
                }
            }

            Field("Профессия или досуг", occupation, { occupation = it.take(60) }, "Например: дизайнер, спорт, рисование")

            Column {
                Field("Зачем вам это приложение?", reason, { reason = it.take(200) }, "Например: хочу изменить жизнь", singleLine = false)
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    REASONS.forEach { r -> SuggestionChip(onClick = { reason = r }, label = { Text(r) }) }
                }
            }

            Button(
                { onSave(Profile(name.trim(), birthday, occupation.trim(), reason.trim())) },
                Modifier.fillMaxWidth().height(52.dp),
                enabled = name.isNotBlank(),
            ) { Text(if (initial == null) "Начать" else "Сохранить") }
            if (name.isBlank()) Text("Чтобы продолжить, введите имя", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }

    if (picking) {
        val millis = birthday.takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = millis,
            initialDisplayedMonthMillis = millis ?: LocalDate.of(2000, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1920..LocalDate.now().year,
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { birthday = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    picking = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton({ picking = false }) { Text("Отмена") } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun Label(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String, singleLine: Boolean = true) {
    Column {
        Label(label)
        OutlinedTextField(
            value, onChange, Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
            shape = FieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}
