package com.unofficial.ahl.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity class representing a search history entry
 */
@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * The search term that was entered by the user
     */
    val searchTerm: String,
    
    /**
     * The timestamp when this search was performed
     */
    val timestamp: Date
) 