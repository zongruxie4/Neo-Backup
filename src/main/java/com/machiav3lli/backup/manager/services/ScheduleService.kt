/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2025  Antonios Hazim
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
package com.machiav3lli.backup.manager.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import com.machiav3lli.backup.ACTION_CANCEL
import com.machiav3lli.backup.ACTION_RUN_SCHEDULE
import com.machiav3lli.backup.EXTRA_NAME
import com.machiav3lli.backup.EXTRA_SCHEDULE_ID
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.repository.ScheduleRepository
import com.machiav3lli.backup.data.preferences.traceSchedule
import com.machiav3lli.backup.manager.handler.showNotification
import com.machiav3lli.backup.manager.tasks.ScheduleWork
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.pages.pref_fakeScheduleDups
import com.machiav3lli.backup.ui.pages.pref_useForegroundInService
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.cancelScheduleAlarm
import com.machiav3lli.backup.utils.extensions.Android
import com.machiav3lli.backup.utils.scheduleAlarmsOnce
import com.machiav3lli.backup.utils.scheduleNextAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

open class ScheduleService : Service() {
    lateinit var notification: Notification
    private var notificationId = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        NeoApp.wakelock(true)
        traceSchedule { "%%%%% ############################################################ ScheduleService create" }
        super.onCreate()
        this.notificationId = SystemUtils.now.toInt()

        if (pref_useForegroundInService.value) {
            createNotificationChannel()
            createForegroundInfo()
            startForeground(notification.hashCode(), this.notification)
        }

        showNotification(
            this.baseContext,
            NeoActivity::class.java,
            notificationId,
            String.format(
                getString(R.string.fetching_action_list),
                getString(R.string.backup)
            ),
            "",
            true
        )
    }

    override fun onDestroy() {
        traceSchedule { "%%%%% ############################################################ ScheduleService destroy" }
        NeoApp.wakelock(false)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scheduleId = intent?.getLongExtra(EXTRA_SCHEDULE_ID, -1L) ?: -1L
        val scheduleName = intent?.getStringExtra(EXTRA_NAME) ?: ""

        NeoApp.wakelock(true)

        traceSchedule {
            var message =
                "[$scheduleId] %%%%% ############################################################ ScheduleService startId=$startId PID=${Process.myPid()} starting for name='$scheduleName'"
            if (Android.minSDK(Build.VERSION_CODES.S)) {
                message += " ui=$isUiContext"
            }
            if (Android.minSDK(Build.VERSION_CODES.Q)) {
                message += " fgsv=$foregroundServiceType"
            }
            message
        }

        if (intent != null) {
            when (val action = intent.action) {
                ACTION_CANCEL       -> {
                    traceSchedule { "[$scheduleId] name='$scheduleName' action=$action" }
                    ScheduleWork.cancel(scheduleId)
                    NeoApp.wakelock(false)
                    traceSchedule { "%%%%% service stop" }
                    stopSelf()
                }

                ACTION_RUN_SCHEDULE -> {
                    // scheduleId already read from extras
                    traceSchedule { "[$scheduleId] name='$scheduleName' action=$action" }
                }

                null                -> {
                    // no action = standard action, simply continue with extra data
                }

                else                -> {
                    traceSchedule { "[$scheduleId] name='$scheduleName' action=$action unknown, ignored" }
                }
            }
        }

        if (scheduleId >= 0) {
            repeat(1 + pref_fakeScheduleDups.value) { count ->
                CoroutineScope(Dispatchers.IO).launch {
                    scheduleNextAlarm(this@ScheduleService, scheduleId, true)
                }
                ScheduleWork.enqueueScheduled(scheduleId, scheduleName)
                traceSchedule { "[$scheduleId] starting task for schedule${if (count > 0) " (dup $count)" else ""}" }
            }
        }

        scheduleAlarmsOnce(this)

        NeoApp.wakelock(false)
        return START_NOT_STICKY
    }

    private fun createForegroundInfo() {
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, NeoActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelIntent = Intent(this, ScheduleService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        this.notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sched_notificationMessage))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_close, getString(R.string.dialogCancel), cancelPendingIntent)
            .build()
    }

    open fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel =
            NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.enableVibration(true)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    companion object {
        private val CHANNEL_ID = ScheduleService::class.java.name

        suspend fun scheduleAll(context: Context) = coroutineScope {
            val scheduleRepo = get<ScheduleRepository>(ScheduleRepository::class.java)

            scheduleRepo.getAll().forEach { schedule ->
                when {
                    !schedule.enabled -> cancelScheduleAlarm(context, schedule.id, schedule.name)
                    else              -> scheduleNextAlarm(context, schedule.id, false)
                }
            }
        }
    }
}