package com.machiav3lli.backup.data.repository

import android.app.Application
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.dao.AppInfoDao
import com.machiav3lli.backup.data.dbs.entity.AppInfo
import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.BackupsCache
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.ShellCommands
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@OptIn(FlowPreview::class)
class PackageRepository(
    private val dao: AppInfoDao,
    private val appContext: Application,
    private val backupsCache: BackupsCache,
) {
    fun getBackupsFlow(): Flow<Map<String, List<Backup>>> =
        backupsCache.observeBackupsMap()

    fun getBackupsMap(): Map<String, List<Backup>> =
        backupsCache.getBackupsMap()

    fun getBackupsListFlow(): Flow<List<Backup>> =
        backupsCache.observeBackupsList()

    fun getBackupsList(): List<Backup> =
        backupsCache.getBackupsList()

    fun getBackupsFlow(packageName: String): Flow<List<Backup>> =
        backupsCache.observeBackups(packageName)

    fun getBackups(packageName: String): List<Backup> =
        backupsCache.getBackups(packageName)

    fun getAppInfosFlow() = dao.getAllFlow()

    suspend fun updatePackage(packageName: String) {
        Package.invalidateCacheForPackage(packageName)
        val new = try {
            Package(appContext, packageName)
        } catch (e: AssertionError) {
            null
        }
        if (new != null && !new.isSpecial) {
            new.refreshFromPackageManager(appContext)
            dao.update(new.packageInfo as AppInfo)
        }
    }

    suspend fun upsertAppInfo(vararg appInfos: AppInfo) =
        dao.upsert(*appInfos)

    suspend fun replaceAppInfos(vararg appInfos: AppInfo) =
        dao.updateList(*appInfos)

    suspend fun updatePackageBackups(packageName: String, backups: List<Backup>) =
        backupsCache.updateBackups(packageName, backups)

    suspend fun replaceAllBackups(backups: List<Backup>) =
        backupsCache.replaceAll(backups)

    suspend fun emptyBackupsTable() =
        backupsCache.clear()

    suspend fun deleteBackupsOf(packageNames: List<String>) =
        backupsCache.removeAll(packageNames)

    fun rewriteBackup(pkg: Package?, backup: Backup, changedBackup: Backup) {
        pkg?.rewriteBackup(backup, changedBackup)
    }

    suspend fun deleteAppInfoOf(packageName: String) = dao.deleteAllOf(packageName)

    suspend fun deleteBackup(pkg: Package?, backup: Backup, onDismiss: () -> Unit) =
        pkg?.let { pkg ->
            pkg.deleteBackup(backup)
            if (!pkg.isInstalled && pkg.backupList.isEmpty() && backup.packageLabel != "? INVALID") {
                dao.deleteAllOf(pkg.packageName)
                onDismiss()
            }
        }

    suspend fun deleteAllBackups(pkg: Package?, onDismiss: () -> Unit) {
        pkg?.let { pkg ->
            pkg.deleteAllBackups()
            if (!pkg.isInstalled && pkg.backupList.isEmpty()) {
                dao.deleteAllOf(pkg.packageName)
                onDismiss()
            }
        }
    }

    @Throws(ShellCommands.ShellActionFailedException::class)
    suspend fun enableDisable(packageName: String, users: List<String>, enable: Boolean) {
        ShellCommands.enableDisable(users, packageName, enable)
    }

    suspend fun uninstall(
        mPackage: Package?,
        users: List<String>,
        onDismiss: () -> Unit,
        showNotification: (String) -> Unit,
    ) {
        mPackage?.let { mPackage ->
            Timber.i("uninstalling: ${mPackage.packageLabel}")
            try {
                ShellCommands.uninstall(
                    users,
                    mPackage.packageName, mPackage.apkPath,
                    mPackage.dataPath, mPackage.isSystem
                )
                if (mPackage.backupList.isEmpty()) {
                    dao.deleteAllOf(mPackage.packageName)
                    onDismiss()
                }
                showNotification(appContext.getString(R.string.uninstallSuccess))
            } catch (e: ShellCommands.ShellActionFailedException) {
                showNotification(appContext.getString(R.string.uninstallFailure))
                e.message?.let { message -> LogsHandler.logErrors(message) }
            }
        }
    }
}