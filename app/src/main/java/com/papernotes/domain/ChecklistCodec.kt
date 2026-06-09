package com.papernotes.domain

/** Ein Eintrag einer Checklisten-Notiz. */
data class ChecklistItem(
    val text: String,
    val checked: Boolean,
)

/**
 * Kodiert Checklisten-Einträge als Zeilen im `body`-Feld einer Notiz:
 * `[ ] offener Eintrag` / `[x] erledigter Eintrag`.
 *
 * Dadurch braucht es keine zweite Room-Tabelle, und Suche, Vorschau, Archiv und
 * Papierkorb funktionieren für Text- und Checklisten-Notizen identisch.
 */
object ChecklistCodec {

    private const val UNCHECKED_PREFIX = "[ ] "
    private const val CHECKED_PREFIX = "[x] "

    fun parse(body: String): List<ChecklistItem> =
        body.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                when {
                    line.startsWith(CHECKED_PREFIX) ->
                        ChecklistItem(line.removePrefix(CHECKED_PREFIX), checked = true)
                    line.startsWith(UNCHECKED_PREFIX) ->
                        ChecklistItem(line.removePrefix(UNCHECKED_PREFIX), checked = false)
                    // tolerant gegenüber von Hand editierten Zeilen
                    else -> ChecklistItem(line, checked = false)
                }
            }

    fun serialize(items: List<ChecklistItem>): String =
        items.joinToString("\n") { item ->
            (if (item.checked) CHECKED_PREFIX else UNCHECKED_PREFIX) + item.text
        }

    fun progress(items: List<ChecklistItem>): Pair<Int, Int> =
        items.count { it.checked } to items.size

    fun allDone(items: List<ChecklistItem>): Boolean =
        items.isNotEmpty() && items.all { it.checked }
}
