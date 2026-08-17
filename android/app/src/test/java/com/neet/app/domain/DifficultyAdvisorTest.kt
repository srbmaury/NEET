package com.neet.app.domain

import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Difficulty
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals

class DifficultyAdvisorTest {

    @Test
    fun `strong topics never receive easy questions`() {
        val strongTopic = TopicStat("PHYSICS", "Kinematics", total = 10, correct = 8)

        repeat(30) { seed ->
            val difficulty = suggestedDifficulty(strongTopic, Random(seed))
            assertEquals(false, difficulty == Difficulty.EASY)
        }
    }

    @Test
    fun `insufficient history uses the default weighted distribution`() {
        val sparseTopic = TopicStat("PHYSICS", "Kinematics", total = 2, correct = 2)

        assertEquals(
            suggestedDifficulty(null, Random(0)),
            suggestedDifficulty(sparseTopic, Random(0)),
        )
    }
}
