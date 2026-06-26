package dev.shsplayer.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.sp
import dev.shsplayer.features.liveTv.DefaultIptvPlaylists
import dev.shsplayer.features.liveTv.IptvCategory
import dev.shsplayer.features.liveTv.IptvChannel
import dev.shsplayer.features.liveTv.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ShsFeaturesActivity — Compose-based activity that hosts the SHS Player custom
 * features (Live TV, Privacy Vault, Wi-Fi Transfer, QR Scanner).
 *
 * Launched from the "SHS Features" card in VLC's MoreFragment. Accepts an
 * `EXTRA_FEATURE` int that determines which screen to show:
 *   - FEATURE_LIVE_TV: IPTV channel browser with categorized playlists
 *   - FEATURE_VAULT: Privacy Vault PIN entry + file list (TODO)
 *   - FEATURE_TRANSFER: Wi-Fi File Transfer QR code (TODO)
 *   - FEATURE_QR: QR scanner camera view (TODO)
 *
 * Currently only Live TV is fully wired (with channel list + play intent).
 * The other 3 features show a "coming soon" placeholder screen with usage
 * instructions for calling the API directly from code.
 */
class ShsFeaturesActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FEATURE = "feature"
        const val FEATURE_LIVE_TV = 0
        const val FEATURE_VAULT = 1
        const val FEATURE_TRANSFER = 2
        const val FEATURE_QR = 3

        fun launch(context: Context, feature: Int) {
            val intent = Intent(context, ShsFeaturesActivity::class.java).apply {
                putExtra(EXTRA_FEATURE, feature)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val feature = intent.getIntExtra(EXTRA_FEATURE, FEATURE_LIVE_TV)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (feature) {
                        FEATURE_LIVE_TV -> LiveTvScreen(onBack = { finish() })
                        FEATURE_VAULT -> PlaceholderScreen("Privacy Vault", """
                            |Privacy Vault API:
                            |
                            |1. Setup: VaultManager.setupVault(ctx, "1234", "question?", "answer")
                            |2. Verify: VaultManager.verifyPassword(ctx, "1234")
                            |3. Move file: VaultManager.moveToVault(ctx, uri, "video")
                            |4. Restore: VaultManager.restoreFromVault(ctx, vaultFile)
                            |5. Delete: VaultManager.deleteFromVault(ctx, vaultFile)
                            |
                            |Vault files are stored at:
                            |context.filesDir/vault/{videos,music}/
                            |
                            |Files are sandboxed (invisible to gallery + other apps).
                            |Secure-erase on delete (overwrite with zeroes).
                        """.trimMargin(), onBack = { finish() })
                        FEATURE_TRANSFER -> PlaceholderScreen("Wi-Fi Transfer", """
                            |Wi-Fi Transfer API:
                            |
                            |val server = WifiTransferServer(context, port = 12345) { file ->
                            |    println("Received: " + file.name)
                            |}
                            |server.start()
                            |
                            |val ip = WifiTransferUtils.getWifiIpAddress(context)
                            |val url = "http://" + ip + ":12345?token=" + server.authToken
                            |val qrBitmap = WifiTransferUtils.generateQrCodeBitmap(url)
                            |
                            |Sender opens URL in browser, uploads files, server saves
                            |to context.filesDir/received/
                            |
                            |Auth: 16-char UUID token in query/header.
                            |No internet needed. Stays on local Wi-Fi.
                        """.trimMargin(), onBack = { finish() })
                        FEATURE_QR -> PlaceholderScreen("QR Scanner", """
                            |QR Scanner Compose API:
                            |
                            |QrScannerView(
                            |    onResult = { scannedValue ->
                            |        // scannedValue is the URL/text in the QR
                            |    },
                            |    modifier = Modifier.fillMaxSize()
                            |)
                            |
                            |Uses CameraX + MLKit Barcode Scanning.
                            |Binds to Activity LifecycleOwner (not Dialog).
                            |PERFORMANCE PreviewView mode.
                        """.trimMargin(), onBack = { finish() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(DefaultIptvPlaylists.availableCategories.firstOrNull() ?: IptvCategory.FREE) }
    var channels by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedCategory) {
        loading = true
        channels = emptyList()
        scope.launch {
            val playlists = DefaultIptvPlaylists.forCategory(selectedCategory)
            val results = withContext(Dispatchers.IO) {
                playlists.flatMap { M3uParser.parse(context, it.url) }
            }
            channels = results.distinctBy { it.url }
            loading = false
        }
    }

    val filteredChannels = if (searchQuery.isBlank()) channels
    else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live TV", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Category chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(DefaultIptvPlaylists.availableCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) },
                    )
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${selectedCategory.displayName} channels…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Loading ${selectedCategory.displayName} channels…")
                        }
                    }
                }
                filteredChannels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredChannels) { channel ->
                            ChannelRow(channel) {
                                // Launch VLC player via ACTION_VIEW
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(android.net.Uri.parse(channel.url), "video/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: IptvChannel, onClick: () -> Unit) {
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, instructions: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Feature ready — UI coming soon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This feature's backend is fully implemented in the " +
                            "shs-features module and ready to use. A polished UI " +
                            "screen will be added in the next release.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Developer API:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E),
                ),
            ) {
                Text(
                    text = instructions,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
