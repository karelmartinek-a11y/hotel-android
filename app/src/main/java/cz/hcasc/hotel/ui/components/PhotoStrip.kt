package cz.hcasc.hotel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * PhotoStrip
 *
 * Zobrazuje 5 slotů pro fotky (miniatury). I když je méně než 5 fotek,
 * stále je vidět obrys všech 5 pozic, aby UX odpovídalo zadání.
 *
 * - Bez scrollování.
 * - Kliknutí může otevřít náhled / odebrání / přidání (dle volající obrazovky).
 * - Neřeší samotné načítání obrázků (to je záměrně mimo, aby komponenta byla lehká).
 */
@Composable
fun PhotoStrip(
    photoCount: Int,
    maxPhotos: Int = 5,
    slotSize: Dp = 56.dp,
    slotSpacing: Dp = 10.dp,
    modifier: Modifier = Modifier,
    onSlotClick: (index: Int, isFilled: Boolean) -> Unit
) {
    val count = photoCount.coerceIn(0, maxPhotos)

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(slotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxPhotos) {
            val filled = i < count
            PhotoSlot(
                index = i,
                filled = filled,
                size = slotSize,
                onClick = { onSlotClick(i, filled) }
            )
        }
    }
}

@Composable
private fun PhotoSlot(
    index: Int,
    filled: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    val borderColor = when {
        filled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    }

    val bgColor = when {
        filled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    Box(
        modifier = Modifier
            .size(size)
            .aspectRatio(1f)
            .clip(shape)
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Placeholder: skutečnou miniaturu vykreslí volající obrazovka (např. AsyncImage)
        // přes parametr, pokud bude potřeba. Zde jen indikace obsazenosti slotu.
        if (!filled) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        } else {
            // Jemný index (1..5) jako fallback, když ještě není thumbnail.
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }

        // Malý roh indikující obsazenost (subtilně)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    if (filled) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                )
        )
    }
}
