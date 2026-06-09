package com.papernotes.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private val Context.delightDataStore: DataStore<Preferences> by preferencesDataStore(name = "delight")

/**
 * Tagesbezogener Kleinkram: wann der Teebeutel zuletzt gezogen wurde (1×/Tag) und
 * wie viele Checklisten-Haken heute gesetzt wurden (für die Tages-Statistik).
 */
@Singleton
class DelightPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lastPulledDay = intPreferencesKey("last_pulled_day_of_year")
    private val checksDay = intPreferencesKey("checks_day_of_year")
    private val checksCount = intPreferencesKey("checks_count")

    /** true, wenn der Teebeutel heute noch nicht gezogen wurde. */
    val availableToday: Flow<Boolean> = context.delightDataStore.data.map { prefs ->
        prefs[lastPulledDay] != currentDayOfYear()
    }

    /** Heute gesetzte Checklisten-Haken (0 nach Tageswechsel). */
    val checksToday: Flow<Int> = context.delightDataStore.data.map { prefs ->
        if (prefs[checksDay] == currentDayOfYear()) prefs[checksCount] ?: 0 else 0
    }

    suspend fun markPulledToday() {
        context.delightDataStore.edit { it[lastPulledDay] = currentDayOfYear() }
    }

    suspend fun incrementChecksToday() {
        context.delightDataStore.edit { prefs ->
            val today = currentDayOfYear()
            val current = if (prefs[checksDay] == today) prefs[checksCount] ?: 0 else 0
            prefs[checksDay] = today
            prefs[checksCount] = current + 1
        }
    }

    private fun currentDayOfYear(): Int =
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
}
