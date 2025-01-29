/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2025 Antonios Hazim
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.machiav3lli.backup.EXTRA_NAME
import com.machiav3lli.backup.EXTRA_SCHEDULE_ID

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, ScheduleService::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, intent?.getLongExtra(EXTRA_SCHEDULE_ID, -1))
            putExtra(EXTRA_NAME, intent?.getStringExtra(EXTRA_NAME) ?: "")
        }
        context?.startService(serviceIntent)
    }
}