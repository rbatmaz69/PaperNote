package com.papernotes.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.ui.components.NoteCard
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.theme.PaperNotesTheme

private val sampleJoy = Note(
    id = 1,
    title = "Sonntagsgedanke",
    body = "Kaffee, ein offenes Fenster und kein Plan – genau richtig.",
    mood = MoodCategory.JOY,
    dogEarFolded = true,
    pinned = true,
)

private val sampleCalm = Note(
    id = 2,
    title = "Atemübung",
    body = "4 Sekunden ein, 6 Sekunden aus. Dreimal.",
    mood = MoodCategory.CALM,
    dogEarFolded = true,
)

private val sampleChecklist = Note(
    id = 3,
    title = "Markt",
    body = "[x] Salbei\n[ ] Zitronen\n[ ] Honig",
    type = NoteType.CHECKLIST,
    mood = MoodCategory.FOCUS,
    dogEarFolded = true,
)

@Preview(name = "Paper background", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun PaperBackgroundPreview() {
    PaperNotesTheme {
        PaperBackground(modifier = Modifier.fillMaxSize()) {}
    }
}

@Preview(name = "Note cards", showBackground = true, widthDp = 280, heightDp = 560)
@Composable
private fun NoteCardPreview() {
    PaperNotesTheme {
        PaperBackground {
            Column(
                modifier = Modifier.padding(20.dp).width(240.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                NoteCard(sampleJoy, {}, {}, {})
                NoteCard(sampleCalm, {}, {}, {})
                NoteCard(sampleChecklist, {}, {}, {})
            }
        }
    }
}

@Preview(
    name = "Mitternachtspapier",
    showBackground = true,
    widthDp = 280,
    heightDp = 560,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NoteCardNightPreview() {
    PaperNotesTheme {
        PaperBackground {
            Column(
                modifier = Modifier.padding(20.dp).width(240.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                NoteCard(sampleJoy, {}, {}, {})
                NoteCard(sampleChecklist, {}, {}, {})
            }
        }
    }
}
