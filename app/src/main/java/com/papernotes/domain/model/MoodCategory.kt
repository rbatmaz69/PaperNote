package com.papernotes.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.papernotes.ui.theme.LocalPaperDark
import com.papernotes.ui.theme.CardCalm
import com.papernotes.ui.theme.CardCalmNight
import com.papernotes.ui.theme.CardDream
import com.papernotes.ui.theme.CardDreamNight
import com.papernotes.ui.theme.CardFocus
import com.papernotes.ui.theme.CardFocusNight
import com.papernotes.ui.theme.CardJoy
import com.papernotes.ui.theme.CardJoyNight
import com.papernotes.ui.theme.CardPlain
import com.papernotes.ui.theme.CardPlainNight
import com.papernotes.ui.theme.CardWarm
import com.papernotes.ui.theme.CardWarmNight
import com.papernotes.ui.theme.Lavender
import com.papernotes.ui.theme.LavenderNight
import com.papernotes.ui.theme.PlainEarNight
import com.papernotes.ui.theme.SageGreen
import com.papernotes.ui.theme.SageGreenNight
import com.papernotes.ui.theme.SkyMist
import com.papernotes.ui.theme.SkyMistNight
import com.papernotes.ui.theme.SoftPeach
import com.papernotes.ui.theme.SoftPeachNight
import com.papernotes.ui.theme.SunnyYellow
import com.papernotes.ui.theme.SunnyYellowNight

/**
 * Stimmung / Kategorie einer Notiz. Die Akzentfarbe färbt das "Eselsohr" (Dog-Ear),
 * die Surface-Farbe die sanfte Kartenfläche — jeweils mit heller und dunkler Variante
 * ("Mitternachtspapier"). Reihenfolge = Reihenfolge im Mood-Picker.
 */
enum class MoodCategory(
    val label: String,
    val accent: Color,
    val accentNight: Color,
    val surface: Color,
    val surfaceNight: Color,
) {
    PLAIN("Schlicht", Color(0xFFE7DFD2), PlainEarNight, CardPlain, CardPlainNight),
    JOY("Freude", SunnyYellow, SunnyYellowNight, CardJoy, CardJoyNight),
    CALM("Ruhe", SageGreen, SageGreenNight, CardCalm, CardCalmNight),
    WARM("Wärme", SoftPeach, SoftPeachNight, CardWarm, CardWarmNight),
    DREAM("Traum", Lavender, LavenderNight, CardDream, CardDreamNight),
    FOCUS("Fokus", SkyMist, SkyMistNight, CardFocus, CardFocusNight);

    companion object {
        fun fromName(name: String?): MoodCategory =
            entries.firstOrNull { it.name == name } ?: PLAIN
    }
}

/** Kartenfläche passend zum aktuellen Theme. */
@Composable
@ReadOnlyComposable
fun MoodCategory.cardSurface(): Color =
    if (LocalPaperDark.current) surfaceNight else surface

/** Eselsohr-/Akzentfarbe passend zum aktuellen Theme. */
@Composable
@ReadOnlyComposable
fun MoodCategory.earAccent(): Color =
    if (LocalPaperDark.current) accentNight else accent
