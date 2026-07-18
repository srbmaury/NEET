package com.neet.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AnsweredQuestionEntity::class,
        MockTestSessionEntity::class,
        MockTestQuestionEntity::class,
        RevisionEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class NeetDatabase : RoomDatabase() {
    abstract fun answerDao(): AnswerDao
    abstract fun mockTestDao(): MockTestDao
    abstract fun revisionDao(): RevisionDao
}
