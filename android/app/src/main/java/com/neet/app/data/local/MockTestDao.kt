package com.neet.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MockTestDao {

    @Insert
    suspend fun insertSession(session: MockTestSessionEntity)

    @Update
    suspend fun updateSession(session: MockTestSessionEntity)

    @Query("SELECT * FROM mock_test_sessions WHERE id = :testId")
    suspend fun getSession(testId: String): MockTestSessionEntity?

    @Query("SELECT * FROM mock_test_sessions WHERE status = '$MOCK_TEST_STATUS_IN_PROGRESS' LIMIT 1")
    suspend fun getActiveSession(): MockTestSessionEntity?

    @Query("SELECT * FROM mock_test_sessions WHERE status = '$MOCK_TEST_STATUS_IN_PROGRESS' LIMIT 1")
    fun activeSessionFlow(): Flow<MockTestSessionEntity?>

    @Query("SELECT * FROM mock_test_sessions WHERE status = '$MOCK_TEST_STATUS_SUBMITTED' ORDER BY submittedAt DESC")
    fun completedTests(): Flow<List<MockTestSessionEntity>>

    /** One-shot bulk read for export — [completedTests] is a Flow meant for live UI observation. */
    @Query("SELECT * FROM mock_test_sessions")
    suspend fun getAllSessionsOnce(): List<MockTestSessionEntity>

    /** Bulk insert used for restore — replaces existing rows with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<MockTestSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<MockTestQuestionEntity>)

    @Query("SELECT * FROM mock_test_questions")
    suspend fun getAllQuestionsOnce(): List<MockTestQuestionEntity>

    @Query("SELECT * FROM mock_test_questions WHERE testId = :testId")
    fun questionsForTest(testId: String): Flow<List<MockTestQuestionEntity>>

    @Query("SELECT * FROM mock_test_questions WHERE testId = :testId")
    suspend fun getQuestionsOnce(testId: String): List<MockTestQuestionEntity>

    /** [answeredAt] is only ever set once — a re-answer keeps the original attempt time, which
     * is what makes "first 10 attempted" in Section B well-defined by chronological order. */
    @Query(
        """
        UPDATE mock_test_questions
        SET selectedOptionKey = :selectedOptionKey, answeredAt = COALESCE(answeredAt, :answeredAt)
        WHERE testId = :testId AND subject = :subject AND section = :section AND sectionIndex = :sectionIndex
        """,
    )
    suspend fun selectAnswer(
        testId: String,
        subject: String,
        section: String,
        sectionIndex: Int,
        selectedOptionKey: String,
        answeredAt: Long,
    )

    @Query(
        """
        UPDATE mock_test_questions
        SET selectedOptionKey = NULL, answeredAt = NULL
        WHERE testId = :testId AND subject = :subject AND section = :section AND sectionIndex = :sectionIndex
        """,
    )
    suspend fun clearAnswer(testId: String, subject: String, section: String, sectionIndex: Int)

    @Query(
        """
        UPDATE mock_test_questions
        SET markedForReview = :marked
        WHERE testId = :testId AND subject = :subject AND section = :section AND sectionIndex = :sectionIndex
        """,
    )
    suspend fun setMarkedForReview(
        testId: String,
        subject: String,
        section: String,
        sectionIndex: Int,
        marked: Boolean,
    )

    @Query(
        """
        UPDATE mock_test_questions
        SET visited = 1
        WHERE testId = :testId AND subject = :subject AND section = :section AND sectionIndex = :sectionIndex
        """,
    )
    suspend fun markVisited(testId: String, subject: String, section: String, sectionIndex: Int)
}
