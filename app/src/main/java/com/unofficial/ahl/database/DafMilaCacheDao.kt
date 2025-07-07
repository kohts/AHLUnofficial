package com.unofficial.ahl.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unofficial.ahl.model.DafMilaCache

/**
 * Data Access Object for the DafMilaCache table
 * Manages cached detailed word data from the Hebrew Academy AJAX API
 */
@Dao
interface DafMilaCacheDao {
    
    /**
     * Get cached detailed word data by keyword
     * @param keyword The Hebrew word keyword to find
     * @return The matching cache entry if found
     */
    @Query("SELECT * FROM daf_mila_cache WHERE keyword = :keyword")
    suspend fun getCachedWordDetails(keyword: String): DafMilaCache?
    
    /**
     * Insert a new cache entry or replace existing one
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(dafMilaCache: DafMilaCache)
    
    /**
     * Delete old cache entries that are older than the given timestamp
     * @param timestamp Entries older than this will be deleted
     * @return The number of entries deleted
     */
    @Query("DELETE FROM daf_mila_cache WHERE timestamp < :timestamp")
    suspend fun deleteOldEntries(timestamp: Long): Int
    
    /**
     * Clear all cached entries
     * @return The number of entries deleted
     */
    @Query("DELETE FROM daf_mila_cache")
    suspend fun clearAllCache(): Int
} 