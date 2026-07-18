package com.neet.backend.routes

import com.neet.backend.db.NotesRepository
import com.neet.backend.model.NotesResponse
import com.neet.backend.model.Subject
import com.neet.backend.openai.OpenAiClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.notesRoutes(notesRepository: NotesRepository, openAi: OpenAiClient) {
    get("/notes/{subject}/{topic}") {
        val subjectParam = call.parameters["subject"].orEmpty()
        val subject = Subject.valueOf(subjectParam) // throws IllegalArgumentException -> 400, matches existing StatusPages handling
        val topic = call.parameters["topic"].orEmpty()
        require(topic.isNotBlank()) { "topic must not be blank" }

        val cached = notesRepository.findCached(subject.name, topic)
        if (cached != null) {
            call.respond(HttpStatusCode.OK, NotesResponse(subject.name, topic, cached, cached = true))
            return@get
        }

        val generated = openAi.generateNotes(subject.name, topic)
        notesRepository.save(subject.name, topic, generated)
        call.respond(HttpStatusCode.OK, NotesResponse(subject.name, topic, generated, cached = false))
    }
}
