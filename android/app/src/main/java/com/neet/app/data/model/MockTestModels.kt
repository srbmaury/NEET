package com.neet.app.data.model

import kotlinx.serialization.Serializable

enum class TestSection { A, B }

@Serializable
data class MockTestSlotRequest(
    val subject: Subject,
    val topic: String,
    val difficulty: Difficulty,
    val section: TestSection,
    val sectionIndex: Int,
)

@Serializable
data class GenerateMockTestRequest(
    val slots: List<MockTestSlotRequest>,
)

@Serializable
data class MockTestQuestionResult(
    val section: TestSection,
    val sectionIndex: Int,
    val question: Question,
)

@Serializable
data class MockTestResponse(
    val testId: String,
    val questions: List<MockTestQuestionResult>,
)
