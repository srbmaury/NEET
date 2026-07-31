package com.neet.app.data.model

import kotlinx.serialization.SerialName
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
data class SolveQuestionImageRequest(
    val imageBase64: String,
    val mimeType: String,
)

@Serializable
data class SolvedQuestion(
    val questionText: String,
    val answer: String,
    val solution: String,
    val keyConcept: String,
    val confidenceNote: String,
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
    @SerialName("correctOptionKey") val correctOptionKey: String,
    val explanation: Explanation,
    val tags: List<String> = emptyList(),
)
