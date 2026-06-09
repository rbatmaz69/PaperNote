package com.papernotes.domain.model

/** Inhalt des "Glücks-Teebeutels" — Gimmick des Tages. */
data class DailyDelight(
    val kind: Kind,
    val text: String,
    val emoji: String,
) {
    enum class Kind { QUOTE, MOOD, MINDFULNESS }
}
