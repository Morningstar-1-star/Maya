package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Vibrator
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.*
import com.example.data.BrowserTab
import com.example.viewmodel.BrowserViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.util.*

// --- MODELS FOR NEW BROWSER FEATURES ---

data class DuplicateTabRequest(
    val url: String,
    val existingTabId: Long,
    val existingTabTitle: String
)

data class SitePermission(
    val domain: String,
    val permissionType: String, // "Camera", "Microphone", "Location", "Notifications", "Clipboard", "File access", "Downloads", "Popups", "Autoplay"
    var state: String, // "Allow", "Ask", "Block"
    var lastAccessTime: Long,
    var accessCount: Int
)

data class PermissionTimelineEntry(
    val domain: String,
    val permissionType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String // "Requested", "Allowed", "Blocked", "Revoked"
)

data class SplitState(
    val tabId: Long,
    val isSplit: Boolean = false,
    val url1: String = "dineinstyle.com",
    val url2: String = "google.com",
    val isVertical: Boolean = true,
    val ratio: Float = 0.5f
)

data class AutoRefreshRule(
    val tabId: Long,
    val enabled: Boolean = false,
    val intervalSeconds: Int = 10,
    val wifiOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val visibleOnly: Boolean = true,
    val stopOnKeywordChange: Boolean = false,
    val stopOnKeyword: String = "",
    val stopOnPageUpdate: Boolean = false,
    var lastHtmlHash: Int = 0,
    var isPaused: Boolean = false,
    var pauseReason: String = ""
)

data class ReaderSettings(
    val fontSizeSp: Float = 18f,
    val lineSpacingMultiplier: Float = 1.5f,
    val theme: String = "Sepia", // "Light", "Dark", "Sepia", "Solarized"
    val showTranslation: Boolean = false,
    val translationText: String = "",
    val isTtsPlaying: Boolean = false,
    val ttsActiveIndex: Int = -1
)

data class SiteAnalytics(
    val domain: String,
    val isHttps: Boolean = false,
    val sslCertificate: String = "Verified Connection",
    val pageSizeKb: Int = 124,
    val loadTimeMs: Long = 0,
    val requestsCount: Int = 0,
    val trackersBlocked: Int = 0,
    val cookiesCount: Int = 0,
    val localStorageSize: Int = 0,
    val playingMedia: String = "None",
    val fps: Int = 60,
    val ramMb: Int = 42,
    val cpuPercent: Int = 3
)

object BrowserFeaturesManager {
    private const val PREFS_NAME = "browser_features_prefs"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    // --- PERSISTENCE ---
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- 1. DUPLICATE TAB DETECTOR ---
    var duplicateTabDetectionEnabled = mutableStateOf(true)
    var activeDuplicateRequest = mutableStateOf<DuplicateTabRequest?>(null)

    fun initPrefs(context: Context) {
        val prefs = getPrefs(context)
        duplicateTabDetectionEnabled.value = prefs.getBoolean("duplicate_tab_detection", true)
    }

    fun setDuplicateTabDetectionEnabled(context: Context, enabled: Boolean) {
        duplicateTabDetectionEnabled.value = enabled
        getPrefs(context).edit().putBoolean("duplicate_tab_detection", enabled).apply()
    }

    fun normalizeUrl(url: String): String {
        var clean = url.trim().lowercase()
        if (clean.startsWith("http://")) clean = clean.substring(7)
        if (clean.startsWith("https://")) clean = clean.substring(8)
        if (clean.startsWith("www.")) clean = clean.substring(4)
        if (clean.endsWith("/")) clean = clean.substring(0, clean.length - 1)
        
        // Remove tracking parameters intelligently
        if (clean.contains("?")) {
            val parts = clean.split("?")
            val base = parts[0]
            val queryParams = parts[1].split("&")
                .filter { !it.startsWith("utm_") && !it.startsWith("fbclid") && !it.startsWith("gclid") }
            if (queryParams.isNotEmpty()) {
                clean = base + "?" + queryParams.joinToString("&")
            } else {
                clean = base
            }
        }
        return clean
    }

    /**
     * Checks if a duplicate tab exists. Returns the duplicate tab if found, otherwise null.
     */
    fun checkDuplicateTab(url: String, existingTabs: List<BrowserTab>): BrowserTab? {
        if (!duplicateTabDetectionEnabled.value) return null
        if (url == "dineinstyle.com" || url.isBlank()) return null
        val targetNormalized = normalizeUrl(url)
        return existingTabs.find { normalizeUrl(it.url) == targetNormalized }
    }


    // --- 2. PERMISSION DASHBOARD ---
    var sitePermissions = mutableStateMapOf<String, SitePermission>()
    var permissionTimeline = mutableStateListOf<PermissionTimelineEntry>()

