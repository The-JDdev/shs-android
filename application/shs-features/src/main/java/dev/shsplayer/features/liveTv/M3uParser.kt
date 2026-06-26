package dev.shsplayer.features.liveTv

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * SHS Player — M3U/M3U8 playlist parser for live IPTV streams.
 *
 * Clean-room implementation. Handles http/content/file sources, extracts
 * tvg-logo/group-title/tvg-id/tvg-name attributes. Supports http/rtmp/rtsp/udp
 * stream URLs.
 */
data class IptvChannel(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
) {
    val category: IptvCategory get() = IptvCategoryResolver.resolve(this)
}

enum class IptvCategory(val displayName: String) {
    BANGLADESH("Bangladesh"),
    SPORTS("Sports"),
    NEWS("News"),
    POPULAR("Popular"),
    FREE("Free Channels"),
    OTHER("Other"),
}

object IptvCategoryResolver {
    private val BD_CHANNEL_KEYWORDS = listOf(
        "atn", "bangla", "bd", "bangladesh", "boishakhi", "channel i", "chattagram",
        "desh", "ekattor", "gtv", "jamuna", "maasranga", "nagorik", "news24",
        "rtv", "sangsad", "somoy", "swift", "tara", "tvsomoy", "zainga",
        "deepto", "asian tv", "ntv", "mytv", "bongo", "bongobd", "rb tv",
        "dhaka", "duronto", "gazi", "islam", "peoples tv", "real tv", "sk tv",
    )
    private val SPORTS_KEYWORDS = listOf(
        "sport", "sports", "espn", "fox sport", "sky sport", "tnt sport",
        "bein", "nba", "nfl", "nhl", "mlb", "ufc", "wwe", "f1", "motogp",
        "epl", "la liga", "serie a", "bundesliga", "champions league",
        "cricket", "football", "soccer", "wrestling", "boxing", "golf",
        "tennis", "olympic", "red bull", "supercross", "live sport",
    )
    private val NEWS_KEYWORDS = listOf(
        "news", "cnn", "bbc", "al jazeera", "ndtv", "abp", "republic",
        "france 24", "dw", "abc news", "nbc news", "cbs news", "sky news",
        "fox news", "msnbc", "rt news", "euro news", "aaj tak", "india today",
        "times now", "wion", "press tv", "trt world", "cnbc", "bloomberg",
        "ary news", "geo news", "samaa", "dawn news",
    )
    private val POPULAR_KEYWORDS = listOf(
        "star plus", "star gold", "sony", "zee tv", "zee cinema", "colors tv",
        "set max", "sab tv", "&tv", "star jalsha", "star pravah", "star vijay",
        "hotstar", "hbo", "showtime", "cinemax", "axn", "fox life",
        "discovery", "national geographic", "animal planet", "history",
        "cartoon", "nickelodeon", "disney", "pogo", "tnt", "tbs", "amc",
        "fx", "syfy", "usa network", "bravo", "mtv", "vh1",
    )

    fun resolve(channel: IptvChannel): IptvCategory {
        val haystack = listOfNotNull(channel.name, channel.group, channel.tvgName)
            .joinToString(" ")
            .lowercase()
        if (BD_CHANNEL_KEYWORDS.any { haystack.contains(it) }) return IptvCategory.BANGLADESH
        if (channel.url.contains(".bd", ignoreCase = true) ||
            channel.tvgId?.endsWith(".bd", ignoreCase = true) == true) return IptvCategory.BANGLADESH
        if (SPORTS_KEYWORDS.any { haystack.contains(it) }) return IptvCategory.SPORTS
        if (NEWS_KEYWORDS.any { haystack.contains(it) }) return IptvCategory.NEWS
        if (POPULAR_KEYWORDS.any { haystack.contains(it) }) return IptvCategory.POPULAR
        return IptvCategory.FREE
    }
}

