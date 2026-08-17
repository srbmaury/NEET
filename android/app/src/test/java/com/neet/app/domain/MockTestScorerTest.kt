package com.neet.app.domain

import com.neet.app.data.model.Subject
import com.neet.app.data.model.TestSection
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockTestScorerTest {

    @Test
    fun `section B scores only the first ten attempted questions`() {
        val answers = (1..11).map { index ->
            MockTestQuestionAnswer(
                questionId = "q$index",
                subject = Subject.PHYSICS,
                section = TestSection.B,
                sectionIndex = index,
                correctOptionKey = "A",
                selectedOptionKey = if (index == 11) "B" else "A",
                answeredAt = index.toLong(),
            )
        }

        val score = MockTestScorer.score(answers)
        val physics = score.subjectScores.single { it.subject == Subject.PHYSICS }

        assertEquals(40, physics.marks)
        assertEquals(10, physics.correctCount)
        assertEquals(0, physics.wrongCount)
        assertTrue(score.scoredQuestions.single { it.questionId == "q10" }.isScored)
        assertFalse(score.scoredQuestions.single { it.questionId == "q11" }.isScored)
    }

    @Test
    fun `section A applies NEET positive and negative marks`() {
        val answers = listOf(
            answer("correct", "A"),
            answer("wrong", "B"),
            answer("blank", null),
        )

        val score = MockTestScorer.score(answers)
        val physics = score.subjectScores.single { it.subject == Subject.PHYSICS }

        assertEquals(3, physics.marks)
        assertEquals(1, physics.correctCount)
        assertEquals(1, physics.wrongCount)
        assertEquals(1, physics.unansweredCount)
    }

    private fun answer(id: String, selectedOptionKey: String?) = MockTestQuestionAnswer(
        questionId = id,
        subject = Subject.PHYSICS,
        section = TestSection.A,
        sectionIndex = 1,
        correctOptionKey = "A",
        selectedOptionKey = selectedOptionKey,
        answeredAt = 1L,
    )
}
