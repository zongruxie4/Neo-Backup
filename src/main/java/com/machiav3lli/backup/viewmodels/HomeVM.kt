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
import com.machiav3lli.backup.STATEFLOW_SUBSCRIBE_BUFFER
import com.machiav3lli.backup.data.dbs.repository.AppExtrasRepository
import com.machiav3lli.backup.data.dbs.repository.BlocklistRepository
import com.machiav3lli.backup.data.dbs.repository.PackageRepository
import com.machiav3lli.backup.data.entity.MainState
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.data.preferences.NeoPrefs
import com.machiav3lli.backup.data.preferences.traceFlows
import com.machiav3lli.backup.ui.pages.pref_newAndUpdatedNotification
import com.machiav3lli.backup.utils.TraceUtils.trace
import com.machiav3lli.backup.utils.applyFilter
import com.machiav3lli.backup.utils.applySearch
import com.machiav3lli.backup.utils.extensions.IconCache
import com.machiav3lli.backup.utils.extensions.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeVM(
    private val packageRepository: PackageRepository,
    blocklistRepository: BlocklistRepository,
    appExtrasRepository: AppExtrasRepository,
    private val prefs: NeoPrefs,
) : MainVM(blocklistRepository, appExtrasRepository) {
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - FLOWS
    private val searchQuery = MutableStateFlow("")
    private val selection = MutableStateFlow(emptySet<String>())
    private val homeSortFilterModelFlow = prefs.homeSortFilterFlow()

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

    private val notBlockedPackages = combine(
        packageRepository.getPackagesFlow(),
        blocklistRepository.getBlocklist(),
    ) { packages, blocklist ->
        packages.filterNot { it.packageName in blocklist }
    }
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - FLOWS end

    init {
        combine(
            packageRepository.getPackagesFlow(),
            blocklistRepository.getBlocklist(),
            homeSortFilterModelFlow,
            extras,
            searchQuery,
            selection,
        ) { packages, blocklist, sortFilter, extras, search, selection ->
            val (filteredPackages, updatedPackages) = packages
                .filterNot { it.packageName in blocklist }
                .let {
                    Pair(
                        it.applySearch(search, extras)
                            .applyFilter(sortFilter, extras.mapValues { it.value.customTags }),
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
                selection = selection,
            )
        }.map { newState ->
            _state.update { newState }
        }.launchIn(viewModelScope)
    }

    fun setSearchQuery(value: String) {
        viewModelScope.launch { searchQuery.update { value } }
    }

    override fun setSortFilter(value: SortFilterModel) {
        viewModelScope.launch {
            prefs.sortHome.set(value.sort)
            prefs.sortAscHome.set(value.sortAsc)
            prefs.mainFilterHome.set(value.mainFilter)
            prefs.backupFilterHome.set(value.backupFilter)
            prefs.installedFilterHome.set(value.installedFilter)
            prefs.launchableFilterHome.set(value.launchableFilter)
            prefs.updatedFilterHome.set(value.updatedFilter)
            prefs.latestFilterHome.set(value.latestFilter)
            prefs.enabledFilterHome.set(value.enabledFilter)
            prefs.tagsFilterHome.set(value.tags)
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
}