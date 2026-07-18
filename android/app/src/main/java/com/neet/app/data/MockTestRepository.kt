package com.neet.app.data

import com.neet.app.data.local.MOCK_TEST_STATUS_IN_PROGRESS
import com.neet.app.data.local.MOCK_TEST_STATUS_SUBMITTED
import com.neet.app.data.local.MockTestDao
import com.neet.app.data.local.MockTestQuestionEntity
import com.neet.app.data.local.MockTestSessionEntity
import com.neet.app.data.model.GenerateMockTestRequest
import com.neet.app.data.model.Question
import com.neet.app.data.model.Subject
import com.neet.app.data.model.TestSection
import com.neet.app.domain.MockTestBuilder
import com.neet.app.domain.MockTestQuestionAnswer
import com.neet.app.domain.MockTestScore
import com.neet.app.domain.MockTestScorer
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/** Full NEET format: 3h20m. */
const val MOCK_TEST_DURATION_MILLIS = 200L * 60_000L

sealed interface MockTestGenerationResult {
    data class Success(val testId: String) : MockTestGenerationResult
    data class Failure(val message: String) : MockTestGenerationResult
}

class MockTestRepository(
    private val mockTestApiService: MockTestApiService,
    private val mockTestDao: MockTestDao,
    private val historyRepository: HistoryRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun startNewTest(): MockTestGenerationResult {
        val slots = MockTestBuilder.buildSlots()
        return try {
            val response = mockTestApiService.generateMockTest(GenerateMockTestRequest(slots))
            val testId = response.testId
            mockTestDao.insertSession(
                MockTestSessionEntity(
                    id = testId,
                    startedAt = System.currentTimeMillis(),
                    durationMillis = MOCK_TEST_DURATION_MILLIS,
                    status = MOCK_TEST_STATUS_IN_PROGRESS,
                ),
            )
            mockTestDao.insertQuestions(
                response.questions.map { result ->
                    MockTestQuestionEntity(
                        testId = testId,
                        subject = result.question.subject.name,
                        section = result.section.name,
                        sectionIndex = result.sectionIndex,
                        questionJson = json.encodeToString(result.question),
                        correctOptionKey = result.question.correctOptionKey,
                    )
                },
            )
            MockTestGenerationResult.Success(testId)
        } catch (e: IOException) {
            MockTestGenerationResult.Failure("Couldn't reach the server. Check your connection and try again.")
        } catch (e: retrofit2.HttpException) {
            MockTestGenerationResult.Failure("The server had trouble generating the test. Please try again.")
        }
    }

    fun questionsForTest(testId: String): Flow<List<MockTestQuestionEntity>> =
        mockTestDao.questionsForTest(testId)

    suspend fun getSession(testId: String): MockTestSessionEntity? = mockTestDao.getSession(testId)

    suspend fun getActiveSession(): MockTestSessionEntity? = mockTestDao.getActiveSession()

    fun activeSessionFlow(): Flow<MockTestSessionEntity?> = mockTestDao.activeSessionFlow()

    fun completedTests(): Flow<List<MockTestSessionEntity>> = mockTestDao.completedTests()

    fun decodeQuestion(entity: MockTestQuestionEntity): Question =
        json.decodeFromString(entity.questionJson)

    suspend fun selectAnswer(testId: String, subject: String, section: String, sectionIndex: Int, optionKey: String) {
        mockTestDao.selectAnswer(testId, subject, section, sectionIndex, optionKey, System.currentTimeMillis())
    }

    suspend fun clearAnswer(testId: String, subject: String, section: String, sectionIndex: Int) {
        mockTestDao.clearAnswer(testId, subject, section, sectionIndex)
    }

    suspend fun toggleMarkedForReview(
        testId: String,
        subject: String,
        section: String,
        sectionIndex: Int,
        marked: Boolean,
    ) {
        mockTestDao.setMarkedForReview(testId, subject, section, sectionIndex, marked)
    }

    suspend fun markVisited(testId: String, subject: String, section: String, sectionIndex: Int) {
        mockTestDao.markVisited(testId, subject, section, sectionIndex)
    }

    /** Auto-submits an IN_PROGRESS session whose deadline has already passed — handles the app
     * having been killed for longer than the full 200-minute test duration. */
    suspend fun reconcileExpiredSession() {
        val active = mockTestDao.getActiveSession() ?: return
        val deadline = active.startedAt + active.durationMillis
        if (System.currentTimeMillis() >= deadline) {
            submitTest(active.id)
        }
    }

    suspend fun submitTest(testId: String): MockTestScore {
        val session = mockTestDao.getSession(testId) ?: error("Mock test $testId not found")
        val entities = mockTestDao.getQuestionsOnce(testId)
        val questions = entities.associateWith { decodeQuestion(it) }

        val answers = entities.map { entity ->
            MockTestQuestionAnswer(
                questionId = questions.getValue(entity).id,
                subject = Subject.valueOf(entity.subject),
                section = TestSection.valueOf(entity.section),
                sectionIndex = entity.sectionIndex,
                correctOptionKey = entity.correctOptionKey,
                selectedOptionKey = entity.selectedOptionKey,
                answeredAt = entity.answeredAt,
            )
        }
        val score = MockTestScorer.score(answers)

        // Only the scored+attempted set feeds practice history/Focus Areas — unattempted scored
        // questions are a time-management gap, not a knowledge gap, and unscored Section-B
        // overflow was never really "attempted" for exam purposes.
        val scoredIds = score.scoredQuestions.filter { it.isScored }.map { it.questionId }.toSet()
        val submittedAt = System.currentTimeMillis()
        val recordsToSave = entities
            .filter { it.selectedOptionKey != null && questions.getValue(it).id in scoredIds }
            .map { entity -> AnsweredQuestionRecord(questions.getValue(entity), entity.selectedOptionKey!!) }
        if (recordsToSave.isNotEmpty()) {
            historyRepository.recordAnswers(recordsToSave, submittedAt)
        }

        mockTestDao.updateSession(
            session.copy(
                status = MOCK_TEST_STATUS_SUBMITTED,
                submittedAt = submittedAt,
                totalScore = score.totalMarks,
                maxPossibleMarks = score.maxPossibleMarks,
                correctCount = score.correctCount,
                wrongCount = score.wrongCount,
                unansweredCount = score.unansweredCount,
                perSubjectScoreJson = json.encodeToString(score.subjectScores),
            ),
        )
        return score
    }
}
