package com.neet.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {

    @Insert
    suspend fun insert(entity: AnsweredQuestionEntity)

    /** Bulk insert used for restore — replaces existing rows with the same id (re-restoring a
     * backup, or a backup that overlaps current data, should not fail or duplicate). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AnsweredQuestionEntity>)

    /** One-shot bulk read for export — [history] is a Flow meant for live UI observation. */
    @Query("SELECT * FROM answered_questions ORDER BY answeredAt DESC")
    suspend fun getAllOnce(): List<AnsweredQuestionEntity>

    @Query(
        """
        SELECT stem FROM answered_questions
        WHERE subject = :subject AND topic = :topic
        ORDER BY answeredAt DESC
        LIMIT :limit
        """,
    )
    suspend fun recentStems(subject: String, topic: String, limit: Int = 15): List<String>

    @Query(
        """
        SELECT subject, COUNT(*) AS total, SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END) AS correct
        FROM answered_questions
        GROUP BY subject
        """,
    )
    fun subjectStats(): Flow<List<SubjectStat>>

    @Query(
        """
        SELECT subject, topic, COUNT(*) AS total, SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END) AS correct
        FROM answered_questions
        GROUP BY subject, topic
        """,
    )
    fun topicStats(): Flow<List<TopicStat>>

    @Query("SELECT * FROM answered_questions ORDER BY answeredAt DESC")
    fun history(): Flow<List<AnsweredQuestionEntity>>

    @Query("SELECT * FROM answered_questions WHERE id = :id")
    suspend fun getById(id: String): AnsweredQuestionEntity?
}
