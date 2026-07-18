package com.neet.backend.openai

import com.neet.backend.config.AppConfig
import com.neet.backend.model.Explanation
import com.neet.backend.model.GenerateQuestionRequest
import com.neet.backend.model.MockTestSlotRequest
import com.neet.backend.model.Question
import com.neet.backend.model.QuestionOption
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.UUID

class OpenAiException(message: String, cause: Throwable? = null) : Exception(message, cause)

private const val CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"
private const val MAX_RATE_LIMIT_RETRIES = 3

class OpenAiClient(private val config: AppConfig) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            // Batch calls (mock-test generation) produce far more tokens than a single
            // question, so this needs headroom well beyond the single-question case.
            requestTimeoutMillis = 90_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun generateQuestion(request: GenerateQuestionRequest): Question {
        val chatRequest = OpenAiChatRequest(
            model = config.openAiModel,
            messages = listOf(
                OpenAiMessage(role = "system", content = PromptBuilder.systemPrompt),
                OpenAiMessage(role = "user", content = PromptBuilder.userPrompt(request)),
            ),
            responseFormat = OpenAiSchema.responseFormat,
        )

        val content = runCatching { callOpenAi(chatRequest) }
            .recoverCatching { callOpenAi(chatRequest) } // one retry on failure
            .getOrElse { throw OpenAiException("Failed to generate question from OpenAI", it) }

        val generated = runCatching { json.decodeFromString<GeneratedQuestionContent>(content) }
            .getOrElse { throw OpenAiException("OpenAI returned malformed question JSON", it) }

        // Best-effort independent verification pass to catch self-contradictory answers
        // (e.g. the model's explanation concludes a different option than correctOptionKey
        // states). Falls back to the original draft if verification itself fails.
        val verified = runCatching { verifyAnswer(generated) }.getOrNull()
        val finalCorrectOptionKey = verified?.correctOptionKey ?: generated.correctOptionKey
        val finalExplanation = verified?.explanation ?: generated.explanation

        return Question(
            id = UUID.randomUUID().toString(),
            subject = request.subject,
            topic = request.topic,
            difficulty = request.difficulty,
            stem = generated.stem,
            options = generated.options.map { QuestionOption(it.key, it.text) },
            correctOptionKey = finalCorrectOptionKey,
            explanation = Explanation(
                whyCorrect = finalExplanation.whyCorrect,
                whyOthersWrong = finalExplanation.whyOthersWrong,
                keyConcept = finalExplanation.keyConcept,
            ),
            tags = generated.tags,
        )
    }

    private suspend fun verifyAnswer(draft: GeneratedQuestionContent): VerifiedAnswer {
        val chatRequest = OpenAiChatRequest(
            model = config.openAiModel,
            temperature = 0.2,
            messages = listOf(
                OpenAiMessage(role = "system", content = PromptBuilder.verificationSystemPrompt),
                OpenAiMessage(
                    role = "user",
                    content = PromptBuilder.verificationUserPrompt(
                        stem = draft.stem,
                        options = draft.options,
                        proposedCorrectOptionKey = draft.correctOptionKey,
                        proposedExplanation = draft.explanation,
                    ),
                ),
            ),
            responseFormat = OpenAiSchema.verificationResponseFormat,
        )
        val content = callOpenAi(chatRequest)
        return json.decodeFromString(content)
    }

    /**
     * Generates a batch of questions for a mock test — one OpenAI call to draft all of
     * [slots] at once, then one call to independently verify the whole batch. Used instead
     * of calling [generateQuestion] per slot to keep a 200-question mock test to a small
     * number of OpenAI calls (see MockTestGenerator).
     */
    suspend fun generateQuestionBatch(slots: List<MockTestSlotRequest>): List<Question> {
        require(slots.isNotEmpty()) { "slots must not be empty" }

        val generateChatRequest = OpenAiChatRequest(
            model = config.openAiModel,
            messages = listOf(
                OpenAiMessage(role = "system", content = PromptBuilder.batchSystemPrompt),
                OpenAiMessage(role = "user", content = PromptBuilder.batchUserPrompt(slots)),
            ),
            responseFormat = OpenAiSchema.questionBatchResponseFormat(slots.size),
        )
        val generateContent = runCatching { callOpenAi(generateChatRequest) }
            .recoverCatching { callOpenAi(generateChatRequest) } // one retry on failure
            .getOrElse { throw OpenAiException("Failed to generate question batch from OpenAI", it) }

        val drafts = runCatching {
            json.decodeFromString<GeneratedQuestionBatch>(generateContent).questions
        }.getOrElse { throw OpenAiException("OpenAI returned malformed question batch JSON", it) }
        if (drafts.size != slots.size) {
            throw OpenAiException("Expected ${slots.size} questions in batch, got ${drafts.size}")
        }

        // Best-effort independent verification pass, same graceful-degradation contract as
        // the single-question path: fall back to each draft's own answer if verification
        // itself fails.
        val verifications = runCatching { verifyAnswerBatch(drafts) }.getOrNull()

        return slots.indices.map { i ->
            val slot = slots[i]
            val draft = drafts[i]
            val verified = verifications?.getOrNull(i)
            val finalCorrectOptionKey = verified?.correctOptionKey ?: draft.correctOptionKey
            val finalExplanation = verified?.explanation ?: draft.explanation

            Question(
                id = UUID.randomUUID().toString(),
                subject = slot.subject,
                topic = slot.topic,
                difficulty = slot.difficulty,
                stem = draft.stem,
                options = draft.options.map { QuestionOption(it.key, it.text) },
                correctOptionKey = finalCorrectOptionKey,
                explanation = Explanation(
                    whyCorrect = finalExplanation.whyCorrect,
                    whyOthersWrong = finalExplanation.whyOthersWrong,
                    keyConcept = finalExplanation.keyConcept,
                ),
                tags = draft.tags,
            )
        }
    }

    private suspend fun verifyAnswerBatch(drafts: List<GeneratedQuestionContent>): List<VerifiedAnswer> {
        val chatRequest = OpenAiChatRequest(
            model = config.openAiModel,
            temperature = 0.2,
            messages = listOf(
                OpenAiMessage(role = "system", content = PromptBuilder.batchVerificationSystemPrompt),
                OpenAiMessage(
                    role = "user",
                    content = PromptBuilder.batchVerificationUserPrompt(drafts),
                ),
            ),
            responseFormat = OpenAiSchema.verificationBatchResponseFormat(drafts.size),
        )
        val content = callOpenAi(chatRequest)
        val verifications = json.decodeFromString<VerifiedAnswerBatch>(content).verifications
        if (verifications.size != drafts.size) {
            throw OpenAiException(
                "Expected ${drafts.size} verifications in batch, got ${verifications.size}",
            )
        }
        return verifications
    }

    private suspend fun callOpenAi(chatRequest: OpenAiChatRequest): String {
        var attempt = 0
        while (true) {
            val response = http.post(CHAT_COMPLETIONS_URL) {
                header("Authorization", "Bearer ${config.openAiApiKey}")
                contentType(ContentType.Application.Json)
                setBody(chatRequest)
            }

            if (response.status == HttpStatusCode.TooManyRequests && attempt < MAX_RATE_LIMIT_RETRIES) {
                val retryAfterSeconds = response.headers["Retry-After"]?.toLongOrNull()
                val backoffMillis = retryAfterSeconds?.times(1000) ?: (1000L shl attempt)
                delay(backoffMillis)
                attempt++
                continue
            }

            if (!response.status.isSuccess()) {
                val body = runCatching { response.bodyAsText() }.getOrDefault("")
                throw OpenAiException("OpenAI request failed with ${response.status}: $body")
            }

            val chatResponse = response.body<OpenAiChatResponse>()
            val message = chatResponse.choices.firstOrNull()?.message
                ?: throw OpenAiException("OpenAI response had no choices")
            return message.content
                ?: throw OpenAiException("OpenAI refused to answer: ${message.refusal ?: "unknown reason"}")
        }
    }
}

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val temperature: Double = 0.8,
    val messages: List<OpenAiMessage>,
    @SerialName("response_format") val responseFormat: JsonObject,
)

@Serializable
private data class OpenAiMessage(val role: String, val content: String)

@Serializable
private data class OpenAiChatResponse(val choices: List<OpenAiChoice> = emptyList())

@Serializable
private data class OpenAiChoice(val message: OpenAiResponseMessage)

@Serializable
private data class OpenAiResponseMessage(
    val role: String,
    val content: String? = null,
    val refusal: String? = null,
)
