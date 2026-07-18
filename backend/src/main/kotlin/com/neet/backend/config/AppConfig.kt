package com.neet.backend.config

import io.github.cdimascio.dotenv.dotenv

data class AppConfig(
    val openAiApiKey: String,
    val openAiModel: String,
    val port: Int,
    val mockTestBatchConcurrency: Int,
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

            return AppConfig(
                openAiApiKey = apiKey,
                openAiModel = env("OPENAI_MODEL") ?: "gpt-4.1-mini",
                port = env("PORT")?.toIntOrNull() ?: 8080,
                mockTestBatchConcurrency = env("MOCK_TEST_BATCH_CONCURRENCY")?.toIntOrNull() ?: 4,
            )
        }
    }
}
