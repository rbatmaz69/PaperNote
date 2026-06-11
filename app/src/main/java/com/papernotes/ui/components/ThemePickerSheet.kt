package com.papernotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.ui.theme.PaperTheme
import com.papernotes.util.rememberPaperHaptics

/**
 * Bottom-Sheet zur Theme-Wahl: ein Raster aus Papier-Swatches. AUTO erscheint als
 * geteilter Hell/Dunkel-Kreis. Auswahl wirkt sofort app-weit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(
    selected: PaperTheme,
    onPick: (PaperTheme) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    val ink = MaterialTheme.colorScheme.onBackground

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Papier wählen",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(PaperTheme.selectable, key = { it.name }) { theme ->
                    ThemeSwatch(
                        theme = theme,
                        selected = theme == selected,
                        onClick = {
                            haptics.tap()
                            onPick(theme)
                        },
                    )
                }
            }

            Text(
                text = "Sicherung",
                style = MaterialTheme.typography.titleMedium,
                color = ink,
                modifier = Modifier.padding(top = 8.dp),
            )
            BackupRow(icon = Icons.Rounded.Save, label = "Sichern") {
                haptics.tap()
                onExport()
            }
            BackupRow(icon = Icons.Rounded.FileOpen, label = "Importieren") {
                haptics.tap()
                onImport()
            }
        }
    }
}

@Composable
private fun BackupRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .paperPress(RoundedCornerShape(14.dp), onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ink)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

@Composable
private fun ThemeSwatch(
    theme: PaperTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.paperPress(RoundedCornerShape(16.dp), onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (theme == PaperTheme.AUTO) {
                // Geteilter Kreis: hell (Tagespapier) / dunkel (Mitternacht)
                Canvas(
                    modifier = Modifier
                        .size(54.dp)
                        .then(
                            if (selected) Modifier.border(2.5.dp, ink, CircleShape) else Modifier,
                        ),
                ) {
                    val r = size.minDimension / 2f
                    drawCircle(PaperTheme.DAYLIGHT.paper, radius = r)
                    drawArc(
                        color = PaperTheme.MIDNIGHT.paper,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(0f, 0f),
                        size = size,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(theme.paper, CircleShape)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) ink else theme.inkFaded.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (theme.dark || theme == PaperTheme.AUTO) Color.White else theme.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
