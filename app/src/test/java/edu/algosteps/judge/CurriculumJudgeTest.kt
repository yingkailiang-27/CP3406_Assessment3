package edu.algosteps.judge

import edu.algosteps.content.Curriculum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumJudgeTest {
    @Test fun eachReferencePassesAllCasesAndEachStarterNeedsAnEdit() {
        for (lesson in Curriculum.lessons) {
            val cases = lesson.samples + lesson.checks
            for (case in cases) {
                val actual = CodeJudge.run(lesson.reference, case.nums, case.target)
                assertEquals("${lesson.id}: $case", case.expected, actual)
            }
            assertTrue("${lesson.id}: starter already passes all tests", cases.any {
                CodeJudge.run(lesson.starter, it.nums, it.target) != it.expected
            })
        }
    }
}
