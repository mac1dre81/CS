package com.credstow.app.data

import com.credstow.app.data.local.KeyDao
import com.credstow.app.data.local.KeyEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [KeyDao] used to exercise repository logic on the JVM. */
class FakeKeyDao : KeyDao {
    private val store = mutableMapOf<String, KeyEntity>()
    private val live = MutableStateFlow<List<KeyEntity>>(emptyList())

    private fun sorted(): List<KeyEntity> =
        store.values.sortedByDescending { it.createdAt }

    private fun push() { live.value = sorted() }

    override suspend fun getAll(): List<KeyEntity> = sorted()
    override fun observeAll(): Flow<List<KeyEntity>> = live
    override suspend fun getById(id: String): KeyEntity? = store[id]
    override suspend fun count(): Int = store.size
    override suspend fun insert(entity: KeyEntity) { store[entity.id] = entity; push() }
    override suspend fun update(entity: KeyEntity) { store[entity.id] = entity; push() }
    override suspend fun deleteById(id: String) { store.remove(id); push() }
    override suspend fun clearAll(): Int { val n = store.size; store.clear(); push(); return n }
}