object M3uParser {
    suspend fun parse(context: Context, source: String): List<IptvChannel> = withContext(Dispatchers.IO) {
        runCatching {
            val content = when {
                source.startsWith("http://") || source.startsWith("https://") -> fetchRemote(source)
                source.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                        BufferedReader(InputStreamReader(input)).readText()
                    } ?: ""
                }
                source.startsWith("file://") -> java.io.File(Uri.parse(source).path ?: "").readText()
                else -> {
                    val f = java.io.File(source)
                    if (f.exists()) f.readText() else ""
                }
            }
            parseContent(content)
        }.getOrDefault(emptyList())
    }

    fun parseContent(content: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                val commaIndex = trimmed.indexOf(',')
                val attributesPart = if (commaIndex > 0) trimmed.substring(0, commaIndex) else trimmed
                val namePart = if (commaIndex > 0) trimmed.substring(commaIndex + 1).trim() else "Unknown"
                currentName = namePart
                currentLogo = extractAttribute(attributesPart, "tvg-logo")
                currentGroup = extractAttribute(attributesPart, "group-title")
                currentTvgId = extractAttribute(attributesPart, "tvg-id")
                currentTvgName = extractAttribute(attributesPart, "tvg-name")
            } else if (!trimmed.startsWith("#")) {
                if (currentName != null && (trimmed.startsWith("http") || trimmed.startsWith("rtmp") ||
                        trimmed.startsWith("rtsp") || trimmed.startsWith("udp"))) {
                    channels.add(IptvChannel(currentName, trimmed, currentLogo, currentGroup, currentTvgId, currentTvgName))
                }
                currentName = null; currentLogo = null; currentGroup = null; currentTvgId = null; currentTvgName = null
            }
        }
        return channels
    }

    private fun extractAttribute(text: String, attrName: String): String? {
        val key = "$attrName=\""
        val start = text.indexOf(key, ignoreCase = true)
        if (start < 0) return null
        val valueStart = start + key.length
        val end = text.indexOf('"', valueStart)
        if (end < 0) return null
        return text.substring(valueStart, end).takeIf { it.isNotBlank() }
    }

    private fun fetchRemote(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 15000; requestMethod = "GET"
            setRequestProperty("User-Agent", "SHSPlayer/1.0")
        }
        return try { conn.inputStream.bufferedReader().readText() } finally { conn.disconnect() }
    }
}

object DefaultIptvPlaylists {
    data class Playlist(val name: String, val url: String, val category: IptvCategory)

    val playlists = listOf(
        Playlist("Bangladesh TV", "https://iptv-org.github.io/iptv/countries/bd.m3u", IptvCategory.BANGLADESH),
        Playlist("India TV", "https://iptv-org.github.io/iptv/countries/in.m3u", IptvCategory.POPULAR),
        Playlist("Sports", "https://iptv-org.github.io/iptv/categories/sports.m3u", IptvCategory.SPORTS),
        Playlist("News", "https://iptv-org.github.io/iptv/categories/news.m3u", IptvCategory.NEWS),
        Playlist("Movies", "https://iptv-org.github.io/iptv/categories/movies.m3u", IptvCategory.POPULAR),
        Playlist("Kids", "https://iptv-org.github.io/iptv/categories/kids.m3u", IptvCategory.POPULAR),
        Playlist("Music", "https://iptv-org.github.io/iptv/categories/music.m3u", IptvCategory.POPULAR),
        Playlist("USA", "https://iptv-org.github.io/iptv/countries/us.m3u", IptvCategory.FREE),
        Playlist("UK", "https://iptv-org.github.io/iptv/countries/uk.m3u", IptvCategory.FREE),
        Playlist("Pakistan", "https://iptv-org.github.io/iptv/countries/pk.m3u", IptvCategory.FREE),
    )

    fun forCategory(category: IptvCategory): List<Playlist> = playlists.filter { it.category == category }
    val availableCategories: List<IptvCategory> get() = IptvCategory.entries.filter { c -> playlists.any { it.category == c } }
}
