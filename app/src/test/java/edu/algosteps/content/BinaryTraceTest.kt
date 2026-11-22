package edu.algosteps.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BinaryTraceTest {
    @Test fun diagramNeverDiscardsAMatchingIndexAndEndsWithTheDeclaredResult() {
        val lesson = Curriculum.byId("binary-search")
        for (case in lesson.samples + lesson.checks) {
            val trace = lesson.trace(case)
            assertEquals(case.nums.indices, trace.first().searchRange)
            val comparisons = trace.filter { it.active.isNotEmpty() }
            for (step in comparisons) {
                val remaining = requireNotNull(step.searchRange)
                assertTrue(step.active.single() in remaining)
                if (case.expected >= 0) assertTrue(case.expected in remaining)
            }
            for ((before, after) in trace.zipWithNext()) {
                val previous = requireNotNull(before.searchRange)
                val next = requireNotNull(after.searchRange)
                assertTrue(next.all { it in previous })
                assertTrue(before.active.single() !in next)
                assertTrue(next.count() <= previous.count() / 2)
            }
            if (case.expected < 0) {
                assertTrue(requireNotNull(trace.last().searchRange).isEmpty())
                assertTrue(trace.last().active.isEmpty())
            } else {
                assertEquals(setOf(case.expected), trace.last().active)
            }
        }
    }
}
