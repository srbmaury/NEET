package com.neet.backend

import com.neet.backend.auth.JwtService
import com.neet.backend.config.AppConfig
import com.neet.backend.db.NotesRepository
import com.neet.backend.db.SyncRepository
import com.neet.backend.db.UserRepository
import com.neet.backend.db.initDatabase
import com.neet.backend.openai.MockTestGenerator
import com.neet.backend.openai.OpenAiClient
import com.neet.backend.plugins.configureAuth
import com.neet.backend.plugins.configureHttp
import com.neet.backend.plugins.configureSerialization
import com.neet.backend.routes.authRoutes
import com.neet.backend.routes.mockTestRoutes
import com.neet.backend.routes.notesRoutes
import com.neet.backend.routes.questionRoutes
import com.neet.backend.routes.syncRoutes
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

fun main() {
    val config = AppConfig.load()
    initDatabase(config)

    embeddedServer(Netty, configure = {
        connector { port = config.port }
        responseWriteTimeoutSeconds = 120
    }) {
        configureSerialization()
        configureHttp()
        configureAuth(config)

        val openAi = OpenAiClient(config)
        val mockTestGenerator = MockTestGenerator(openAi, config.mockTestBatchConcurrency)
        val userRepository = UserRepository()
        val syncRepository = SyncRepository()
        val notesRepository = NotesRepository()
        val jwtService = JwtService(config)
        routing {
            questionRoutes(openAi)
            mockTestRoutes(mockTestGenerator)
            authRoutes(userRepository, jwtService)
            syncRoutes(syncRepository)
            notesRoutes(notesRepository, openAi)
        }
    }.start(wait = true)
}
