package com.neet.backend.routes

import com.neet.backend.db.NotesRepository
import com.neet.backend.model.NoteCard
import com.neet.backend.model.NotesResponse
import com.neet.backend.model.Subject
import com.neet.backend.openai.OpenAiClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun Route.notesRoutes(notesRepository: NotesRepository, openAi: OpenAiClient) {
    get("/notes/{subject}/{topic}") {
        val subjectParam = call.parameters["subject"].orEmpty()
        val subject = Subject.valueOf(subjectParam) // throws IllegalArgumentException -> 400, matches existing StatusPages handling
        val topic = call.parameters["topic"].orEmpty()
        require(topic.isNotBlank()) { "topic must not be blank" }

        val cached = notesRepository.findCached(subject.name, topic)
        if (cached != null) {
            val cards = json.decodeFromString<List<NoteCard>>(cached.cardsJson)
            call.respond(
                HttpStatusCode.OK,
                NotesResponse(subject.name, topic, cached.contentMarkdown, cards, cached = true),
            )
            return@get
        }

        val generated = openAi.generateNotes(subject.name, topic)
        val cards = generated.cards.map { NoteCard(it.term, it.content, it.type) }
        notesRepository.save(subject.name, topic, generated.contentMarkdown, json.encodeToString(cards))
        call.respond(
            HttpStatusCode.OK,
            NotesResponse(subject.name, topic, generated.contentMarkdown, cards, cached = false),
        )
    }
}
