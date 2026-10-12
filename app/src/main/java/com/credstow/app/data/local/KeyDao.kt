package com.credstow.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data-access surface. Declared as a plain interface so the Kotlin/JVM unit-test
 * fake and the Room-generated implementation both implement the same contract.
 */
@Dao
interface KeyDao {

    @Query("SELECT * FROM vault_keys ORDER BY created_at DESC")
    suspend fun getAll(): List<KeyEntity>

    @Query("SELECT * FROM vault_keys ORDER BY created_at DESC")
    fun observeAll(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM vault_keys ORDER BY :sortBy DESC")
    fun observeAllSorted(sortBy: String): Flow<List<KeyEntity>>

    @Query("SELECT * FROM vault_keys WHERE id = :id")
    suspend fun getById(id: String): KeyEntity?

    @Query("SELECT COUNT(*) FROM vault_keys")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KeyEntity)

    @Update
    suspend fun update(entity: KeyEntity)

    @Query("UPDATE vault_keys SET expiry_date = :expiryDate, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateExpiryDate(id: String, expiryDate: Long?, updatedAt: Long)

    @Query("UPDATE vault_keys SET last_used_at = :lastUsedAt WHERE id = :id")
    suspend fun updateLastUsed(id: String, lastUsedAt: Long)

    @Query("DELETE FROM vault_keys WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_keys")
    suspend fun clearAll(): Int
}

/** Sort column identifiers — matched against KeyEntity column names in SQL ORDER BY. */
enum class SortBy(val column: String) {
    CreatedAt("created_at"),
    LastUsedAt("last_used_at"),
    ExpiryDate("expiry_date"),
    Label("label"),
    ;
}
