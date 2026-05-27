package com.r4bb1t.blockerspam.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {

    // ── Blocked Calls ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedCall(call: BlockedCall)

    @Query("""
        SELECT number, COUNT(*) as callCount, MAX(timestamp) as lastCallTime
        FROM blocked_calls
        GROUP BY number
        ORDER BY lastCallTime DESC
    """)
    fun getBlockedCallSummaries(): Flow<List<BlockedCallSummary>>

    @Query("SELECT * FROM blocked_calls WHERE number = :number ORDER BY timestamp DESC")
    fun getCallsForNumber(number: String): Flow<List<BlockedCall>>

    @Query("DELETE FROM blocked_calls WHERE number = :number")
    suspend fun deleteCallsForNumber(number: String)

    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE timestamp >= :since")
    fun getCountSince(since: Long): Flow<Int>

    // ── Whitelist ──────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelist(entry: WhitelistEntry)

    @Delete
    suspend fun deleteWhitelist(entry: WhitelistEntry)

    @Query("SELECT COUNT(*) > 0 FROM whitelist WHERE number = :number")
    suspend fun isWhitelisted(number: String): Boolean

    @Query("SELECT * FROM whitelist ORDER BY addedAt DESC")
    fun getAllWhitelisted(): Flow<List<WhitelistEntry>>
}
