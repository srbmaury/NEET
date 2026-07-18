package com.neet.backend.db

import com.neet.backend.model.SyncAnsweredQuestion
import com.neet.backend.model.SyncMockTestQuestion
import com.neet.backend.model.SyncMockTestSession
import com.neet.backend.model.SyncPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SyncRepository {

    /** Non-destructive upsert-by-id, scoped to [userId] — same merge semantics as the app's
     * existing local REPLACE-on-id restore logic, just server-side. Uses batchUpsert (one
     * multi-row statement per chunk) rather than a per-row upsert loop — a full mock test alone
     * is 200 rows, and one network round-trip per row against a remote Postgres instance turned
     * a sync into a 2+ minute operation; batching is essential once sync runs automatically
     * (login/logout) rather than behind a manual retry button.
     *
     * shouldReturnGeneratedValues = false is required for the batching to actually happen: with
     * the (default-true) generated-values return, the PostgreSQL JDBC driver cannot combine the
     * batch into one multi-row statement (it needs each row's result individually) and silently
     * falls back to one round-trip per row — all our ids are client-supplied, not DB-generated,
     * so there's nothing to return anyway. */
    suspend fun push(userId: Uuid, payload: SyncPayload) = withContext(Dispatchers.IO) {
        transaction {
            if (payload.answers.isNotEmpty()) {
                AnsweredQuestions.batchUpsert(payload.answers, shouldReturnGeneratedValues = false) { a ->
                    this[AnsweredQuestions.id] = a.id
                    this[AnsweredQuestions.userId] = userId
                    this[AnsweredQuestions.subject] = a.subject
                    this[AnsweredQuestions.topic] = a.topic
                    this[AnsweredQuestions.difficulty] = a.difficulty
                    this[AnsweredQuestions.stem] = a.stem
                    this[AnsweredQuestions.selectedOptionKey] = a.selectedOptionKey
                    this[AnsweredQuestions.selectedOptionText] = a.selectedOptionText
                    this[AnsweredQuestions.correctOptionKey] = a.correctOptionKey
                    this[AnsweredQuestions.correctOptionText] = a.correctOptionText
                    this[AnsweredQuestions.isCorrect] = a.isCorrect
                    this[AnsweredQuestions.answeredAt] = a.answeredAt
                    this[AnsweredQuestions.questionJson] = a.questionJson
                }
            }
            if (payload.mockTestSessions.isNotEmpty()) {
                MockTestSessions.batchUpsert(payload.mockTestSessions, shouldReturnGeneratedValues = false) { s ->
                    this[MockTestSessions.id] = s.id
                    this[MockTestSessions.userId] = userId
                    this[MockTestSessions.startedAt] = s.startedAt
                    this[MockTestSessions.durationMillis] = s.durationMillis
                    this[MockTestSessions.status] = s.status
                    this[MockTestSessions.submittedAt] = s.submittedAt
                    this[MockTestSessions.totalScore] = s.totalScore
                    this[MockTestSessions.maxPossibleMarks] = s.maxPossibleMarks
                    this[MockTestSessions.correctCount] = s.correctCount
                    this[MockTestSessions.wrongCount] = s.wrongCount
                    this[MockTestSessions.unansweredCount] = s.unansweredCount
                    this[MockTestSessions.perSubjectScoreJson] = s.perSubjectScoreJson
                }
            }
            if (payload.mockTestQuestions.isNotEmpty()) {
                MockTestQuestions.batchUpsert(payload.mockTestQuestions, shouldReturnGeneratedValues = false) { q ->
                    this[MockTestQuestions.testId] = q.testId
                    this[MockTestQuestions.userId] = userId
                    this[MockTestQuestions.subject] = q.subject
                    this[MockTestQuestions.section] = q.section
                    this[MockTestQuestions.sectionIndex] = q.sectionIndex
                    this[MockTestQuestions.questionJson] = q.questionJson
                    this[MockTestQuestions.correctOptionKey] = q.correctOptionKey
                    this[MockTestQuestions.selectedOptionKey] = q.selectedOptionKey
                    this[MockTestQuestions.answeredAt] = q.answeredAt
                    this[MockTestQuestions.visited] = q.visited
                    this[MockTestQuestions.markedForReview] = q.markedForReview
                }
            }
        }
    }

    suspend fun pull(userId: Uuid): SyncPayload = withContext(Dispatchers.IO) {
        transaction {
            val answers = AnsweredQuestions.selectAll()
                .where { AnsweredQuestions.userId eq userId }
                .map {
                    SyncAnsweredQuestion(
                        id = it[AnsweredQuestions.id],
                        subject = it[AnsweredQuestions.subject],
                        topic = it[AnsweredQuestions.topic],
                        difficulty = it[AnsweredQuestions.difficulty],
                        stem = it[AnsweredQuestions.stem],
                        selectedOptionKey = it[AnsweredQuestions.selectedOptionKey],
                        selectedOptionText = it[AnsweredQuestions.selectedOptionText],
                        correctOptionKey = it[AnsweredQuestions.correctOptionKey],
                        correctOptionText = it[AnsweredQuestions.correctOptionText],
                        isCorrect = it[AnsweredQuestions.isCorrect],
                        answeredAt = it[AnsweredQuestions.answeredAt],
                        questionJson = it[AnsweredQuestions.questionJson],
                    )
                }

            val sessions = MockTestSessions.selectAll()
                .where { MockTestSessions.userId eq userId }
                .map {
                    SyncMockTestSession(
                        id = it[MockTestSessions.id],
                        startedAt = it[MockTestSessions.startedAt],
                        durationMillis = it[MockTestSessions.durationMillis],
                        status = it[MockTestSessions.status],
                        submittedAt = it[MockTestSessions.submittedAt],
                        totalScore = it[MockTestSessions.totalScore],
                        maxPossibleMarks = it[MockTestSessions.maxPossibleMarks],
                        correctCount = it[MockTestSessions.correctCount],
                        wrongCount = it[MockTestSessions.wrongCount],
                        unansweredCount = it[MockTestSessions.unansweredCount],
                        perSubjectScoreJson = it[MockTestSessions.perSubjectScoreJson],
                    )
                }

            val questions = MockTestQuestions.selectAll()
                .where { MockTestQuestions.userId eq userId }
                .map {
                    SyncMockTestQuestion(
                        testId = it[MockTestQuestions.testId],
                        subject = it[MockTestQuestions.subject],
                        section = it[MockTestQuestions.section],
                        sectionIndex = it[MockTestQuestions.sectionIndex],
                        questionJson = it[MockTestQuestions.questionJson],
                        correctOptionKey = it[MockTestQuestions.correctOptionKey],
                        selectedOptionKey = it[MockTestQuestions.selectedOptionKey],
                        answeredAt = it[MockTestQuestions.answeredAt],
                        visited = it[MockTestQuestions.visited],
                        markedForReview = it[MockTestQuestions.markedForReview],
                    )
                }

            SyncPayload(
                exportedAt = System.currentTimeMillis(),
                answers = answers,
                mockTestSessions = sessions,
                mockTestQuestions = questions,
            )
        }
    }
}
