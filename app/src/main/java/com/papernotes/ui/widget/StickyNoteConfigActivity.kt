package com.papernotes.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.ui.theme.PaperTheme
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.components.paperPress
import com.papernotes.ui.theme.PaperNotesTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Konfigurations-Bildschirm beim Platzieren des Haftnotiz-Widgets: Notiz **und** Widget-Farbe
 * wählen. Speichert beides im pro-Instanz-State und aktualisiert gezielt dieses Widget.
 */
@AndroidEntryPoint
class StickyNoteConfigActivity : ComponentActivity() {

    @Inject lateinit var repository: NoteRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, resultIntent())

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            PaperNotesTheme {
                val notes by repository.observeActiveNotes().collectAsState(initial = emptyList())
                ConfigScreen(notes = notes, onPick = ::choose)
            }
        }
    }

    private fun choose(note: Note, palette: String) {
        // Synchron speichern, dann direkt rendern (klassisches updateAppWidget wirkt sofort).
        WidgetPrefs.setConfig(this, appWidgetId, note.id, palette)
        StickyNoteWidgets.update(applicationContext, appWidgetId)
        android.util.Log.d("StickyWidget", "config saved id=$appWidgetId note=${note.id} palette=$palette")
        setResult(RESULT_OK, resultIntent())
        finish()
    }

    private fun resultIntent() =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

/** Auswahlmöglichkeiten der Widget-Farbe: Stimmung + die festen Papier-Themes. */
private data class PaletteChoice(val key: String, val label: String, val swatch: Color, val ring: Color)

private val paletteChoices: List<PaletteChoice> = buildList {
    add(PaletteChoice(PALETTE_MOOD, "Stimmung", Color(0xFFE7DFD2), Color(0xFF3A322E)))
    PaperTheme.selectable
        .filter { it != PaperTheme.AUTO }
        .forEach { add(PaletteChoice(it.name, it.label, it.paper, it.ink)) }
}

@Composable
private fun ConfigScreen(notes: List<Note>, onPick: (Note, String) -> Unit) {
    var palette by remember { mutableStateOf(PALETTE_MOOD) }

    PaperBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
        ) {
            Text(
                text = "Widget-Farbe",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                paletteChoices.forEach { choice ->
                    val selected = choice.key == palette
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .paperPress(CircleShape) { palette = choice.key }
                                .background(choice.swatch, CircleShape)
                                .then(
                                    if (selected) Modifier.border(3.dp, choice.ring, CircleShape)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                ),
                        )
                        Text(
                            text = choice.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            StickyNotePreview(note = notes.firstOrNull(), palette = palette)

            Text(
                text = "Welche Notiz?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notes, key = { it.id }) { note ->
                    NoteRow(note = note, onClick = { onPick(note, palette) })
                }
            }
        }
    }
}

/** Live-Vorschau des Haftnotiz-Widgets: spiegelt sofort die gewählte Farbe wider. */
@Composable
private fun StickyNotePreview(note: Note?, palette: String) {
    val night = isSystemInDarkTheme()
    val mood = note?.mood ?: MoodCategory.PLAIN
    val (paper, ink) = resolveColors(palette, mood, night)
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .background(paper, shape)
            .padding(16.dp),
    ) {
        Text(
            text = note?.title?.ifBlank { "Vorschau" } ?: "Vorschau",
            style = MaterialTheme.typography.titleMedium,
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = note?.preview?.ifBlank { "So sieht dein Zettel aus." } ?: "So sieht dein Zettel aus.",
            style = MaterialTheme.typography.bodyMedium,
            color = ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .paperPress(shape) { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(16.dp),
    ) {
        Text(
            text = note.title.ifBlank { note.preview },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (note.title.isNotBlank() && note.preview.isNotBlank()) {
            Text(
                text = note.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
