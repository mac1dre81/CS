package com.keyvault.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** Single-table Room database for vault key metadata. Version 1, no migration yet. */
@Database(entities = [KeyEntity::class], version = 1, exportSchema = false)
abstract class KeyDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
}
