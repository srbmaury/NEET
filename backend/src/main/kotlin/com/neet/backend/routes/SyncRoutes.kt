package com.neet.backend.routes

import com.neet.backend.db.SyncRepository
import com.neet.backend.model.SyncPayload
import com.neet.backend.plugins.AUTH_JWT
import com.neet.backend.plugins.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.syncRoutes(syncRepository: SyncRepository) {
    authenticate(AUTH_JWT) {
        post("/sync/push") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            val payload = call.receive<SyncPayload>()
            syncRepository.push(userId, payload)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/sync/pull") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            call.respond(syncRepository.pull(userId))
        }
    }
}
