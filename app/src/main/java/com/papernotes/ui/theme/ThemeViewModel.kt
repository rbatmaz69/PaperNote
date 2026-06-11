package com.papernotes.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papernotes.data.prefs.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hält das gewählte [PaperTheme] (persistiert in [SettingsPreferences]). Activity-weit
 * geteilt: MainActivity liest es fürs App-Theme, der Theme-Picker setzt es.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settings: SettingsPreferences,
) : ViewModel() {

    val theme: StateFlow<PaperTheme> =
        settings.themeKey
            .map { PaperTheme.fromKey(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PaperTheme.AUTO,
            )

    /** true, sobald DataStore das gespeicherte Theme geliefert hat (für den Splash). */
    val ready: StateFlow<Boolean> =
        settings.themeKey
            .map { true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setTheme(theme: PaperTheme) = viewModelScope.launch {
        settings.setThemeKey(theme.name)
    }
}
