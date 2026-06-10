package com.papernotes.domain

/**
 * Wählbares Stempel-Motiv einer Stempelkarte – der Tinten-Abdruck, der bei jedem gestempelten
 * Tag erscheint. [CHECK] ist der Standard (entspricht dem ursprünglichen Haken-Look).
 */
enum class StampMotif {
    CHECK,
    STAR,
    HEART,
    DROP,
    LEAF,
    SUN;

    companion object {
        fun fromOrdinal(value: Int): StampMotif = entries.getOrElse(value) { CHECK }
    }
}

/**
 * Kodiert eine Stempelkarte (Gewohnheit) im `body`-Feld einer Notiz: ein optionales
 * Motiv-Token `m<ordinal>` (z. B. `m1`), gefolgt von den gestempelten Tagen als
 * kommaseparierte Epoch-Tage (`java.time.LocalDate.toEpochDay()`).
 *
 * Wie beim [ChecklistCodec] braucht es dadurch keine zweite Room-Tabelle; Suche, Vorschau,
 * Archiv und Papierkorb funktionieren für Stempelkarten identisch zu anderen Notizen. Das
 * Motiv-Token wird von [parse] automatisch ignoriert (es ist kein gültiger Long).
 */
object StampCodec {

    private const val MOTIF_PREFIX = "m"

    fun parse(body: String): Set<Long> =
        body.split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()

    /** Liest das gewählte Motiv aus dem `body` (Token `m<ordinal>`); Fallback [StampMotif.CHECK]. */
    fun motif(body: String): StampMotif =
        body.split(',')
            .firstNotNullOfOrNull { token ->
                token.trim()
                    .takeIf { it.startsWith(MOTIF_PREFIX) }
                    ?.removePrefix(MOTIF_PREFIX)
                    ?.toIntOrNull()
                    ?.let { StampMotif.fromOrdinal(it) }
            }
            ?: StampMotif.CHECK

    fun serialize(days: Set<Long>, motif: StampMotif = StampMotif.CHECK): String =
        (listOf("$MOTIF_PREFIX${motif.ordinal}") + days.sorted().map { it.toString() })
            .joinToString(",")

    fun isStamped(days: Set<Long>, day: Long): Boolean = day in days

    /** Gesamtzahl der gestempelten Tage. */
    fun total(days: Set<Long>): Int = days.size

    /**
     * Länge der aktuellen Serie: aufeinanderfolgende gestempelte Tage, die an [today] enden –
     * oder an [today]−1, falls heute (noch) nicht gestempelt ist (die Serie „lebt", bis ein
     * Tag ausgelassen wurde). 0, wenn weder heute noch gestern gestempelt ist.
     */
    fun streak(days: Set<Long>, today: Long): Long {
        if (days.isEmpty()) return 0L
        val anchor = when {
            today in days -> today
            (today - 1) in days -> today - 1
            else -> return 0L
        }
        var count = 0L
        var day = anchor
        while (day in days) {
            count++
            day--
        }
        return count
    }

    /** Längste je erreichte Serie zusammenhängender gestempelter Tage (für die „Rekord"-Anzeige). */
    fun longestStreak(days: Set<Long>): Long {
        if (days.isEmpty()) return 0L
        val sorted = days.sorted()
        var best = 1L
        var current = 1L
        for (i in 1 until sorted.size) {
            current = if (sorted[i] == sorted[i - 1] + 1) current + 1 else 1L
            if (current > best) best = current
        }
        return best
    }
}
