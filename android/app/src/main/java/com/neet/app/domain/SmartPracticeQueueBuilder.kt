package com.neet.app.domain

import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Subject

private const val WEAK_ACCURACY_THRESHOLD = 60
private const val SMART_PRACTICE_QUEUE_SIZE = 10

private data class Candidate(
    val subject: Subject,
    val topic: String,
    val weightageRank: Int,
    val statusRank: Int,
    val accuracyPercent: Int?,
)

/**
 * Ranks every topic across all 4 subjects — unlike the Progress tab's Focus Areas, which is
 * filtered to whichever subject is currently selected — by weightage, then attempted/weak status,
 * then accuracy, and takes the top 10. A deliberate sibling to that ranking rather than a reuse of
 * it, since bending its subject-filtered contract to also serve an all-subjects caller would make
 * it harder to reason about either use.
 */
fun buildSmartPracticeQueue(topicStats: List<TopicStat>): List<Pair<Subject, String>> {
    val statsByKey = topicStats.associateBy { it.subject to it.topic }

    return TopicCatalog.allTopics().mapNotNull { (subject, topic) ->
        val stat = statsByKey[subject.name to topic]
        val accuracyPercent = stat?.takeIf { it.total > 0 }?.let { it.correct * 100 / it.total }
        val statusRank = when {
            stat == null || stat.total == 0 -> 2 // not attempted
            accuracyPercent != null && accuracyPercent < WEAK_ACCURACY_THRESHOLD -> 1 // weak
            else -> return@mapNotNull null // already solid here — nothing to gain by re-queuing
        }
        Candidate(subject, topic, weightageRank(TopicCatalog.weightageOf(subject, topic)), statusRank, accuracyPercent)
    }.sortedWith(
        compareByDescending<Candidate> { it.weightageRank }
            .thenByDescending { it.statusRank }
            .thenBy { it.accuracyPercent ?: -1 },
    ).take(SMART_PRACTICE_QUEUE_SIZE)
        .map { it.subject to it.topic }
}

private fun weightageRank(weightage: Weightage): Int = when (weightage) {
    Weightage.HIGH -> 3
    Weightage.MEDIUM -> 2
    Weightage.LOW -> 1
}
