package com.unofficial.ahl.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.unofficial.ahl.model.SearchHistory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the search_history table
 */
@Dao
interface SearchHistoryDao {
    /**
     * Insert a new search history entry
     * @param searchHistory The search history entry to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistory)
    
    /**
     * Get the most recent search history entries, limited by count
     * @param limit The maximum number of entries to return
     * @return A list of search history entries, ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 10): Flow<List<SearchHistory>>
    
    /**
     * Delete old search history entries, keeping only the most recent ones
     * @param keepCount The number of entries to keep
     */
    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun deleteOldSearches(keepCount: Int = 10)
    
    /**
     * Delete all search history entries
     */
    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
    
    /**
     * Check if a search term already exists in the history
     * @param searchTerm The search term to check
     * @return True if the search term exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM search_history WHERE searchTerm = :searchTerm)")
    suspend fun searchTermExists(searchTerm: String): Boolean
} 