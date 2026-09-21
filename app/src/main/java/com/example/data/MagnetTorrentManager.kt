package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ParsedMagnet(
    val rawUri: String,
    val displayName: String,
    val infoHash: String,
    val trackers: List<String>,
    val exactLength: Long? = null
)

object MagnetTorrentManager {
    val activeMagnetRequest = mutableStateOf<ParsedMagnet?>(null)

    fun isMagnetUrl(url: String): Boolean {
        return url.trim().startsWith("magnet:?", ignoreCase = true)
    }

    fun isTorrentUrl(url: String): Boolean {
        val clean = url.trim().lowercase()
        return clean.endsWith(".torrent") || clean.contains(".torrent?")
    }

    fun parseMagnetUri(rawUri: String): ParsedMagnet {
        var name = "Unknown Torrent"
        var hash = ""
        val trackers = mutableListOf<String>()
        var length: Long? = null

        try {
            val query = if (rawUri.contains("?")) rawUri.substringAfter("?") else rawUri
            val pairs = query.split("&")
            for (pair in pairs) {
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = try {
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                    } catch (e: Exception) {
                        parts[1]
                    }

                    when (key.lowercase()) {
                        "dn" -> name = value
                        "xt" -> {
                            // Format: urn:btih:<hash> or urn:btmh:<hash>
                            hash = value.removePrefix("urn:btih:").removePrefix("urn:btmh:")
                        }
                        "tr" -> {
                            if (value.isNotBlank() && !trackers.contains(value)) {
                                trackers.add(value)
                            }
                        }
                        "xl" -> {
                            length = value.toLongOrNull()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (hash.isBlank()) {
            // Try fallback regex extraction
            val hashMatch = Regex("xt=urn:btih:([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE).find(rawUri)
            if (hashMatch != null) {
                hash = hashMatch.groupValues[1]
            }
        }

        return ParsedMagnet(
            rawUri = rawUri,
            displayName = if (name.isNotBlank()) name else (if (hash.isNotEmpty()) "Torrent [${hash.take(8)}...]" else "Torrent"),
            infoHash = hash.uppercase(),
            trackers = trackers,
            exactLength = length
        )
    }

    fun openInExternalBitTorrentClient(context: Context, magnetUri: String): Boolean {
        val uri = Uri.parse(magnetUri)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            val pm = context.packageManager
            val activities = pm.queryIntentActivities(intent, 0)
            if (activities.isNotEmpty()) {
                context.startActivity(intent)
                true
            } else {
                // Try viewing application/x-bittorrent
                val torrentIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/x-bittorrent")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(torrentIntent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openTorrentClientStoreSearch(context: Context) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=torrent client&c=apps")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=torrent+client&c=apps")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Magnet") {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMagnet(context: Context, name: String, magnetUri: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, name)
                putExtra(Intent.EXTRA_TEXT, "$name\n\n$magnetUri")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Torrent Magnet").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
