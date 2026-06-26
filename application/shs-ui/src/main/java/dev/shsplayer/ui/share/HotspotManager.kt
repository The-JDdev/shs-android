package dev.shsplayer.ui.share

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.security.SecureRandom

/**
 * SHS Player — local-only hotspot manager.
 *
 * Original SHS Player code (clean-room reimplementation, inspired by the
 * TrebleShot open-source project). Wraps WifiManager.startLocalOnlyHotspot
 * (API 26+) for sending files without an existing Wi-Fi network. Generates
 * a 6-digit PIN via SecureRandom for pairing.
 */
object PinGenerator {
    fun generate(): String = String.format("%06d", SecureRandom().nextInt(1_000_000))
}

abstract class HotspotManager(protected val context: Context) {
    abstract fun start(onStarted: (ssid: String, password: String) -> Unit)
    abstract fun stop()
}

@RequiresApi(Build.VERSION_CODES.O)
class OreoHotspotManager(context: Context) : HotspotManager(context) {
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override fun start(onStarted: (ssid: String, String) -> Unit) {
        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                    this@OreoHotspotManager.reservation = reservation
                    val config = reservation?.softApConfiguration
                    val ssid = config?.ssid ?: "SHS_Player"
                    val pass = config?.passphrase ?: PinGenerator.generate()
                    onStarted(ssid, pass)
                }
                override fun onStopped() { reservation = null }
                override fun onFailed(reason: Int) { reservation = null }
            }, null)
        } catch (e: SecurityException) {
            // Caller should check permissions before calling start()
        }
    }

    override fun stop() {
        reservation?.close()
        reservation = null
    }
}
