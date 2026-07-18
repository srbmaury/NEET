package com.neet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

const val MOCK_TEST_STATUS_IN_PROGRESS = "IN_PROGRESS"
const val MOCK_TEST_STATUS_SUBMITTED = "SUBMITTED"

@Serializable
@Entity(tableName = "mock_test_sessions")
data class MockTestSessionEntity(
    @PrimaryKey val id: String,
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
@Entity(
    tableName = "mock_test_questions",
    primaryKeys = ["testId", "subject", "section", "sectionIndex"],
)
data class MockTestQuestionEntity(
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
