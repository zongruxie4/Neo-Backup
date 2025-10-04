package com.machiav3lli.backup.data.dbs.repository

import android.app.Application
import com.machiav3lli.backup.data.dbs.Converters
import com.machiav3lli.backup.data.dbs.DB
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.preferences.traceSchedule
import com.machiav3lli.backup.manager.tasks.ScheduleWork
import com.machiav3lli.backup.utils.cancelScheduleAlarm
import com.machiav3lli.backup.utils.scheduleNextAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleRepository(
    private val db: DB,
    private val appContext: Application,
) {
    private val cc = Dispatchers.IO

    fun getAllFlow() = db.getScheduleDao().getAllFlow()
        .flowOn(cc)

    suspend fun getAll() = db.getScheduleDao().getAll()

    fun getScheduleFlow(id: Flow<Long>) = id.flatMapLatest {
        db.getScheduleDao().getByIdFlow(it)
    }.flowOn(cc)

    suspend fun getSchedule(id: Long) = db.getScheduleDao().getById(id)

    suspend fun getSchedule(name: String) = db.getScheduleDao().getByName(name)

    fun getCustomListFlow(id: Flow<Long>): Flow<Set<String>> = id.flatMapLatest {
        db.getScheduleDao().getCustomListFlow(it)
            .map { string -> Converters().toStringSet(string) }
    }.flowOn(cc)

    fun getBlockListFlow(id: Flow<Long>): Flow<Set<String>> = id.flatMapLatest {
        db.getScheduleDao().getBlockListFlow(it)
            .map { string -> Converters().toStringSet(string) }
    }.flowOn(cc)

    suspend fun addNew(withSpecial: Boolean) {
        db.getScheduleDao().insert(
            Schedule.Builder() // Set id to 0 to make the database generate a new id
                .withId(0)
                .withSpecial(withSpecial)
                .build()
        )
    }

    suspend fun update(value: Schedule) = db.getScheduleDao().update(value)

    suspend fun updateSchedule(schedule: Schedule, rescheduleBoolean: Boolean) {
        db.getScheduleDao().update(schedule)
        if (schedule.enabled) {
            traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> ${if (rescheduleBoolean) "re-" else ""}schedule" }
            scheduleNextAlarm(
                appContext,
                schedule.id,
                rescheduleBoolean
            )
        } else {
            traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> cancelAlarm" }
            cancelScheduleAlarm(appContext, schedule.id, schedule.name)
            ScheduleWork.cancel(schedule.id)
        }
    }

    suspend fun deleteById(id: Long) {
        db.getScheduleDao().deleteById(id)
        ScheduleWork.cancel(id)
    }
}

