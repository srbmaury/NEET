package com.neet.backend.openai

import com.neet.backend.model.MockTestQuestionResult
import com.neet.backend.model.MockTestSlotRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val BATCH_SIZE = 15
private const val MAX_ATTEMPTS_PER_BATCH = 3

/**
 * Generates a full mock test's worth of questions. Slots are grouped into subject-homogeneous
 * batches of [BATCH_SIZE] and each batch is generated (draft + verify) in a single pair of
 * OpenAI calls via [OpenAiClient.generateQuestionBatch] — a 200-question test is ~14-16 batches
 * (~32 calls) rather than 400 individual calls. Batches run concurrently, bounded by a semaphore,
 * so the whole test finishes in a few minutes instead of many.
 */
class MockTestGenerator(
    private val openAi: OpenAiClient,
    concurrency: Int,
) {
    private val semaphore = Semaphore(concurrency)

    suspend fun generateAll(slots: List<MockTestSlotRequest>): List<MockTestQuestionResult> {
        val batches = slots.groupBy { it.subject }.values.flatMap { it.chunked(BATCH_SIZE) }

        return coroutineScope {
            batches.map { batch ->
                async { semaphore.withPermit { generateBatchWithRetry(batch) } }
            }.awaitAll().flatten()
        }
    }

    private suspend fun generateBatchWithRetry(
        batch: List<MockTestSlotRequest>,
    ): List<MockTestQuestionResult> {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS_PER_BATCH) { attempt ->
            try {
                val questions = openAi.generateQuestionBatch(batch)
                return batch.indices.map { i ->
                    MockTestQuestionResult(
                        section = batch[i].section,
                        sectionIndex = batch[i].sectionIndex,
                        question = questions[i],
                    )
                }
            } catch (e: OpenAiException) {
                lastError = e
                if (attempt < MAX_ATTEMPTS_PER_BATCH - 1) delay(500L * (attempt + 1))
            }
        }
        val subject = batch.firstOrNull()?.subject
        throw OpenAiException(
            "Failed to generate batch for $subject after $MAX_ATTEMPTS_PER_BATCH attempts",
            lastError,
        )
    }
}
