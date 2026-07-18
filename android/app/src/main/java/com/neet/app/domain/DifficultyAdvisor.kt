package com.neet.app.domain

import com.neet.app.data.local.TopicStat
import com.neet.app.data.model.Difficulty
import kotlin.random.Random

/**
 * Difficulty is no longer a manual chip the student picks — most people default to Easy and
 * plateau there. Instead each practice question's difficulty is drawn from a weighted-random
 * distribution biased so Medium+Hard always outweigh Easy, shifted further toward Hard as the
 * student's recent accuracy on that topic improves. Easy is never eliminated (even strong topics
 * get an occasional easy warm-up) but it's never the majority weight either.
 */
fun suggestedDifficulty(stat: TopicStat?, random: Random = Random.Default): Difficulty {
    val accuracy = stat?.takeIf { it.total >= 3 }?.let { it.correct * 100 / it.total }
    val weights = when {
        accuracy == null -> listOf(Difficulty.EASY to 1, Difficulty.MEDIUM to 3, Difficulty.HARD to 2)
        accuracy < 40 -> listOf(Difficulty.EASY to 2, Difficulty.MEDIUM to 3, Difficulty.HARD to 1)
        accuracy < 70 -> listOf(Difficulty.EASY to 1, Difficulty.MEDIUM to 3, Difficulty.HARD to 2)
        else -> listOf(Difficulty.EASY to 0, Difficulty.MEDIUM to 2, Difficulty.HARD to 4)
    }
    return weightedRandomPick(weights, random)
}

private fun <T> weightedRandomPick(weights: List<Pair<T, Int>>, random: Random): T {
    val total = weights.sumOf { it.second }
    var roll = random.nextInt(total)
    for ((value, weight) in weights) {
        if (roll < weight) return value
        roll -= weight
    }
    return weights.last().first
}
