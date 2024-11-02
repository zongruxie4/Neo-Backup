package com.machiav3lli.backup.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object PrefKey {
    // SortFilter.home
    val SORT_ASC_HOME = booleanPreferencesKey("sortAsc_home")
    val SORT_HOME = intPreferencesKey("sort_home")
    val MAIN_FILTER_HOME = intPreferencesKey("main_filter_home")
    val BACKUP_FILTER_HOME = intPreferencesKey("backup_filter_home")
    val INSTALLED_FILTER_HOME = intPreferencesKey("installed_filter_home")
    val LAUNCHABLE_FILTER_HOME = intPreferencesKey("launchable_filter_home")
    val UPDATED_FILTER_HOME = intPreferencesKey("updated_filter_home")
    val LATEST_FILTER_HOME = intPreferencesKey("latest_filter_home")
    val ENABLED_FILTER_HOME = intPreferencesKey("enabled_filter_home")
}