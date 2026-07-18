package com.neet.backend.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** [cardsJson] is null for rows written before flashcards existed (or by an older deploy) —
 * callers treat that the same as an empty card list, not as a cache miss. */
data class CachedNotes(val contentMarkdown: String, val cardsJson: String?)

class NotesRepository {

    suspend fun findCached(subject: String, topic: String): CachedNotes? = withContext(Dispatchers.IO) {
        transaction {
            TopicNotes.selectAll()
                .where { (TopicNotes.subject eq subject) and (TopicNotes.topic eq topic) }
                .firstOrNull()
                ?.let { CachedNotes(it[TopicNotes.contentMarkdown], it[TopicNotes.cardsJson]) }
        }
    }

    suspend fun save(subject: String, topic: String, contentMarkdown: String, cardsJson: String) =
        withContext(Dispatchers.IO) {
            transaction {
                TopicNotes.upsert {
                    it[TopicNotes.subject] = subject
                    it[TopicNotes.topic] = topic
                    it[TopicNotes.contentMarkdown] = contentMarkdown
                    it[TopicNotes.cardsJson] = cardsJson
                    it[generatedAt] = System.currentTimeMillis()
                }
            }
        }
}
