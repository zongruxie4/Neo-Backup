package com.machiav3lli.backup.dbs.repository

import android.app.Application
import com.machiav3lli.backup.NeoApp.Companion.getBackups
import com.machiav3lli.backup.R
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.entity.AppInfo
import com.machiav3lli.backup.dbs.entity.Backup
import com.machiav3lli.backup.entity.Package
import com.machiav3lli.backup.entity.Package.Companion.invalidateCacheForPackage
import com.machiav3lli.backup.handler.LogsHandler
import com.machiav3lli.backup.handler.ShellCommands
import com.machiav3lli.backup.handler.toPackageList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class PackageRepository(
    private val db: ODatabase,
    private val appContext: Application,
) {
    private val cc = Dispatchers.IO
    private val jcc = Dispatchers.IO + SupervisorJob()

    fun getPackagesFlow(): Flow<List<Package>> =
        db.getAppInfoDao().getAllFlow()
            .map { it.toPackageList(appContext, emptyList(), getBackups()) }
            .flowOn(cc)

    fun getBackupsFlow(): Flow<Map<String, List<Backup>>> =
        db.getBackupDao().getAllFlow()
            .map { it.groupBy(Backup::packageName) }
            .flowOn(cc)

    suspend fun getBackups(packageName: String): List<Backup> = withContext(jcc) {
        db.getBackupDao().get(packageName)
    }

    suspend fun updatePackage(packageName: String) {
        withContext(jcc) {
            invalidateCacheForPackage(packageName)
            val new = Package(appContext, packageName)
            if (!new.isSpecial) {
                new.refreshFromPackageManager(appContext)
                db.getAppInfoDao().update(new.packageInfo as AppInfo)
            }
        }
    }

    fun upsertAppInfo(vararg appInfos: AppInfo) {
        db.getAppInfoDao().upsert(*appInfos)
    }

    fun replaceAppInfos(vararg appInfos: AppInfo) {
        db.getAppInfoDao().updateList(*appInfos)
    }

    suspend fun updateBackups(packageName: String, backups: List<Backup>) {
        db.getBackupDao().updateList(packageName, backups)
    }

    fun replaceBackups(vararg backups: Backup) {
        db.getBackupDao().updateList(*backups)
    }

    suspend fun rewriteBackup(pkg: Package?, backup: Backup, changedBackup: Backup) {
        withContext(jcc) {
            pkg?.rewriteBackup(backup, changedBackup)
        }
    }

    fun deleteAppInfoOf(packageName: String) = db.getAppInfoDao().deleteAllOf(packageName)

    suspend fun deleteBackup(pkg: Package?, backup: Backup, onDismiss: () -> Unit) {
        withContext(jcc) {
            pkg?.let { pkg ->
                pkg.deleteBackup(backup)
                if (!pkg.isInstalled && pkg.backupList.isEmpty() && backup.packageLabel != "? INVALID") {
                    db.getAppInfoDao().deleteAllOf(pkg.packageName)
                    onDismiss()
                }
            }
        }
    }

    suspend fun deleteAllBackups(pkg: Package?, onDismiss: () -> Unit) {
        withContext(jcc) {
            pkg?.let { pkg ->
                pkg.deleteAllBackups()
                if (!pkg.isInstalled && pkg.backupList.isEmpty()) {
                    db.getAppInfoDao().deleteAllOf(pkg.packageName)
                    onDismiss()
                }
            }
        }
    }

    @Throws(ShellCommands.ShellActionFailedException::class)
    suspend fun enableDisable(packageName: String, users: List<String>, enable: Boolean) {
        withContext(jcc) {
            ShellCommands.enableDisable(users, packageName, enable)
        }
    }

    suspend fun uninstall(
        mPackage: Package?,
        users: List<String>,
        onDismiss: () -> Unit,
        showNotification: (String) -> Unit,
    ) {
        withContext(jcc) {
            mPackage?.let { mPackage ->
                Timber.i("uninstalling: ${mPackage.packageLabel}")
                try {
                    ShellCommands.uninstall(
                        users,
                        mPackage.packageName, mPackage.apkPath,
                        mPackage.dataPath, mPackage.isSystem
                    )
                    if (mPackage.backupList.isEmpty()) {
                        db.getAppInfoDao().deleteAllOf(mPackage.packageName)
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
}