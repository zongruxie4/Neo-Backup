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

import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.dbs.entity.AppExtras
import com.machiav3lli.backup.dbs.entity.Schedule
import com.machiav3lli.backup.dbs.repository.AppExtrasRepository
import com.machiav3lli.backup.dbs.repository.ScheduleRepository
import com.machiav3lli.backup.utils.NeoViewModel
import com.machiav3lli.backup.utils.TraceUtils.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleVM(
    private val scheduleRepository: ScheduleRepository,
    appExtrasRepository: AppExtrasRepository,
) : NeoViewModel() {
    private val ioScope = viewModelScope.plus(Dispatchers.IO)
    private val _scheduleID = MutableStateFlow(-1L)

    val schedule: StateFlow<Schedule?> = scheduleRepository.getScheduleFlow(_scheduleID)
        //TODO hg42 .trace { "*** schedule <<- ${it}" }     // what can here be null? (something is null that is not declared as nullable)
        .stateIn(
            ioScope,
            SharingStarted.Eagerly,
            Schedule(0)
        )

    val customList = scheduleRepository.getCustomListFlow(_scheduleID)
    val blockList = scheduleRepository.getBlockListFlow(_scheduleID)

    val allTags =
        //------------------------------------------------------------------------------------------ allTags
        appExtrasRepository.getAllFlow()
            .mapLatest { it.flatMap(AppExtras::customTags) }
            .trace { "*** tags <<- ${it.size}" }
            .stateIn(
                ioScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    fun setSchedule(value: Long) {
        viewModelScope.launch { _scheduleID.update { value } }
    }

    fun updateSchedule(schedule: Schedule?, rescheduleBoolean: Boolean) {
        viewModelScope.launch {
            schedule?.let { scheduleRepository.updateSchedule(it, rescheduleBoolean) }
        }
    }

    fun deleteSchedule() {
        viewModelScope.launch {
            scheduleRepository.deleteById(_scheduleID.value)
        }
    }
}