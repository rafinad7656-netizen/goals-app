package com.rafinad.goals

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalTest {
    @Test fun progress() {
        assertEquals(0f, Goal("a").progress)
        assertEquals(0.5f, Goal("a", listOf(Task("x", true), Task("y"))).progress)
    }
}
