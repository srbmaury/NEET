package com.neet.backend.routes

import com.neet.backend.model.GenerateQuestionRequest
import com.neet.backend.openai.OpenAiClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.questionRoutes(openAi: OpenAiClient) {
    post("/questions/generate") {
        val request = call.receive<GenerateQuestionRequest>()
        val question = openAi.generateQuestion(request)
        call.respond(HttpStatusCode.OK, question)
    }
}
