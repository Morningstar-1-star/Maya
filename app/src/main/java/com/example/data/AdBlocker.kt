package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object AdBlocker {
    private const val TAG = "AdBlocker"
    
    // Memory-efficient HashSet for extremely fast O(1) host lookups
    private val blockedHosts = java.util.Collections.synchronizedSet(HashSet<String>())
    
    // In-memory cache for full URL lookups to avoid regex/parsing overhead on every asset request
    private val decisionCache = ConcurrentHashMap<String, Boolean>()
    
    // Fallback static list of very common ad and tracking domains
    private val fallbackAdHosts = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "googletagservices.com", "googletagmanager.com", "google-analytics.com",
        "adnxs.com", "taboola.com", "outbrain.com", "criteo.com", "adroll.com",
        "carbonads.net", "buy-sell-ads.com", "scorecardresearch.com", "quantserve.com",
        "moatads.com", "krxd.net", "popads.net", "popcash.net", "propellerads.com",
        "exoclick.com", "juicyads.com", "ero-advertising.com", "mgid.com",
        "revcontent.com", "zergnet.com", "disqus.com", "addthis.com", "sharethis.com",
        "coinhive.com", "hotjar.com", "optimizely.com", "amplitude.com", "segment.io",
        "mixpanel.com", "flurry.com", "admob.com", "adcolony.com", "unityads.com",
        "fyber.com", "chartboost.com", "vungle.com", "ironsrc.com", "applovin.com",
        "tapjoy.com", "leadbolt.com", "adform.net", "rubiconproject.com", "pubmatic.com",
        "openx.net", "adblade.com", "yieldmo.com", "bidswitch.net", "smartadserver.com",
        "indexww.com", "casalemedia.com", "sovrn.com", "exponential.com", "media.net",
        "clickadu.com", "adsterra.com", "hilltopads.com", "popunder.net", "onclickads.net",
        "admaven.com", "adcash.com", "infolinks.com", "yandex.ru", "mail.ru", "bidvertiser.com",
        "revenuehits.com", "exdynsrv.com", "popads.net", "trafficforce.net", "juicyads.com",
        "ad-delivery.net", "adservice.google.com", "analytics.google.com", "stats.g.doubleclick.net",
        "mparticle.com", "bugsnag.com", "sentry.io", "crashlytics.com", "branch.io"
    )

    // Common URL keywords that indicate ads, popups, or tracking endpoints
    private val adKeywords = listOf(
        "/ads/", "/ad-banner", "/ad-box", "/show_ads", "/ad_status",
        "googleads", "ads-twitter", "adserver", "popunder", "adtech",
        "tracker", "analytics", "coinhive", "sponsored-links", "sponsored_post",
        "ad_type=", "banner-ads", "cookie-consent", "cookiebanner", "onetrust",
        "cookiebot", "trustarc", "cookie-notice", "cookiebar", "ad-delivery",
        "ads.js", "prebid", "adloader", "advertisement", "adtarget",
        "adsystem", "serving-sys", "gemini.yahoo", "advertising.com",
        "fbds", "facebook.com/tr", "popunder", "popup", "pop_under", "popup_ad",
        "pixel.gif", "telemetry", "beacons", "track.js"
    )

    // Tracking query parameters that we can strip from URLs to preserve privacy
    private val trackingParams = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_cid", "utm_reader",
        "gclid", "gclsrc", "fbclid", "msclkid", "yclid", "mc_eid", "dclid", "twclid", "pf_rd_r", "pf_rd_m"
    )

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        // Load fallback rules immediately so the app has ad-blocking capabilities on first launch
        blockedHosts.addAll(fallbackAdHosts)

        // Load locally cached hosts if they exist
        CoroutineScope(Dispatchers.IO).launch {
            loadCachedHosts(context)
            // Trigger automatic updates in background
            updateFilterLists(context)
        }
    }

    private fun loadCachedHosts(context: Context) {
        try {
            val cacheFile = File(context.filesDir, "blocked_hosts.txt")
            if (cacheFile.exists()) {
                val hosts = cacheFile.readLines()
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                if (hosts.isNotEmpty()) {
                    blockedHosts.addAll(hosts)
                    Log.d(TAG, "Loaded ${hosts.size} cached hosts from local storage.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached hosts: ${e.message}")
        }
    }

    private fun saveCachedHosts(context: Context, hosts: Set<String>) {
        try {
            val cacheFile = File(context.filesDir, "blocked_hosts.txt")
            cacheFile.writeText(hosts.joinToString("\n"))
            Log.d(TAG, "Successfully saved ${hosts.size} hosts to local storage.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cached hosts: ${e.message}")
        }
    }

    // Downloads and parses StevenBlack/OISD hosts format asynchronously
    fun updateFilterLists(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val urls = listOf(
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", // StevenBlack hosts
                "https://small.oisd.nl" // OISD small list
            )
            val newHosts = HashSet<String>()
            newHosts.addAll(fallbackAdHosts) // Keep fallbacks

            for (urlString in urls) {
                try {
                    Log.d(TAG, "Downloading filter list: $urlString")
                    val url = URL(urlString)
                    val connection = url.openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    connection.getInputStream().bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                                continue
                            }
                            // Formats like "0.0.0.0 domain.com" or "127.0.0.1 domain.com" or just "domain.com"
                            val parts = trimmed.split(Regex("\\s+"))
                            if (parts.size >= 2) {
                                val host = parts[1].trim().lowercase()
                                if (host != "localhost" && host != "127.0.0.1" && host.isNotEmpty()) {
                                    newHosts.add(host)
                                }
                            } else if (parts.size == 1) {
                                val host = parts[0].trim().lowercase()
                                if (host.isNotEmpty()) {
                                    newHosts.add(host)
                                }
                            }
                        }
                    }
                    Log.d(TAG, "Successfully parsed filter list: $urlString")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update filter list from $urlString: ${e.message}")
                }
            }

            if (newHosts.size > fallbackAdHosts.size) {
                blockedHosts.clear()
                blockedHosts.addAll(newHosts)
                decisionCache.clear()
                saveCachedHosts(context, newHosts)
                Log.d(TAG, "Total active blocked hosts count: ${blockedHosts.size}")
            }
        }
    }

    fun isAdRequest(urlStr: String): Boolean {
        // Return from cache if we already evaluated this URL
        decisionCache[urlStr]?.let { return it }

        val lowerUrl = urlStr.lowercase()
        val uri = try {
            android.net.Uri.parse(urlStr)
        } catch (e: Exception) {
            null
        } ?: return false

        val host = uri.host?.lowercase() ?: ""

        // Exempt critical local / home or search query domains
        if (host.isEmpty() || host == "localhost" || host == "127.0.0.1" || 
            host == "dineinstyle.com" || host.contains("google.com/search") ||
            host == "google.com" || host.endsWith(".google.com")
        ) {
            decisionCache[urlStr] = false
            return false
        }

        // 1. Direct host-based block (matches direct domain or any parent domain)
        var tempHost = host
        while (tempHost.contains(".")) {
            if (blockedHosts.contains(tempHost)) {
                decisionCache[urlStr] = true
                return true
            }
            tempHost = tempHost.substringAfter(".", "")
            if (tempHost.isEmpty()) break
        }

        // 2. Keyword check on full URL
        if (adKeywords.any { lowerUrl.contains(it) }) {
            decisionCache[urlStr] = true
            return true
        }

        decisionCache[urlStr] = false
        return false
    }

    // Strips tracking query parameters from URLs
    fun cleanTrackingParameters(urlStr: String): String {
        try {
            val uri = android.net.Uri.parse(urlStr)
            if (uri.query.isNullOrEmpty()) return urlStr

            val paramNames = uri.queryParameterNames
            var hasTracking = false
            for (p in paramNames) {
                if (trackingParams.contains(p.lowercase())) {
                    hasTracking = true
                    break
                }
            }

            if (!hasTracking) return urlStr

            val builder = uri.buildUpon().clearQuery()
            for (p in paramNames) {
                if (!trackingParams.contains(p.lowercase())) {
                    val values = uri.getQueryParameters(p)
                    for (v in values) {
                        builder.appendQueryParameter(p, v)
                    }
                }
            }
            return builder.build().toString()
        } catch (e: Exception) {
            return urlStr
        }
    }
}
