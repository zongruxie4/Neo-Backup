package com.machiav3lli.backup.data.entity

import com.machiav3lli.backup.data.dbs.entity.Backup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupsCache {
    private val dispatcher = Dispatchers.Default
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val backupsMap = MutableStateFlow(mapOf<String, List<Backup>>())

    fun observeBackupsMap(): Flow<Map<String, List<Backup>>> = backupsMap
        .debounce { 500L }
        .flowOn(dispatcher)

    fun observeBackupsList(): Flow<List<Backup>> =
        backupsMap.map { it.values.flatten() }
            .flowOn(dispatcher)

    fun observeBackups(packageName: String): Flow<List<Backup>> =
        backupsMap.map { it[packageName] ?: emptyList() }
            .flowOn(dispatcher)

    fun getBackups(packageName: String): List<Backup> =
        backupsMap.value[packageName] ?: emptyList()

    fun getBackupsMap(): Map<String, List<Backup>> = backupsMap.value

    fun getBackupsList(): List<Backup> = backupsMap.value.values.flatten()

    // SYNC SETTERS
    suspend fun updateBackups(packageName: String, backups: List<Backup>) =
        withContext(dispatcher) {
            backupsMap.update {
                it.toMutableMap().apply {
                    put(packageName, backups)
                }
            }
        }

    suspend fun replaceAll(backups: List<Backup>) = withContext(dispatcher) {
        backupsMap.update { backups.groupBy { it.packageName } }
    }

    suspend fun remove(packageName: String) = withContext(dispatcher) {
        backupsMap.update {
            it.toMutableMap().apply {
                remove(packageName)
            }
        }
    }

    suspend fun removeAll(packageNames: List<String>) = withContext(dispatcher) {
        backupsMap.update {
            it.toMutableMap().apply {
                for (pn in packageNames) {
                    remove(pn)
                }
            }
        }
    }

    suspend fun clear() = withContext(dispatcher) {
        backupsMap.update { emptyMap() }
    }

    // ASYNC SETTERS
    fun updateBackupsAsync(packageName: String, backups: List<Backup>) = scope.launch {
        backupsMap.update {
            it.toMutableMap().apply {
                put(packageName, backups)
            }
        }
    }

    fun replaceAllAsync(backups: List<Backup>) = scope.launch {
        backupsMap.update { backups.groupBy { it.packageName } }
    }

    fun removeAsnyc(packageName: String) = scope.launch {
        backupsMap.update {
            it.toMutableMap().apply {
                remove(packageName)
            }
        }
    }

    fun removeAllAsync(packageNames: List<String>) = scope.launch {
        backupsMap.update {
            it.toMutableMap().apply {
                for (pn in packageNames) {
                    remove(pn)
                }
            }
        }
    }

    fun clearAsync() = scope.launch {
        backupsMap.update { emptyMap() }
    }

    // SPECIAL GETTERS
    fun hasBackups(packageName: String): Boolean = backupsMap.value.containsKey(packageName)

    fun getBackupsCount(): Int = backupsMap.value.values.sumOf { it.size }

    fun getPackagesWithBackups(): Set<String> = backupsMap.value.keys
}