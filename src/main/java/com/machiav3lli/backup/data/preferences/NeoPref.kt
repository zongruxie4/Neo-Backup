package com.machiav3lli.backup.data.preferences

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

open class PrefBoolean(
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean = false,
    onChange: (Boolean) -> Unit = {}
) : PrefDelegate<Boolean>(titleId, summaryId, dataStore, key, defaultValue, onChange)

open class PrefInt(
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<Int>,
    val defaultValue: Int = 0,
    val entries: List<Int>,
    onChange: (Int) -> Unit = {}
) : PrefDelegate<Int>(titleId, summaryId, dataStore, key, defaultValue, onChange)

class StringSetPref(
    @StringRes titleId: Int = -1,
    @StringRes summaryId: Int = -1,
    dataStore: DataStore<Preferences>,
    val defaultValue: Set<String> = emptySet(),
    val key: Preferences.Key<Set<String>>,
    onChange: (Set<String>) -> Unit = {},
) : PrefDelegate<Set<String>>(titleId, summaryId, dataStore, key, defaultValue, onChange)

abstract class PrefDelegate<T : Any>(
    @StringRes var titleId: Int = -1,
    @StringRes var summaryId: Int = -1,
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val defaultValue: T,
    val onChange: (T) -> Unit
) {
    private val flow: Flow<T> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[key] ?: defaultValue
        }
        .distinctUntilChanged()

    val value: T
        get() = runBlocking(Dispatchers.IO) {
            currentValue()
        }

    val state: State<T>
        @Composable
        get() = flow.collectAsState(initial = defaultValue)

    fun flow(): Flow<T> = flow

    suspend fun set(newValue: T) {
        if (newValue == currentValue()) return
        dataStore.edit { prefs -> prefs[key] = newValue }
        onChange(newValue)
    }

    private suspend fun currentValue(): T {
        return dataStore.data.first()[key] ?: defaultValue
    }
}