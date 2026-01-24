package cz.hcasc.hotel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Room picker grid per SPEC:
 * - 29 tiles total
 * - MUST be shown without scrolling in the intended layout.
 *   (The parent screen should allocate enough height; this composable itself does not force scroll.)
 *
 * Rooms:
 * 101–109, 201–210, 301–310
 */
@Composable
fun RoomGrid(
    selectedRoom: Int?,
    onRoomSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 5,
    tileHeight: Dp = 44.dp,
    contentPadding: PaddingValues = PaddingValues(8.dp),
) {
    val rooms = buildRooms()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding
    ) {
        items(rooms, key = { it }) { room ->
            RoomTile(
                room = room,
                selected = selectedRoom == room,
                height = tileHeight,
                onClick = { onRoomSelected(room) }
            )
        }
    }
}

@Composable
private fun RoomTile(
    room: Int,
    selected: Boolean,
    height: Dp,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium

    val bg = if (selected) {
        // Accent surface (cyan-ish) but keep dark theme.
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val border = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(width = 1.dp, color = border, shape = shape)
            .height(height)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = room.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun buildRooms(): List<Int> {
    val out = ArrayList<Int>(29)
    for (r in 101..109) out.add(r)
    for (r in 201..210) out.add(r)
    for (r in 301..310) out.add(r)
    return out
}
