package com.unofficial.ahl.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unofficial.ahl.model.SearchCache

/**
 * Data Access Object for the SearchCache table
 */
@Dao
interface SearchCacheDao {
    
    /**
     * Get a cached search result by search term
     * @param searchTerm The normalized search term to find
     * @return The matching cache entry if found
     */
    @Query("SELECT * FROM search_cache WHERE searchTerm = :searchTerm")
    suspend fun getCachedSearch(searchTerm: String): SearchCache?
    
    /**
     * Insert a new cache entry or replace existing one
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(searchCache: SearchCache)
    
    /**
     * Delete old cache entries that are older than the given timestamp
     * @param timestamp Entries older than this will be deleted
     * @return The number of entries deleted
     */
    @Query("DELETE FROM search_cache WHERE timestamp < :timestamp")
    suspend fun deleteOldEntries(timestamp: Long): Int
} 