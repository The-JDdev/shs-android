package dev.shsplayer.ui.share

import android.content.Context
import android.net.wifi.WifiManager

/**
 * SHS Player — TrebleShot-compatible QR share format.
 *
 * Original SHS Player code (clean-room reimplementation). Format:
 *   Hotspot mode: hs;{pin};{ssid};{bssid};{password};end
 *   Wi-Fi LAN mode: wf;{pin};{ssid};{bssid};{ip};end
 */
object QrShareFormat {

    sealed class ParsedQr {
        data class Hotspot(val pin: String, val ssid: String, val bssid: String, val password: String) : ParsedQr()
        data class Wifi(val pin: String, val ssid: String, val bssid: String, val hostIp: String) : ParsedQr()
    }

    fun encodeHotspot(pin: String, ssid: String, bssid: String, password: String): String =
        "hs;$pin;$ssid;$bssid;$password;end"

    fun encodeWifi(pin: String, ssid: String, bssid: String, ip: String): String =
        "wf;$pin;$ssid;$bssid;$ip;end"

    fun parse(content: String): ParsedQr? {
        val parts = content.split(";")
        if (parts.size < 6 || parts.last() != "end") return null
        return when (parts[0]) {
            "hs" -> ParsedQr.Hotspot(parts[1], parts[2], parts[3], parts[4])
            "wf" -> ParsedQr.Wifi(parts[1], parts[2], parts[3], parts[4])
            else -> null
        }
    }

    /**
     * Get the device's Wi-Fi IP address (byte-swapped from WifiManager int).
     */
    fun getWifiIpAddress(context: Context): String? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        val ip = wm.connectionInfo.ipAddress
        if (ip == 0) return null
        return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }
}
