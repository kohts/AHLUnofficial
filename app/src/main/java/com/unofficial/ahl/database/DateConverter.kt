package com.unofficial.ahl.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Converter class to store Date objects in Room database
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
} 