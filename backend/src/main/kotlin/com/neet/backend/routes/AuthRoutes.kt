package com.neet.backend.routes

import com.neet.backend.auth.EmailAlreadyExistsException
import com.neet.backend.auth.InvalidCredentialsException
import com.neet.backend.auth.JwtService
import com.neet.backend.db.UserRepository
import com.neet.backend.model.AuthResponse
import com.neet.backend.model.LoginRequest
import com.neet.backend.model.SignupRequest
import com.neet.backend.plugins.AUTH_JWT
import com.neet.backend.plugins.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import org.mindrot.jbcrypt.BCrypt

fun Route.authRoutes(userRepository: UserRepository, jwtService: JwtService) {
    post("/auth/signup") {
        val request = call.receive<SignupRequest>()
        val email = request.email.trim().lowercase()
        require(email.isNotBlank() && email.contains("@")) { "A valid email is required" }
        require(request.password.length >= 8) { "Password must be at least 8 characters" }

        if (userRepository.findByEmail(email) != null) {
            throw EmailAlreadyExistsException("An account with this email already exists")
        }

        val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val userId = userRepository.create(email, passwordHash)
        call.respond(HttpStatusCode.Created, AuthResponse(jwtService.generateToken(userId), userId.toString()))
    }

    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val email = request.email.trim().lowercase()

        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException("Invalid email or password")
        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }

        call.respond(HttpStatusCode.OK, AuthResponse(jwtService.generateToken(user.id), user.id.toString()))
    }

    authenticate(AUTH_JWT) {
        delete("/auth/account") {
            userRepository.delete(call.principal<JWTPrincipal>()!!.userId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
