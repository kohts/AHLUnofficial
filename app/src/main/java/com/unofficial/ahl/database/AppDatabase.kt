package com.unofficial.ahl.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unofficial.ahl.model.SearchCache
import com.unofficial.ahl.model.SearchHistory
import com.unofficial.ahl.model.DafMilaCache

/**
 * The Room database for this app
 */
@Database(entities = [SearchCache::class, SearchHistory::class, DafMilaCache::class], version = 3, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun searchCacheDao(): SearchCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun dafMilaCacheDao(): DafMilaCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Migration from version 1 to version 2
         * Adds the search_history table while preserving existing data
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new search_history table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `search_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`searchTerm` TEXT NOT NULL, " + 
                    "`timestamp` INTEGER NOT NULL)"
                )
            }
        }
        
        /**
         * Migration from version 2 to version 3
         * Adds the daf_mila_cache table for detailed word data
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new daf_mila_cache table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daf_mila_cache` (" +
                    "`keyword` TEXT PRIMARY KEY NOT NULL, " +
                    "`apiResponse` TEXT NOT NULL, " + 
                    "`timestamp` INTEGER NOT NULL)"
                )
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 