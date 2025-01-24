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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.activities.NeoActivity
import com.machiav3lli.backup.dbs.entity.AppExtras
import com.machiav3lli.backup.dbs.entity.Backup
import com.machiav3lli.backup.dbs.repository.AppExtrasRepository
import com.machiav3lli.backup.dbs.repository.PackageRepository
import com.machiav3lli.backup.handler.ShellCommands.Companion.currentProfile
import com.machiav3lli.backup.handler.showNotification
import com.machiav3lli.backup.ui.compose.MutableComposableStateFlow
import com.machiav3lli.backup.utils.NeoViewModel
import com.machiav3lli.backup.utils.SystemUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

@OptIn(ExperimentalCoroutinesApi::class)
class AppVM(
    private val appExtrasRepository: AppExtrasRepository,
    private val packageRepository: PackageRepository,
) : NeoViewModel() {
    private val ioScope = viewModelScope.plus(Dispatchers.IO)

    private val packageName: MutableStateFlow<String> = MutableStateFlow("")
    val pkg = combine(
        packageName,
        packageRepository.getPackagesFlow(),
        packageRepository.getBackupsFlow(),
    ) { name, pkgs, bkups ->
        pkgs.find { it.packageName == name }
    }
        .stateIn(
            ioScope,
            SharingStarted.Eagerly,
            null
        )

    var appExtras = appExtrasRepository.getFlow(packageName).mapLatest {
        it ?: AppExtras(packageName.value ?: "")
    }.stateIn(
        ioScope,
        SharingStarted.Eagerly,
        AppExtras(packageName.value)
    )

    val snackbarText = MutableComposableStateFlow( // TODO change to MutableStateFlow
        "",
        viewModelScope,
        "snackBarText"
    )

    private var notificationId: Int = SystemUtils.now.toInt()
    val refreshNow = mutableStateOf(true)
    val dismissNow = mutableStateOf(false)

    fun setApp(pn: String) {
        viewModelScope.launch { packageName.update { pn } }
    }

    fun uninstallApp() {
        viewModelScope.launch {
            val users = listOf(currentProfile.toString())
            packageRepository.uninstall(
                mPackage = pkg.value,
                users = users,
                onDismiss = { dismissNow.value = true }
            ) { message ->
                showNotification(
                    NeoApp.NB,
                    NeoActivity::class.java,
                    notificationId++,
                    pkg.value?.packageLabel,
                    message,
                    true
                )
            }
            refreshNow.value = true
        }
    }

    fun enableDisableApp(packageName: String, enable: Boolean) {
        viewModelScope.launch {
            val users = listOf(currentProfile.toString())
            packageRepository.enableDisable(packageName, users, enable)
            refreshNow.value = true
        }
    }

    fun deleteBackup(backup: Backup) {              //TODO hg42 launchDeleteBackup ?
        viewModelScope.launch {
            packageRepository.deleteBackup(pkg.value, backup) {
                dismissNow.value = true
            }
            refreshNow.value = true
        }
    }

    fun deleteAllBackups() {
        viewModelScope.launch {
            packageRepository.deleteAllBackups(pkg.value) {
                dismissNow.value = true
            }
            refreshNow.value = true
        }
    }

    fun setExtras(packageName: String, appExtras: AppExtras?) {
        viewModelScope.launch {
            appExtrasRepository.replaceExtras(packageName, appExtras)
            refreshNow.value = true
        }
    }

    fun rewriteBackup(backup: Backup, changedBackup: Backup) {
        viewModelScope.launch {
            packageRepository.rewriteBackup(pkg.value, backup, changedBackup)
        }
    }
}