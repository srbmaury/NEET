package com.neet.backend.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

/** Deliberately minimal — auth-only, no display name/category/exam-year fields. */
object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

/** Mirrors Android's AnsweredQuestionEntity 1:1, scoped by userId. */
object AnsweredQuestions : Table("answered_questions") {
    val id = varchar("id", 64)
    val userId = uuid("user_id").index()
    val subject = varchar("subject", 32)
    val topic = varchar("topic", 255)
    val difficulty = varchar("difficulty", 16)
    val stem = text("stem")
    val selectedOptionKey = varchar("selected_option_key", 8)
    val selectedOptionText = text("selected_option_text")
    val correctOptionKey = varchar("correct_option_key", 8)
    val correctOptionText = text("correct_option_text")
    val isCorrect = bool("is_correct")
    val answeredAt = long("answered_at")
    val questionJson = text("question_json")

    override val primaryKey = PrimaryKey(id)
}

/** Mirrors Android's MockTestSessionEntity 1:1, scoped by userId. */
object MockTestSessions : Table("mock_test_sessions") {
    val id = varchar("id", 64)
    val userId = uuid("user_id").index()
    val startedAt = long("started_at")
    val durationMillis = long("duration_millis")
    val status = varchar("status", 16)
    val submittedAt = long("submitted_at").nullable()
    val totalScore = integer("total_score").nullable()
    val maxPossibleMarks = integer("max_possible_marks").nullable()
    val correctCount = integer("correct_count").nullable()
    val wrongCount = integer("wrong_count").nullable()
    val unansweredCount = integer("unanswered_count").nullable()
    val perSubjectScoreJson = text("per_subject_score_json").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Mirrors Android's MockTestQuestionEntity 1:1, scoped by userId. Composite PK matches the
 * Room entity's (testId, subject, section, sectionIndex). */
object MockTestQuestions : Table("mock_test_questions") {
    val testId = varchar("test_id", 64)
    val userId = uuid("user_id").index()
    val subject = varchar("subject", 32)
    val section = varchar("section", 4)
    val sectionIndex = integer("section_index")
    val questionJson = text("question_json")
    val correctOptionKey = varchar("correct_option_key", 8)
    val selectedOptionKey = varchar("selected_option_key", 8).nullable()
    val answeredAt = long("answered_at").nullable()
    val visited = bool("visited")
    val markedForReview = bool("marked_for_review")

    override val primaryKey = PrimaryKey(testId, subject, section, sectionIndex)
}

/** Global cache of generated concept/formula notes, keyed by (subject, topic) — NOT user-scoped.
 * The content is identical for every student studying "Kinematics", so it's generated once ever
 * (first request pays the OpenAI cost) and served from here for every subsequent request from
 * any user, indefinitely. */
object TopicNotes : Table("topic_notes") {
    val subject = varchar("subject", 32)
    val topic = varchar("topic", 255)
    val contentMarkdown = text("content_markdown")
    val generatedAt = long("generated_at")

    override val primaryKey = PrimaryKey(subject, topic)
}
