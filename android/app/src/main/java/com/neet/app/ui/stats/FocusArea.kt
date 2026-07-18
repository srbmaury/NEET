package com.neet.app.ui.stats

import com.neet.app.data.model.Subject
import com.neet.app.domain.Weightage

enum class FocusStatus { NOT_ATTEMPTED, WEAK }

/** A topic worth prioritizing: either never practiced, or practiced with weak accuracy. */
data class FocusArea(
    val subject: Subject,
    val topic: String,
    val weightage: Weightage,
    val status: FocusStatus,
    val accuracyPercent: Int?,
)
