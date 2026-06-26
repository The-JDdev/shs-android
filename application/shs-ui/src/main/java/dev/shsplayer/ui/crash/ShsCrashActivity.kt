package dev.shsplayer.ui.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.shsplayer.ui.theme.ShsPlayerTheme

/**
 * SHS Player — crash reporter activity.
 *
 * Original SHS Player code (clean-room reimplementation). Shown when the app
 * crashes. Displays the exception stack trace + logcat output, with Share,
 * Copy, and Restart buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
class ShsCrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val exception = intent.getStringExtra(ShsGlobalExceptionHandler.EXTRA_EXCEPTION) ?: "Unknown error"
        val logcat = intent.getStringExtra(ShsGlobalExceptionHandler.EXTRA_LOGCAT) ?: ""
        setContent {
            ShsPlayerTheme {
                CrashScreen(
                    exception = exception,
                    logcat = logcat,
                    onShare = { shareCrash(exception, logcat) },
                    onCopy = { copyToClipboard("$exception\n\n$logcat") },
                    onRestart = { restartApp() },
                )
            }
        }
    }

    private fun shareCrash(exception: String, logcat: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SHS Player crash report")
            putExtra(Intent.EXTRA_TEXT, "$exception\n\n--- LOGCAT ---\n$logcat")
        }
        startActivity(Intent.createChooser(intent, "Share crash report"))
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("SHS crash", text))
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun restartApp() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashScreen(
    exception: String,
    logcat: String,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onRestart: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHS Player crashed", fontWeight = FontWeight.Bold) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = "The app encountered an unexpected error. You can share the crash details below to help fix it.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text("Crash log:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            ) {
                Text(
                    text = exception,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState()),
                )
            }
            if (logcat.isNotBlank()) {
                Text("Logcat (last 500 lines):", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                ) {
                    Text(
                        text = logcat,
                        color = Color(0xFFB0B0B0),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
                Button(onClick = onRestart, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Restart")
                }
            }
        }
    }
}