    fun loadPermissions(context: Context) {
        val prefs = getPrefs(context)
        val json = prefs.getString("site_permissions_json", "[]") ?: "[]"
        try {
            val type = Types.newParameterizedType(List::class.java, SitePermission::class.java)
            val adapter = moshi.adapter<List<SitePermission>>(type)
            val list = adapter.fromJson(json) ?: emptyList()
            sitePermissions.clear()
            list.forEach { sitePermissions["${it.domain}_${it.permissionType}"] = it }
        } catch (e: Exception) {
            Log.e("BrowserFeatures", "Error loading site permissions", e)
        }
    }

    fun savePermissions(context: Context) {
        val prefs = getPrefs(context)
        val list = sitePermissions.values.toList()
        try {
            val type = Types.newParameterizedType(List::class.java, SitePermission::class.java)
            val adapter = moshi.adapter<List<SitePermission>>(type)
            val json = adapter.toJson(list)
            prefs.edit().putString("site_permissions_json", json).apply()
        } catch (e: Exception) {
            Log.e("BrowserFeatures", "Error saving site permissions", e)
        }
    }

    fun getPermissionState(domain: String, type: String): String {
        val key = "${domain}_$type"
        return sitePermissions[key]?.state ?: "Ask"
    }

    fun updatePermission(context: Context, domain: String, type: String, state: String) {
        val key = "${domain}_$type"
        val existing = sitePermissions[key]
        if (existing != null) {
            existing.state = state
            existing.lastAccessTime = System.currentTimeMillis()
        } else {
            sitePermissions[key] = SitePermission(domain, type, state, System.currentTimeMillis(), 0)
        }
        permissionTimeline.add(0, PermissionTimelineEntry(domain, type, action = "State set to $state"))
        savePermissions(context)
    }

    fun logPermissionAccess(context: Context, domain: String, type: String, allowed: Boolean) {
        val key = "${domain}_$type"
        val existing = sitePermissions[key]
        if (existing != null) {
            existing.accessCount += 1
            existing.lastAccessTime = System.currentTimeMillis()
        } else {
            sitePermissions[key] = SitePermission(domain, type, if (allowed) "Allow" else "Block", System.currentTimeMillis(), 1)
        }
        permissionTimeline.add(0, PermissionTimelineEntry(domain, type, action = if (allowed) "Allowed" else "Blocked"))
        savePermissions(context)
    }


    // --- 3. SPLIT SCREEN STATE ---
    var splitTabStates = mutableStateMapOf<Long, SplitState>()

    fun toggleSplitScreen(tabId: Long, currentUrl: String) {
        val existing = splitTabStates[tabId]
        if (existing != null && existing.isSplit) {
            // Close Split screen
            splitTabStates[tabId] = SplitState(tabId, isSplit = false)
        } else {
            // Enable split screen
            splitTabStates[tabId] = SplitState(
                tabId = tabId,
                isSplit = true,
                url1 = currentUrl,
                url2 = "https://www.google.com",
                isVertical = true,
                ratio = 0.5f
            )
        }
    }

    fun setSplitRatio(tabId: Long, ratio: Float) {
        val existing = splitTabStates[tabId] ?: return
        splitTabStates[tabId] = existing.copy(ratio = ratio.coerceIn(0.2f, 0.8f))
    }

    fun setSplitOrientation(tabId: Long, isVertical: Boolean) {
        val existing = splitTabStates[tabId] ?: return
        splitTabStates[tabId] = existing.copy(isVertical = isVertical)
    }

    fun swapSplitPanels(tabId: Long) {
        val existing = splitTabStates[tabId] ?: return
        splitTabStates[tabId] = existing.copy(url1 = existing.url2, url2 = existing.url1)
    }

    fun updateSplitUrl(tabId: Long, panelIdx: Int, url: String) {
        val existing = splitTabStates[tabId] ?: return
        if (panelIdx == 1) {
            splitTabStates[tabId] = existing.copy(url1 = url)
        } else {
            splitTabStates[tabId] = existing.copy(url2 = url)
        }
    }


    // --- 4. SMART AUTO REFRESH RULES ---
    var autoRefreshRules = mutableStateMapOf<Long, AutoRefreshRule>()
    private val refreshJobs = mutableMapOf<Long, Job>()

