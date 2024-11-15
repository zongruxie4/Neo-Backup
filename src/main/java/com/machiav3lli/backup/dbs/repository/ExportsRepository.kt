package com.machiav3lli.backup.dbs.repository

import android.app.Application
import com.machiav3lli.backup.R
import com.machiav3lli.backup.activities.MainActivityX
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.entity.Schedule
import com.machiav3lli.backup.entity.StorageFile
import com.machiav3lli.backup.handler.ExportsHandler
import com.machiav3lli.backup.handler.showNotification
import com.machiav3lli.backup.utils.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportsRepository(
    val handler: ExportsHandler,
    private val db: ODatabase,
    private val appContext: Application,
) {
    suspend fun recreateExports() = withContext(Dispatchers.IO) {
        handler.readExports()
    }

    suspend fun exportSchedules() {
        withContext(Dispatchers.IO) {
            handler.exportSchedules()
        }
    }

    suspend fun import(schedule: Schedule) {
        withContext(Dispatchers.IO) {
            db.getScheduleDao().insert(
                Schedule.Builder() // Set id to 0 to make the database generate a new id
                    .withId(0)
                    .import(schedule)
                    .build()
            )
        }
        showNotification(
            appContext, MainActivityX::class.java, SystemUtils.now.toInt(),
            appContext.getString(R.string.sched_imported), schedule.name, false
        )
    }

    suspend fun delete(exportFile: StorageFile) {
        withContext(Dispatchers.IO) {
            exportFile.delete()
        }
    }
}