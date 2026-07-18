package com.neet.app.domain

import com.neet.app.data.model.Subject
import com.neet.app.data.model.TestSection
import kotlinx.serialization.Serializable

data class MockTestQuestionAnswer(
    val questionId: String,
    val subject: Subject,
    val section: TestSection,
    val sectionIndex: Int,
    val correctOptionKey: String,
    val selectedOptionKey: String?,
    val answeredAt: Long?,
)

data class ScoredQuestion(
    val questionId: String,
    val isScored: Boolean,
    val isCorrect: Boolean,
    val marks: Int,
)

@Serializable
data class SubjectScore(
    val subject: Subject,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val marks: Int,
    val maxMarks: Int,
)

data class MockTestScore(
    val totalMarks: Int,
    val maxPossibleMarks: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val subjectScores: List<SubjectScore>,
    val scoredQuestions: List<ScoredQuestion>,
)

/**
 * Pure scoring logic, no Android/Room dependency — directly unit-testable with hand-built
 * [MockTestQuestionAnswer] fixtures.
 */
object MockTestScorer {
    private const val MARKS_CORRECT = 4
    private const val MARKS_WRONG = -1
    private const val SECTION_B_SCORED_LIMIT = 10

    fun score(answers: List<MockTestQuestionAnswer>): MockTestScore {
        val bySubject = answers.groupBy { it.subject }
        val subjectScores = mutableListOf<SubjectScore>()
        val allScoredQuestions = mutableListOf<ScoredQuestion>()

        for (subject in Subject.entries) {
            val subjectAnswers = bySubject[subject].orEmpty()
            val sectionA = subjectAnswers.filter { it.section == TestSection.A }
            val sectionB = subjectAnswers.filter { it.section == TestSection.B }

            // Real NEET Section B rule: 15 questions offered, only the first 10 *attempted*
            // (by chronological order) are scored. That allocation of 10 scored slots is a
            // fixed structural property of the exam format (35 A + 10 of 15 B = 45
            // questions/subject = 180 marks, matching real NEET's fixed 720 total) — it does
            // NOT shrink just because a student attempts fewer than 10. An unfilled slot scores
            // 0 and counts as unanswered, exactly like a blank Section-A question, but still
            // counts toward the subject's max marks.
            val sectionBScoredSlotCount = minOf(SECTION_B_SCORED_LIMIT, sectionB.size)
            val scoredSectionBIds = sectionB
                .filter { it.selectedOptionKey != null }
                .sortedBy { it.answeredAt ?: Long.MAX_VALUE }
                .take(SECTION_B_SCORED_LIMIT)
                .map { it.questionId }
                .toSet()

            var correct = 0
            var wrong = 0
            var unanswered = 0
            var marks = 0

            val scoredQuestions = subjectAnswers.map { answer ->
                val isScored = answer.section == TestSection.A || answer.questionId in scoredSectionBIds
                if (!isScored) {
                    return@map ScoredQuestion(answer.questionId, isScored = false, isCorrect = false, marks = 0)
                }
                when {
                    answer.selectedOptionKey == null -> {
                        unanswered++
                        ScoredQuestion(answer.questionId, isScored = true, isCorrect = false, marks = 0)
                    }
                    answer.selectedOptionKey == answer.correctOptionKey -> {
                        correct++
                        marks += MARKS_CORRECT
                        ScoredQuestion(answer.questionId, isScored = true, isCorrect = true, marks = MARKS_CORRECT)
                    }
                    else -> {
                        wrong++
                        marks += MARKS_WRONG
                        ScoredQuestion(answer.questionId, isScored = true, isCorrect = false, marks = MARKS_WRONG)
                    }
                }
            }

            // Section-B scored slots that were never filled (fewer than 10 attempted) have no
            // backing question but still count as unanswered against the fixed max.
            unanswered += sectionBScoredSlotCount - scoredSectionBIds.size

            allScoredQuestions += scoredQuestions
            subjectScores += SubjectScore(
                subject = subject,
                correctCount = correct,
                wrongCount = wrong,
                unansweredCount = unanswered,
                marks = marks,
                maxMarks = (sectionA.size + sectionBScoredSlotCount) * MARKS_CORRECT,
            )
        }

        return MockTestScore(
            totalMarks = subjectScores.sumOf { it.marks },
            maxPossibleMarks = subjectScores.sumOf { it.maxMarks },
            correctCount = subjectScores.sumOf { it.correctCount },
            wrongCount = subjectScores.sumOf { it.wrongCount },
            unansweredCount = subjectScores.sumOf { it.unansweredCount },
            subjectScores = subjectScores,
            scoredQuestions = allScoredQuestions,
        )
    }
}
