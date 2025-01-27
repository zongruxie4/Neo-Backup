package com.machiav3lli.backup.data.entity

import com.machiav3lli.backup.data.dbs.entity.Schedule

data class MainState(
    val packages: List<Package> = emptyList(),
    val filteredPackages: List<Package> = emptyList(),
    val blocklist: Set<String> = emptySet(),
    val schedules: List<Schedule> = emptyList(),
    val searchQuery: String = "",
    val sortFilter: SortFilterModel = SortFilterModel(),
    val selection: Set<String> = emptySet()
)