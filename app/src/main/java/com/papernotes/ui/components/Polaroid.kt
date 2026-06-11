package com.papernotes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import com.papernotes.util.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TapeTan = Color(0xFFCBB994)
private val PhotoBackdrop = Color(0xFFEDEDE6)

/** Dekodiert das App-interne Foto [name] (auf [Dispatchers.IO]); null während des Ladens/fehlt. */
@Composable
fun rememberPhotoBitmap(name: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, key1 = name) {
        value = withContext(Dispatchers.IO) {
            val file = PhotoStore.file(context, name)
            if (file.exists()) {
                runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()
            } else {
                null
            }
        }
    }.value
}

/**
 * Ein angehängtes Foto als aufgeklebtes Polaroid: weißer Rahmen mit breiterem unteren Rand,
 * leicht gekippt, Klebestreifen oben, weicher Schatten. [onRemove] (Editor) blendet ein ✕ ein.
 */
@Composable
fun Polaroid(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    rotation: Float = -2.5f,
    maxImageHeight: Dp = 180.dp,
) {
    val bitmap = rememberPhotoBitmap(name)
    val frame = RoundedCornerShape(3.dp)

    Box(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        Column(
            modifier = Modifier
                .shadow(6.dp, frame, clip = false)
                .background(Color.White, frame)
                .paperPress(frame, onClick = onClick)
                .padding(6.dp)
                .padding(bottom = 18.dp),
        ) {
            val imageShape = RoundedCornerShape(2.dp)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxImageHeight)
                        .clip(imageShape),
                )
            } else {
                // Platzhalterfläche, solange das Bild dekodiert wird.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(imageShape)
                        .background(PhotoBackdrop),
                )
            }
        }

        // Klebestreifen oben mittig – wie aufgeklebt.
        WashiTape(
            color = TapeTan,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp),
        )

        // Entfernen (nur wenn [onRemove] gesetzt, d.h. im Editor).
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .paperPress(CircleShape) { onRemove() }
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Foto entfernen",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
