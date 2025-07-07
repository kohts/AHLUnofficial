package com.unofficial.ahl.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.unofficial.ahl.database.DateConverter
import java.util.Date

/**
 * Entity representing cached detailed word data from the Hebrew Academy AJAX API
 * Stores the complex JSON response containing etymology, historical references, and related content
 */
@Entity(tableName = "daf_mila_cache")
data class DafMilaCache(
    @PrimaryKey
    val keyword: String,
    
    // Stores the full JSON response from the AJAX API
    // Contains MillonHaHoveList, ErekhHismagList, related_posts, etc.
    val apiResponse: String,
    
    // Timestamp when this cache entry was created
    val timestamp: Date
) 