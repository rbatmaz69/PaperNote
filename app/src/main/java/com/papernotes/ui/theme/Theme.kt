package com.papernotes.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
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
 * Das aktuell *aufgelöste* (nicht animierte) Papier-Theme. Liefert die stabilen
 * Palette-Farben (paper/ink/…), die sich nur bei echtem Theme-Wechsel ändern – nicht
 * pro Frame während der Farb-Überblendung. [PaperBackground] speist daraus seinen
 * Vollbild-Hintergrund, damit das teure Shader-Bitmap nur einmal pro Theme gebacken
 * wird statt bei jedem Frame des Crossfades.
 */
val LocalPaperTheme = compositionLocalOf { PaperTheme.DAYLIGHT }

/**
 * PaperNotes baut sein warmes [androidx.compose.material3.ColorScheme] aus dem gewählten
 * [PaperTheme]. Stimmungsfarben kommen über [com.papernotes.domain.model.MoodCategory] dazu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperNotesTheme(
    theme: PaperTheme = PaperTheme.AUTO,
    // Solange das gespeicherte Theme noch nicht geladen ist (Splash läuft), NICHT animieren:
    // sonst sieht man beim Start die Farben vom Default (AUTO) ins gespeicherte Theme über-
    // blenden. Erst nach dem Laden animieren → spätere bewusste Theme-Wechsel bleiben weich.
    animateThemeChange: Boolean = true,
    content: @Composable () -> Unit,
) {
    val resolved = theme.resolve(isSystemInDarkTheme())
    val target = if (resolved.dark) resolved.darkScheme() else resolved.lightScheme()

    // Theme-Wechsel weich überblenden: die Leitfarben animiert nachführen.
    val scheme = target.animated(animate = animateThemeChange)

    // Dezenter Papier-/Tinten-Ripple für Material-Buttons (z. B. IconButtons) statt
    // hartem Default – passt zum Papier-Druck der übrigen Flächen.
    val ripple = RippleConfiguration(color = target.onSurface)

    CompositionLocalProvider(
        LocalPaperDark provides resolved.dark,
        LocalPaperTheme provides resolved,
        LocalRippleConfiguration provides ripple,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PaperTypography,
            shapes = PaperShapes,
            content = content,
        )
    }
}

/** Führt die Leitfarben des Schemas animiert nach – ergibt eine weiche Theme-Überblendung. */
@Composable
private fun ColorScheme.animated(animate: Boolean): ColorScheme {
    // Ohne Animation: Farben sofort übernehmen (kein animateColorAsState ⇒ kein Einblenden).
    if (!animate) return this
    val spec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 450)
    return copy(
        primary = animateColorAsState(primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(onPrimary, spec, label = "onPrimary").value,
        secondary = animateColorAsState(secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(onSecondary, spec, label = "onSecondary").value,
        tertiary = animateColorAsState(tertiary, spec, label = "tertiary").value,
        onTertiary = animateColorAsState(onTertiary, spec, label = "onTertiary").value,
        background = animateColorAsState(background, spec, label = "background").value,
        onBackground = animateColorAsState(onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(surface, spec, label = "surface").value,
        onSurface = animateColorAsState(onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(onSurfaceVariant, spec, label = "onSurfaceVariant").value,
        outline = animateColorAsState(outline, spec, label = "outline").value,
    )
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
