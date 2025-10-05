/*
 * Neo Backup: open-source apps backup and restore app.
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
import com.machiav3lli.backup.MAIN_FILTER_DEFAULT
import com.machiav3lli.backup.MAIN_FILTER_DEFAULT_WITHOUT_SPECIAL
import com.machiav3lli.backup.STATEFLOW_SUBSCRIBE_BUFFER
import com.machiav3lli.backup.data.dbs.repository.BlocklistRepository
import com.machiav3lli.backup.data.dbs.repository.PackageRepository
import com.machiav3lli.backup.data.entity.MainState
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.data.preferences.NeoPrefs
import com.machiav3lli.backup.data.preferences.traceFlows
import com.machiav3lli.backup.ui.navigation.NavItem
import com.machiav3lli.backup.ui.pages.pref_newAndUpdatedNotification
import com.machiav3lli.backup.utils.TraceUtils.trace
import com.machiav3lli.backup.utils.applySearchAndFilter
import com.machiav3lli.backup.utils.extensions.IconCache
import com.machiav3lli.backup.utils.extensions.NeoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainVM(
    private val packageRepository: PackageRepository,
    private val blocklistRepository: BlocklistRepository,
    private val prefs: NeoPrefs,
) : NeoViewModel() {
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - FLOWS

    val homeState: StateFlow<MainState>
        private field = MutableStateFlow(MainState())
    val backupState: StateFlow<MainState>
        private field = MutableStateFlow(MainState())
    val restoreState: StateFlow<MainState>
        private field = MutableStateFlow(MainState())

    private val searchQuery = MutableStateFlow("")

    private val selection = MutableStateFlow(emptySet<String>())
    private val homeSortFilterModelFlow = prefs.homeSortFilterFlow()
    private val backupSortFilterModelFlow = prefs.backupSortFilterFlow()
    private val restoreSortFilterModelFlow = prefs.restoreSortFilterFlow()

    val packageMap =
        //========================================================================================== packageList
        combine(
            packageRepository.getPackagesFlow(),
            packageRepository.getBackupsFlow()
        ) { pkgs, backups ->

            traceFlows {
                "******************** packages-db: ${pkgs.size} backups-db: ${
                    backups.map { it.value.size }.sum()
                }"
            }

            IconCache.dropAllButUsed(pkgs.drop(0))

            traceFlows { "***** packages ->> ${pkgs.size}" }
            pkgs
        }
            .mapLatest { it.associateBy(Package::packageName) }
            .trace { "*** packageMap <<- ${it.size}" }
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
                emptyMap()
            )

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - FLOWS end

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                packageRepository.getPackagesFlow(),
                blocklistRepository.getBlocklist(),
                homeSortFilterModelFlow,
                searchQuery,
                selection,
            ) { packages, blocklist, sortFilter, search, selection ->
                val (filteredPackages, updatedPackages) = packages
                    .filterNot { it.packageName in blocklist }
                    .let {
                        Pair(
                            it.applySearchAndFilter(search, emptyMap(), sortFilter),
                            it.filter { item ->
                                item.isUpdated || (pref_newAndUpdatedNotification.value && item.isNew)
                            }
                        )
                    }

                MainState(
                    packages = packages,
                    filteredPackages = filteredPackages,
                    updatedPackages = updatedPackages,
                    blocklist = blocklist,
                    searchQuery = search,
                    sortFilter = sortFilter,
                    selection = selection
                )
            }.collect { newState ->
                homeState.update { newState }
            }
        }
        viewModelScope.launch {
            combine(
                packageRepository.getPackagesFlow(),
                blocklistRepository.getBlocklist(),
                backupSortFilterModelFlow,
                searchQuery,
                selection,
            ) { packages, blocklist, sortFilter, search, selection ->
                val filteredPackages = packages
                    .filterNot { it.packageName in blocklist }
                    .applySearchAndFilter(search, emptyMap(), sortFilter)

                MainState(
                    packages = packages,
                    filteredPackages = filteredPackages,
                    blocklist = blocklist,
                    searchQuery = search,
                    sortFilter = sortFilter,
                    selection = selection
                )
            }.collect { newState ->
                backupState.update { newState }
            }
        }
        viewModelScope.launch {
            combine(
                packageRepository.getPackagesFlow(),
                blocklistRepository.getBlocklist(),
                restoreSortFilterModelFlow,
                searchQuery,
                selection,
            ) { packages, blocklist, sortFilter, search, selection ->
                val filteredPackages = packages
                    .filterNot { it.packageName in blocklist }
                    .applySearchAndFilter(search, emptyMap(), sortFilter)

                MainState(
                    packages = packages,
                    filteredPackages = filteredPackages,
                    blocklist = blocklist,
                    searchQuery = search,
                    sortFilter = sortFilter,
                    selection = selection
                )
            }.collect { newState ->
                restoreState.update { newState }
            }
        }
    }

    fun setSearchQuery(value: String) {
        viewModelScope.launch { searchQuery.update { value } }
    }

    fun setSortFilter(value: SortFilterModel, sourcePage: NavItem) {
        viewModelScope.launch {
            when (sourcePage) {
                NavItem.Backup -> {
                    prefs.sortBackup.set(value.sort)
                    prefs.sortAscBackup.set(value.sortAsc)
                    prefs.mainFilterBackup.set(value.mainFilter)
                    prefs.backupFilterBackup.set(value.backupFilter)
                    prefs.installedFilterBackup.set(value.installedFilter)
                    prefs.launchableFilterBackup.set(value.launchableFilter)
                    prefs.updatedFilterBackup.set(value.updatedFilter)
                    prefs.latestFilterBackup.set(value.latestFilter)
                    prefs.enabledFilterBackup.set(value.enabledFilter)
                }

                NavItem.Restore -> {
                    prefs.sortRestore.set(value.sort)
                    prefs.sortAscRestore.set(value.sortAsc)
                    prefs.mainFilterRestore.set(value.mainFilter)
                    prefs.backupFilterRestore.set(value.backupFilter)
                    prefs.installedFilterRestore.set(value.installedFilter)
                    prefs.launchableFilterRestore.set(value.launchableFilter)
                    prefs.updatedFilterRestore.set(value.updatedFilter)
                    prefs.latestFilterRestore.set(value.latestFilter)
                    prefs.enabledFilterRestore.set(value.enabledFilter)
                }

                else -> {
                    prefs.sortHome.set(value.sort)
                    prefs.sortAscHome.set(value.sortAsc)
                    prefs.mainFilterHome.set(value.mainFilter)
                    prefs.backupFilterHome.set(value.backupFilter)
                    prefs.installedFilterHome.set(value.installedFilter)
                    prefs.launchableFilterHome.set(value.launchableFilter)
                    prefs.updatedFilterHome.set(value.updatedFilter)
                    prefs.latestFilterHome.set(value.latestFilter)
                    prefs.enabledFilterHome.set(value.enabledFilter)
                }
            }
        }
    }

    fun onEnableSpecials(enable: Boolean) {
        viewModelScope.launch {
            val filter = if (enable) MAIN_FILTER_DEFAULT
            else MAIN_FILTER_DEFAULT_WITHOUT_SPECIAL
            prefs.mainFilterHome.set(prefs.mainFilterHome.value and filter)
            prefs.mainFilterBackup.set(prefs.mainFilterBackup.value and filter)
            prefs.mainFilterRestore.set(prefs.mainFilterRestore.value and filter)
        }
    }

    fun toggleSelection(packageName: String) {
        viewModelScope.launch {
            selection.update { current ->
                if (current.contains(packageName)) {
                    current - packageName
                } else {
                    current + packageName
                }
            }
        }
    }

    fun updatePackage(packageName: String) {
        viewModelScope.launch {
            packageRepository.updatePackage(packageName)
        }
    }

    fun addToBlocklist(packageName: String) {
        viewModelScope.launch {
            blocklistRepository.addToGlobalBlocklist(packageName)
        }
    }

    fun updateBlocklist(packages: Set<String>) {
        viewModelScope.launch {
            blocklistRepository.updateGlobalBlocklist(packages)
        }
    }
}