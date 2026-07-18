package com.neet.app.data

import java.io.IOException

sealed interface SyncResult {
    data class Success(val restoreResult: RestoreResult) : SyncResult
    data class Failure(val message: String) : SyncResult
}

/**
 * Cross-device sync built entirely on top of the existing backup/restore data shape
 * ([HistoryRepository.buildBackupPayload]/[HistoryRepository.applyBackupPayload]) — sync is just
 * that same snapshot pushed to/pulled from the server automatically instead of a manually saved
 * file. v1 is full-snapshot push-then-pull, not incremental delta sync: this app has one student
 * on their own account, so "new phone" is the realistic scenario, not concurrent multi-device
 * edits — id-based non-destructive merge (already used for restore) covers it without needing
 * conflict-resolution machinery nobody asked for.
 */
class SyncRepository(
    private val syncApiService: SyncApiService,
    private val historyRepository: HistoryRepository,
) {
    suspend fun push(): SyncResult = runSyncCall {
        syncApiService.push(historyRepository.buildBackupPayload())
        RestoreResult(0, 0)
    }

    suspend fun pull(): SyncResult = runSyncCall {
        historyRepository.applyBackupPayload(syncApiService.pull())
    }

    /** Push whatever's local (covers edits since last sync), then pull (covers edits made on
     * another device since last sync) — order doesn't cause data loss either direction since
     * merge is additive-by-id, not last-write-wins. */
    suspend fun syncNow(): SyncResult {
        val pushResult = push()
        if (pushResult is SyncResult.Failure) return pushResult
        return pull()
    }

    private suspend inline fun runSyncCall(call: () -> RestoreResult): SyncResult {
        return try {
            SyncResult.Success(call())
        } catch (e: IOException) {
            SyncResult.Failure("Couldn't reach the server. Check your connection and try again.")
        } catch (e: retrofit2.HttpException) {
            SyncResult.Failure("Sync failed. Please try again.")
        }
    }
}
