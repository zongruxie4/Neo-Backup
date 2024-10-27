/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.dao.ScheduleDao
import com.machiav3lli.backup.dbs.entity.AppExtras
import com.machiav3lli.backup.dbs.entity.Schedule
import com.machiav3lli.backup.preferences.traceSchedule
import com.machiav3lli.backup.utils.TraceUtils.trace
import com.machiav3lli.backup.utils.cancelAlarm
import com.machiav3lli.backup.utils.scheduleAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

class ScheduleVM(
    val id: Long,
    database: ODatabase,
) : ViewModel(), KoinComponent {
    private val scheduleDB: ScheduleDao = database.getScheduleDao()

    val schedule: StateFlow<Schedule?> = scheduleDB.getScheduleFlow(id)
        //TODO hg42 .trace { "*** schedule <<- ${it}" }     // what can here be null? (something is null that is not declared as nullable)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            Schedule(0)
        )
    val customList = scheduleDB.getCustomListFlow(id)
    val blockList = scheduleDB.getBlockListFlow(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTags =
        //------------------------------------------------------------------------------------------ allTags
        database.getAppExtrasDao().getAllFlow()
            .mapLatest { it.flatMap(AppExtras::customTags) }
            .trace { "*** tags <<- ${it.size}" }
            .stateIn(
                viewModelScope + Dispatchers.IO,
                SharingStarted.Eagerly,
                emptyList()
            )

    fun updateSchedule(schedule: Schedule?, rescheduleBoolean: Boolean) {
        viewModelScope.launch {
            schedule?.let { updateS(it, rescheduleBoolean) }
        }
    }

    private suspend fun updateS(schedule: Schedule, rescheduleBoolean: Boolean) {
        withContext(Dispatchers.IO) {
            scheduleDB.update(schedule)
            if (schedule.enabled) {
                traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> ${if (rescheduleBoolean) "re-" else ""}schedule" }
                scheduleAlarm(
                    getKoin().get(),
                    schedule.id,
                    rescheduleBoolean
                )
            } else {
                traceSchedule { "[$schedule.id] ScheduleViewModel.updateS -> cancelAlarm" }
                cancelAlarm(getKoin().get(), schedule.id)
            }
        }
    }

    fun deleteSchedule() {
        viewModelScope.launch {
            deleteS()
        }
    }

    private suspend fun deleteS() {
        withContext(Dispatchers.IO) {
            scheduleDB.deleteById(id)
        }
    }
}