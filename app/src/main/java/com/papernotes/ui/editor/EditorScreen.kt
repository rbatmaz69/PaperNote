package com.papernotes.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import com.papernotes.domain.toShareText
import com.papernotes.ui.components.ConfettiBurst
import com.papernotes.ui.components.DogEar
import com.papernotes.ui.components.MoodPickerSheet
import com.papernotes.ui.components.PaperBackground
import com.papernotes.ui.components.NoteLinkPickerSheet
import com.papernotes.ui.components.PaperPlaneOverlay
import com.papernotes.ui.components.PaperPlaneRequest
import com.papernotes.ui.components.ReminderSheet
import com.papernotes.ui.components.paperPress
import com.papernotes.util.findActivity
import com.papernotes.util.rememberPaperHaptics
import com.papernotes.util.sharePlainText

/**
 * "Clean Writing Mode": Beim Öffnen blenden die System-Leisten sanft aus – es bleibt nur
 * Papier, Text und Tastatur. Die Karte morpht via Shared-Element fließend in den Vollbild-Editor.
 * Checklisten-Notizen bekommen statt des Fließtexts den [ChecklistEditor].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EditorScreen(
    noteId: Long,
    newType: NoteType,
    session: Int,
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onOpenLinkedNote: (Long) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val haptics = rememberPaperHaptics()
    val view = LocalView.current
    val context = LocalContext.current

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Ergebnis egal: Alarm läuft, Notification erscheint erst nach Erteilung. */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(session) { viewModel.load(noteId, newType, session) }
    val note by viewModel.note.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val focusRequestId by viewModel.focusRequest.collectAsStateWithLifecycle()
    val linkedNotes by viewModel.linkedNotes.collectAsStateWithLifecycle()
    val candidateNotes by viewModel.candidateNotes.collectAsStateWithLifecycle()

    var showMood by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showLinkPicker by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableStateOf<Int?>(null) }
    var editorBounds by remember { mutableStateOf(Rect.Zero) }
    var shareRequest by remember { mutableStateOf<PaperPlaneRequest?>(null) }
    var shareText by remember { mutableStateOf("") }
    val bodyFocus = remember { FocusRequester() }
    val ink = MaterialTheme.colorScheme.onBackground
    // Stimmungswechsel: Editor-Hintergrund weich durchwaschen.
    val noteSurface by animateColorAsState(note.mood.cardSurface(), tween(320), label = "editorSurface")

    // Konfetti, wenn der letzte offene Eintrag abgehakt wurde
    LaunchedEffect(celebration) {
        if (celebration > 0) {
            haptics.confirm()
            confettiKey = celebration
        }
    }

    // System-Leisten ausblenden, beim Verlassen wiederherstellen.
    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // Bei neuer Text-Notiz direkt Tastatur/Fokus (Checkliste fokussiert ihre erste Zeile selbst).
    LaunchedEffect(Unit) {
        if (noteId <= 0L && newType == NoteType.TEXT) bodyFocus.requestFocus()
    }

    fun goBack() {
        viewModel.flush()
        onBack()
    }

    BackHandler { goBack() }

    val insets = WindowInsets.safeDrawing.asPaddingValues()

    PaperBackground(
        baseColor = note.mood.cardSurface(),
        dotGrid = true,
    ) {
        with(sharedScope) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { editorBounds = it.boundsInRoot() }
                    .sharedBounds(
                        rememberSharedContentState(key = "note-$noteId"),
                        animatedVisibilityScope = animatedScope,
                    )
                    .padding(
                        top = insets.calculateTopPadding() + 8.dp,
                        bottom = 8.dp,
                    )
                    .imePadding(),
            ) {
                // Kopfzeile: Zurück + Eselsohr (Stimmung)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {
                        haptics.tap()
                        goBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Zurück",
                            tint = ink,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DogEar(
                            folded = note.dogEarFolded,
                            accent = note.mood.earAccent(),
                            onToggle = {
                                haptics.tick()
                                viewModel.toggleDogEar()
                            },
                            onLongPress = { showMood = true },
                        )
                        // Sichtbarer Einstieg zu Stimmung / Anheften / Löschen
                        IconButton(onClick = {
                            haptics.tap()
                            showMood = true
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Optionen",
                                tint = ink,
                            )
                        }
                    }
                }

                // Sprung-Chips zu per rotem Faden verknüpften Notizen
                if (linkedNotes.isNotEmpty()) {
                    LinkedNoteChips(
                        notes = linkedNotes,
                        onOpen = { id ->
                            haptics.tap()
                            viewModel.flush()
                            onOpenLinkedNote(id)
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                if (note.type == NoteType.CHECKLIST) {
                    ChecklistEditor(
                        items = items,
                        focusRequestId = focusRequestId,
                        accentColor = note.mood.earAccent(),
                        onConsumeFocusRequest = viewModel::consumeFocusRequest,
                        onToggle = viewModel::toggleItem,
                        onTextChange = viewModel::setItemText,
                        onAddAfter = viewModel::addItem,
                        onRemove = viewModel::removeItem,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        header = {
                            TitleField(
                                title = note.title,
                                onTitleChange = viewModel::onTitleChange,
                                onNext = {},
                            )
                        },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        TitleField(
                            title = note.title,
                            onTitleChange = viewModel::onTitleChange,
                            onNext = { bodyFocus.requestFocus() },
                        )

                        BasicTextField(
                            value = note.body,
                            onValueChange = viewModel::onBodyChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                            cursorBrush = SolidColor(ink),
                            decorationBox = { inner ->
                                if (note.body.isEmpty()) {
                                    Text(
                                        text = "Schreib los …",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(bodyFocus)
                                .padding(bottom = 48.dp),
                        )
                    }
                }
            }
        }

        // Konfetti-Overlay über dem ganzen Editor
        confettiKey?.let { key ->
            Box(modifier = Modifier.fillMaxSize()) {
                ConfettiBurst(trigger = key, onFinished = { confettiKey = null })
            }
        }

        // Papierflieger-Animation → Android-Teilen-Auswahl
        shareRequest?.let { req ->
            PaperPlaneOverlay(
                request = req,
                onFinished = {
                    context.sharePlainText(shareText)
                    shareRequest = null
                },
            )
        }

        if (showMood) {
            MoodPickerSheet(
                selected = note.mood,
                pinned = note.pinned,
                hasReminder = note.hasReminder,
                onPick = {
                    haptics.tick()
                    viewModel.setMood(it)
                    showMood = false
                },
                onTogglePin = {
                    haptics.tap()
                    viewModel.togglePin()
                    showMood = false
                },
                onSetReminder = {
                    showMood = false
                    showReminder = true
                },
                onLink = {
                    showMood = false
                    showLinkPicker = true
                },
                onShare = {
                    showMood = false
                    val text = note.toShareText()
                    if (editorBounds != Rect.Zero) {
                        shareText = text
                        shareRequest = PaperPlaneRequest(note.id, editorBounds, noteSurface)
                    } else {
                        context.sharePlainText(text)
                    }
                },
                onDelete = {
                    showMood = false
                    haptics.crumple()
                    viewModel.moveToTrash { onBack() }
                },
                onDismiss = { showMood = false },
            )
        }

        if (showReminder) {
            ReminderSheet(
                currentReminderAt = note.reminderAt,
                onPick = { at ->
                    haptics.tick()
                    viewModel.setReminder(at)
                    ensureNotificationPermission()
                    showReminder = false
                },
                onClear = {
                    haptics.tap()
                    viewModel.setReminder(null)
                    showReminder = false
                },
                onDismiss = { showReminder = false },
            )
        }

        if (showLinkPicker) {
            val linkedIds = linkedNotes.map { it.id }.toSet()
            NoteLinkPickerSheet(
                candidates = candidateNotes,
                linkedIds = linkedIds,
                onToggle = { otherId ->
                    haptics.tick()
                    if (otherId in linkedIds) {
                        viewModel.unlinkNotes(otherId)
                    } else {
                        viewModel.linkNotes(otherId)
                    }
                },
                onDismiss = { showLinkPicker = false },
            )
        }
    }
}

/** Antippbare Chips ("🧵 Titel") zu verknüpften Notizen – Tap springt direkt hinüber. */
@Composable
private fun LinkedNoteChips(
    notes: List<com.papernotes.domain.model.Note>,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thread = Color(0xFFB3402F)
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        notes.forEach { linked ->
            Row(
                modifier = Modifier
                    .paperPress(RoundedCornerShape(50)) { onOpen(linked.id) }
                    .background(thread.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(thread, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = linked.title.ifBlank { linked.preview },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun TitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        textStyle = MaterialTheme.typography.headlineMedium.copy(color = ink),
        cursorBrush = SolidColor(ink),
        singleLine = true,
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        decorationBox = { inner ->
            if (title.isEmpty()) {
                Text(
                    text = "Titel",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            inner()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
    )
}
