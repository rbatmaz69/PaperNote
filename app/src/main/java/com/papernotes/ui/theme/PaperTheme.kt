package com.papernotes.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Wählbares Papier-Theme. Jedes Theme definiert die Papier-/Tinten-Palette; die
 * Stimmungs-Akzente ([com.papernotes.domain.model.MoodCategory]) kommen darüber.
 *
 * [AUTO] trägt keine eigenen Farben – es wird im Composable über die System-Einstellung
 * zu [DAYLIGHT] bzw. [MIDNIGHT] aufgelöst (siehe [resolve]).
 */
enum class PaperTheme(
    val label: String,
    val dark: Boolean,
    val paper: Color,
    val paperDim: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaded: Color,
) {
    AUTO("Automatisch", false, PaperCream, PaperCreamDim, Espresso, Anthracite, InkFaded),
    DAYLIGHT("Tagespapier", false, PaperCream, PaperCreamDim, Espresso, Anthracite, InkFaded),
    SAGE("Salbei", false, SagePaper, SagePaperDim, SageInk, SageInkSoft, SageInkFaded),
    SUNRISE("Sonnenaufgang", false, SunrisePaper, SunrisePaperDim, SunriseInk, SunriseInkSoft, SunriseInkFaded),
    MIDNIGHT("Mitternacht", true, MidnightPaper, MidnightPaperDim, InkCream, InkCreamSoft, InkCreamFaded),
    LAVENDER_NIGHT("Lavendelnacht", true, LavNightPaper, LavNightPaperDim, LavNightInk, LavNightInkSoft, LavNightInkFaded),
    CHARCOAL("Kohle", true, CharcoalPaper, CharcoalPaperDim, CharcoalInk, CharcoalInkSoft, CharcoalInkFaded);

    /** Löst [AUTO] anhand der System-Dunkelheit auf; alle anderen geben sich selbst zurück. */
    fun resolve(systemDark: Boolean): PaperTheme = when (this) {
        AUTO -> if (systemDark) MIDNIGHT else DAYLIGHT
        else -> this
    }

    companion object {
        /** In der Auswahl angezeigte Themes (AUTO zuerst, danach hell → dunkel). */
        val selectable: List<PaperTheme> =
            listOf(AUTO, DAYLIGHT, SAGE, SUNRISE, MIDNIGHT, LAVENDER_NIGHT, CHARCOAL)

        fun fromKey(key: String?): PaperTheme =
            entries.firstOrNull { it.name == key } ?: AUTO
    }
}
