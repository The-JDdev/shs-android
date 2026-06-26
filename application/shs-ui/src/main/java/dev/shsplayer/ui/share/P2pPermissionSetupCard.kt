package dev.shsplayer.ui.share

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * SHS Player — modern P2P permission + auto-enable UX.
 *
 * Original SHS Player code (clean-room reimplementation). Shows live status
 * chips (Wi-Fi / Location / Permissions) + stepped action button. Uses
 * ActivityResultContracts to open system settings for Wi-Fi/Location.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun P2pPermissionSetupCard(
    onAllReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var wifiEnabled by remember { mutableStateOf(isWifiEnabled(context)) }
    var locationEnabled by remember { mutableStateOf(isLocationEnabled(context)) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        wifiEnabled = isWifiEnabled(context)
        locationEnabled = isLocationEnabled(context)
    }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
    }
    val permissionsState = rememberMultiplePermissionsState(permissions = requiredPermissions)
    val allPermsGranted = permissionsState.revokedPermissions.isEmpty()
    val allReady = wifiEnabled && locationEnabled && allPermsGranted

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("P2P Sharing Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "To send and receive files over local Wi-Fi, SHS Player needs Wi-Fi and Location turned on. " +
                    "Location is required by Android to identify nearby devices — it is never stored or transmitted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Wi-Fi", wifiEnabled, Modifier.weight(1f))
                StatusChip("Location", locationEnabled, Modifier.weight(1f))
                StatusChip("Permissions", allPermsGranted, Modifier.weight(1f))
            }

            Button(
                onClick = {
                    when {
                        !wifiEnabled -> {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Intent(Settings.ACTION_WIFI_SETTINGS)
                            } else {
                                @Suppress("DEPRECATION")
                                val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                @Suppress("DEPRECATION")
                                wm.isWifiEnabled = true
                                null
                            }
                            intent?.let { settingsLauncher.launch(it) }
                        }
                        !locationEnabled -> {
                            settingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        !allPermsGranted -> permissionsState.launchMultiplePermissionRequest()
                        else -> onAllReady()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                val label = when {
                    !wifiEnabled -> "Tap to turn on Wi-Fi"
                    !locationEnabled -> "Tap to turn on Location"
                    !allPermsGranted -> "Grant permissions"
                    else -> "Start sharing files"
                }
                Text(label, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (!wifiEnabled) Icons.Default.Wifi else Icons.Default.LocationOn,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, enabled: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$label: ",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                if (enabled) "ON" else "OFF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

private fun isWifiEnabled(context: Context): Boolean {
    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return false
    return wm.isWifiEnabled
}

private fun isLocationEnabled(context: Context): Boolean {
    val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        lm.isLocationEnabled
    } else {
        @Suppress("DEPRECATION")
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
