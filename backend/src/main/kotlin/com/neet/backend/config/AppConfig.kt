package com.neet.backend.config

import io.github.cdimascio.dotenv.dotenv

data class AppConfig(
    val openAiApiKey: String,
    val openAiModel: String,
    val port: Int,
    val mockTestBatchConcurrency: Int,
    val databaseUrl: String,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
) {
    companion object {
        fun load(): AppConfig {
            val dotenv = dotenv {
                directory = "./"
                ignoreIfMissing = true
            }

            fun env(key: String): String? = System.getenv(key) ?: dotenv[key]

            val apiKey = env("OPENAI_API_KEY")
                ?.takeIf { it.isNotBlank() }
                ?: error(
                    "OPENAI_API_KEY is not set. Copy backend/.env.example to backend/.env " +
                        "and fill in your own OpenAI API key."
                )

            val databaseUrl = env("DATABASE_URL")
                ?.takeIf { it.isNotBlank() }
                ?: error(
                    "DATABASE_URL is not set. Add your NeonDB connection string to backend/.env " +
                        "(the postgresql://... string from the Neon dashboard works as-is)."
                )

            val jwtSecret = env("JWT_SECRET")
                ?.takeIf { it.isNotBlank() }
                ?: error(
                    "JWT_SECRET is not set. Add a long random string to backend/.env — this " +
                        "signs auth tokens, so treat it like a password."
                )

            return AppConfig(
                openAiApiKey = apiKey,
                openAiModel = env("OPENAI_MODEL") ?: "gpt-4.1-mini",
                port = env("PORT")?.toIntOrNull() ?: 8080,
                mockTestBatchConcurrency = env("MOCK_TEST_BATCH_CONCURRENCY")?.toIntOrNull() ?: 4,
                databaseUrl = databaseUrl,
                jwtSecret = jwtSecret,
                jwtIssuer = env("JWT_ISSUER") ?: "neet-backend",
                jwtAudience = env("JWT_AUDIENCE") ?: "neet-app",
            )
        }
    }
}
