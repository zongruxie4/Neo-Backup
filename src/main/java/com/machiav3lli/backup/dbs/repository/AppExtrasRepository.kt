package com.machiav3lli.backup.dbs.repository

import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.entity.AppExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AppExtrasRepository(
    private val db: ODatabase
) {
    fun getAllFlow() = db.getAppExtrasDao().getAllFlow()
        .flowOn(Dispatchers.IO)

    fun getAll() = db.getAppExtrasDao().getAll()

    fun getFlow(packageName: Flow<String?>) = packageName.flatMapLatest {
        db.getAppExtrasDao().getFlow(it)
    }.flowOn(Dispatchers.IO)

    suspend fun replaceExtras(packageName: String, appExtras: AppExtras?) {
        withContext(Dispatchers.IO) {
            if (appExtras != null) db.getAppExtrasDao().upsert(appExtras)
            else db.getAppExtrasDao().deleteByPackageName(packageName)
        }
    }
}