package com.neet.app.domain

import com.neet.app.data.model.Difficulty
import com.neet.app.data.model.MockTestSlotRequest
import com.neet.app.data.model.Subject
import com.neet.app.data.model.TestSection

private const val SECTION_A_COUNT = 35
private const val SECTION_B_COUNT = 15

/** Builds the 200 client-side slots for a full NEET mock test: 4 subjects x (35 Section-A +
 * 15 Section-B), topics weighted toward HIGH/MEDIUM (pool weight 3/2/1) via [TopicCatalog],
 * fixed 20% EASY / 60% MEDIUM / 20% HARD difficulty split. */
object MockTestBuilder {

    fun buildSlots(random: kotlin.random.Random = kotlin.random.Random): List<MockTestSlotRequest> =
        Subject.entries.flatMap { subject -> buildSubjectSlots(subject, random) }

    private fun buildSubjectSlots(subject: Subject, random: kotlin.random.Random): List<MockTestSlotRequest> {
        val totalCount = SECTION_A_COUNT + SECTION_B_COUNT
        val topics = weightedTopicPool(subject).shuffled(random)
        val difficulties = difficultySequence(totalCount).shuffled(random)

        return (0 until totalCount).map { i ->
            val section = if (i < SECTION_A_COUNT) TestSection.A else TestSection.B
            val sectionIndex = if (i < SECTION_A_COUNT) i else i - SECTION_A_COUNT
            MockTestSlotRequest(
                subject = subject,
                topic = topics[i % topics.size],
                difficulty = difficulties[i],
                section = section,
                sectionIndex = sectionIndex,
            )
        }
    }

    private fun weightedTopicPool(subject: Subject): List<String> =
        TopicCatalog.topicsFor(subject).flatMap { topic ->
            val weight = when (TopicCatalog.weightageOf(subject, topic)) {
                Weightage.HIGH -> 3
                Weightage.MEDIUM -> 2
                Weightage.LOW -> 1
            }
            List(weight) { topic }
        }

    private fun difficultySequence(count: Int): List<Difficulty> {
        val easyCount = (count * 0.2).toInt()
        val hardCount = (count * 0.2).toInt()
        val mediumCount = count - easyCount - hardCount
        return List(easyCount) { Difficulty.EASY } +
            List(mediumCount) { Difficulty.MEDIUM } +
            List(hardCount) { Difficulty.HARD }
    }
}
