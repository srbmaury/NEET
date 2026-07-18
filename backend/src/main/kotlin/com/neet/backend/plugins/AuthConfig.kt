package com.neet.backend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.neet.backend.config.AppConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val AUTH_JWT = "auth-jwt"

@OptIn(ExperimentalUuidApi::class)
fun JWTPrincipal.userId(): Uuid = Uuid.parse(payload.getClaim("userId").asString())

fun Application.configureAuth(config: AppConfig) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = config.jwtIssuer
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwtSecret))
                    .withIssuer(config.jwtIssuer)
                    .withAudience(config.jwtAudience)
                    .build(),
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (!userId.isNullOrBlank()) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
