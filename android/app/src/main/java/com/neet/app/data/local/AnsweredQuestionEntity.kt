package com.neet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "answered_questions")
data class AnsweredQuestionEntity(
    @PrimaryKey val id: String,
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

data class SubjectStat(
    val subject: String,
    val total: Int,
    val correct: Int,
)

data class TopicStat(
    val subject: String,
    val topic: String,
    val total: Int,
    val correct: Int,
)

data class MistakeTopicStat(
    val subject: String,
    val topic: String,
    val wrongCount: Int,
)
