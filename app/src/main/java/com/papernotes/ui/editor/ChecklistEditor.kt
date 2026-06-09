package com.papernotes.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.papernotes.ui.components.PaperCheckbox
import com.papernotes.util.rememberPaperHaptics

/**
 * Editor-Body für Checklisten-Notizen: handgezeichnete Checkboxen, Enter legt die nächste
 * Zeile an, Backspace auf leerer Zeile löscht sie, Erledigtes wird durchgestrichen und
 * sinkt animiert nach unten ([androidx.compose.foundation.lazy.LazyItemScope.animateItem]).
 *
 * [header] rendert den Titel-Bereich als erstes Listen-Element (gleicher Scroll-Container).
 */
@Composable
fun ChecklistEditor(
    items: List<EditableChecklistItem>,
    focusRequestId: Long?,
    accentColor: Color,
    onConsumeFocusRequest: () -> Unit,
    onToggle: (Long) -> Unit,
    onTextChange: (Long, String) -> Unit,
    onAddAfter: (Long?) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    val ink = MaterialTheme.colorScheme.onBackground

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header") { header() }

        items(items, key = { it.uiId }) { item ->
            val focusRequester = remember { FocusRequester() }
            val textAlpha by animateFloatAsState(
                targetValue = if (item.checked) 0.45f else 1f,
                animationSpec = tween(220),
                label = "itemAlpha",
            )

            LaunchedEffect(focusRequestId) {
                if (focusRequestId == item.uiId) {
                    focusRequester.requestFocus()
                    onConsumeFocusRequest()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PaperCheckbox(
                    checked = item.checked,
                    color = accentColor,
                    inkColor = ink,
                    modifier = Modifier.clickable {
                        haptics.tick()
                        onToggle(item.uiId)
                    },
                )

                BasicTextField(
                    value = item.text,
                    onValueChange = { onTextChange(item.uiId, it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = ink,
                        textDecoration =
                            if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    cursorBrush = SolidColor(ink),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (item.text.isNotBlank()) onAddAfter(item.uiId)
                        },
                    ),
                    decorationBox = { inner ->
                        if (item.text.isEmpty()) {
                            Text(
                                text = "Eintrag …",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        inner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .alpha(textAlpha)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Backspace && item.text.isEmpty()) {
                                onRemove(item.uiId)
                                true
                            } else {
                                false
                            }
                        },
                )

                // Sichtbares Löschen pro Zeile (Soft-Tastatur-tauglich)
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Eintrag löschen",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            haptics.tap()
                            onRemove(item.uiId)
                        },
                )
            }
        }

        item(key = "add") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .clickable {
                        haptics.tap()
                        onAddAfter(null)
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "＋  Eintrag",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
