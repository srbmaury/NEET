package com.neet.backend.model

import kotlinx.serialization.Serializable

/**
 * Mirrors the Android app's local BackupPayload / Room entities field-for-field, so the exact
 * same JSON the app already produces for manual backup/restore can be POSTed here as-is — no
 * separate wire format to maintain for sync vs. backup.
 */
@Serializable
data class SyncAnsweredQuestion(
    val id: String,
    val subject: String,
    val topic: String,
    val difficulty: String,
    val stem: String,
    val selectedOptionKey: String,
    val selectedOptionText: String,
    val correctOptionKey: String,
    val correctOptionText: String,
    val isCorrect: Boolean,
    val answeredAt: Long,
    val questionJson: String,
)

@Serializable
data class SyncMockTestSession(
    val id: String,
    val startedAt: Long,
    val durationMillis: Long,
    val status: String,
    val submittedAt: Long? = null,
    val totalScore: Int? = null,
    val maxPossibleMarks: Int? = null,
    val correctCount: Int? = null,
    val wrongCount: Int? = null,
    val unansweredCount: Int? = null,
    val perSubjectScoreJson: String? = null,
)

@Serializable
data class SyncMockTestQuestion(
    val testId: String,
    val subject: String,
    val section: String,
    val sectionIndex: Int,
    val questionJson: String,
    val correctOptionKey: String,
    val selectedOptionKey: String? = null,
    val answeredAt: Long? = null,
    val visited: Boolean = false,
    val markedForReview: Boolean = false,
)

@Serializable
data class SyncPayload(
    val version: Int = 2,
    val exportedAt: Long,
    val answers: List<SyncAnsweredQuestion> = emptyList(),
    val mockTestSessions: List<SyncMockTestSession> = emptyList(),
    val mockTestQuestions: List<SyncMockTestQuestion> = emptyList(),
)
