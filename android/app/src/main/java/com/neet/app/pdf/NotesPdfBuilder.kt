package com.neet.app.pdf

import com.neet.app.data.NotesRepository
import com.neet.app.data.NotesResult
import com.neet.app.data.model.Subject
import com.neet.app.domain.TopicCatalog
import com.neet.app.ui.reference.quickReferenceMarkdown
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

// Matches the concurrency used when warming the server-side cache for all topics — gentle on the
// backend, since a fresh (never-before-requested) topic costs a real OpenAI call, not just a
// cache read.
private const val MAX_CONCURRENT_FETCHES = 4

/**
 * Builds one combined Markdown document: the Quick Reference sheet, followed by every syllabus
 * topic's notes grouped by subject — multiple topics flow onto shared pages (this isn't meant to
 * be 84 one-topic pages), pagination is handled later by [renderMarkdownToPdf] purely based on
 * how much content actually fits.
 *
 * [onProgress] is called after each topic's fetch resolves (completed, total) — topics are
 * fetched with bounded parallelism, but the returned document always lists them in a stable
 * subject-then-topic order regardless of fetch completion order.
 */
suspend fun buildAllNotesMarkdown(
    notesRepository: NotesRepository,
    onProgress: (completed: Int, total: Int) -> Unit,
): String = coroutineScope {
    val topics = TopicCatalog.allTopics()
    val semaphore = Semaphore(MAX_CONCURRENT_FETCHES)
    val completedCount = AtomicInteger(0)

    val fetches = topics.map { (subject, topic) ->
        async {
            val result = semaphore.withPermit { notesRepository.getNotes(subject, topic) }
            onProgress(completedCount.incrementAndGet(), topics.size)
            Triple(subject, topic, result)
        }
    }.awaitAll()

    buildString {
        appendLine("# NEET Notes & Quick Reference")
        appendLine()
        appendLine("## Quick Reference")
        appendLine()
        appendLine(quickReferenceMarkdown)
        appendLine()

        var currentSubject: Subject? = null
        for ((subject, topic, result) in fetches) {
            if (subject != currentSubject) {
                currentSubject = subject
                appendLine("## ${subject.name.lowercase().replaceFirstChar { it.uppercase() }}")
                appendLine()
            }
            appendLine("### $topic")
            appendLine()
            when (result) {
                is NotesResult.Success -> appendLine(result.notes.contentMarkdown)
                is NotesResult.Failure -> appendLine("_Could not load notes for this topic (${result.message})_")
            }
            appendLine()
        }
    }
}
