package com.machiav3lli.backup.data.repository

import com.machiav3lli.backup.PACKAGES_LIST_GLOBAL_ID
import com.machiav3lli.backup.data.dbs.dao.BlocklistDao
import com.machiav3lli.backup.data.dbs.entity.Blocklist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BlocklistRepository(
    private val dao: BlocklistDao
) {
    fun getBlocklist(): Flow<Set<String>> =
        dao.getAllFlow()
            .map { it.mapNotNull { item -> item.packageName }.toSet() }

    fun getGlobalBlocklist(): Flow<Set<String>> =
        dao.getGlobalFlow()
            .map { it.mapNotNull { item -> item.packageName }.toSet() }

    suspend fun loadGlobalBlocklistOf() =
        dao.getBlocklistedPackages(PACKAGES_LIST_GLOBAL_ID)

    suspend fun addToGlobalBlocklist(packageName: String) {
        dao.insert(
            Blocklist.Builder()
                .withId(0)
                .withBlocklistId(PACKAGES_LIST_GLOBAL_ID)
                .withPackageName(packageName)
                .build()
        )
    }

    suspend fun updateGlobalBlocklist(packages: Set<String>) {
        dao.deleteById(PACKAGES_LIST_GLOBAL_ID)
        packages.forEach { packageName ->
            dao.insert(
                Blocklist.Builder()
                    .withId(0)
                    .withBlocklistId(PACKAGES_LIST_GLOBAL_ID)
                    .withPackageName(packageName)
                    .build()
            )
        }
    }
}