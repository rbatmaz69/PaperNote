package com.papernotes.data.delight

import com.papernotes.domain.model.DailyDelight
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liefert das "Gimmick des Tages" für den Glücks-Teebeutel. Vollständig offline:
 * der Tag im Jahr wählt deterministisch einen Eintrag, sodass derselbe Tag denselben
 * Glücksmoment zeigt (und über Mitternacht wechselt).
 */
@Singleton
class DailyDelightProvider @Inject constructor() {

    fun forToday(now: Long = System.currentTimeMillis()): DailyDelight {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        return delights[dayOfYear % delights.size]
    }

    private val delights: List<DailyDelight> = listOf(
        DailyDelight(DailyDelight.Kind.QUOTE, "Kleine Schritte sind auch Schritte.", "🌱"),
        DailyDelight(DailyDelight.Kind.MINDFULNESS, "Atme dreimal tief ein – und wieder aus.", "🌬️"),
        DailyDelight(DailyDelight.Kind.MOOD, "Wofür bist du heute dankbar?", "✨"),
        DailyDelight(DailyDelight.Kind.QUOTE, "Du musst nicht alles auf einmal schaffen.", "🍃"),
        DailyDelight(DailyDelight.Kind.MINDFULNESS, "Spür den Boden unter deinen Füßen.", "🦶"),
        DailyDelight(DailyDelight.Kind.QUOTE, "Auch ein langsamer Tag ist ein guter Tag.", "🐌"),
        DailyDelight(DailyDelight.Kind.MOOD, "Was hat dich zuletzt zum Lächeln gebracht?", "🙂"),
        DailyDelight(DailyDelight.Kind.MINDFULNESS, "Trink einen Schluck Wasser – ganz bewusst.", "💧"),
        DailyDelight(DailyDelight.Kind.QUOTE, "Sei sanft zu dir. Du gibst dein Bestes.", "🤍"),
        DailyDelight(DailyDelight.Kind.MOOD, "Welche Farbe hat dein Tag heute?", "🎨"),
        DailyDelight(DailyDelight.Kind.QUOTE, "Pausen sind kein Stillstand.", "☕"),
        DailyDelight(DailyDelight.Kind.MINDFULNESS, "Schau kurz aus dem Fenster. Was siehst du?", "🪟"),
        DailyDelight(DailyDelight.Kind.QUOTE, "Heute reicht, dass du da bist.", "🌤️"),
        DailyDelight(DailyDelight.Kind.MOOD, "Schenk dir heute ein kleines Ja.", "💛"),
    )
}
