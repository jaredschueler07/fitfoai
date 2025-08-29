package com.runningcoach.v2.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spotify.protocol.types.ListItem

@Composable
fun PlaylistSelectionDialog(
    playlists: List<ListItem>,
    onPlaylistSelected: (ListItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a Playlist") },
        text = {
            LazyColumn {
                items(playlists) { playlist ->
                    Text(
                        text = playlist.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistSelected(playlist) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}