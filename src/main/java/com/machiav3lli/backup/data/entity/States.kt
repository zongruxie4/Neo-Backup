package com.machiav3lli.backup.data.entity

data class MainState(
    val packages: List<Package> = emptyList(),
    val filteredPackages: List<Package> = emptyList(),
    val updatedPackages: List<Package> = emptyList(),
    val blocklist: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortFilter: SortFilterModel = SortFilterModel(),
    val selection: Set<String> = emptySet()
)