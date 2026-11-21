package com.rafinad.goals

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class NudgeKind { EVENING, NIGHT, MORNING }

/** 22:00–23:59 — пора спать, 00:00–04:59 — убеждаем лечь, 05:00–10:59 — начни с главного. */
fun nudgeKind(hour: Int): NudgeKind? = when (hour) {
    22, 23 -> NudgeKind.EVENING
    in 0..4 -> NudgeKind.NIGHT
    in 5..10 -> NudgeKind.MORNING
    else -> null
}

data class Fact(val text: String, val source: String)

// Только проверяемые данные из открытых научных источников.
val SLEEP_FACTS = listOf(
    Fact("Взрослым нужно не меньше 7 часов сна в сутки. Меньше — выше риск лишнего веса, диабета, болезней сердца и депрессии.",
        "Американская академия медицины сна, 2015"),
    Fact("После 17–19 часов без сна внимание и реакция ухудшаются так же, как при 0,05‰ алкоголя в крови.",
        "Williamson & Feyer, Occupational and Environmental Medicine, 2000"),
    Fact("Две недели сна по 6 часов ухудшают внимание почти как сутки без сна — и человек сам этого не замечает.",
        "Van Dongen et al., Sleep, 2003"),
    Fact("Люди, спящие меньше 6 часов, в 4 раза чаще простужаются после контакта с вирусом, чем спящие больше 7.",
        "Prather et al., Sleep, 2015"),
    Fact("Экран перед сном подавляет мелатонин: засыпаешь позже, а утром чувствуешь себя менее бодрым.",
        "Chang et al., PNAS, 2015"),
    Fact("Во сне мозг закрепляет то, что вы узнали за день. Недосып буквально стирает часть прогресса.",
        "Walker & Stickgold, Annual Review of Psychology, 2006"),
)

val MORNING_TIPS = listOf(
    Fact("Сначала — главное. Срочные мелочи всегда найдутся, важное — само не сделается.", "Стивен Кови, «7 навыков высокоэффективных людей»"),
    Fact("Съешьте лягушку: начните день с самой важной и трудной задачи, остальное пойдёт легче.", "Брайан Трейси, «Съешьте лягушку!»"),
    Fact("20% дел дают 80% результата. Найдите эти 20% и сделайте их первыми.", "Принцип Парето"),
    Fact("Не открывайте соцсети и почту, пока не сделали первое важное дело.", "Кэл Ньюпорт, «В работу с головой»"),
)

@Composable
fun NudgeBlock(kind: NudgeKind, mainGoal: String) {
    val fact = remember(kind) { (if (kind == NudgeKind.MORNING) MORNING_TIPS else SLEEP_FACTS).random() }
    val (title, lead) = when (kind) {
        NudgeKind.EVENING -> "Пора готовиться ко сну" to
            "Отложите телефон — завтрашние дела подождут. Лягте до полуночи, и утром будет больше сил."
        NudgeKind.NIGHT -> "Уже за полночь" to
            "Сон сейчас важнее любых дел. Вот почему:"
        NudgeKind.MORNING -> "Начните с главного" to
            if (mainGoal.isNotBlank()) "Главная цель дня: «$mainGoal». Сделайте её первой, пока голова свежая."
            else "Запишите главную цель дня и сделайте её первой, пока голова свежая."
    }
    val c = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = c.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(lead)
            Text(fact.text, style = MaterialTheme.typography.bodyMedium)
            Text(fact.source, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }
}
