package com.neet.app.data

import com.neet.app.data.local.AnswerDao
import com.neet.app.data.local.AnsweredQuestionEntity
import com.neet.app.data.local.MockTestDao
import com.neet.app.data.local.MockTestQuestionEntity
import com.neet.app.data.local.MockTestSessionEntity
import com.neet.app.data.local.SubjectStat
import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AnsweredQuestionRecord(
    val question: Question,
    val selectedOptionKey: String,
)

data class RestoreResult(val answerCount: Int, val mockTestCount: Int)

/**
 * On-disk backup format. [version] lets future changes stay readable by older/newer app builds —
 * [mockTestSessions]/[mockTestQuestions] default to empty so a pre-mock-test (version 1) backup
 * still restores cleanly.
 */
@Serializable
data class BackupPayload(
    val version: Int = 2,
    val exportedAt: Long,
    val answers: List<AnsweredQuestionEntity>,
    val mockTestSessions: List<MockTestSessionEntity> = emptyList(),
    val mockTestQuestions: List<MockTestQuestionEntity> = emptyList(),
)

class HistoryRepository(
    private val answerDao: AnswerDao,
    private val mockTestDao: MockTestDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun recordAnswer(question: Question, selectedOptionKey: String) {
        answerDao.insert(toEntity(question, selectedOptionKey, System.currentTimeMillis()))
    }

    /** Bulk variant used when submitting a mock test — records only the scored, attempted
     * questions from the test in one write, feeding the same table/Focus-Areas signal as
     * regular practice. */
    suspend fun recordAnswers(records: List<AnsweredQuestionRecord>, answeredAt: Long = System.currentTimeMillis()) {
        answerDao.insertAll(records.map { toEntity(it.question, it.selectedOptionKey, answeredAt) })
    }

    private fun toEntity(question: Question, selectedOptionKey: String, answeredAt: Long): AnsweredQuestionEntity {
        val selectedText = question.options.firstOrNull { it.key == selectedOptionKey }?.text.orEmpty()
        val correctText = question.options.firstOrNull { it.key == question.correctOptionKey }?.text.orEmpty()
        return AnsweredQuestionEntity(
            id = question.id,
            subject = question.subject.name,
            topic = question.topic,
            difficulty = question.difficulty.name,
            stem = question.stem,
            selectedOptionKey = selectedOptionKey,
            selectedOptionText = selectedText,
            correctOptionKey = question.correctOptionKey,
            correctOptionText = correctText,
            isCorrect = selectedOptionKey == question.correctOptionKey,
            answeredAt = answeredAt,
            questionJson = json.encodeToString(question),
        )
    }

    suspend fun getAnsweredQuestion(id: String): AnsweredQuestionRecord? {
        val entity = answerDao.getById(id) ?: return null
        return AnsweredQuestionRecord(
            question = json.decodeFromString(entity.questionJson),
            selectedOptionKey = entity.selectedOptionKey,
        )
    }

    suspend fun recentStems(subject: String, topic: String): List<String> =
        answerDao.recentStems(subject, topic)

    fun subjectStats(): Flow<List<SubjectStat>> = answerDao.subjectStats()

    fun topicStats(): Flow<List<TopicStat>> = answerDao.topicStats()

    fun history(): Flow<List<AnsweredQuestionEntity>> = answerDao.history()

    /** Builds the full local snapshot as a typed payload for cross-device sync — the only
     * consumer now that manual file backup/restore has been replaced by account sync. */
    suspend fun buildBackupPayload(): BackupPayload = BackupPayload(
        exportedAt = System.currentTimeMillis(),
        answers = answerDao.getAllOnce(),
        mockTestSessions = mockTestDao.getAllSessionsOnce(),
        mockTestQuestions = mockTestDao.getAllQuestionsOnce(),
    )

    /** Merges a pulled sync snapshot non-destructively: existing rows are kept, rows with an id
     * also present in the payload are overwritten by it, and new rows are added. */
    suspend fun applyBackupPayload(payload: BackupPayload): RestoreResult {
        answerDao.insertAll(payload.answers)
        if (payload.mockTestSessions.isNotEmpty()) mockTestDao.insertSessions(payload.mockTestSessions)
        if (payload.mockTestQuestions.isNotEmpty()) mockTestDao.insertQuestions(payload.mockTestQuestions)
        return RestoreResult(payload.answers.size, payload.mockTestSessions.size)
    }
}
