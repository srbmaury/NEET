package com.neet.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.neet.backend.config.AppConfig
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TOKEN_LIFETIME_DAYS = 90L

/**
 * A single long-lived token instead of refresh-token rotation — this is a solo/small-scale app,
 * so the rotation/revocation machinery a real multi-session product needs isn't justified here.
 * If a token expires, the user just logs in again.
 */
@OptIn(ExperimentalUuidApi::class)
class JwtService(private val config: AppConfig) {

    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    fun generateToken(userId: Uuid): String =
        JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withClaim("userId", userId.toString())
            .withExpiresAt(Date.from(Date().toInstant().plusMillis(TimeUnit.DAYS.toMillis(TOKEN_LIFETIME_DAYS))))
            .sign(algorithm)
}
