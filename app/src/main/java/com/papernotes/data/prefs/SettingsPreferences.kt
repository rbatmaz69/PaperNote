package com.papernotes.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App-Einstellungen (persistiert): aktuell das gewählte Papier-Theme. */
@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeKeyPref = stringPreferencesKey("paper_theme")

    /** Schlüssel des gewählten [com.papernotes.ui.theme.PaperTheme] (Default: AUTO). */
    val themeKey: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[themeKeyPref] ?: "AUTO"
    }

    suspend fun setThemeKey(key: String) {
        context.settingsDataStore.edit { it[themeKeyPref] = key }
    }
}
