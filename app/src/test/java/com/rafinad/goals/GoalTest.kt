package com.rafinad.goals

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalTest {
    @Test fun progress() {
        assertEquals(0f, Goal("a").progress)
        assertEquals(0.5f, Goal("a", listOf(Task("x", true), Task("y"))).progress)
    }

    @Test fun balanceCountsIncomeMinusExpense() {
        assertEquals(0L, balance(emptyList()))
        assertEquals(700L, balance(listOf(Money(1000, true, "", "2026-11-19"), Money(300, false, "", "2026-11-19"))))
    }

    @Test fun totalsGroupByCategoryAndSortDesc() {
        val items = listOf(
            Money(100, false, "", "2026-11-19", "Семья", "👧"),
            Money(500, false, "", "2026-11-19", "Работа", "💻"),
            Money(200, false, "", "2026-11-19", "Семья", "👧"),
            Money(9000, true, "", "2026-11-19", "Зарплата", "💼"),
        )
        assertEquals(
            listOf(CategoryTotal("💻", "Работа", 500), CategoryTotal("👧", "Семья", 300)),
            totalsByCategory(items, income = false),
        )
    }

    @Test fun greetingByHour() {
        assertEquals("Доброе утро", greeting(8))
        assertEquals("Добрый день", greeting(13))
        assertEquals("Добрый вечер", greeting(20))
        assertEquals("Доброй ночи", greeting(2))
    }

    @Test fun birthdayMatchesDayAndMonthOnly() {
        val today = java.time.LocalDate.of(2026, 11, 20)
        assertEquals(true, isBirthday("1995-11-20", today))
        assertEquals(false, isBirthday("1995-11-21", today))
        assertEquals(false, isBirthday("", today))
    }

    @Test fun nudgeByHour() {
        assertEquals(null, nudgeKind(21))
        assertEquals(NudgeKind.EVENING, nudgeKind(22))
        assertEquals(NudgeKind.EVENING, nudgeKind(23))
        assertEquals(NudgeKind.NIGHT, nudgeKind(0))
        assertEquals(NudgeKind.NIGHT, nudgeKind(4))
        assertEquals(NudgeKind.MORNING, nudgeKind(5))
        assertEquals(null, nudgeKind(11))
    }

    @Test fun expensesSummedPerDay() {
        val items = listOf(
            Money(100, false, "", "2026-11-20"),
            Money(250, false, "", "2026-11-20"),
            Money(5000, true, "", "2026-11-20"),
            Money(40, false, "", "2026-11-21"),
        )
        assertEquals(mapOf("2026-11-20" to 350L, "2026-11-21" to 40L), expensesByDay(items))
    }

    @Test fun compactAmounts() {
        assertEquals("450", compact(450))
        assertEquals("12к", compact(12_500))
        assertEquals("1,2м", compact(1_250_000))
    }

    @Test fun listHelpers() {
        assertEquals(listOf(1, 9, 3), listOf(1, 2, 3).replace(1, 9))
        assertEquals(listOf(1, 3), listOf(1, 2, 3).without(1))
    }
}
