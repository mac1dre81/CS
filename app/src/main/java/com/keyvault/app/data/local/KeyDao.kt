package com.keyvault.app.data.local

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

    @Query("SELECT * FROM vault_keys WHERE id = :id")
    suspend fun getById(id: String): KeyEntity?

    @Query("SELECT COUNT(*) FROM vault_keys")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KeyEntity)

    @Update
    suspend fun update(entity: KeyEntity)

    @Query("DELETE FROM vault_keys WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_keys")
    suspend fun clearAll(): Int
}
