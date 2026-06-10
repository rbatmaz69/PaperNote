package com.papernotes.domain

import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import java.time.LocalDate

private const val DONE_GLYPH = "☑" // ☑
private const val OPEN_GLYPH = "☐" // ☐

/**
 * Wandelt eine Notiz in einen schön lesbaren Klartext zum Teilen um (Papierflieger ✈️).
 *
 * Text-Notizen werden als Titel + Leerzeile + Fließtext exportiert, Checklisten mit
 * hübschen Kästchen-Glyphen (☑/☐) statt der internen `[x]`/`[ ]`-Kodierung
 * (siehe [com.papernotes.domain.ChecklistCodec]). Leere Felder werden weggelassen.
 */
fun Note.toShareText(): String {
    val title = title.trim()

    val content = when (type) {
        NoteType.CHECKLIST ->
            checklist.joinToString("\n") { item ->
                (if (item.checked) DONE_GLYPH else OPEN_GLYPH) + " " + item.text
            }
        NoteType.STAMPCARD -> {
            val today = LocalDate.now().toEpochDay()
            "Strähne: ${StampCodec.streak(stamps, today)} Tage · ${StampCodec.total(stamps)}× gestempelt"
        }
        NoteType.TEXT -> body.trim()
    }

    return listOf(title, content)
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
        .trim()
}
