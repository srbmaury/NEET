package com.neet.backend

import com.neet.backend.plugins.configureHttp
import com.neet.backend.plugins.configureSerialization
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionRoutesTest {

    @Test
    fun `unknown route returns 404`() = testApplication {
        application {
            configureSerialization()
            configureHttp()
            routing { }
        }
        val response = client.get("/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
