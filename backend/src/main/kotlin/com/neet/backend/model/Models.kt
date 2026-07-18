package com.neet.backend.model

import kotlinx.serialization.Serializable

enum class Subject { PHYSICS, CHEMISTRY, BOTANY, ZOOLOGY }

enum class Difficulty { EASY, MEDIUM, HARD }

@Serializable
data class GenerateQuestionRequest(
    val subject: Subject,
    val topic: String,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val excludeStems: List<String> = emptyList(),
)

@Serializable
data class QuestionOption(
    val key: String,
    val text: String,
)

@Serializable
data class Explanation(
    val whyCorrect: String,
    val whyOthersWrong: String,
    val keyConcept: String,
)

@Serializable
data class Question(
    val id: String,
    val subject: Subject,
    val topic: String,
    val difficulty: Difficulty,
    val stem: String,
    val options: List<QuestionOption>,
    val correctOptionKey: String,
    val explanation: Explanation,
    val tags: List<String>,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)
