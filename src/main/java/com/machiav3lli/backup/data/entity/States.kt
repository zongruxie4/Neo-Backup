package com.machiav3lli.backup.data.entity

import com.machiav3lli.backup.data.dbs.entity.Schedule

data class MainState(
    val packages: List<Package> = emptyList(),
    val filteredPackages: List<Package> = emptyList(),
    val updatedPackages: List<Package> = emptyList(),
    val blocklist: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortFilter: SortFilterModel = SortFilterModel(),
    val selection: Set<String> = emptySet(),
)

data class SchedulerState(
    val schedules: List<Schedule> = emptyList(),
    val blocklist: Set<String> = emptySet(),
    val tagsMap: Map<String, Set<String>> = emptyMap(),
    val tagsList: Set<String> = emptySet(),
)

data class ScheduleState(
    val schedule: Schedule? = null,
    val blockList: Set<String> = emptySet(),
    val customList: Set<String> = emptySet(),
    val globalBlockList: Set<String> = emptySet(),
    val tagsMap: Map<String, Set<String>> = emptyMap(),
    val tagsList: Set<String> = emptySet(),
)