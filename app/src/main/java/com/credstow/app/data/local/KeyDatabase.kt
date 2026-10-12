package com.credstow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Single-table Room database for vault key metadata.
 *
 * Schema JSON files are exported under app/schemas/ for version control and
 * migration verification. See [DatabaseMigrations] for the migration registry.
 */
@Database(entities = [KeyEntity::class], version = DatabaseMigrations.CURRENT_VERSION, exportSchema = true)
abstract class KeyDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
}

