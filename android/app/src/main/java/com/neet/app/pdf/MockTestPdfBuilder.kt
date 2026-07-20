package com.neet.app.pdf

import com.neet.app.data.local.MockTestQuestionEntity
import com.neet.app.data.local.MockTestSessionEntity
import com.neet.app.data.model.Question
import com.neet.app.data.model.Subject
import java.text.DateFormat
import java.util.Date

/**
 * Builds one Markdown document for a completed mock test: overall + per-subject score, then every
 * question with its stem, options, the student's answer, the correct answer, and why — grouped by
 * subject in the order the test was taken. All data is already local (Room); no network calls.
 */
fun buildMockTestMarkdown(
    session: MockTestSessionEntity,
    questions: List<MockTestQuestionEntity>,
    decodeQuestion: (MockTestQuestionEntity) -> Question,
): String = buildString {
    appendLine("# Mock Test Review")
    appendLine()
    val submittedAt = session.submittedAt
    if (submittedAt != null) {
        appendLine("Submitted: ${DateFormat.getDateTimeInstance().format(Date(submittedAt))}")
        appendLine()
    }
    if (session.totalScore != null && session.maxPossibleMarks != null) {
        appendLine(
            "**Score: ${session.totalScore} / ${session.maxPossibleMarks}** — " +
                "${session.correctCount ?: 0} correct, ${session.wrongCount ?: 0} wrong, " +
                "${session.unansweredCount ?: 0} unanswered",
        )
        appendLine()
    }

    val bySubject = questions
        .sortedBy { it.sectionIndex }
        .groupBy { Subject.valueOf(it.subject) }

    var questionNumber = 0
    for (subject in Subject.entries) {
        val subjectQuestions = bySubject[subject] ?: continue
        appendLine("## ${subject.name.lowercase().replaceFirstChar { it.uppercase() }}")
        appendLine()
        for (entity in subjectQuestions) {
            questionNumber += 1
            val question = decodeQuestion(entity)
            appendLine("### Q$questionNumber (${entity.section})")
            appendLine()
            appendLine(question.stem)
            appendLine()
            question.options.forEach { option ->
                appendLine("- ${option.key}. ${option.text}")
            }
            appendLine()
            val selectedKey = entity.selectedOptionKey
            val selectedText = question.options.firstOrNull { it.key == selectedKey }?.text
            if (selectedKey != null && selectedText != null) {
                appendLine("**Your answer:** $selectedKey. $selectedText")
            } else {
                appendLine("**Your answer:** Not answered")
            }
            val correctText = question.options.firstOrNull { it.key == entity.correctOptionKey }?.text
            appendLine("**Correct answer:** ${entity.correctOptionKey}. $correctText")
            appendLine()
            appendLine(question.explanation.whyCorrect)
            appendLine()
        }
    }
}
