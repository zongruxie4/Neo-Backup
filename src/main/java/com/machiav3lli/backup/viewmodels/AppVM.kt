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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.STATEFLOW_SUBSCRIBE_BUFFER
import com.machiav3lli.backup.data.dbs.entity.AppExtras
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.repository.AppExtrasRepository
import com.machiav3lli.backup.data.repository.BlocklistRepository
import com.machiav3lli.backup.data.repository.PackageRepository
import com.machiav3lli.backup.manager.handler.BackupRestoreHelper
import com.machiav3lli.backup.manager.handler.ShellCommands.Companion.currentProfile
import com.machiav3lli.backup.manager.handler.showNotification
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.utils.SystemUtils
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
class AppVM(
    private val appExtrasRepository: AppExtrasRepository,
    private val packageRepository: PackageRepository,
    private val blocklistRepository: BlocklistRepository,
) : NeoViewModel() {
    private val packageName: MutableStateFlow<String> = MutableStateFlow("")

    val pkg = combine(
        packageName,
        packageRepository.getPackagesFlow(),
        packageRepository.getBackupsListFlow(),
    ) { name, pkgs, bkups ->
        pkgs.find { it.packageName == name }
    }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
            null
        )

    val appExtras = appExtrasRepository.getFlow(packageName).mapLatest {
        it ?: AppExtras(packageName.value ?: "")
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(STATEFLOW_SUBSCRIBE_BUFFER),
        AppExtras(packageName.value)
    )

    val snackbarText: StateFlow<String>
        field = MutableStateFlow("")

    private var notificationId: Int = SystemUtils.now.toInt()
    val dismissNow = mutableStateOf(false)

    fun setApp(pn: String) {
        viewModelScope.launch { packageName.update { pn } }
    }

    fun setSnackbarText(value: String) {
        viewModelScope.launch { snackbarText.update { value } }
    }

    fun updatePackage(packageName: String) {
        viewModelScope.launch {
            packageRepository.updatePackage(packageName)
        }
    }

    fun uninstallApp() {
        viewModelScope.launch {
            pkg.value?.let { pkg ->
                val users = listOf(currentProfile.toString())
                packageRepository.uninstall(
                    mPackage = pkg,
                    users = users,
                    onDismiss = { dismissNow.value = true }
                ) { message ->
                    showNotification(
                        NeoApp.NB,
                        NeoActivity::class.java,
                        notificationId++,
                        pkg.packageLabel,
                        message,
                        true
                    )
                }
                updatePackage(pkg.packageName)
            }
        }
    }

    fun enableDisableApp(packageName: String, enable: Boolean) {
        viewModelScope.launch {
            val users = listOf(currentProfile.toString())
            packageRepository.enableDisable(packageName, users, enable)
            updatePackage(packageName)
        }
    }

    fun deleteBackup(backup: Backup) {              //TODO hg42 launchDeleteBackup ?
        viewModelScope.launch {
            packageRepository.deleteBackup(pkg.value, backup) {
                dismissNow.value = true
            }
            updatePackage(backup.packageName)
        }
    }

    fun deleteAllBackups() {
        viewModelScope.launch {
            packageRepository.deleteAllBackups(pkg.value) {
                dismissNow.value = true
            }
            updatePackage(packageName.value)
        }
    }

    fun setExtras(packageName: String, appExtras: AppExtras?) {
        viewModelScope.launch {
            appExtrasRepository.replaceExtras(packageName, appExtras)
            updatePackage(packageName)
        }
    }

    fun enforceBackupsLimit(pkg: Package) {
        viewModelScope.launch {
            BackupRestoreHelper.housekeepingPackageBackups(pkg)
            updatePackage(pkg.packageName)
        }
    }

    fun rewriteBackup(backup: Backup, changedBackup: Backup) {
        viewModelScope.launch {
            packageRepository.rewriteBackup(pkg.value, backup, changedBackup)
        }
    }

    fun addToBlocklist(packageName: String) {
        viewModelScope.launch {
            blocklistRepository.addToGlobalBlocklist(packageName)
        }
    }
}