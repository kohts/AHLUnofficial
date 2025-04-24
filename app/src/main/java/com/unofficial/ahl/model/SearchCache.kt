package com.unofficial.ahl.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.unofficial.ahl.database.DateConverter
import java.util.Date

/**
 * Entity representing a cached search result from the API
 */
@Entity(tableName = "search_cache")
data class SearchCache(
    @PrimaryKey
    val searchTerm: String,
    
    // Stores the full JSON response from the API
    val apiResponse: String,
    
    // Timestamp when this cache entry was created
    val timestamp: Date
) 