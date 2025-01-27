package com.machiav3lli.backup.data.dbs.repository

import com.machiav3lli.backup.data.dbs.DB
import com.machiav3lli.backup.data.dbs.entity.AppExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AppExtrasRepository(
    private val db: DB
) {
    private val cc = Dispatchers.IO
    private val jcc = Dispatchers.IO + SupervisorJob()

    fun getAllFlow() = db.getAppExtrasDao().getAllFlow()
        .flowOn(cc)

    suspend fun getAll() = withContext(jcc) {
        db.getAppExtrasDao().getAll()
    }

    fun getFlow(packageName: Flow<String?>) = packageName.flatMapLatest {
        db.getAppExtrasDao().getFlow(it)
    }.flowOn(cc)

    suspend fun replaceExtras(packageName: String, appExtras: AppExtras?) {
        withContext(jcc) {
            if (appExtras != null) db.getAppExtrasDao().upsert(appExtras)
            else db.getAppExtrasDao().deleteByPackageName(packageName)
        }
    }
}