package com.papernotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.earAccent
import com.papernotes.ui.theme.Terracotta

/**
 * Bottom-Sheet als Kontext-Menü einer Notiz: Stimmung (Eselsohr-Farbe) wählen,
 * mit Washi-Tape anheften/lösen, oder zerknüllen & löschen ([onDelete]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodPickerSheet(
    selected: MoodCategory,
    pinned: Boolean,
    hasReminder: Boolean,
    onPick: (MoodCategory) -> Unit,
    onTogglePin: () -> Unit,
    onSetReminder: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Stimmung",
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MoodCategory.entries.forEach { mood ->
                    val isSelected = mood == selected
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(mood.earAccent(), CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, ink, CircleShape)
                                } else Modifier,
                            )
                            .clickable { onPick(mood) },
                    )
                }
            }

            // Washi-Tape: anheften / lösen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onTogglePin() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = ink,
                )
                Text(
                    text = if (pinned) "Washi-Tape lösen" else "Mit Washi-Tape anheften",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                )
            }

            // Erinnerung setzen / ändern
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onSetReminder() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = ink,
                )
                Text(
                    text = if (hasReminder) "Erinnerung ändern" else "Erinnerung setzen",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                )
            }

            // Als Papierflieger teilen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onShare() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    tint = ink,
                )
                Text(
                    text = "Als Papierflieger teilen",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Terracotta.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .clickable { onDelete() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color(0xFFC97B5F),
                )
                Text(
                    text = "Zerknüllen & in den Papierkorb",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFC97B5F),
                )
            }
        }
    }
}
