package com.machiav3lli.backup.data.dbs.repository

import android.app.Application
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.DB
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.ExportsHandler
import com.machiav3lli.backup.manager.handler.showNotification
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.utils.SystemUtils

class ExportsRepository(
    val handler: ExportsHandler,
    private val db: DB,
    private val appContext: Application,
) {
    suspend fun recreateExports() = handler.readExports()
    suspend fun exportSchedules() = handler.exportSchedules()

    suspend fun import(schedule: Schedule) {
        db.getScheduleDao().insert(
            Schedule.Builder() // Set id to 0 to make the database generate a new id
                .withId(0)
                .import(schedule)
                .build()
        )
        showNotification(
            appContext, NeoActivity::class.java, SystemUtils.now.toInt(),
            appContext.getString(R.string.sched_imported), schedule.name, false
        )
    }

    suspend fun delete(exportFile: StorageFile) = exportFile.delete()
}