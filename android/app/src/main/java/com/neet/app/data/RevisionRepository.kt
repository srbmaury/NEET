package com.neet.app.data

import com.neet.app.data.local.RevisionDao
import com.neet.app.data.local.RevisionEntity
import kotlinx.coroutines.flow.Flow

class RevisionRepository(private val revisionDao: RevisionDao) {

    fun getAll(): Flow<List<RevisionEntity>> = revisionDao.getAll()

    suspend fun incrementRevision(topic: String, subject: String?) {
        revisionDao.incrementRevision(topic, subject)
    }

    /** Splits comma-separated input into individual custom topic names, trims and drops blanks,
     * and adds each at revision count 0 if not already tracked. */
    suspend fun addCustomTopics(commaSeparated: String) {
        commaSeparated.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { topic -> revisionDao.addTopicIfAbsent(topic, subject = null) }
    }
}
