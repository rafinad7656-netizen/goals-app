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

    @Test fun listHelpers() {
        assertEquals(listOf(1, 9, 3), listOf(1, 2, 3).replace(1, 9))
        assertEquals(listOf(1, 3), listOf(1, 2, 3).without(1))
    }
}
