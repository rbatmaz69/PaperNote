package com.papernotes.domain.model

/**
 * Liniierung des Notizpapiers: blanko, liniert, kariert oder gepunktet. Wird zart hinter
 * dem Text gezeichnet (siehe `Modifier.paperRuling`).
 */
enum class PaperStyle(val label: String) {
    BLANK("Blanko"),
    LINED("Liniert"),
    GRID("Kariert"),
    DOTTED("Gepunktet");

    companion object {
        fun fromName(name: String?): PaperStyle =
            entries.firstOrNull { it.name == name } ?: BLANK
    }
}
