package com.papernotes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * Sagt Komponenten, ob das *aktuelle Papier-Theme* dunkel ist – unabhängig von der
 * System-Einstellung (wichtig, wenn der Nutzer ein festes Theme wählt). Wird von
 * [PaperNotesTheme] bereitgestellt.
 *
 * Bewusst `compositionLocalOf` (nicht `static`): Der Wert ändert sich zur Laufzeit beim
 * Theme-Wechsel und wird tief in Lazy-Grid-Items gelesen – nur das leser-verfolgende
 * `compositionLocalOf` invalidiert diese Items zuverlässig.
 */
val LocalPaperDark = compositionLocalOf { false }

/**
 * PaperNotes baut sein warmes [androidx.compose.material3.ColorScheme] aus dem gewählten
 * [PaperTheme]. Stimmungsfarben kommen über [com.papernotes.domain.model.MoodCategory] dazu.
 */
@Composable
fun PaperNotesTheme(
    theme: PaperTheme = PaperTheme.AUTO,
    content: @Composable () -> Unit,
) {
    val resolved = theme.resolve(isSystemInDarkTheme())
    val scheme = if (resolved.dark) resolved.darkScheme() else resolved.lightScheme()

    CompositionLocalProvider(LocalPaperDark provides resolved.dark) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PaperTypography,
            shapes = PaperShapes,
            content = content,
        )
    }
}

private fun PaperTheme.lightScheme() = lightColorScheme(
    primary = ink,
    onPrimary = paper,
    secondary = SageGreen,
    onSecondary = ink,
    tertiary = SoftPeach,
    onTertiary = ink,
    background = paper,
    onBackground = ink,
    surface = paperDim,
    onSurface = ink,
    surfaceVariant = paperDim,
    onSurfaceVariant = inkSoft,
    outline = inkFaded,
)

private fun PaperTheme.darkScheme() = darkColorScheme(
    primary = ink,
    onPrimary = paper,
    secondary = SageGreenNight,
    onSecondary = ink,
    tertiary = SoftPeachNight,
    onTertiary = ink,
    background = paper,
    onBackground = ink,
    surface = paperDim,
    onSurface = ink,
    surfaceVariant = paperDim,
    onSurfaceVariant = inkSoft,
    outline = inkFaded,
)
