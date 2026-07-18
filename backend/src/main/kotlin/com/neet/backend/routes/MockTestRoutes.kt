package com.neet.backend.routes

import com.neet.backend.model.GenerateMockTestRequest
import com.neet.backend.model.MockTestResponse
import com.neet.backend.openai.MockTestGenerator
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID

private const val MAX_SLOTS = 250

fun Route.mockTestRoutes(mockTestGenerator: MockTestGenerator) {
    post("/mock-test/generate") {
        val request = call.receive<GenerateMockTestRequest>()
        require(request.slots.isNotEmpty() && request.slots.size <= MAX_SLOTS) {
            "slots must contain between 1 and $MAX_SLOTS entries"
        }

        val results = mockTestGenerator.generateAll(request.slots)
        call.respond(HttpStatusCode.OK, MockTestResponse(UUID.randomUUID().toString(), results))
    }
}