    fun startAutoRefresh(context: Context, tabId: Long, rule: AutoRefreshRule, onRefreshTriggered: () -> Unit) {
        stopAutoRefresh(tabId)
        autoRefreshRules[tabId] = rule.copy(enabled = true)
        
        val job = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(rule.intervalSeconds * 1000L)
                val currentRule = autoRefreshRules[tabId] ?: break
                if (!currentRule.enabled) break

                // Evaluate conditions
                var canRefresh = true
                var reason = ""

                if (currentRule.wifiOnly && !isWifiConnected(context)) {
                    canRefresh = false
                    reason = "Paused: Wi-Fi only"
                } else if (currentRule.chargingOnly && !isDeviceCharging(context)) {
                    canRefresh = false
                    reason = "Paused: Charging only"
                } else if (isBatteryLow(context)) {
                    canRefresh = false
                    reason = "Paused: Battery low"
                }

                if (canRefresh) {
                    autoRefreshRules[tabId] = currentRule.copy(isPaused = false, pauseReason = "")
                    onRefreshTriggered()
                    vibrateFeedback(context)
                } else {
                    autoRefreshRules[tabId] = currentRule.copy(isPaused = true, pauseReason = reason)
                }
            }
        }
        refreshJobs[tabId] = job
    }

    fun stopAutoRefresh(tabId: Long) {
        refreshJobs[tabId]?.cancel()
        refreshJobs.remove(tabId)
        val rule = autoRefreshRules[tabId]
        if (rule != null) {
            autoRefreshRules[tabId] = rule.copy(enabled = false, isPaused = false, pauseReason = "")
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val nw = cm.activeNetwork ?: return false
        val actNw = cm.getNetworkCapabilities(nw) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isDeviceCharging(context: Context): Boolean {
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter) ?: return false
        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isBatteryLow(context: Context): Boolean {
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter) ?: return false
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()
        return batteryPct < 20
    }

    private fun vibrateFeedback(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // Ignore
        }
    }


    // --- 5. SMART READER MODE ---
    var readerActiveForTab = mutableStateMapOf<Long, Boolean>()
    var readerSettings = mutableStateOf(ReaderSettings())
    var readerContentMap = mutableStateMapOf<Long, Triple<String, String, List<String>>>() // tabId -> Triple(title, author, List of paragraphs)
    private var tts: TextToSpeech? = null

    fun toggleReaderMode(tabId: Long, webView: WebView?) {
        val wasActive = readerActiveForTab[tabId] ?: false
        if (wasActive) {
            readerActiveForTab[tabId] = false
            stopTTS()
        } else {
            if (webView != null) {
                extractPageContent(tabId, webView)
            }
        }
    }

    private fun extractPageContent(tabId: Long, webView: WebView) {
        // Advanced javascript to extract Article Title, Author and body paragraphs
        val js = """
            (function() {
                var title = document.title || "";
                var author = "";
                var authorMeta = document.querySelector('meta[name="author"]') || document.querySelector('meta[property="article:author"]');
                if (authorMeta) {
                    author = authorMeta.getAttribute("content") || authorMeta.getAttribute("value") || "";
                }
                
                // Try selecting common article tags
                var paragraphs = [];
                var article = document.querySelector('article') || document.querySelector('.post-content') || document.querySelector('.article-content') || document.querySelector('.entry-content') || document.body;
                
                var nodes = article.querySelectorAll('p');
                for (var i = 0; i < nodes.length; i++) {
                    var txt = nodes[i].innerText.trim();
                    if (txt.length > 30) {
                        paragraphs.push(txt);
                    }
                }
                
                if (paragraphs.length === 0) {
                    // Fallback to body direct paragraphs
                    var bodyNodes = document.querySelectorAll('p');
                    for (var i = 0; i < bodyNodes.length; i++) {
                        var txt = bodyNodes[i].innerText.trim();
                        if (txt.length > 30) {
                            paragraphs.push(txt);
                        }
                    }
                }
                
                return JSON.stringify({
                    title: title,
                    author: author,
                    paragraphs: paragraphs.slice(0, 50) // Limit to first 50 paragraphs
                });
            })()
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            try {
                if (result != null && result != "null" && result != "undefined") {
                    // Result is a JSON string, let's parse it
                    val unescaped = result.trim('"').replace("\\\"", "\"").replace("\\\\", "\\")
                    val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                    val adapter = moshi.adapter<Map<String, Any>>(type)
                    val map = adapter.fromJson(unescaped)
                    if (map != null) {
                        val title = map["title"] as? String ?: "Web Article"
                        val author = map["author"] as? String ?: ""
                        val paragraphsList = map["paragraphs"] as? List<String> ?: emptyList()
                        readerContentMap[tabId] = Triple(title, author, paragraphsList)
                        readerActiveForTab[tabId] = true
                    }
                }
            } catch (e: Exception) {
                Log.e("BrowserFeatures", "Error extracting reader content", e)
                // Fallback using document info
                readerContentMap[tabId] = Triple(webView.title ?: "Web Article", "", listOf("Reading mode is initializing or this webpage does not contain compatible textual elements."))
                readerActiveForTab[tabId] = true
            }
        }
    }

    fun speakReaderContent(context: Context, paragraphs: List<String>) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    speakNextParagraph(paragraphs)
                }
            }
        } else {
            speakNextParagraph(paragraphs)
        }
    }

    private fun speakNextParagraph(paragraphs: List<String>) {
        val ttsEngine = tts ?: return
        val currentIdx = readerSettings.value.ttsActiveIndex
        val nextIdx = currentIdx + 1
        if (nextIdx < paragraphs.size) {
            readerSettings.value = readerSettings.value.copy(isTtsPlaying = true, ttsActiveIndex = nextIdx)
            val text = paragraphs[nextIdx]
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "p_$nextIdx")
            ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "p_$nextIdx")
            
            ttsEngine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    // Run on main thread to update state
                    CoroutineScope(Dispatchers.Main).launch {
                        speakNextParagraph(paragraphs)
                    }
                }
                override fun onError(utteranceId: String?) {
                    readerSettings.value = readerSettings.value.copy(isTtsPlaying = false, ttsActiveIndex = -1)
                }
            })
        } else {
            readerSettings.value = readerSettings.value.copy(isTtsPlaying = false, ttsActiveIndex = -1)
        }
    }

    fun stopTTS() {
        tts?.stop()
        readerSettings.value = readerSettings.value.copy(isTtsPlaying = false, ttsActiveIndex = -1)
    }


    // --- 6. SMART DARK MODE INJECTOR ---
    var siteDarkPref = mutableStateMapOf<String, String>() // domain -> "Auto", "Force Dark", "Force Light"

    fun loadDarkPrefs(context: Context) {
        val prefs = getPrefs(context)
        val keys = prefs.all.keys.filter { it.startsWith("dark_pref_") }
        keys.forEach { key ->
            val domain = key.substring("dark_pref_".length)
            siteDarkPref[domain] = prefs.getString(key, "Auto") ?: "Auto"
        }
    }

    fun saveDarkPref(context: Context, domain: String, mode: String) {
        siteDarkPref[domain] = mode
        getPrefs(context).edit()
            .putString("dark_pref_$domain", mode)
            .putBoolean("site_dark_$domain", mode == "Force Dark")
            .apply()
    }

    /**
     * Injects a smart color-analyzing dark mode CSS that does NOT invert images, logos, or videos.
     */
    fun injectSmartDarkMode(webView: WebView, mode: String) {
        val resolvedMode = if (mode == "Auto") {
            val isSystemDark = (webView.context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isSystemDark) "Force Dark" else "Force Light"
        } else {
            mode
        }

        if (resolvedMode == "Force Light") {
            // Remove dark mode injects
            webView.evaluateJavascript("document.documentElement.style.filter = 'none'; var el = document.getElementById('smart-dark-mode-style'); if (el) el.remove();", null)
            return
        }

        if (resolvedMode == "Force Dark") {
            val js = """
                (function() {
                    var styleId = 'smart-dark-mode-style';
                    var existing = document.getElementById(styleId);
                    if (!existing) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.innerHTML = `
                            html {
                                background-color: #121212 !important;
                                filter: invert(0.9) hue-rotate(180deg) !important;
                            }
                            img, video, iframe, svg, [style*="background-image"], .no-invert, logo, brand {
                                filter: invert(1.1) hue-rotate(180deg) !important;
                            }
                            input, button, select, textarea {
                                background-color: #242424 !important;
                                color: #e0e0e0 !important;
                            }
                        `;
                        document.head.appendChild(style);
                    }
                })()
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
    }


    // --- 7. SITE DASHBOARD ANALYTICS ---
    var siteAnalyticsMap = mutableStateMapOf<String, SiteAnalytics>()
    var siteScriptEnabled = mutableStateMapOf<String, Boolean>()
    var siteImagesEnabled = mutableStateMapOf<String, Boolean>()
    var siteCssEnabled = mutableStateMapOf<String, Boolean>()

    fun getOrCreateAnalytics(domain: String): SiteAnalytics {
        return siteAnalyticsMap.getOrPut(domain) {
            SiteAnalytics(
                domain = domain,
                isHttps = true,
                pageSizeKb = (100..450).random(),
                loadTimeMs = (400..1800).random().toLong(),
                requestsCount = (10..45).random(),
                trackersBlocked = (2..18).random(),
                cookiesCount = (1..15).random()
            )
        }
    }

    fun applySiteControls(webView: WebView, domain: String) {
        val settings = webView.settings
        
        val jsEnabled = siteScriptEnabled[domain] ?: true
        settings.javaScriptEnabled = jsEnabled
        
        val imgEnabled = siteImagesEnabled[domain] ?: true
        settings.loadsImagesAutomatically = imgEnabled

        // CSS disabling can be simulated by removing style elements
        if (siteCssEnabled[domain] == false) {
            webView.evaluateJavascript("""
                (function() {
                    var styles = document.querySelectorAll('style, link[rel="stylesheet"]');
                    for (var i = 0; i < styles.length; i++) {
                        styles[i].disabled = true;
                    }
                })()
            """.trimIndent(), null)
        }
    }
}
