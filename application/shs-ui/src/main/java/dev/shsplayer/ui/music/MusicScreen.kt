package dev.shsplayer.ui.music

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SHS Player — Music library screen.
 *
 * Original SHS Player code (clean-room reimplementation, simplified). 5 tabs:
 * Files, Folders, Favourites, Recent, Playlists. Reads audio via MediaStore.
 *
 * Tapping a song launches VLC's video player (via ACTION_VIEW with audio MIME)
 * since SHS Android uses LibVLC as the sole playback engine.
 */
data class MusicItem(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArt: Uri? = null,
)

data class MusicFolder(
    val name: String,
    val path: String,
    val itemCount: Int,
)

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Files", "Folders", "Favourites", "Recent", "Playlists")
    var songs by remember { mutableStateOf<List<MusicItem>>(emptyList()) }
    var folders by remember { mutableStateOf<List<MusicFolder>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedTab) {
        if (selectedTab in 0..1) {
            loading = true
            scope.launch {
                val (loadedSongs, loadedFolders) = withContext(Dispatchers.IO) {
                    val s = loadAudioFiles(context)
                    val f = loadAudioFolders(context, s)
                    s to f
                }
                songs = loadedSongs
                folders = loadedFolders
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) },
                )
            }
        }
        if (selectedTab == 0) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
            )
        }
        when (selectedTab) {
            0 -> if (loading) LoadingView() else FilesTab(songs, searchQuery, onPlay = { play(context, it) })
            1 -> if (loading) LoadingView() else FoldersTab(folders)
            2 -> EmptyView("Favourites", "Tap the heart icon on a song to add it here.")
            3 -> EmptyView("Recent", "Recently played songs will appear here.")
            4 -> EmptyView("Playlists", "Create playlists from the + button.")
        }
    }
}

@Composable
private fun FilesTab(songs: List<MusicItem>, search: String, onPlay: (MusicItem) -> Unit) {
    val filtered = if (search.isBlank()) songs
    else songs.filter { it.title.contains(search, ignoreCase = true) || it.artist.contains(search, ignoreCase = true) }
    LazyColumn {
        items(filtered) { song ->
            SongRow(song, onClick = { onPlay(song) })
        }
    }
}

@Composable
private fun FoldersTab(folders: List<MusicFolder>) {
    LazyColumn {
        items(folders) { folder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("${folder.itemCount} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: MusicItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatDuration(song.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyView(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

private fun play(context: Context, song: MusicItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(song.uri, "audio/*")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%d:%02d", m, s)
}

private fun loadAudioFiles(context: Context): List<MusicItem> {
    val songs = mutableListOf<MusicItem>()
    val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
    )
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
    context.contentResolver.query(collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} = 1", null, sortOrder)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val albumId = cursor.getLong(albumIdCol)
            val uri = ContentUris.withAppendedId(collection, id)
            val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
            songs.add(MusicItem(
                id = id,
                title = cursor.getString(titleCol) ?: "Unknown",
                artist = cursor.getString(artistCol) ?: "Unknown Artist",
                durationMs = cursor.getLong(durationCol),
                uri = uri,
                albumArt = albumArtUri,
            ))
        }
    }
    return songs
}

private fun loadAudioFolders(context: Context, songs: List<MusicItem>): List<MusicFolder> {
    val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    val folders = mutableMapOf<String, Int>()
    val projection = arrayOf(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
    context.contentResolver.query(collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} = 1", null, null)?.use { cursor ->
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameCol) ?: "Unknown"
            folders[name] = (folders[name] ?: 0) + 1
        }
    }
    return folders.map { (name, count) -> MusicFolder(name, "", count) }.sortedBy { it.name.lowercase() }
}
