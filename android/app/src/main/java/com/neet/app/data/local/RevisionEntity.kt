package com.neet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A manual, self-reported revision counter — deliberately separate from the automatic,
 * accuracy-based tracking in AnsweredQuestionEntity/TopicStat. [subject] is null for topics the
 * student typed in themselves (not part of the fixed NEET syllabus in TopicCatalog); [topic]
 * itself is the primary key since real NEET topic names are already unique across subjects. */
@Entity(tableName = "revisions")
data class RevisionEntity(
    @PrimaryKey val topic: String,
    val subject: String?,
    val revisionCount: Int,
)
