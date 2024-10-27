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
package com.machiav3lli.backup.entity

import android.os.Parcel
import android.os.Parcelable
import com.machiav3lli.backup.BACKUP_FILTER_DEFAULT
import com.machiav3lli.backup.EnabledFilter
import com.machiav3lli.backup.InstalledFilter
import com.machiav3lli.backup.LatestFilter
import com.machiav3lli.backup.LaunchableFilter
import com.machiav3lli.backup.MAIN_FILTER_DEFAULT
import com.machiav3lli.backup.Sort
import com.machiav3lli.backup.UpdatedFilter

data class SortFilterModel(
    var sort: Int = Sort.LABEL.ordinal,
    var sortAsc: Boolean = true,
    var mainFilter: Int = MAIN_FILTER_DEFAULT,
    var backupFilter: Int = BACKUP_FILTER_DEFAULT,
    var installedFilter: Int = InstalledFilter.ALL.ordinal,
    var launchableFilter: Int = LaunchableFilter.ALL.ordinal,
    var updatedFilter: Int = UpdatedFilter.ALL.ordinal,
    var latestFilter: Int = LatestFilter.ALL.ordinal,
    var enabledFilter: Int = EnabledFilter.ALL.ordinal,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
    ) {
    }

    constructor(sortFilterCode: String?, specialFilterCode: String?) : this() {
        sortFilterCode?.let {
            sort = sortFilterCode[0].digitToInt()
            sortAsc = sortFilterCode[1].digitToInt() == 1
            mainFilter = sortFilterCode[2].digitToInt()
            backupFilter = sortFilterCode.substring(4).toInt()
        }
        specialFilterCode?.let {
            installedFilter = specialFilterCode[0].digitToInt()
            launchableFilter = specialFilterCode[1].digitToInt()
            updatedFilter = specialFilterCode[2].digitToInt()
            latestFilter = specialFilterCode[3].digitToInt()
            enabledFilter = specialFilterCode[4].digitToInt()
        }
    }

    val specialFilter: SpecialFilter
        get() = SpecialFilter(
            installedFilter,
            launchableFilter,
            updatedFilter,
            latestFilter,
            enabledFilter
        )

    override fun toString(): String =
        "$sort${sortAsc.compareTo(false)}${mainFilter}0$backupFilter,$installedFilter$launchableFilter$updatedFilter$latestFilter$enabledFilter"

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(sort)
        parcel.writeByte(if (sortAsc) 1 else 0)
        parcel.writeInt(mainFilter)
        parcel.writeInt(backupFilter)
        parcel.writeInt(installedFilter)
        parcel.writeInt(launchableFilter)
        parcel.writeInt(updatedFilter)
        parcel.writeInt(latestFilter)
        parcel.writeInt(enabledFilter)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SortFilterModel> {
        override fun createFromParcel(parcel: Parcel): SortFilterModel {
            return SortFilterModel(parcel)
        }

        override fun newArray(size: Int): Array<SortFilterModel?> {
            return arrayOfNulls(size)
        }
    }
}

class SpecialFilter(
    val installedFilter: Int = InstalledFilter.ALL.ordinal,
    val launchableFilter: Int = LaunchableFilter.ALL.ordinal,
    val updatedFilter: Int = UpdatedFilter.ALL.ordinal,
    val latestFilter: Int = LatestFilter.ALL.ordinal,
    val enabledFilter: Int = EnabledFilter.ALL.ordinal,
)