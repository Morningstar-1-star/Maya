package com.example.network

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance network and DNS pre-resolution engine.
 * Pre-resolves DNS records and warms up connection pools for instant page navigation.
 */
object SpeedBooster {
    private const val TAG = "SpeedBooster"
    private val resolvedHosts = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Pre-resolve top search and content domains on startup
    private val TOP_DOMAINS = listOf(
        "google.com",
        "www.google.com",
        "wikipedia.org",
        "github.com",
        "youtube.com",
        "duckduckgo.com",
        "bing.com",
        "reddit.com",
        "news.ycombinator.com"
    )

    fun init(context: Context) {
        scope.launch {
            try {
                TOP_DOMAINS.forEach { host ->
                    preResolveDns(host)
                }
            } catch (e: Exception) {
                Log.d(TAG, "DNS prewarm failed: ${e.message}")
            }
        }
    }

    /**
     * Pre-resolves the DNS host asynchronously when user is typing in URL bar or hovering links.
     */
    fun preResolveUrl(urlString: String) {
        if (urlString.isBlank()) return
        val cleanUrl = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            "https://$urlString"
        } else {
            urlString
        }
        val host = try {
            Uri.parse(cleanUrl).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return

        preResolveDns(host)
    }

    fun preResolveDns(host: String) {
        if (host.isBlank() || host == "localhost" || host == "127.0.0.1") return
        val now = System.currentTimeMillis()
        val lastResolved = resolvedHosts[host]
        // Cache DNS pre-resolution for 5 minutes
        if (lastResolved != null && now - lastResolved < 300_000) {
            return
        }

        scope.launch {
            try {
                resolvedHosts[host] = now
                InetAddress.getAllByName(host)
            } catch (ignored: Exception) {
                // Ignore transient lookup errors
            }
        }
    }
}
