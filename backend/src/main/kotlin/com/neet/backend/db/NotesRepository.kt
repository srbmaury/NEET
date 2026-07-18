package com.neet.backend.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class NotesRepository {

    suspend fun findCached(subject: String, topic: String): String? = withContext(Dispatchers.IO) {
        transaction {
            TopicNotes.selectAll()
                .where { (TopicNotes.subject eq subject) and (TopicNotes.topic eq topic) }
                .firstOrNull()
                ?.get(TopicNotes.contentMarkdown)
        }
    }

    suspend fun save(subject: String, topic: String, contentMarkdown: String) = withContext(Dispatchers.IO) {
        transaction {
            TopicNotes.upsert {
                it[TopicNotes.subject] = subject
                it[TopicNotes.topic] = topic
                it[TopicNotes.contentMarkdown] = contentMarkdown
                it[generatedAt] = System.currentTimeMillis()
            }
        }
    }
}
