package com.papernotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val CalendarRed = Color(0xFFB3402F)
private val PageCream = Color(0xFFFBF6EC)

/**
 * Kleines Abreiß-Tischkalenderblatt für eine Countdown-Notiz: roter Kopfstreifen mit
 * Bindungsringen, Monatskürzel, große Tageszahl und eine Kaption mit den Resttagen
 * („noch N" / „heute" / „vorbei"). Am Stichtag im [accent] hervorgehoben. Tap öffnet das
 * Countdown-Sheet zum Ändern/Abreißen.
 */
@Composable
fun CalendarPage(
    countdownAt: Long,
    now: Long,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = Instant.ofEpochMilli(countdownAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target)

    val month = target.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN).uppercase(Locale.GERMAN)
    val isToday = days == 0L
    val caption = when {
        days > 0L -> "noch $days"
        isToday -> "heute"
        else -> "vorbei"
    }
    val headerColor = if (isToday) accent else CalendarRed
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier = modifier
            .rotate(-4f)
            .width(52.dp)
            .shadow(3.dp, shape, clip = false)
            .background(PageCream, shape)
            .paperPress(shape, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Kopfstreifen mit Bindungsringen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .background(headerColor),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(PageCream, CircleShape),
                )
            }
        }
        Text(
            text = month,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = headerColor,
            modifier = Modifier.padding(top = 3.dp),
        )
        Text(
            text = "${target.dayOfMonth}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = if (isToday) accent else MaterialTheme.colorScheme.outline,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(bottom = 5.dp),
        )
    }
}
