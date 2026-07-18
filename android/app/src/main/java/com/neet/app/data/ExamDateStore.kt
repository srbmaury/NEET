package com.neet.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.examDataStore by preferencesDataStore(name = "exam")

/** A NEET exam date isn't sensitive like [com.neet.app.data.auth.SecureTokenStore]'s auth
 * token — stored as a plain epoch-day in DataStore Preferences, no encryption needed. */
class ExamDateStore(private val context: Context) {

    private val examDateKey = longPreferencesKey("exam_date_epoch_day")

    suspend fun saveExamDate(epochDay: Long) {
        context.examDataStore.edit { it[examDateKey] = epochDay }
    }

    fun examDateFlow(): Flow<Long?> = context.examDataStore.data.map { it[examDateKey] }

    suspend fun clear() {
        context.examDataStore.edit { it.clear() }
    }
}
