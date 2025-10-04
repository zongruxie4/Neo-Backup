package com.machiav3lli.backup.data.dbs.repository

import com.machiav3lli.backup.PACKAGES_LIST_GLOBAL_ID
import com.machiav3lli.backup.data.dbs.DB
import com.machiav3lli.backup.data.dbs.entity.Blocklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class BlocklistRepository(
    private val db: DB
) {
    private val cc = Dispatchers.IO

    fun getBlocklistFlow(): Flow<Set<String>> =
        db.getBlocklistDao().getAllFlow()
            .map { it.mapNotNull { item -> item.packageName }.toSet() }
            .flowOn(cc)

    suspend fun getBlocklistedPackages(blocklistId: Long) =
        db.getBlocklistDao().getBlocklistedPackages(blocklistId)

    suspend fun addToBlocklist(packageName: String) {
        db.getBlocklistDao().insert(
            Blocklist.Builder()
                .withId(0)
                .withBlocklistId(PACKAGES_LIST_GLOBAL_ID)
                .withPackageName(packageName)
                .build()
        )
    }

    suspend fun updateBlocklist(packages: Set<String>) {
        db.getBlocklistDao().deleteById(PACKAGES_LIST_GLOBAL_ID)
        packages.forEach { packageName ->
            db.getBlocklistDao().insert(
                Blocklist.Builder()
                    .withId(0)
                    .withBlocklistId(PACKAGES_LIST_GLOBAL_ID)
                    .withPackageName(packageName)
                    .build()
            )
        }
    }
}