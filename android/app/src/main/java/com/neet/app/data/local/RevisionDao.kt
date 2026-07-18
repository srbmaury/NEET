package com.neet.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisionDao {

    @Query("SELECT * FROM revisions ORDER BY topic")
    fun getAll(): Flow<List<RevisionEntity>>

    @Upsert
    suspend fun upsert(entity: RevisionEntity)

    @Query(
        """
        INSERT INTO revisions (topic, subject, revisionCount) VALUES (:topic, :subject, 1)
        ON CONFLICT(topic) DO UPDATE SET revisionCount = revisionCount + 1
        """,
    )
    suspend fun incrementRevision(topic: String, subject: String?)

    /** Adds a custom topic at count 0 — a no-op if that topic already has a row (syllabus topics
     * only get a row once first revised via [incrementRevision]; this lets a custom topic show up
     * in the list immediately, before it's ever been revised). */
    @Query("INSERT OR IGNORE INTO revisions (topic, subject, revisionCount) VALUES (:topic, :subject, 0)")
    suspend fun addTopicIfAbsent(topic: String, subject: String?)
}
