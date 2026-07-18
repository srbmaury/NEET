package com.neet.backend

import com.neet.backend.config.AppConfig
import com.neet.backend.openai.MockTestGenerator
import com.neet.backend.openai.OpenAiClient
import com.neet.backend.plugins.configureHttp
import com.neet.backend.plugins.configureSerialization
import com.neet.backend.routes.mockTestRoutes
import com.neet.backend.routes.questionRoutes
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

fun main() {
    val config = AppConfig.load()

    embeddedServer(Netty, configure = {
        connector { port = config.port }
        responseWriteTimeoutSeconds = 120
    }) {
        configureSerialization()
        configureHttp()

        val openAi = OpenAiClient(config)
        val mockTestGenerator = MockTestGenerator(openAi, config.mockTestBatchConcurrency)
        routing {
            questionRoutes(openAi)
            mockTestRoutes(mockTestGenerator)
        }
    }.start(wait = true)
}
