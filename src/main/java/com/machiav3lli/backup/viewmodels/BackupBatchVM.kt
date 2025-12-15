package com.machiav3lli.backup.viewmodels

import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.data.dbs.repository.AppExtrasRepository
import com.machiav3lli.backup.data.dbs.repository.BlocklistRepository
import com.machiav3lli.backup.data.dbs.repository.PackageRepository
import com.machiav3lli.backup.data.entity.MainState
import com.machiav3lli.backup.data.entity.SortFilterModel
import com.machiav3lli.backup.data.preferences.NeoPrefs
import com.machiav3lli.backup.utils.applyFilter
import com.machiav3lli.backup.utils.applySearch
import com.machiav3lli.backup.utils.extensions.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BackupBatchVM(
    packageRepository: PackageRepository,
    blocklistRepository: BlocklistRepository,
    appExtrasRepository: AppExtrasRepository,
    private val prefs: NeoPrefs,
) : BatchVM(blocklistRepository, appExtrasRepository) {
    private val searchQuery = MutableStateFlow("")
    private val selection = MutableStateFlow(emptySet<String>())
    private val sortFilterModelFlow = prefs.backupSortFilterFlow()

    init {
        combine(
            packageRepository.getPackagesFlow(),
            blocklistRepository.getBlocklist(),
            sortFilterModelFlow,
            extras,
            searchQuery,
            selection,
        ) { packages, blocklist, sortFilter, extras, search, selection ->
            val filteredPackages = packages
                .filterNot { it.packageName in blocklist }
                .applySearch(search, extras)
                .applyFilter(sortFilter, extras.mapValues { it.value.customTags })

            MainState(
                packages = packages,
                filteredPackages = filteredPackages,
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
            prefs.sortBackup.set(value.sort)
            prefs.sortAscBackup.set(value.sortAsc)
            prefs.mainFilterBackup.set(value.mainFilter)
            prefs.backupFilterBackup.set(value.backupFilter)
            prefs.installedFilterBackup.set(value.installedFilter)
            prefs.launchableFilterBackup.set(value.launchableFilter)
            prefs.updatedFilterBackup.set(value.updatedFilter)
            prefs.latestFilterBackup.set(value.latestFilter)
            prefs.enabledFilterBackup.set(value.enabledFilter)
            prefs.tagsFilterBackup.set(value.tags)
        }
    }
}