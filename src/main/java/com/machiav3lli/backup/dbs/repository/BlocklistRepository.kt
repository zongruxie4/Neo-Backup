package com.machiav3lli.backup.dbs.repository

import com.machiav3lli.backup.PACKAGES_LIST_GLOBAL_ID
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.entity.Blocklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BlocklistRepository(
    private val db: ODatabase
) {
    private val cc = Dispatchers.IO
    private val jcc = Dispatchers.IO + SupervisorJob()

    fun getBlocklistFlow(): Flow<Set<String>> =
        db.getBlocklistDao().getAllFlow()
            .map { it.mapNotNull { item -> item.packageName }.toSet() }
            .flowOn(cc)

    suspend fun getBlocklistedPackages(blocklistId: Long) = withContext(jcc) {
        db.getBlocklistDao().getBlocklistedPackages(blocklistId)
    }

    suspend fun addToBlocklist(packageName: String) {
        withContext(jcc) {
            db.getBlocklistDao().insert(
                Blocklist.Builder()
                    .withId(0)
                    .withBlocklistId(PACKAGES_LIST_GLOBAL_ID)
                    .withPackageName(packageName)
                    .build()
            )
        }
    }

    suspend fun updateBlocklist(packages: Set<String>) {
        withContext(jcc) {
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
}