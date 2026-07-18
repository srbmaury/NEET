package com.neet.backend.plugins

import com.neet.backend.auth.EmailAlreadyExistsException
import com.neet.backend.auth.InvalidCredentialsException
import com.neet.backend.model.ErrorResponse
import com.neet.backend.openai.OpenAiException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureHttp() {
    install(CallLogging)

    install(CORS) {
        anyMethod()
        allowMethod(HttpMethod.Options)
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    install(StatusPages) {
        exception<JsonConvertException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_request", cause.message ?: "Malformed request body"),
            )
        }
        exception<OpenAiException> { call, cause ->
            call.respond(
                HttpStatusCode.BadGateway,
                ErrorResponse("generation_failed", cause.message ?: "Question generation failed"),
            )
        }
        exception<EmailAlreadyExistsException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("email_taken", cause.message ?: "An account with this email already exists"),
            )
        }
        exception<InvalidCredentialsException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("invalid_credentials", cause.message ?: "Invalid email or password"),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("invalid_request", cause.message ?: "Invalid request"),
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal_error", cause.message ?: "Unexpected error"),
            )
        }
    }
}
