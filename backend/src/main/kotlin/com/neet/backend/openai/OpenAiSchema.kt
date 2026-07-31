package com.neet.backend.openai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The subset of a [com.neet.backend.model.Question] that we actually ask OpenAI to
 * generate. subject/topic/difficulty/id are filled in by the backend itself rather
 * than trusted from the model's output.
 */
@Serializable
data class GeneratedQuestionContent(
    val stem: String,
    val options: List<GeneratedOption>,
    val correctOptionKey: String,
    val explanation: GeneratedExplanation,
    val tags: List<String>,
)

@Serializable
data class GeneratedOption(val key: String, val text: String)

@Serializable
data class GeneratedExplanation(
    val whyCorrect: String,
    val whyOthersWrong: String,
    val keyConcept: String,
)

/** Result of the independent verification pass — see [OpenAiClient.verifyAnswer]. */
@Serializable
data class VerifiedAnswer(
    val correctOptionKey: String,
    val explanation: GeneratedExplanation,
)

/** Reference notes for a topic — no "correct answer" concept here, so unlike question
 * generation this doesn't need an independent verification pass. [cards] are the same
 * concepts/formulas as [contentMarkdown], broken into individual flashcard-sized pieces —
 * generated in the same call rather than a second one. */
@Serializable
data class GeneratedNotes(val contentMarkdown: String, val cards: List<NoteCard>)

@Serializable
data class NoteCard(val term: String, val content: String, val type: String)

/** Wrapper for batch generation — OpenAI's structured outputs require an object at the root,
 * not a bare array, so batches are wrapped in a single named array property. */
@Serializable
data class GeneratedQuestionBatch(val questions: List<GeneratedQuestionContent>)

@Serializable
data class VerifiedAnswerBatch(val verifications: List<VerifiedAnswer>)

/** Builds the strict `json_schema` response_format sent to OpenAI's chat completions API. */
object OpenAiSchema {

    // NOTE: object properties initialize top-to-bottom, so schema pieces must be
    // declared before responseFormat, which composes them.

    private val optionSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("key") {
                put("type", "string")
                put("enum", buildJsonArray { add("A"); add("B"); add("C"); add("D") })
            }
            putJsonObject("text") { put("type", "string") }
        }
        put("required", buildJsonArray { add("key"); add("text") })
        put("additionalProperties", false)
    }

    private val explanationSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("whyCorrect") { put("type", "string") }
            putJsonObject("whyOthersWrong") { put("type", "string") }
            putJsonObject("keyConcept") { put("type", "string") }
        }
        put(
            "required",
            buildJsonArray { add("whyCorrect"); add("whyOthersWrong"); add("keyConcept") },
        )
        put("additionalProperties", false)
    }

    private val schema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("stem") { put("type", "string") }
            putJsonObject("options") {
                put("type", "array")
                put("minItems", 4)
                put("maxItems", 4)
                put("items", optionSchema)
            }
            putJsonObject("correctOptionKey") {
                put("type", "string")
                put("enum", buildJsonArray { add("A"); add("B"); add("C"); add("D") })
            }
            put("explanation", explanationSchema)
            putJsonObject("tags") {
                put("type", "array")
                putJsonObject("items") { put("type", "string") }
            }
        }
        put(
            "required",
            buildJsonArray {
                add("stem"); add("options"); add("correctOptionKey")
                add("explanation"); add("tags")
            },
        )
        put("additionalProperties", false)
    }

    val responseFormat: JsonObject = buildJsonObject {
        put("type", "json_schema")
        putJsonObject("json_schema") {
            put("name", "neet_question")
            put("strict", true)
            put("schema", schema)
        }
    }

    private val verificationSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("correctOptionKey") {
                put("type", "string")
                put("enum", buildJsonArray { add("A"); add("B"); add("C"); add("D") })
            }
            put("explanation", explanationSchema)
        }
        put("required", buildJsonArray { add("correctOptionKey"); add("explanation") })
        put("additionalProperties", false)
    }

    val verificationResponseFormat: JsonObject = buildJsonObject {
        put("type", "json_schema")
        putJsonObject("json_schema") {
            put("name", "neet_answer_verification")
            put("strict", true)
            put("schema", verificationSchema)
        }
    }

    /** Batch variant of [responseFormat] — an object wrapping an array of exactly [count]
     * questions, in the same order as the slots sent to the model. */
    fun questionBatchResponseFormat(count: Int): JsonObject = buildJsonObject {
        put("type", "json_schema")
        putJsonObject("json_schema") {
            put("name", "neet_question_batch")
            put("strict", true)
            putJsonObject("schema") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("questions") {
                        put("type", "array")
                        put("minItems", count)
                        put("maxItems", count)
                        put("items", schema)
                    }
                }
                put("required", buildJsonArray { add("questions") })
                put("additionalProperties", false)
            }
        }
    }

    /** Batch variant of [verificationResponseFormat] — an object wrapping an array of exactly
     * [count] verifications, in the same order as the drafts sent to the model. */
    fun verificationBatchResponseFormat(count: Int): JsonObject = buildJsonObject {
        put("type", "json_schema")
        putJsonObject("json_schema") {
            put("name", "neet_answer_verification_batch")
            put("strict", true)
            putJsonObject("schema") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("verifications") {
                        put("type", "array")
                        put("minItems", count)
                        put("maxItems", count)
                        put("items", verificationSchema)
                    }
                }
                put("required", buildJsonArray { add("verifications") })
                put("additionalProperties", false)
            }
        }
    }

    private val noteCardSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("term") { put("type", "string") }
            putJsonObject("content") { put("type", "string") }
            putJsonObject("type") {
                put("type", "string")
                put("enum", buildJsonArray { add("CONCEPT"); add("FORMULA") })
            }
        }
        put("required", buildJsonArray { add("term"); add("content"); add("type") })
        put("additionalProperties", false)
    }

    private val notesSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("contentMarkdown") { put("type", "string") }
            putJsonObject("cards") {
                put("type", "array")
                put("items", noteCardSchema)
            }
        }
        put("required", buildJsonArray { add("contentMarkdown"); add("cards") })
        put("additionalProperties", false)
    }

    val notesResponseFormat: JsonObject = buildJsonObject {
        put("type", "json_schema")
        putJsonObject("json_schema") {
            put("name", "neet_topic_notes")
            put("strict", true)
            put("schema", notesSchema)
        }
    }

    private val solvedQuestionSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("questionText") { put("type", "string") }
            putJsonObject("answer") { put("type", "string") }
            putJsonObject("solution") { put("type", "string") }
            putJsonObject("keyConcept") { put("type", "string") }
            putJsonObject("confidenceNote") { put("type", "string") }
        }
        put("required", buildJsonArray {
            add("questionText"); add("answer"); add("solution"); add("keyConcept"); add("confidenceNote")
        })
        put("additionalProperties", false)
    }

    val solvedQuestionResponseFormat: JsonObject = buildJsonObject {
        put("type", "json_schema")
        putJsonObject("json_schema") {
            put("name", "neet_photo_solution")
            put("strict", true)
            put("schema", solvedQuestionSchema)
        }
    }
}
