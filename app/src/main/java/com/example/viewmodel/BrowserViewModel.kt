package com.example.viewmodel

import android.app.Application
import com.example.ui.WebViewPool
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Bookmark
import com.example.data.BrowserRepository
import com.example.data.BrowserTab
import com.example.data.HistoryEntry
import com.example.data.HomepageShortcut
import com.example.data.CapturedMedia
import com.example.data.UserScript
import com.example.data.DnsManager
import com.example.data.AdBlocker
import com.example.data.VideoTrimmerHelper
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TabLayoutStyle {
    CAROUSEL,
    GRID,
    STACKED
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository

    // Base database flows
    val allTabs: StateFlow<List<BrowserTab>>
    val allHistory: StateFlow<List<HistoryEntry>>
    val allBookmarks: StateFlow<List<Bookmark>>
    val allShortcuts: StateFlow<List<HomepageShortcut>>
    val allCapturedMedia: StateFlow<List<CapturedMedia>>
    val likedSavedMedia: StateFlow<List<CapturedMedia>>
    val allUserScripts: StateFlow<List<UserScript>>
    val allDownloads: StateFlow<List<com.example.data.DownloadEntry>>

    // Custom Wallpaper State
    private val prefs = application.getSharedPreferences("browser_settings", Context.MODE_PRIVATE)
    private val _customWallpaperUrl = MutableStateFlow<String?>(prefs.getString("custom_wallpaper_url", null))
    val customWallpaperUrl: StateFlow<String?> = _customWallpaperUrl.asStateFlow()

    // Show News Section State
    private val _showNewsSection = MutableStateFlow<Boolean>(prefs.getBoolean("show_news_section", true))
    val showNewsSection: StateFlow<Boolean> = _showNewsSection.asStateFlow()

    // UI state states
    private val _activeTabId = MutableStateFlow<Long?>(null)
    val activeTabId: StateFlow<Long?> = _activeTabId.asStateFlow()

    // Sensitive tab lock states
    private val _unlockedTabIds = MutableStateFlow<Set<Long>>(emptySet())
    val unlockedTabIds: StateFlow<Set<Long>> = _unlockedTabIds.asStateFlow()

    // Page loading duration states for Resource Inspector
    private val pageStartTimes = mutableMapOf<Long, Long>()
    private val _pageLoadTimes = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val pageLoadTimes: StateFlow<Map<Long, Long>> = _pageLoadTimes.asStateFlow()

    // Custom spoiler/de-clutter keywords
    private val _customDeClutterKeywords = MutableStateFlow<List<String>>(
        prefs.getString("de_clutter_keywords", "spoiler,politics,celebrity")
            ?.split(",")?.filter { it.isNotBlank() } ?: listOf("spoiler", "politics", "celebrity")
    )
    val customDeClutterKeywords: StateFlow<List<String>> = _customDeClutterKeywords.asStateFlow()

    private val _currentUrlInput = MutableStateFlow("")
    val currentUrlInput: StateFlow<String> = _currentUrlInput.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private var suggestionsJob: kotlinx.coroutines.Job? = null

    private val _adBlockerOn = MutableStateFlow(prefs.getBoolean("ad_blocker_enabled", true))
    val adBlockerOn: StateFlow<Boolean> = _adBlockerOn.asStateFlow()

    // --- Web Cleaner Settings ---
    private val _elementInspectionEnabled = MutableStateFlow(prefs.getBoolean("element_inspection_enabled", false))
    val elementInspectionEnabled: StateFlow<Boolean> = _elementInspectionEnabled.asStateFlow()

    private val _blockAreaEnabled = MutableStateFlow(prefs.getBoolean("block_area_enabled", true))
    val blockAreaEnabled: StateFlow<Boolean> = _blockAreaEnabled.asStateFlow()

    private val _overlayBlockerEnabled = MutableStateFlow(prefs.getBoolean("overlay_blocker_enabled", false))
    val overlayBlockerEnabled: StateFlow<Boolean> = _overlayBlockerEnabled.asStateFlow()

    private val _popupBlockerMode = MutableStateFlow(prefs.getString("popup_blocker_mode", "weak") ?: "weak")
    val popupBlockerMode: StateFlow<String> = _popupBlockerMode.asStateFlow()

    private val _blockAppExecutionEnabled = MutableStateFlow(prefs.getBoolean("block_app_execution_enabled", true))
    val blockAppExecutionEnabled: StateFlow<Boolean> = _blockAppExecutionEnabled.asStateFlow()

    private val _lockScreenEnabled = MutableStateFlow(prefs.getBoolean("lock_screen_enabled", false))
    val lockScreenEnabled: StateFlow<Boolean> = _lockScreenEnabled.asStateFlow()

    private val _blockedImagesEnabled = MutableStateFlow(prefs.getBoolean("blocked_images_enabled", false))
    val blockedImagesEnabled: StateFlow<Boolean> = _blockedImagesEnabled.asStateFlow()

    // Whitelists & Blocked lists (persistent)
    private val _adWhitelist = MutableStateFlow(prefs.getStringSet("ad_whitelist", emptySet()) ?: emptySet())
    val adWhitelist: StateFlow<Set<String>> = _adWhitelist.asStateFlow()

    private val _overlayWhitelist = MutableStateFlow(prefs.getStringSet("overlay_whitelist", emptySet()) ?: emptySet())
    val overlayWhitelist: StateFlow<Set<String>> = _overlayWhitelist.asStateFlow()

    private val _popupWhitelist = MutableStateFlow(prefs.getStringSet("popup_whitelist", emptySet()) ?: emptySet())
    val popupWhitelist: StateFlow<Set<String>> = _popupWhitelist.asStateFlow()

    private val _blockedLinks = MutableStateFlow(prefs.getStringSet("blocked_links", emptySet()) ?: emptySet())
    val blockedLinks: StateFlow<Set<String>> = _blockedLinks.asStateFlow()

    private val _blockedImages = MutableStateFlow(prefs.getStringSet("blocked_images", emptySet()) ?: emptySet())
    val blockedImages: StateFlow<Set<String>> = _blockedImages.asStateFlow()

    private val _customBlockedElements = MutableStateFlow(prefs.getStringSet("custom_blocked_elements", emptySet()) ?: emptySet())
    val customBlockedElements: StateFlow<Set<String>> = _customBlockedElements.asStateFlow()

    private val _adFilters = MutableStateFlow<List<com.example.data.AdFilterSubscription>>(emptyList())
    val adFilters: StateFlow<List<com.example.data.AdFilterSubscription>> = _adFilters.asStateFlow()

    // Element Block Confirm Dialog State
    private val _pendingBlockElementSelector = MutableStateFlow<String?>(null)
    val pendingBlockElementSelector: StateFlow<String?> = _pendingBlockElementSelector.asStateFlow()

    private val _pendingBlockElementHtml = MutableStateFlow<String?>(null)
    val pendingBlockElementHtml: StateFlow<String?> = _pendingBlockElementHtml.asStateFlow()

    fun showBlockElementConfirm(selector: String, html: String) {
        _pendingBlockElementSelector.value = selector
        _pendingBlockElementHtml.value = html
    }

    fun dismissBlockElementConfirm() {
        _pendingBlockElementSelector.value = null
        _pendingBlockElementHtml.value = null
    }


    // Custom DNS Settings State
    private val _dnsEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("dns_enabled", false))
    val dnsEnabled: StateFlow<Boolean> = _dnsEnabled.asStateFlow()

    private val _dnsMode = MutableStateFlow<String>(prefs.getString("dns_mode", "preset") ?: "preset")
    val dnsMode: StateFlow<String> = _dnsMode.asStateFlow()

    private val _dnsPresetId = MutableStateFlow<String>(prefs.getString("dns_preset_id", "adguard") ?: "adguard")
    val dnsPresetId: StateFlow<String> = _dnsPresetId.asStateFlow()

    private val _dnsCustomValue = MutableStateFlow<String>(prefs.getString("dns_custom_value", "") ?: "")
    val dnsCustomValue: StateFlow<String> = _dnsCustomValue.asStateFlow()

    // Settings Screens and Navigation States
    private val _isSettingsScreenVisible = MutableStateFlow(false)
    val isSettingsScreenVisible: StateFlow<Boolean> = _isSettingsScreenVisible.asStateFlow()

    private val _currentSettingsSubScreen = MutableStateFlow("main") // "main", "search_engine", "video_options", "privacy_guard", "advanced_privacy"
    val currentSettingsSubScreen: StateFlow<String> = _currentSettingsSubScreen.asStateFlow()

    // Smart Auto-Routing States
    private val _smartAutoRouting = MutableStateFlow(prefs.getBoolean("smart_auto_routing", true))
    val smartAutoRouting: StateFlow<Boolean> = _smartAutoRouting.asStateFlow()

    private val _smartProxyRotator = MutableStateFlow(prefs.getBoolean("smart_proxy_rotator", true))
    val smartProxyRotator: StateFlow<Boolean> = _smartProxyRotator.asStateFlow()

    private val _smartTorActive = MutableStateFlow(prefs.getBoolean("smart_tor_active", true))
    val smartTorActive: StateFlow<Boolean> = _smartTorActive.asStateFlow()

    private val _activeRoutingStatus = MutableStateFlow("Direct Connection")
    val activeRoutingStatus: StateFlow<String> = _activeRoutingStatus.asStateFlow()

    // AI Automation & Smart Concepts
    private val _aiAutoGroupingEnabled = MutableStateFlow(prefs.getBoolean("ai_auto_grouping", true))
    val aiAutoGroupingEnabled: StateFlow<Boolean> = _aiAutoGroupingEnabled.asStateFlow()

    private val _aiSmartAdBlockerEnabled = MutableStateFlow(prefs.getBoolean("ai_smart_adblocker", true))
    val aiSmartAdBlockerEnabled: StateFlow<Boolean> = _aiSmartAdBlockerEnabled.asStateFlow()

    private val _aiSmartSummarizerEnabled = MutableStateFlow(prefs.getBoolean("ai_smart_summarizer", true))
    val aiSmartSummarizerEnabled: StateFlow<Boolean> = _aiSmartSummarizerEnabled.asStateFlow()

    // AI Summary Dialog States
    private val _isSummaryDialogVisible = MutableStateFlow(false)
    val isSummaryDialogVisible: StateFlow<Boolean> = _isSummaryDialogVisible.asStateFlow()

    // Per-site Preferences Trigger for Compose reactive updates
    private val _perSitePrefsTrigger = MutableStateFlow(0)
    val perSitePrefsTrigger: StateFlow<Int> = _perSitePrefsTrigger.asStateFlow()

    private val _summaryContent = MutableStateFlow("")
    val summaryContent: StateFlow<String> = _summaryContent.asStateFlow()

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading.asStateFlow()

    fun setSummaryDialogVisible(visible: Boolean) {
        _isSummaryDialogVisible.value = visible
    }

    fun summarizeCurrentPage(webText: String) {
        _isSummaryDialogVisible.value = true
        _isSummaryLoading.value = true
        _summaryContent.value = "AI is scanning the webpage text content and composing a summary..."
        viewModelScope.launch {
            val summary = generatePageSummaryWithGemini(webText)
            _summaryContent.value = summary
            _isSummaryLoading.value = false
        }
    }

    private suspend fun generatePageSummaryWithGemini(pageText: String): String {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "Please configure your GEMINI_API_KEY in the Secrets panel of AI Studio to activate the AI Page Summarizer."
        }
        
        val prompt = "Please provide a concise, readable, and structured summary of the following webpage content. Focus on the main key points and takeaways:\n\n$pageText"
        
        val request = com.example.data.GenerateContentRequest(
            contents = listOf(com.example.data.Content(
                parts = listOf(com.example.data.Part(text = prompt))
            ))
        )
        
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = com.example.data.RetrofitClient.service.generateContent(apiKey, request)
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "Could not generate summary. No text candidates returned."
            } catch (e: Exception) {
                "Error generating summary: ${e.localizedMessage ?: "Unknown Error"}"
            }
        }
    }

    // Search Engine Configuration
    private val _searchEngineName = MutableStateFlow(prefs.getString("search_engine_name", "Google") ?: "Google")
    val searchEngineName: StateFlow<String> = _searchEngineName.asStateFlow()

    private val _searchEngineUrl = MutableStateFlow(prefs.getString("search_engine_url", "https://www.google.com/search?q=%s") ?: "https://www.google.com/search?q=%s")
    val searchEngineUrl: StateFlow<String> = _searchEngineUrl.asStateFlow()

    private val _searchEngineShortcut = MutableStateFlow(prefs.getString("search_engine_shortcut", "g") ?: "g")
    val searchEngineShortcut: StateFlow<String> = _searchEngineShortcut.asStateFlow()

    // Video Options States
    private val _videoListenInBackground = MutableStateFlow(prefs.getBoolean("video_listen_background", true))
    val videoListenInBackground: StateFlow<Boolean> = _videoListenInBackground.asStateFlow()

    private val _videoShowToolbar = MutableStateFlow(prefs.getBoolean("video_show_toolbar", true))
    val videoShowToolbar: StateFlow<Boolean> = _videoShowToolbar.asStateFlow()

    private val _videoShowMenu = MutableStateFlow(prefs.getBoolean("video_show_menu", true))
    val videoShowMenu: StateFlow<Boolean> = _videoShowMenu.asStateFlow()

    private val _videoYoutubeOption = MutableStateFlow(prefs.getString("video_youtube_option", "Standard Ad-Free") ?: "Standard Ad-Free")
    val videoYoutubeOption: StateFlow<String> = _videoYoutubeOption.asStateFlow()

    // UC Premium Video Player States
    private val _useUcPlayerEngine = MutableStateFlow(prefs.getBoolean("use_uc_player_engine", true))
    val useUcPlayerEngine: StateFlow<Boolean> = _useUcPlayerEngine.asStateFlow()

    private val _ucPlayerGestureControls = MutableStateFlow(prefs.getBoolean("uc_player_gesture_controls", true))
    val ucPlayerGestureControls: StateFlow<Boolean> = _ucPlayerGestureControls.asStateFlow()

    private val _ucPlayerShowSpeedMeter = MutableStateFlow(prefs.getBoolean("uc_player_show_speed_meter", true))
    val ucPlayerShowSpeedMeter: StateFlow<Boolean> = _ucPlayerShowSpeedMeter.asStateFlow()

    private val _ucPlayerDefaultSpeed = MutableStateFlow(prefs.getFloat("uc_player_default_speed", 1.0f))
    val ucPlayerDefaultSpeed: StateFlow<Float> = _ucPlayerDefaultSpeed.asStateFlow()

    // Active Player Status
    private val _ucPlayerActive = MutableStateFlow(false)
    val ucPlayerActive: StateFlow<Boolean> = _ucPlayerActive.asStateFlow()

    private val _ucPlayerVideoUrl = MutableStateFlow("")
    val ucPlayerVideoUrl: StateFlow<String> = _ucPlayerVideoUrl.asStateFlow()

    private val _ucPlayerVideoTitle = MutableStateFlow("")
    val ucPlayerVideoTitle: StateFlow<String> = _ucPlayerVideoTitle.asStateFlow()

    private val _detectedVideoActionMedia = MutableStateFlow<CapturedMedia?>(null)
    val detectedVideoActionMedia: StateFlow<CapturedMedia?> = _detectedVideoActionMedia.asStateFlow()

    fun setDetectedVideoActionMedia(media: CapturedMedia?) {
        _detectedVideoActionMedia.value = media
    }

    // Background Player & Video Queue states
    private var backgroundMediaPlayer: android.media.MediaPlayer? = null

    private val _backgroundVideo = MutableStateFlow<CapturedMedia?>(null)
    val backgroundVideo: StateFlow<CapturedMedia?> = _backgroundVideo.asStateFlow()

    private val _isBackgroundVideoPlaying = MutableStateFlow(false)
    val isBackgroundVideoPlaying: StateFlow<Boolean> = _isBackgroundVideoPlaying.asStateFlow()

    private val _videoQueue = MutableStateFlow<List<CapturedMedia>>(emptyList())
    val videoQueue: StateFlow<List<CapturedMedia>> = _videoQueue.asStateFlow()

    // Privacy Guard States
    private val _alwaysUseHttps = MutableStateFlow(prefs.getBoolean("always_use_https", true))
    val alwaysUseHttps: StateFlow<Boolean> = _alwaysUseHttps.asStateFlow()

    private val _removeFingerprint = MutableStateFlow(prefs.getBoolean("remove_fingerprint", true))
    val removeFingerprint: StateFlow<Boolean> = _removeFingerprint.asStateFlow()

    private val _scriptControlEnabled = MutableStateFlow(prefs.getBoolean("script_control_enabled", true))
    val scriptControlEnabled: StateFlow<Boolean> = _scriptControlEnabled.asStateFlow()

    private val _cookieManagementMode = MutableStateFlow(prefs.getString("cookie_management_mode", "block_third_party") ?: "block_third_party")
    val cookieManagementMode: StateFlow<String> = _cookieManagementMode.asStateFlow()

    private val _stopAppRedirects = MutableStateFlow(prefs.getBoolean("stop_app_redirects", true))
    val stopAppRedirects: StateFlow<Boolean> = _stopAppRedirects.asStateFlow()

    private val _safeBrowsingEnabled = MutableStateFlow(prefs.getBoolean("safe_browsing_enabled", true))
    val safeBrowsingEnabled: StateFlow<Boolean> = _safeBrowsingEnabled.asStateFlow()

    private val _doNotTrack = MutableStateFlow(prefs.getBoolean("do_not_track", true))
    val doNotTrack: StateFlow<Boolean> = _doNotTrack.asStateFlow()

    private val _autoDeAmp = MutableStateFlow(prefs.getBoolean("auto_de_amp", true))
    val autoDeAmp: StateFlow<Boolean> = _autoDeAmp.asStateFlow()

    private val _globalPrivacyControl = MutableStateFlow(prefs.getBoolean("global_privacy_control", true))
    val globalPrivacyControl: StateFlow<Boolean> = _globalPrivacyControl.asStateFlow()

    // New Privacy and Security States
    private val _thirdPartyCookiesSetting = MutableStateFlow(prefs.getString("third_party_cookies_setting", "block_incognito") ?: "block_incognito")
    val thirdPartyCookiesSetting: StateFlow<String> = _thirdPartyCookiesSetting.asStateFlow()

    private val _incognitoTrackingProtections = MutableStateFlow(prefs.getString("incognito_tracking_protections", "limited") ?: "limited")
    val incognitoTrackingProtections: StateFlow<String> = _incognitoTrackingProtections.asStateFlow()

    private val _adsPrivacyTopics = MutableStateFlow(prefs.getBoolean("ads_privacy_topics", true))
    val adsPrivacyTopics: StateFlow<Boolean> = _adsPrivacyTopics.asStateFlow()

    private val _adsPrivacySiteSuggested = MutableStateFlow(prefs.getBoolean("ads_privacy_site_suggested", true))
    val adsPrivacySiteSuggested: StateFlow<Boolean> = _adsPrivacySiteSuggested.asStateFlow()

    private val _isInPictureInPictureMode = MutableStateFlow(false)
    val isInPictureInPictureMode: StateFlow<Boolean> = _isInPictureInPictureMode.asStateFlow()

    fun setIsInPictureInPictureMode(value: Boolean) {
        _isInPictureInPictureMode.value = value
        if (value) {
            _ucPlayerActive.value = true
        }
    }

    private val _adsPrivacyMeasurement = MutableStateFlow(prefs.getBoolean("ads_privacy_measurement", true))
    val adsPrivacyMeasurement: StateFlow<Boolean> = _adsPrivacyMeasurement.asStateFlow()

    private val _preloadPagesMode = MutableStateFlow(prefs.getString("preload_pages_mode", "standard") ?: "standard")
    val preloadPagesMode: StateFlow<String> = _preloadPagesMode.asStateFlow()

    private val _lockIncognitoTabs = MutableStateFlow(prefs.getBoolean("lock_incognito_tabs", false))
    val lockIncognitoTabs: StateFlow<Boolean> = _lockIncognitoTabs.asStateFlow()

    private val _safeBrowsingLevel = MutableStateFlow(prefs.getString("safe_browsing_level", "standard") ?: "standard")
    val safeBrowsingLevel: StateFlow<String> = _safeBrowsingLevel.asStateFlow()

    private val _warnPasswordCompromised = MutableStateFlow(prefs.getBoolean("warn_password_compromised", true))
    val warnPasswordCompromised: StateFlow<Boolean> = _warnPasswordCompromised.asStateFlow()

    private val _jsOptimisationAndSecurity = MutableStateFlow(prefs.getBoolean("js_optimisation_and_security", true))
    val jsOptimisationAndSecurity: StateFlow<Boolean> = _jsOptimisationAndSecurity.asStateFlow()

    private val _performanceEngineEnabled = MutableStateFlow(prefs.getBoolean("performance_engine_enabled", false))
    val performanceEngineEnabled: StateFlow<Boolean> = _performanceEngineEnabled.asStateFlow()

    private val _v8JitMode = MutableStateFlow(prefs.getString("v8_jit_mode", "TurboFan (Full JIT)") ?: "TurboFan (Full JIT)")
    val v8JitMode: StateFlow<String> = _v8JitMode.asStateFlow()

    private val _v8OptimizationFlags = MutableStateFlow<Set<String>>(prefs.getStringSet("v8_optimization_flags", setOf("Ignition", "Sparkplug", "TurboFan", "Concurrent JIT", "Memory Reduction")) ?: setOf("Ignition", "Sparkplug", "TurboFan", "Concurrent JIT", "Memory Reduction"))
    val v8OptimizationFlags: StateFlow<Set<String>> = _v8OptimizationFlags.asStateFlow()

    private val _jsOptExceptions = MutableStateFlow<Set<String>>(prefs.getStringSet("js_opt_exceptions", emptySet()) ?: emptySet())
    val jsOptExceptions: StateFlow<Set<String>> = _jsOptExceptions.asStateFlow()

    private val _autofillEnabled = MutableStateFlow(prefs.getBoolean("autofill_enabled", true))
    val autofillEnabled: StateFlow<Boolean> = _autofillEnabled.asStateFlow()

    private val _autoClearMode = MutableStateFlow(prefs.getString("auto_clear_mode", "Never") ?: "Never")
    val autoClearMode: StateFlow<String> = _autoClearMode.asStateFlow()

    private val _savedPasswords = MutableStateFlow<List<SavedPassword>>(emptyList())
    val savedPasswords: StateFlow<List<SavedPassword>> = _savedPasswords.asStateFlow()

    fun addJsOptException(domain: String) {
        val updated = _jsOptExceptions.value.toMutableSet()
        updated.add(domain)
        _jsOptExceptions.value = updated
        prefs.edit().putStringSet("js_opt_exceptions", updated).apply()
    }

    fun removeJsOptException(domain: String) {
        val updated = _jsOptExceptions.value.toMutableSet()
        updated.remove(domain)
        _jsOptExceptions.value = updated
        prefs.edit().putStringSet("js_opt_exceptions", updated).apply()
    }

    fun setAutofillEnabled(enabled: Boolean) {
        _autofillEnabled.value = enabled
        prefs.edit().putBoolean("autofill_enabled", enabled).apply()
    }

    fun setAutoClearMode(mode: String) {
        _autoClearMode.value = mode
        prefs.edit().putString("auto_clear_mode", mode).apply()
    }

    fun loadSavedPasswords() {
        val set = prefs.getStringSet("saved_passwords_set", emptySet()) ?: emptySet()
        val list = set.mapIndexed { index, str ->
            val parts = str.split("|")
            SavedPassword(
                id = index.toLong(),
                site = parts.getOrNull(0) ?: "",
                username = parts.getOrNull(1) ?: "",
                password = parts.getOrNull(2) ?: ""
            )
        }
        _savedPasswords.value = list
    }

    fun addSavedPassword(site: String, username: String, password: String) {
        val set = prefs.getStringSet("saved_passwords_set", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add("$site|$username|$password")
        prefs.edit().putStringSet("saved_passwords_set", set).apply()
        loadSavedPasswords()
    }

    fun deleteSavedPassword(site: String, username: String, password: String) {
        val set = prefs.getStringSet("saved_passwords_set", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.remove("$site|$username|$password")
        prefs.edit().putStringSet("saved_passwords_set", set).apply()
        loadSavedPasswords()
    }

    suspend fun clearAllBookmarks() {
        repository.clearAllBookmarks()
    }

    private val _accessPaymentMethods = MutableStateFlow(prefs.getBoolean("access_payment_methods", true))
    val accessPaymentMethods: StateFlow<Boolean> = _accessPaymentMethods.asStateFlow()

    // Link long-press custom context menu states
    private val _linkContextMenuUrl = MutableStateFlow<String?>(null)
    val linkContextMenuUrl: StateFlow<String?> = _linkContextMenuUrl.asStateFlow()

    private val _linkContextMenuText = MutableStateFlow<String?>(null)
    val linkContextMenuText: StateFlow<String?> = _linkContextMenuText.asStateFlow()

    fun showLinkContextMenu(url: String, text: String) {
        _linkContextMenuUrl.value = url
        _linkContextMenuText.value = text
    }

    fun hideLinkContextMenu() {
        _linkContextMenuUrl.value = null
        _linkContextMenuText.value = null
    }

    // Appearance & Accessibility States
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "dark") ?: "dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _webZoomLevel = MutableStateFlow(prefs.getFloat("web_zoom_level", 1.0f))
    val webZoomLevel: StateFlow<Float> = _webZoomLevel.asStateFlow()

    private val _forceDarkWebpages = MutableStateFlow(prefs.getBoolean("force_dark_webpages", false))
    val forceDarkWebpages: StateFlow<Boolean> = _forceDarkWebpages.asStateFlow()

    private val _webTextZoom = MutableStateFlow(prefs.getFloat("web_text_zoom", 1.0f))
    val webTextZoom: StateFlow<Float> = _webTextZoom.asStateFlow()

    private val _hideDistractingItems = MutableStateFlow(prefs.getBoolean("hide_distracting_items", false))
    val hideDistractingItems: StateFlow<Boolean> = _hideDistractingItems.asStateFlow()

    // Customize Address Bar States
    private val _addressBarPosition = MutableStateFlow(prefs.getString("address_bar_position", "bottom") ?: "bottom")
    val addressBarPosition: StateFlow<String> = _addressBarPosition.asStateFlow()

    private val _autoHideBar = MutableStateFlow(prefs.getBoolean("auto_hide_bar", false))
    val autoHideBar: StateFlow<Boolean> = _autoHideBar.asStateFlow()

    private val _swipeForFullscreen = MutableStateFlow(prefs.getBoolean("swipe_for_fullscreen", true))
    val swipeForFullscreen: StateFlow<Boolean> = _swipeForFullscreen.asStateFlow()

    private val _swipeToViewTabs = MutableStateFlow(prefs.getBoolean("swipe_to_view_tabs", true))
    val swipeToViewTabs: StateFlow<Boolean> = _swipeToViewTabs.asStateFlow()

    private val _showFullUrl = MutableStateFlow(prefs.getBoolean("show_full_url", false))
    val showFullUrl: StateFlow<Boolean> = _showFullUrl.asStateFlow()

    private val _hideBottomToolbar = MutableStateFlow(prefs.getBoolean("hide_bottom_toolbar", false))
    val hideBottomToolbar: StateFlow<Boolean> = _hideBottomToolbar.asStateFlow()

    // Customize Menu States
    private val _menuShowReader = MutableStateFlow(prefs.getBoolean("menu_show_reader", true))
    val menuShowReader: StateFlow<Boolean> = _menuShowReader.asStateFlow()

    private val _menuPageZoom = MutableStateFlow(prefs.getBoolean("menu_page_zoom", true))
    val menuPageZoom: StateFlow<Boolean> = _menuPageZoom.asStateFlow()

    private val _menuFindOnPage = MutableStateFlow(prefs.getBoolean("menu_find_on_page", true))
    val menuFindOnPage: StateFlow<Boolean> = _menuFindOnPage.asStateFlow()

    private val _menuRequestDesktop = MutableStateFlow(prefs.getBoolean("menu_request_desktop", true))
    val menuRequestDesktop: StateFlow<Boolean> = _menuRequestDesktop.asStateFlow()

    private val _menuAddToHome = MutableStateFlow(prefs.getBoolean("menu_add_to_home", false))
    val menuAddToHome: StateFlow<Boolean> = _menuAddToHome.asStateFlow()

    private val _menuDeveloperTools = MutableStateFlow(prefs.getBoolean("menu_developer_tools", false))
    val menuDeveloperTools: StateFlow<Boolean> = _menuDeveloperTools.asStateFlow()

    // Start Page / Home Customize States
    private val _homeShowFavorites = MutableStateFlow(prefs.getBoolean("home_show_favorites", true))
    val homeShowFavorites: StateFlow<Boolean> = _homeShowFavorites.asStateFlow()

    private val _homeShowICloudTabs = MutableStateFlow(prefs.getBoolean("home_show_icloud_tabs", true))
    val homeShowICloudTabs: StateFlow<Boolean> = _homeShowICloudTabs.asStateFlow()

    // Map tab ID -> count of ads blocked
    private val _blockedAdsMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val blockedAdsMap: StateFlow<Map<Long, Int>> = _blockedAdsMap.asStateFlow()

    // New premium features states
    private val _lowPowerModeEnabled = MutableStateFlow(prefs.getBoolean("low_power_mode_enabled", false))
    val lowPowerModeEnabled: StateFlow<Boolean> = _lowPowerModeEnabled.asStateFlow()

    private val _bypassPaywallsEnabled = MutableStateFlow(prefs.getBoolean("bypass_paywalls_enabled", false))
    val bypassPaywallsEnabled: StateFlow<Boolean> = _bypassPaywallsEnabled.asStateFlow()

    private val _fullScreenReading = MutableStateFlow(false)
    val fullScreenReading: StateFlow<Boolean> = _fullScreenReading.asStateFlow()

    private val _tabDesktopModes = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val tabDesktopModes: StateFlow<Map<Long, Boolean>> = _tabDesktopModes.asStateFlow()

    private val _tabReadTimes = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val tabReadTimes: StateFlow<Map<Long, Int>> = _tabReadTimes.asStateFlow()

    private val _tabSortMode = MutableStateFlow("Default")
    val tabSortMode: StateFlow<String> = _tabSortMode.asStateFlow()

    // Reader Mode States
    private val _isReaderModeActive = MutableStateFlow(false)
    val isReaderModeActive: StateFlow<Boolean> = _isReaderModeActive.asStateFlow()

    private val _readerTitle = MutableStateFlow("")
    val readerTitle: StateFlow<String> = _readerTitle.asStateFlow()

    private val _readerContent = MutableStateFlow("")
    val readerContent: StateFlow<String> = _readerContent.asStateFlow()

    private val _readerTextSize = MutableStateFlow(prefs.getInt("reader_text_size", 16))
    val readerTextSize: StateFlow<Int> = _readerTextSize.asStateFlow()

    private val _readerTheme = MutableStateFlow(prefs.getString("reader_theme", "Sepia") ?: "Sepia")
    val readerTheme: StateFlow<String> = _readerTheme.asStateFlow()

    private val _readerFontFamily = MutableStateFlow(prefs.getString("reader_font_family", "Serif") ?: "Serif")
    val readerFontFamily: StateFlow<String> = _readerFontFamily.asStateFlow()

    val activeTabReadTime = combine(activeTabId, _tabReadTimes) { activeId, readTimes ->
        if (activeId != null) readTimes[activeId] else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _quickTabStripVisible = MutableStateFlow(true)
    val quickTabStripVisible: StateFlow<Boolean> = _quickTabStripVisible.asStateFlow()

    // Overlay visibility
    private val _isAdBlockerPopupVisible = MutableStateFlow(false)
    val isAdBlockerPopupVisible: StateFlow<Boolean> = _isAdBlockerPopupVisible.asStateFlow()

    private val _isTabSwitcherVisible = MutableStateFlow(false)
    val isTabSwitcherVisible: StateFlow<Boolean> = _isTabSwitcherVisible.asStateFlow()

    private val _tabLayoutStyle = MutableStateFlow(
        TabLayoutStyle.valueOf(
            prefs.getString("tab_layout_style", TabLayoutStyle.CAROUSEL.name) ?: TabLayoutStyle.CAROUSEL.name
        )
    )
    val tabLayoutStyle: StateFlow<TabLayoutStyle> = _tabLayoutStyle.asStateFlow()

    fun setTabLayoutStyle(style: TabLayoutStyle) {
        _tabLayoutStyle.value = style
        prefs.edit().putString("tab_layout_style", style.name).apply()
    }

    private val _isBookmarksHistorySheetVisible = MutableStateFlow(false)
    val isBookmarksHistorySheetVisible: StateFlow<Boolean> = _isBookmarksHistorySheetVisible.asStateFlow()

    private val _isMediaStudioVisible = MutableStateFlow(false)
    val isMediaStudioVisible: StateFlow<Boolean> = _isMediaStudioVisible.asStateFlow()

    private val _currentWebsiteThemeColor = MutableStateFlow<String?>(null)
    val currentWebsiteThemeColor: StateFlow<String?> = _currentWebsiteThemeColor.asStateFlow()

    fun updateWebsiteThemeColor(colorStr: String?) {
        _currentWebsiteThemeColor.value = colorStr
    }

    // Reality Filters Toggles
    private val _realityClickbaitFilter = MutableStateFlow(prefs.getBoolean("reality_clickbait_filter", false))
    val realityClickbaitFilter: StateFlow<Boolean> = _realityClickbaitFilter.asStateFlow()

    private val _realityAiBadge = MutableStateFlow(prefs.getBoolean("reality_ai_badge", false))
    val realityAiBadge: StateFlow<Boolean> = _realityAiBadge.asStateFlow()

    private val _realitySponsoredBlock = MutableStateFlow(prefs.getBoolean("reality_sponsored_block", false))
    val realitySponsoredBlock: StateFlow<Boolean> = _realitySponsoredBlock.asStateFlow()

    fun setRealityClickbaitFilter(enabled: Boolean) {
        _realityClickbaitFilter.value = enabled
        prefs.edit().putBoolean("reality_clickbait_filter", enabled).apply()
    }

    fun setRealityAiBadge(enabled: Boolean) {
        _realityAiBadge.value = enabled
        prefs.edit().putBoolean("reality_ai_badge", enabled).apply()
    }

    fun setRealitySponsoredBlock(enabled: Boolean) {
        _realitySponsoredBlock.value = enabled
        prefs.edit().putBoolean("reality_sponsored_block", enabled).apply()
    }

    // Per-site Preference Getters/Setters
    fun getSiteZoom(domain: String): Float {
        return if (domain.isBlank()) 1.0f else prefs.getFloat("site_zoom_$domain", 1.0f)
    }

    fun setSiteZoom(domain: String, zoom: Float) {
        if (domain.isBlank()) return
        prefs.edit().putFloat("site_zoom_$domain", zoom).apply()
        _perSitePrefsTrigger.value += 1
    }

    fun getSiteForceDark(domain: String): Boolean {
        return if (domain.isBlank()) false else prefs.getBoolean("site_dark_$domain", false)
    }

    fun setSiteForceDark(domain: String, enabled: Boolean) {
        if (domain.isBlank()) return
        prefs.edit().putBoolean("site_dark_$domain", enabled).apply()
        _perSitePrefsTrigger.value += 1
    }

    fun getSiteAdBlock(domain: String): Boolean {
        return if (domain.isBlank()) true else prefs.getBoolean("site_adblock_$domain", true)
    }

    fun setSiteAdBlock(domain: String, enabled: Boolean) {
        if (domain.isBlank()) return
        prefs.edit().putBoolean("site_adblock_$domain", enabled).apply()
        _perSitePrefsTrigger.value += 1
    }

    fun getSiteScriptsEnabled(domain: String): Boolean {
        return if (domain.isBlank()) true else prefs.getBoolean("site_scripts_$domain", true)
    }

    fun setSiteScriptsEnabled(domain: String, enabled: Boolean) {
        if (domain.isBlank()) return
        prefs.edit().putBoolean("site_scripts_$domain", enabled).apply()
        _perSitePrefsTrigger.value += 1
    }

    // Sheet tab: 0 for Bookmarks, 1 for History
    private val _activeSheetTab = MutableStateFlow(0)
    val activeSheetTab: StateFlow<Int> = _activeSheetTab.asStateFlow()

    // Web navigation capabilities (per tab ID)
    private val _canGoBackMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val canGoBackMap: StateFlow<Map<Long, Boolean>> = _canGoBackMap.asStateFlow()

    private val _canGoForwardMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val canGoForwardMap: StateFlow<Map<Long, Boolean>> = _canGoForwardMap.asStateFlow()

    private val _loadingProgressMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val loadingProgressMap: StateFlow<Map<Long, Int>> = _loadingProgressMap.asStateFlow()

    // Website Evolution notification states
    private val _websiteEvolutionAlert = MutableStateFlow<Pair<String, Int>?>(null)
    val websiteEvolutionAlert: StateFlow<Pair<String, Int>?> = _websiteEvolutionAlert.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    init {
        AdBlocker.initialize(application)
        _adFilters.value = loadAdFilters()
        
        // Only trigger network update/sync on startup if the local cache file is missing or empty
        val cacheFile = java.io.File(application.filesDir, "blocked_hosts.txt")
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            syncAdBlockerFilters()
        }
        
        AdBlocker.setAdWhitelist(_adWhitelist.value)
        val database = AppDatabase.getDatabase(application)
        repository = BrowserRepository(database.browserDao())

        allTabs = combine(
            repository.allTabs,
            _tabSortMode,
            _tabReadTimes
        ) { tabs, sortMode, readTimes ->
            if (sortMode == "ReadTime") {
                tabs.sortedBy { readTimes[it.id] ?: 1 }
            } else {
                tabs
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allHistory = repository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allBookmarks = repository.allBookmarks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allShortcuts = repository.allShortcuts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCapturedMedia = repository.allCapturedMedia.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        likedSavedMedia = repository.likedSavedMedia.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allUserScripts = repository.allUserScripts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allDownloads = repository.allDownloads.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate default user scripts if empty
        viewModelScope.launch {
            val existingScripts = repository.allUserScripts.first()
            if (existingScripts.isEmpty()) {
                repository.insertUserScript(
                    UserScript(
                        name = "Auto Scroll Page",
                        description = "Adds an auto-scroller. Press Shift + S to toggle automatic slow down-scrolling on long articles.",
                        matchUrl = "*",
                        code = "(function() {\n    let scrolling = false;\n    let scrollInterval;\n    window.addEventListener('keydown', function(e) {\n        if (e.shiftKey && e.key.toLowerCase() === 's') {\n            scrolling = !scrolling;\n            if (scrolling) {\n                scrollInterval = setInterval(() => { window.scrollBy(0, 1); }, 30);\n                console.log(\"Auto Scroll Enabled\");\n            } else {\n                clearInterval(scrollInterval);\n                console.log(\"Auto Scroll Disabled\");\n            }\n        }\n    });\n})();",
                        isEnabled = true
                    )
                )
                repository.insertUserScript(
                    UserScript(
                        name = "Emerald Tint Reader",
                        description = "Changes the webpage background to a soft, warm emerald tint for pleasant reading and eye relief.",
                        matchUrl = "*",
                        code = "(function() {\n    const style = document.createElement('style');\n    style.innerHTML = 'body, main, article { background-color: #f0fdf4 !important; color: #166534 !important; }';\n    document.head.appendChild(style);\n})();",
                        isEnabled = false
                    )
                )
                repository.insertUserScript(
                    UserScript(
                        name = "Highlight Ad Frames",
                        description = "Highlights remaining frames and ad containers with a prominent neon yellow dashed border.",
                        matchUrl = "*",
                        code = "(function() {\n    const style = document.createElement('style');\n    style.innerHTML = 'iframe, [class*=\"ad-\"], [id*=\"ad-\"] { border: 2px dashed #facc15 !important; opacity: 0.8 !important; }';\n    document.head.appendChild(style);\n})();",
                        isEnabled = false
                    )
                )
            }
        }

        // Prepopulate default shortcuts if empty
        viewModelScope.launch {
            val existing = repository.allShortcuts.first()
            if (existing.isEmpty()) {
                repository.insertShortcut(HomepageShortcut(title = "Amazon", url = "https://www.amazon.com"))
                repository.insertShortcut(HomepageShortcut(title = "iDB", url = "https://www.idownloadblog.com"))
                repository.insertShortcut(HomepageShortcut(title = "Increase platelet count...", url = "https://www.google.com/search?q=increase+platelet+count"))
                repository.insertShortcut(HomepageShortcut(title = "Thrombocytopenia (lo...", url = "https://www.mayoclinic.org"))
                repository.insertShortcut(HomepageShortcut(title = "Apple", url = "https://www.apple.com"))
                repository.insertShortcut(HomepageShortcut(title = "Wikipedia", url = "https://www.wikipedia.org"))
                repository.insertShortcut(HomepageShortcut(title = "Google", url = "https://www.google.com"))
                repository.insertShortcut(HomepageShortcut(title = "Ankur iDB", url = "https://www.idownloadblog.com"))
            }
        }

        // Sync active tab ID and URL input when tabs change
        viewModelScope.launch {
            allTabs.collect { tabs ->
                if (tabs.isEmpty()) {
                    // Prepopulate with a default tab if none exist
                    createDefaultTab()
                } else {
                    val selected = tabs.find { it.isSelected }
                    if (selected != null) {
                        _activeTabId.value = selected.id
                        // Only sync URL input if it is not currently focused/edited by the user or if we just switched
                        if (_currentUrlInput.value != selected.url && !isUserTyping) {
                            _currentUrlInput.value = selected.url
                        }
                    } else {
                        // If no tab is selected, select the first one
                        selectTab(tabs.first().id)
                    }
                }
            }
        }
        loadSavedPasswords()
        checkWatchedWebsitesForUpdates()
    }

    private var isUserTyping = false

    fun setUserTyping(typing: Boolean) {
        isUserTyping = typing
    }

    fun updateUrlInput(url: String) {
        _currentUrlInput.value = url
    }

    private suspend fun createDefaultTab() {
        val defaultTab = BrowserTab(
            title = "Start Page",
            url = "dineinstyle.com",
            isSelected = true
        )
        repository.insertTab(defaultTab)
    }

    fun loadUrl(url: String) {
        val tabId = _activeTabId.value ?: return
        val formattedUrl = AdBlocker.cleanTrackingParameters(formatUrl(url))
        _currentUrlInput.value = formattedUrl

        viewModelScope.launch {
            val tabs = allTabs.value
            val currentTab = tabs.find { it.id == tabId }
            if (currentTab != null) {
                repository.updateTab(currentTab.copy(url = formattedUrl, title = getDomainName(formattedUrl)))
                clearBlockedAds(tabId)
                // Add to history
                if (formattedUrl != "dineinstyle.com") {
                    repository.insertHistory(
                        HistoryEntry(
                            title = getDomainName(formattedUrl),
                            url = formattedUrl
                        )
                    )
                }
            }
        }
    }

    fun updateTabTitleAndUrl(tabId: Long, title: String, url: String) {
        viewModelScope.launch {
            val tabs = allTabs.value
            val tab = tabs.find { it.id == tabId }
            if (tab != null) {
                repository.updateTab(tab.copy(title = title, url = url))
                if (tabId == _activeTabId.value) {
                    _currentUrlInput.value = url
                }
                runAIAutoGroupingForTab(tabId, title, url)
            }
        }
    }

    fun addTab(url: String = "dineinstyle.com") {
        val cleanUrl = if (url == "dineinstyle.com") url else AdBlocker.cleanTrackingParameters(url)
        viewModelScope.launch {
            repository.deselectAllTabs()
            val newTab = BrowserTab(
                title = if (cleanUrl == "dineinstyle.com") "Start Page" else getDomainName(cleanUrl),
                url = cleanUrl,
                isSelected = true
            )
            val newId = repository.insertTab(newTab)
            _activeTabId.value = newId
            _currentUrlInput.value = cleanUrl
            _isTabSwitcherVisible.value = false // Close tab switcher when tab is added
        }
    }

    fun closeAllTabs() {
        viewModelScope.launch {
            repository.clearAllTabs()
            createDefaultTab()
        }
    }

    fun closeTab(tabId: Long) {
        viewModelScope.launch {
            val tabs = allTabs.value
            if (tabs.size <= 1) {
                // If closing the last tab, clear everything and create a default
                repository.clearAllTabs()
                createDefaultTab()
            } else {
                val tabToClose = tabs.find { it.id == tabId }
                repository.deleteTabById(tabId)
                _blockedAdsMap.value = _blockedAdsMap.value.minus(tabId)
                _canGoBackMap.value = _canGoBackMap.value.minus(tabId)
                _canGoForwardMap.value = _canGoForwardMap.value.minus(tabId)
                _loadingProgressMap.value = _loadingProgressMap.value.minus(tabId)

                if (tabToClose?.isSelected == true) {
                    // Select another tab
                    val remainingTabs = tabs.filter { it.id != tabId }
                    if (remainingTabs.isNotEmpty()) {
                        repository.selectTab(remainingTabs.first().id)
                    }
                }
            }
        }
    }

    fun selectTab(tabId: Long) {
        viewModelScope.launch {
            repository.selectTab(tabId)
            val tab = allTabs.value.find { it.id == tabId }
            if (tab != null) {
                _activeTabId.value = tabId
                _currentUrlInput.value = tab.url
            }
            _isTabSwitcherVisible.value = false // Close tab switcher when switched
        }
    }

    fun setTabGroup(tabId: Long, groupName: String?) {
        viewModelScope.launch {
            val tab = allTabs.value.find { it.id == tabId }
            if (tab != null) {
                repository.updateTab(tab.copy(groupName = groupName?.trim()?.ifEmpty { null }))
            }
        }
    }

    fun groupTabs(tabId1: Long, tabId2: Long) {
        viewModelScope.launch {
            val tab1 = allTabs.value.find { it.id == tabId1 }
            val tab2 = allTabs.value.find { it.id == tabId2 }
            if (tab1 != null && tab2 != null) {
                val groupName = tab2.groupName ?: "Group ${tab2.id}"
                repository.updateTab(tab2.copy(groupName = groupName))
                repository.updateTab(tab1.copy(groupName = groupName))
            }
        }
    }

    fun toggleTabLock(tabId: Long) {
        viewModelScope.launch {
            val tab = allTabs.value.find { it.id == tabId }
            if (tab != null) {
                repository.updateTab(tab.copy(isLocked = !tab.isLocked))
            }
        }
    }

    fun unlockTab(tabId: Long) {
        _unlockedTabIds.value = _unlockedTabIds.value + tabId
    }

    fun lockTab(tabId: Long) {
        _unlockedTabIds.value = _unlockedTabIds.value - tabId
    }

    fun recordPageStart(tabId: Long) {
        pageStartTimes[tabId] = System.currentTimeMillis()
    }

    fun recordPageFinished(tabId: Long) {
        val startTime = pageStartTimes[tabId]
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            val currentMap = _pageLoadTimes.value.toMutableMap()
            currentMap[tabId] = duration
            _pageLoadTimes.value = currentMap
        }
    }

    fun addCustomDeClutterKeyword(keyword: String) {
        val clean = keyword.trim().lowercase()
        if (clean.isNotEmpty() && !_customDeClutterKeywords.value.contains(clean)) {
            val newList = _customDeClutterKeywords.value + clean
            _customDeClutterKeywords.value = newList
            prefs.edit().putString("de_clutter_keywords", newList.joinToString(",")).apply()
        }
    }

    fun removeCustomDeClutterKeyword(keyword: String) {
        val clean = keyword.trim().lowercase()
        if (_customDeClutterKeywords.value.contains(clean)) {
            val newList = _customDeClutterKeywords.value - clean
            _customDeClutterKeywords.value = newList
            prefs.edit().putString("de_clutter_keywords", newList.joinToString(",")).apply()
        }
    }

    fun toggleBookmarkWatchMode(url: String) {
        viewModelScope.launch {
            val bookmark = repository.getBookmarkByUrl(url)
            if (bookmark != null) {
                repository.updateBookmark(bookmark.copy(isWatchMode = !bookmark.isWatchMode))
            }
        }
    }

    fun updateBookmarkTextHash(url: String, textHash: String) {
        viewModelScope.launch {
            val bookmark = repository.getBookmarkByUrl(url)
            if (bookmark != null) {
                repository.updateBookmark(bookmark.copy(lastTextHash = textHash))
            }
        }
    }

    fun setWebsiteEvolutionAlert(url: String, newCount: Int) {
        _websiteEvolutionAlert.value = Pair(url, newCount)
    }

    fun dismissWebsiteEvolutionAlert() {
        _websiteEvolutionAlert.value = null
    }

    fun clearBookmarkTextHashAndReload(url: String, reloadTrigger: () -> Unit) {
        viewModelScope.launch {
            val bookmark = repository.getBookmarkByUrl(url)
            if (bookmark != null) {
                repository.updateBookmark(bookmark.copy(lastTextHash = null, hasUpdateAlert = false, lastUpdateDetails = null))
                reloadTrigger()
                _websiteEvolutionAlert.value = null
            }
        }
    }

    fun extractTextFromHtml(html: String): List<String> {
        var cleanHtml = html
        cleanHtml = cleanHtml.replace(Regex("(?s)<script.*?>.*?</script>", RegexOption.IGNORE_CASE), "")
        cleanHtml = cleanHtml.replace(Regex("(?s)<style.*?>.*?</style>", RegexOption.IGNORE_CASE), "")
        cleanHtml = cleanHtml.replace(Regex("(?s)<!--.*?-->"), "")
        
        val items = mutableListOf<String>()
        val pattern = Regex(">([^<]+)<")
        val matches = pattern.findAll(cleanHtml)
        for (match in matches) {
            val text = match.groupValues[1].trim()
            if (text.length > 15) {
                val cleanText = text
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&nbsp;", " ")
                if (cleanText.length > 15 && !cleanText.startsWith("{") && !cleanText.endsWith("}")) {
                    items.add(cleanText)
                }
            }
        }
        return items
    }

    fun checkWatchedWebsitesForUpdates() {
        if (_isCheckingUpdates.value) return
        _isCheckingUpdates.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val watched = repository.getWatchedBookmarks()
                if (watched.isNotEmpty()) {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    for (bookmark in watched) {
                        try {
                            val request = okhttp3.Request.Builder()
                                .url(bookmark.url)
                                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                                .build()

                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val html = response.body?.string() ?: ""
                                    if (html.isNotBlank()) {
                                        val newTexts = extractTextFromHtml(html)
                                        val newHashes = newTexts.map { it.hashCode().toString() }

                                        val savedHashesJson = bookmark.lastTextHash
                                        if (savedHashesJson.isNullOrBlank()) {
                                            val jsonStr = org.json.JSONArray(newHashes).toString()
                                            repository.updateBookmark(
                                                bookmark.copy(
                                                    lastTextHash = jsonStr,
                                                    hasUpdateAlert = false,
                                                    lastUpdateDetails = null
                                                )
                                            )
                                        } else {
                                            val savedArray = org.json.JSONArray(savedHashesJson)
                                            val savedSet = mutableSetOf<String>()
                                            for (i in 0 until savedArray.length()) {
                                                savedSet.add(savedArray.getString(i))
                                            }

                                            val newItems = mutableListOf<String>()
                                            val updatedHashes = savedSet.toMutableSet()

                                            for (i in 0 until newTexts.size) {
                                                val h = newHashes[i]
                                                if (!savedSet.contains(h)) {
                                                    newItems.add(newTexts[i])
                                                    updatedHashes.add(h)
                                                }
                                            }

                                            if (newItems.isNotEmpty()) {
                                                val detailsJson = org.json.JSONArray(newItems).toString()
                                                val updatedHashesJson = org.json.JSONArray(updatedHashes.toList()).toString()

                                                repository.updateBookmark(
                                                    bookmark.copy(
                                                        lastTextHash = updatedHashesJson,
                                                        hasUpdateAlert = true,
                                                        lastUpdateDetails = detailsJson
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCheckingUpdates.value = false
            }
        }
    }

    fun clearBookmarkUpdateAlert(url: String) {
        viewModelScope.launch {
            val bookmark = repository.getBookmarkByUrl(url)
            if (bookmark != null) {
                repository.updateBookmark(bookmark.copy(hasUpdateAlert = false, lastUpdateDetails = null))
            }
        }
    }

    fun addTabToGroup(groupName: String, url: String = "dineinstyle.com") {
        val cleanUrl = if (url == "dineinstyle.com") url else AdBlocker.cleanTrackingParameters(url)
        viewModelScope.launch {
            repository.deselectAllTabs()
            val newTab = BrowserTab(
                title = if (cleanUrl == "dineinstyle.com") "Start Page" else getDomainName(cleanUrl),
                url = cleanUrl,
                isSelected = true,
                groupName = groupName.trim().ifEmpty { null }
            )
            val newId = repository.insertTab(newTab)
            _activeTabId.value = newId
            _currentUrlInput.value = cleanUrl
            _isTabSwitcherVisible.value = false
        }
    }

    fun openLinkInNewTabInGroup(url: String) {
        viewModelScope.launch {
            val tabs = allTabs.value
            val activeId = _activeTabId.value
            val currentTab = tabs.find { it.id == activeId }
            if (currentTab != null) {
                val rawDomain = getDomainName(currentTab.url)
                val cleanDomain = if (rawDomain.endsWith(".com") || rawDomain.endsWith(".org") || rawDomain.endsWith(".net")) {
                    rawDomain.substring(0, rawDomain.lastIndexOf('.'))
                } else {
                    rawDomain
                }
                val groupName = currentTab.groupName ?: "${cleanDomain.replaceFirstChar { it.uppercase() }} Stack".trim().ifEmpty { "My Stack" }
                if (currentTab.groupName == null) {
                    repository.updateTab(currentTab.copy(groupName = groupName))
                }
                addTabToGroup(groupName, url)
            } else {
                addTabToGroup("Group", url)
            }
        }
    }

    fun removeGroup(groupName: String) {
        viewModelScope.launch {
            allTabs.value.filter { it.groupName == groupName }.forEach { tab ->
                repository.updateTab(tab.copy(groupName = null))
            }
        }
    }

    fun deleteGroupTabs(groupName: String) {
        viewModelScope.launch {
            val tabsToDelete = allTabs.value.filter { it.groupName == groupName }
            tabsToDelete.forEach { tab ->
                closeTab(tab.id)
            }
        }
    }

    fun toggleQuickTabStrip() {
        _quickTabStripVisible.value = !_quickTabStripVisible.value
    }

    fun toggleAdBlocker() {
        _adBlockerOn.value = !_adBlockerOn.value
    }

    fun setDnsEnabled(enabled: Boolean) {
        _dnsEnabled.value = enabled
        prefs.edit().putBoolean("dns_enabled", enabled).apply()
        DnsManager.clearCache()
    }

    fun setDnsMode(mode: String) {
        _dnsMode.value = mode
        prefs.edit().putString("dns_mode", mode).apply()
        DnsManager.clearCache()
    }

    fun setDnsPresetId(presetId: String) {
        _dnsPresetId.value = presetId
        prefs.edit().putString("dns_preset_id", presetId).apply()
        DnsManager.clearCache()
    }

    fun setDnsCustomValue(value: String) {
        _dnsCustomValue.value = value
        prefs.edit().putString("dns_custom_value", value).apply()
        DnsManager.clearCache()
    }

    fun incrementBlockedAds(tabId: Long) {
        val currentCount = _blockedAdsMap.value[tabId] ?: 0
        _blockedAdsMap.value = _blockedAdsMap.value.plus(tabId to currentCount + 1)
    }

    fun clearBlockedAds(tabId: Long) {
        _blockedAdsMap.value = _blockedAdsMap.value.plus(tabId to 0)
    }

    fun toggleAdBlockerPopup() {
        _isAdBlockerPopupVisible.value = !_isAdBlockerPopupVisible.value
        _isTabSwitcherVisible.value = false
        _isBookmarksHistorySheetVisible.value = false
    }

    fun setAdBlockerPopupVisible(visible: Boolean) {
        _isAdBlockerPopupVisible.value = visible
    }

    fun setSettingsScreenVisible(visible: Boolean) {
        _isSettingsScreenVisible.value = visible
        if (visible) {
            _currentSettingsSubScreen.value = "main"
        }
    }

    fun setSettingsSubScreen(sub: String) {
        _currentSettingsSubScreen.value = sub
    }

    fun setSmartAutoRouting(enabled: Boolean) {
        _smartAutoRouting.value = enabled
        prefs.edit().putBoolean("smart_auto_routing", enabled).apply()
        updateRoutingStatus(if (enabled) "Smart Engine Active" else "Direct Connection")
    }

    fun setSmartProxyRotator(enabled: Boolean) {
        _smartProxyRotator.value = enabled
        prefs.edit().putBoolean("smart_proxy_rotator", enabled).apply()
    }

    fun setSmartTorActive(enabled: Boolean) {
        _smartTorActive.value = enabled
        prefs.edit().putBoolean("smart_tor_active", enabled).apply()
    }

    fun setAIAutoGroupingEnabled(enabled: Boolean) {
        _aiAutoGroupingEnabled.value = enabled
        prefs.edit().putBoolean("ai_auto_grouping", enabled).apply()
    }

    fun setAISmartAdBlockerEnabled(enabled: Boolean) {
        _aiSmartAdBlockerEnabled.value = enabled
        prefs.edit().putBoolean("ai_smart_adblocker", enabled).apply()
    }

    fun setAISmartSummarizerEnabled(enabled: Boolean) {
        _aiSmartSummarizerEnabled.value = enabled
        prefs.edit().putBoolean("ai_smart_summarizer", enabled).apply()
    }

    fun triggerAIAutoGrouping() {
        viewModelScope.launch {
            val tabs = allTabs.value
            val groupedTabIds = mutableSetOf<Long>()
            
            // Explicit topics based on WWE, Apple, Reading/Research, Social, Shopping, Search
            val topics = listOf(
                "WWE Wrestling" to listOf("wwe", "wrestling", "smackdown", "raw", "aew", "wrestlemania", "royal rumble", "roman reigns"),
                "Apple News" to listOf("apple.com", "wwdc", "macbook", "iphone", "ipad", "steve jobs", "ios", "swiftui"),
                "Reading & Research" to listOf("medium.com", "substack", "wikipedia.org", "britannica.com", "blogger", "wordpress"),
                "Social Media" to listOf("twitter.com", "x.com", "facebook.com", "instagram.com", "reddit.com", "linkedin.com"),
                "Shopping" to listOf("amazon.com", "ebay.com", "shopify", "target.com", "walmart.com", "aliexpress.com"),
                "Search Engines" to listOf("google.com", "bing.com", "duckduckgo")
            )
            
            for ((groupName, keywords) in topics) {
                val matchingTabs = tabs.filter { tab ->
                    !groupedTabIds.contains(tab.id) && 
                    keywords.any { kw -> 
                        tab.title.contains(kw, ignoreCase = true) || tab.url.contains(kw, ignoreCase = true) 
                    }
                }
                if (matchingTabs.isNotEmpty()) {
                    matchingTabs.forEach { tab ->
                        repository.updateTab(tab.copy(groupName = groupName))
                        groupedTabIds.add(tab.id)
                    }
                }
            }
            
            // Domain grouping for remaining tabs
            val remainingTabs = allTabs.value.filter { !groupedTabIds.contains(it.id) && it.groupName == null }
            val domainGroups = remainingTabs.groupBy { getDomainName(it.url).lowercase() }
            
            for ((rawDomain, domainTabs) in domainGroups) {
                if (domainTabs.size >= 2 && rawDomain.isNotBlank() && rawDomain != "dineinstyle.com") {
                    val cleanDomain = if (rawDomain.endsWith(".com") || rawDomain.endsWith(".org") || rawDomain.endsWith(".net")) {
                        rawDomain.substring(0, rawDomain.lastIndexOf('.'))
                    } else {
                        rawDomain
                    }
                    val groupName = "${cleanDomain.replaceFirstChar { it.uppercase() }} Stack"
                    domainTabs.forEach { tab ->
                        repository.updateTab(tab.copy(groupName = groupName))
                    }
                }
            }
        }
    }

    fun runAIAutoGroupingForTab(tabId: Long, title: String, url: String) {
        if (!_aiAutoGroupingEnabled.value) return
        viewModelScope.launch {
            val tabs = allTabs.value
            val tab = tabs.find { it.id == tabId } ?: return@launch
            if (tab.groupName != null) return@launch
            
            val topics = listOf(
                "WWE Wrestling" to listOf("wwe", "wrestling", "smackdown", "raw", "aew", "wrestlemania", "royal rumble", "roman reigns"),
                "Apple News" to listOf("apple.com", "wwdc", "macbook", "iphone", "ipad", "steve jobs", "ios", "swiftui"),
                "Reading & Research" to listOf("medium.com", "substack", "wikipedia.org", "britannica.com", "blogger", "wordpress"),
                "Social Media" to listOf("twitter.com", "x.com", "facebook.com", "instagram.com", "reddit.com", "linkedin.com"),
                "Shopping" to listOf("amazon.com", "ebay.com", "shopify", "target.com", "walmart.com", "aliexpress.com"),
                "Search Engines" to listOf("google.com", "bing.com", "duckduckgo")
            )
            
            for ((groupName, keywords) in topics) {
                if (keywords.any { kw -> title.contains(kw, ignoreCase = true) || url.contains(kw, ignoreCase = true) }) {
                    repository.updateTab(tab.copy(groupName = groupName))
                    return@launch
                }
            }
            
            val currentDomain = getDomainName(url).lowercase()
            if (currentDomain.isNotBlank() && currentDomain != "dineinstyle.com") {
                val existingGroupTab = tabs.find { it.id != tabId && getDomainName(it.url).lowercase() == currentDomain && it.groupName != null }
                if (existingGroupTab != null) {
                    repository.updateTab(tab.copy(groupName = existingGroupTab.groupName))
                    return@launch
                }
                
                val existingUngroupedTab = tabs.find { it.id != tabId && getDomainName(it.url).lowercase() == currentDomain && it.groupName == null }
                if (existingUngroupedTab != null) {
                    val cleanDomain = if (currentDomain.endsWith(".com") || currentDomain.endsWith(".org") || currentDomain.endsWith(".net")) {
                        currentDomain.substring(0, currentDomain.lastIndexOf('.'))
                    } else {
                        currentDomain
                    }
                    val groupName = "${cleanDomain.replaceFirstChar { it.uppercase() }} Stack"
                    repository.updateTab(existingUngroupedTab.copy(groupName = groupName))
                    repository.updateTab(tab.copy(groupName = groupName))
                }
            }
        }
    }

    fun updateRoutingStatus(status: String) {
        _activeRoutingStatus.value = status
    }

    fun setCustomSearchEngine(name: String, url: String, shortcut: String) {
        _searchEngineName.value = name
        _searchEngineUrl.value = url
        _searchEngineShortcut.value = shortcut
        prefs.edit()
            .putString("search_engine_name", name)
            .putString("search_engine_url", url)
            .putString("search_engine_shortcut", shortcut)
            .apply()
    }

    fun setVideoListenInBackground(enabled: Boolean) {
        _videoListenInBackground.value = enabled
        prefs.edit().putBoolean("video_listen_background", enabled).apply()
    }

    fun setVideoShowToolbar(enabled: Boolean) {
        _videoShowToolbar.value = enabled
        prefs.edit().putBoolean("video_show_toolbar", enabled).apply()
    }

    fun setVideoShowMenu(enabled: Boolean) {
        _videoShowMenu.value = enabled
        prefs.edit().putBoolean("video_show_menu", enabled).apply()
    }

    fun setVideoYoutubeOption(option: String) {
        _videoYoutubeOption.value = option
        prefs.edit().putString("video_youtube_option", option).apply()
    }

    fun setUseUcPlayerEngine(enabled: Boolean) {
        _useUcPlayerEngine.value = enabled
        prefs.edit().putBoolean("use_uc_player_engine", enabled).apply()
    }

    fun setUcPlayerGestureControls(enabled: Boolean) {
        _ucPlayerGestureControls.value = enabled
        prefs.edit().putBoolean("uc_player_gesture_controls", enabled).apply()
    }

    fun setUcPlayerShowSpeedMeter(enabled: Boolean) {
        _ucPlayerShowSpeedMeter.value = enabled
        prefs.edit().putBoolean("uc_player_show_speed_meter", enabled).apply()
    }

    fun setUcPlayerDefaultSpeed(speed: Float) {
        _ucPlayerDefaultSpeed.value = speed
        prefs.edit().putFloat("uc_player_default_speed", speed).apply()
    }

    fun setUcPlayerActive(active: Boolean) {
        _ucPlayerActive.value = active
    }

    fun setUcPlayerVideoUrl(url: String) {
        _ucPlayerVideoUrl.value = url
    }

    fun setUcPlayerVideoTitle(title: String) {
        _ucPlayerVideoTitle.value = title
    }

    // Background Player Actions
    fun playBackgroundVideo(media: CapturedMedia) {
        _backgroundVideo.value = media
        _isBackgroundVideoPlaying.value = true
        
        backgroundMediaPlayer?.release()
        backgroundMediaPlayer = null
        
        try {
            backgroundMediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(media.url)
                setOnPreparedListener { mp ->
                    mp.start()
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            mp.playbackParams = mp.playbackParams.setSpeed(_ucPlayerDefaultSpeed.value)
                        }
                    } catch (e: Exception) {}
                }
                setOnCompletionListener {
                    playNextInQueue()
                }
                setOnErrorListener { _, _, _ ->
                    playNextInQueue()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            playNextInQueue()
        }
    }

    fun toggleBackgroundVideoPlay() {
        backgroundMediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isBackgroundVideoPlaying.value = false
            } else {
                mp.start()
                _isBackgroundVideoPlaying.value = true
            }
        } ?: run {
            _backgroundVideo.value?.let { playBackgroundVideo(it) }
        }
    }

    fun stopBackgroundVideo() {
        backgroundMediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (e: Exception) {}
            release()
        }
        backgroundMediaPlayer = null
        _backgroundVideo.value = null
        _isBackgroundVideoPlaying.value = false
    }

    fun playNextInQueue() {
        val currentQueue = _videoQueue.value
        if (currentQueue.isNotEmpty()) {
            val nextMedia = currentQueue.first()
            _videoQueue.value = currentQueue.drop(1)
            playBackgroundVideo(nextMedia)
        } else {
            stopBackgroundVideo()
        }
    }

    // Video Queue Actions
    fun addToVideoQueue(media: CapturedMedia) {
        val current = _videoQueue.value.toMutableList()
        if (!current.any { it.url == media.url }) {
            current.add(media)
            _videoQueue.value = current
        }
    }

    fun removeFromVideoQueue(id: Long) {
        _videoQueue.value = _videoQueue.value.filter { it.id != id }
    }

    fun clearVideoQueue() {
        _videoQueue.value = emptyList()
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val list = _videoQueue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val element = list.removeAt(fromIndex)
            list.add(toIndex, element)
            _videoQueue.value = list
        }
    }

    override fun onCleared() {
        super.onCleared()
        backgroundMediaPlayer?.release()
        backgroundMediaPlayer = null
    }

    fun setAlwaysUseHttps(enabled: Boolean) {
        _alwaysUseHttps.value = enabled
        prefs.edit().putBoolean("always_use_https", enabled).apply()
    }

    fun setRemoveFingerprint(enabled: Boolean) {
        _removeFingerprint.value = enabled
        prefs.edit().putBoolean("remove_fingerprint", enabled).apply()
    }

    fun setScriptControlEnabled(enabled: Boolean) {
        _scriptControlEnabled.value = enabled
        prefs.edit().putBoolean("script_control_enabled", enabled).apply()
    }

    fun setCookieManagementMode(mode: String) {
        _cookieManagementMode.value = mode
        prefs.edit().putString("cookie_management_mode", mode).apply()
    }

    fun setStopAppRedirects(enabled: Boolean) {
        _stopAppRedirects.value = enabled
        prefs.edit().putBoolean("stop_app_redirects", enabled).apply()
    }

    fun setSafeBrowsingEnabled(enabled: Boolean) {
        _safeBrowsingEnabled.value = enabled
        prefs.edit().putBoolean("safe_browsing_enabled", enabled).apply()
    }

    fun setDoNotTrack(enabled: Boolean) {
        _doNotTrack.value = enabled
        prefs.edit().putBoolean("do_not_track", enabled).apply()
    }

    fun setAutoDeAmp(enabled: Boolean) {
        _autoDeAmp.value = enabled
        prefs.edit().putBoolean("auto_de_amp", enabled).apply()
    }

    fun setGlobalPrivacyControl(enabled: Boolean) {
        _globalPrivacyControl.value = enabled
        prefs.edit().putBoolean("global_privacy_control", enabled).apply()
    }

    fun setThirdPartyCookiesSetting(value: String) {
        _thirdPartyCookiesSetting.value = value
        prefs.edit().putString("third_party_cookies_setting", value).apply()
    }

    fun setIncognitoTrackingProtections(value: String) {
        _incognitoTrackingProtections.value = value
        prefs.edit().putString("incognito_tracking_protections", value).apply()
    }

    fun setAdsPrivacyTopics(enabled: Boolean) {
        _adsPrivacyTopics.value = enabled
        prefs.edit().putBoolean("ads_privacy_topics", enabled).apply()
    }

    fun setAdsPrivacySiteSuggested(enabled: Boolean) {
        _adsPrivacySiteSuggested.value = enabled
        prefs.edit().putBoolean("ads_privacy_site_suggested", enabled).apply()
    }

    fun setAdsPrivacyMeasurement(enabled: Boolean) {
        _adsPrivacyMeasurement.value = enabled
        prefs.edit().putBoolean("ads_privacy_measurement", enabled).apply()
    }

    fun setPreloadPagesMode(value: String) {
        _preloadPagesMode.value = value
        prefs.edit().putString("preload_pages_mode", value).apply()
    }

    fun setLockIncognitoTabs(enabled: Boolean) {
        _lockIncognitoTabs.value = enabled
        prefs.edit().putBoolean("lock_incognito_tabs", enabled).apply()
    }

    fun setSafeBrowsingLevel(value: String) {
        _safeBrowsingLevel.value = value
        prefs.edit().putString("safe_browsing_level", value).apply()
    }

    fun setWarnPasswordCompromised(enabled: Boolean) {
        _warnPasswordCompromised.value = enabled
        prefs.edit().putBoolean("warn_password_compromised", enabled).apply()
    }

    fun setJsOptimisationAndSecurity(enabled: Boolean) {
        _jsOptimisationAndSecurity.value = enabled
        prefs.edit().putBoolean("js_optimisation_and_security", enabled).apply()
    }

    fun setPerformanceEngineEnabled(enabled: Boolean) {
        _performanceEngineEnabled.value = enabled
        prefs.edit().putBoolean("performance_engine_enabled", enabled).apply()
    }

    fun setV8JitMode(mode: String) {
        _v8JitMode.value = mode
        prefs.edit().putString("v8_jit_mode", mode).apply()
    }

    fun setV8OptimizationFlags(flags: Set<String>) {
        _v8OptimizationFlags.value = flags
        prefs.edit().putStringSet("v8_optimization_flags", flags).apply()
    }

    fun setAccessPaymentMethods(enabled: Boolean) {
        _accessPaymentMethods.value = enabled
        prefs.edit().putBoolean("access_payment_methods", enabled).apply()
    }

    fun deleteBrowsingData(history: Boolean, cookies: Boolean, cache: Boolean, tabs: Boolean) {
        viewModelScope.launch {
            if (history) {
                repository.clearAllHistory()
            }
            if (cookies) {
                try {
                    android.webkit.CookieManager.getInstance().removeAllCookies(null)
                    android.webkit.CookieManager.getInstance().flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (cache) {
                try {
                    val context = getApplication<Application>().applicationContext
                    context.cacheDir.deleteRecursively()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (tabs) {
                repository.clearAllTabs()
                createDefaultTab()
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setWebZoomLevel(level: Float) {
        _webZoomLevel.value = level
        prefs.edit().putFloat("web_zoom_level", level).apply()
    }

    fun setForceDarkWebpages(enabled: Boolean) {
        _forceDarkWebpages.value = enabled
        prefs.edit().putBoolean("force_dark_webpages", enabled).apply()
    }

    fun setWebTextZoom(zoom: Float) {
        _webTextZoom.value = zoom
        prefs.edit().putFloat("web_text_zoom", zoom).apply()
    }

    fun setHideDistractingItems(enabled: Boolean) {
        _hideDistractingItems.value = enabled
        prefs.edit().putBoolean("hide_distracting_items", enabled).apply()
    }

    fun setAddressBarPosition(position: String) {
        _addressBarPosition.value = position
        prefs.edit().putString("address_bar_position", position).apply()
    }

    fun setAutoHideBar(enabled: Boolean) {
        _autoHideBar.value = enabled
        prefs.edit().putBoolean("auto_hide_bar", enabled).apply()
    }

    fun setSwipeForFullscreen(enabled: Boolean) {
        _swipeForFullscreen.value = enabled
        prefs.edit().putBoolean("swipe_for_fullscreen", enabled).apply()
    }

    fun setSwipeToViewTabs(enabled: Boolean) {
        _swipeToViewTabs.value = enabled
        prefs.edit().putBoolean("swipe_to_view_tabs", enabled).apply()
    }

    fun setShowFullUrl(enabled: Boolean) {
        _showFullUrl.value = enabled
        prefs.edit().putBoolean("show_full_url", enabled).apply()
    }

    fun setHideBottomToolbar(enabled: Boolean) {
        _hideBottomToolbar.value = enabled
        prefs.edit().putBoolean("hide_bottom_toolbar", enabled).apply()
    }

    fun setMenuShowReader(enabled: Boolean) {
        _menuShowReader.value = enabled
        prefs.edit().putBoolean("menu_show_reader", enabled).apply()
    }

    fun setMenuPageZoom(enabled: Boolean) {
        _menuPageZoom.value = enabled
        prefs.edit().putBoolean("menu_page_zoom", enabled).apply()
    }

    fun setMenuFindOnPage(enabled: Boolean) {
        _menuFindOnPage.value = enabled
        prefs.edit().putBoolean("menu_find_on_page", enabled).apply()
    }

    fun setMenuRequestDesktop(enabled: Boolean) {
        _menuRequestDesktop.value = enabled
        prefs.edit().putBoolean("menu_request_desktop", enabled).apply()
    }

    fun setMenuAddToHome(enabled: Boolean) {
        _menuAddToHome.value = enabled
        prefs.edit().putBoolean("menu_add_to_home", enabled).apply()
    }

    fun setMenuDeveloperTools(enabled: Boolean) {
        _menuDeveloperTools.value = enabled
        prefs.edit().putBoolean("menu_developer_tools", enabled).apply()
    }

    fun setHomeShowFavorites(enabled: Boolean) {
        _homeShowFavorites.value = enabled
        prefs.edit().putBoolean("home_show_favorites", enabled).apply()
    }

    fun setHomeShowICloudTabs(enabled: Boolean) {
        _homeShowICloudTabs.value = enabled
        prefs.edit().putBoolean("home_show_icloud_tabs", enabled).apply()
    }

    fun swipeToNextTab() {
        val tabs = allTabs.value
        val activeId = _activeTabId.value ?: return
        val currentIndex = tabs.indexOfFirst { it.id == activeId }
        if (currentIndex != -1 && currentIndex < tabs.size - 1) {
            selectTab(tabs[currentIndex + 1].id)
        }
    }

    fun swipeToPreviousTab() {
        val tabs = allTabs.value
        val activeId = _activeTabId.value ?: return
        val currentIndex = tabs.indexOfFirst { it.id == activeId }
        if (currentIndex != -1 && currentIndex > 0) {
            selectTab(tabs[currentIndex - 1].id)
        }
    }

    fun toggleTabSwitcher() {
        _isTabSwitcherVisible.value = !_isTabSwitcherVisible.value
        _isAdBlockerPopupVisible.value = false
        _isBookmarksHistorySheetVisible.value = false
    }

    fun setTabSwitcherVisible(visible: Boolean) {
        _isTabSwitcherVisible.value = visible
    }

    fun toggleBookmarksHistorySheet(initialTab: Int = 0) {
        _activeSheetTab.value = initialTab
        _isBookmarksHistorySheetVisible.value = !_isBookmarksHistorySheetVisible.value
        _isAdBlockerPopupVisible.value = false
        _isTabSwitcherVisible.value = false
    }

    fun setBookmarksHistorySheetVisible(visible: Boolean) {
        _isBookmarksHistorySheetVisible.value = visible
    }

    fun setSheetTab(tabIndex: Int) {
        _activeSheetTab.value = tabIndex
    }

    fun updateNavigationState(tabId: Long, canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBackMap.value = _canGoBackMap.value.plus(tabId to canGoBack)
        _canGoForwardMap.value = _canGoForwardMap.value.plus(tabId to canGoForward)
    }

    fun updateLoadingProgress(tabId: Long, progress: Int) {
        _loadingProgressMap.value = _loadingProgressMap.value.plus(tabId to progress)
    }

    fun updateSearchQuery(query: String) {
        try {
            _searchQuery.value = query
            suggestionsJob?.cancel()
            if (query.isBlank()) {
                _searchSuggestions.value = emptyList()
                return
            }
            suggestionsJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    kotlinx.coroutines.delay(300)
                    val url = "https://suggestqueries.google.com/complete/search?client=chrome&q=" + java.net.URLEncoder.encode(query, "UTF-8")
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            val jsonArray = org.json.JSONArray(bodyString)
                            val suggestionsArray = jsonArray.optJSONArray(1)
                            val suggestionsList = mutableListOf<String>()
                            if (suggestionsArray != null) {
                                for (i in 0 until suggestionsArray.length()) {
                                    suggestionsList.add(suggestionsArray.optString(i))
                                }
                            }
                            _searchSuggestions.value = suggestionsList
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            if (repository.isBookmarked(url)) {
                repository.deleteBookmarkByUrl(url)
            } else {
                repository.insertBookmark(Bookmark(title = title, url = url))
            }
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            if (!repository.isBookmarked(url)) {
                repository.insertBookmark(Bookmark(title = title, url = url))
            }
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmarkById(id)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    // --- Custom Wallpaper and Shortcuts Methods ---
    fun updateWallpaperUrl(url: String?) {
        _customWallpaperUrl.value = url
        prefs.edit().putString("custom_wallpaper_url", url).apply()
    }

    fun updateShowNewsSection(show: Boolean) {
        _showNewsSection.value = show
        prefs.edit().putBoolean("show_news_section", show).apply()
    }

    fun addShortcut(title: String, url: String, iconUrl: String? = null) {
        viewModelScope.launch {
            val formattedUrl = formatUrl(url)
            repository.insertShortcut(
                HomepageShortcut(
                    title = title,
                    url = formattedUrl,
                    iconUrl = iconUrl
                )
            )
        }
    }

    fun updateShortcut(id: Long, title: String, url: String, iconUrl: String?) {
        viewModelScope.launch {
            val formattedUrl = formatUrl(url)
            repository.insertShortcut(
                HomepageShortcut(
                    id = id,
                    title = title,
                    url = formattedUrl,
                    iconUrl = iconUrl
                )
            )
        }
    }

    fun deleteShortcut(id: Long) {
        viewModelScope.launch {
            repository.deleteShortcutById(id)
        }
    }

    private fun formatUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed == "dineinstyle.com") return "dineinstyle.com"
        
        // Handle onion address automatically if smart auto-routing is enabled
        if (trimmed.contains(".onion")) {
            val cleanOnion = trimmed.replace("http://", "").replace("https://", "").trim()
            if (smartAutoRouting.value || smartTorActive.value) {
                updateRoutingStatus("Tor Gateway Active")
                return "https://$cleanOnion.pet"
            }
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        
        // Check if it looks like a URL
        val isUrl = (trimmed.contains(".") && !trimmed.contains(" ")) || trimmed.contains(".onion")
        return if (isUrl) {
            if (trimmed.contains(".onion")) {
                val cleanOnion = trimmed.replace("http://", "").replace("https://", "").trim()
                if (smartAutoRouting.value || smartTorActive.value) {
                    updateRoutingStatus("Tor Gateway Active")
                    "https://$cleanOnion.pet"
                } else {
                    "http://$cleanOnion"
                }
            } else {
                "https://$trimmed"
            }
        } else {
            // Custom search engine URL formatting
            val engineUrl = searchEngineUrl.value
            val encodedQuery = try {
                java.net.URLEncoder.encode(trimmed, "UTF-8")
            } catch (e: Exception) {
                trimmed.replace(" ", "+")
            }
            if (engineUrl.contains("%s")) {
                engineUrl.replace("%s", encodedQuery)
            } else {
                "$engineUrl$encodedQuery"
            }
        }
    }

    private fun getDomainName(url: String): String {
        if (url == "dineinstyle.com") return "dineinstyle.com"
        return try {
            val uri = java.net.URI(url)
            val domain = uri.host ?: ""
            if (domain.startsWith("www.")) domain.substring(4) else domain
        } catch (e: Exception) {
            url
        }
    }

    // --- Captured Media Methods ---
    fun toggleMediaStudio() {
        _isMediaStudioVisible.value = !_isMediaStudioVisible.value
        _isAdBlockerPopupVisible.value = false
        _isTabSwitcherVisible.value = false
        _isBookmarksHistorySheetVisible.value = false
    }

    fun setMediaStudioVisible(visible: Boolean) {
        _isMediaStudioVisible.value = visible
    }

    fun captureMedia(url: String, type: String, pageTitle: String, pageUrl: String) {
        if (url.isBlank() || url.length < 12) return
        val lower = url.lowercase()

        // 1. Instantly drop if the host is blacklisted as an ad or tracker
        if (AdBlocker.isAdRequest(url)) return

        // 2. Extra robust check on common code, analytics, and static tracker files
        if (lower.endsWith(".js") || lower.endsWith(".css") || lower.contains("analytics") ||
            lower.contains("googleads") || lower.contains("doubleclick") || lower.contains("pixel") ||
            lower.contains("favicon") || lower.contains("adservice") || lower.contains("adsystem")
        ) return

        // 3. Specifically for videos, filter common ad patterns and short ad clips
        if (type == "video") {
            val adVideoKeywords = listOf(
                "/ads/", "/ad/", "advert", "sponsor", "popunder", "popup", "banner",
                "preroll", "midroll", "postroll", "vast", "vpaid", "click", "tracker",
                "telemetry", "analytic", "pixel", "count", "metric", "beacon", "stat",
                "promo", "offer", "marketing", "campaign", "conversion", "retargeting",
                "mgid", "taboola", "outbrain", "exoclick", "juicyads", "ero-advertising",
                "trafficforce", "onclick", "popcash", "popads", "propellerads", "adsterra",
                "admaven", "adcash", "yandex", "mail.ru", "bidvertiser", "revenuehits"
            )
            if (adVideoKeywords.any { lower.contains(it) }) return
        }

        viewModelScope.launch {
            val existing = repository.getMediaByUrl(url)
            if (existing != null) {
                // Already captured, just refresh timestamp to make it current
                repository.updateMedia(existing.copy(timestamp = System.currentTimeMillis()))
            } else {
                repository.insertMedia(
                    CapturedMedia(
                        url = url,
                        type = type,
                        pageTitle = pageTitle,
                        pageUrl = pageUrl
                    )
                )
            }
        }
    }

    fun toggleLikeMedia(id: Long) {
        viewModelScope.launch {
            val mediaList = allCapturedMedia.value
            val media = mediaList.find { it.id == id }
            if (media != null) {
                repository.updateMedia(media.copy(isLiked = !media.isLiked))
            }
        }
    }

    fun toggleSaveMedia(id: Long) {
        viewModelScope.launch {
            val mediaList = allCapturedMedia.value
            val media = mediaList.find { it.id == id }
            if (media != null) {
                repository.updateMedia(media.copy(isSaved = !media.isSaved))
            }
        }
    }

    fun saveVideoToPlaylist(media: CapturedMedia) {
        viewModelScope.launch {
            val existing = repository.getMediaByUrl(media.url)
            if (existing != null) {
                repository.updateMedia(existing.copy(isSaved = true, type = "video"))
            } else {
                repository.insertMedia(
                    media.copy(
                        id = 0, // ensure auto-increment
                        isSaved = true,
                        type = "video"
                    )
                )
            }
        }
    }

    fun saveVideoToPlaylist(title: String, url: String, pageUrl: String) {
        viewModelScope.launch {
            val existing = repository.getMediaByUrl(url)
            if (existing != null) {
                repository.updateMedia(existing.copy(isSaved = true, type = "video"))
            } else {
                repository.insertMedia(
                    CapturedMedia(
                        url = url,
                        type = "video",
                        pageTitle = title,
                        pageUrl = pageUrl,
                        isSaved = true
                    )
                )
            }
        }
    }

    fun deleteMedia(id: Long) {
        viewModelScope.launch {
            repository.deleteMediaById(id)
        }
    }

    fun clearCapturedMediaHistory() {
        viewModelScope.launch {
            repository.clearAllCapturedMedia()
        }
    }

    fun insertUserScript(script: UserScript) {
        viewModelScope.launch {
            repository.insertUserScript(script)
        }
    }

    fun updateUserScript(script: UserScript) {
        viewModelScope.launch {
            repository.updateUserScript(script)
        }
    }

    fun deleteUserScript(script: UserScript) {
        viewModelScope.launch {
            repository.deleteUserScript(script)
        }
    }

    fun deleteUserScriptById(id: Long) {
        viewModelScope.launch {
            repository.deleteUserScriptById(id)
        }
    }

    // --- Downloads Helpers ---
    fun insertDownload(download: com.example.data.DownloadEntry) {
        viewModelScope.launch {
            repository.insertDownload(download)
        }
    }

    fun updateDownload(download: com.example.data.DownloadEntry) {
        viewModelScope.launch {
            repository.updateDownload(download)
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.deleteDownloadById(id)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
        }
    }

    // --- Web Cleaner Methods & Subscriptions ---
    fun setElementInspectionEnabled(enabled: Boolean) {
        _elementInspectionEnabled.value = enabled
        prefs.edit().putBoolean("element_inspection_enabled", enabled).apply()
    }

    fun setBlockAreaEnabled(enabled: Boolean) {
        _blockAreaEnabled.value = enabled
        prefs.edit().putBoolean("block_area_enabled", enabled).apply()
    }

    fun setOverlayBlockerEnabled(enabled: Boolean) {
        _overlayBlockerEnabled.value = enabled
        prefs.edit().putBoolean("overlay_blocker_enabled", enabled).apply()
    }

    fun setPopupBlockerMode(mode: String) {
        _popupBlockerMode.value = mode
        prefs.edit().putString("popup_blocker_mode", mode).apply()
    }

    fun setBlockAppExecutionEnabled(enabled: Boolean) {
        _blockAppExecutionEnabled.value = enabled
        prefs.edit().putBoolean("block_app_execution_enabled", enabled).apply()
    }

    fun setLockScreenEnabled(enabled: Boolean) {
        _lockScreenEnabled.value = enabled
        prefs.edit().putBoolean("lock_screen_enabled", enabled).apply()
    }

    fun setBlockedImagesEnabled(enabled: Boolean) {
        _blockedImagesEnabled.value = enabled
        prefs.edit().putBoolean("blocked_images_enabled", enabled).apply()
    }

    // Whitelist & Blocklist editors
    fun addAdWhitelist(domain: String) {
        val clean = domain.trim().lowercase()
        if (clean.isNotEmpty()) {
            val next = _adWhitelist.value + clean
            _adWhitelist.value = next
            prefs.edit().putStringSet("ad_whitelist", next).apply()
            AdBlocker.setAdWhitelist(next)
        }
    }

    fun removeAdWhitelist(domain: String) {
        val next = _adWhitelist.value - domain.trim().lowercase()
        _adWhitelist.value = next
        prefs.edit().putStringSet("ad_whitelist", next).apply()
        AdBlocker.setAdWhitelist(next)
    }

    fun addOverlayWhitelist(domain: String) {
        val clean = domain.trim().lowercase()
        if (clean.isNotEmpty()) {
            val next = _overlayWhitelist.value + clean
            _overlayWhitelist.value = next
            prefs.edit().putStringSet("overlay_whitelist", next).apply()
        }
    }

    fun removeOverlayWhitelist(domain: String) {
        val next = _overlayWhitelist.value - domain.trim().lowercase()
        _overlayWhitelist.value = next
        prefs.edit().putStringSet("overlay_whitelist", next).apply()
    }

    fun addPopupWhitelist(domain: String) {
        val clean = domain.trim().lowercase()
        if (clean.isNotEmpty()) {
            val next = _popupWhitelist.value + clean
            _popupWhitelist.value = next
            prefs.edit().putStringSet("popup_whitelist", next).apply()
        }
    }

    fun removePopupWhitelist(domain: String) {
        val next = _popupWhitelist.value - domain.trim().lowercase()
        _popupWhitelist.value = next
        prefs.edit().putStringSet("popup_whitelist", next).apply()
    }

    fun addBlockedLink(link: String) {
        val clean = link.trim().lowercase()
        if (clean.isNotEmpty()) {
            val next = _blockedLinks.value + clean
            _blockedLinks.value = next
            prefs.edit().putStringSet("blocked_links", next).apply()
        }
    }

    fun removeBlockedLink(link: String) {
        val next = _blockedLinks.value - link.trim().lowercase()
        _blockedLinks.value = next
        prefs.edit().putStringSet("blocked_links", next).apply()
    }

    fun addBlockedImage(pattern: String) {
        val clean = pattern.trim().lowercase()
        if (clean.isNotEmpty()) {
            val next = _blockedImages.value + clean
            _blockedImages.value = next
            prefs.edit().putStringSet("blocked_images", next).apply()
        }
    }

    fun removeBlockedImage(pattern: String) {
        val next = _blockedImages.value - pattern.trim().lowercase()
        _blockedImages.value = next
        prefs.edit().putStringSet("blocked_images", next).apply()
    }

    fun addCustomBlockedElement(selector: String) {
        val clean = selector.trim()
        if (clean.isNotEmpty()) {
            val next = _customBlockedElements.value + clean
            _customBlockedElements.value = next
            prefs.edit().putStringSet("custom_blocked_elements", next).apply()
        }
    }

    fun removeCustomBlockedElement(selector: String) {
        val next = _customBlockedElements.value - selector.trim()
        _customBlockedElements.value = next
        prefs.edit().putStringSet("custom_blocked_elements", next).apply()
    }

    fun isPopupWhitelisted(host: String): Boolean {
        var tempHost = host.lowercase().trim()
        while (tempHost.contains(".")) {
            if (_popupWhitelist.value.contains(tempHost)) {
                return true
            }
            tempHost = tempHost.substringAfter(".", "")
            if (tempHost.isEmpty()) break
        }
        return false
    }

    fun isOverlayWhitelisted(host: String): Boolean {
        var tempHost = host.lowercase().trim()
        while (tempHost.contains(".")) {
            if (_overlayWhitelist.value.contains(tempHost)) {
                return true
            }
            tempHost = tempHost.substringAfter(".", "")
            if (tempHost.isEmpty()) break
        }
        return false
    }

    fun isHostWhitelisted(host: String): Boolean {
        var tempHost = host.lowercase().trim()
        while (tempHost.contains(".")) {
            if (_adWhitelist.value.contains(tempHost)) {
                return true
            }
            tempHost = tempHost.substringAfter(".", "")
            if (tempHost.isEmpty()) break
        }
        return false
    }

    fun isLinkBlocked(url: String): Boolean {
        val lowerUrl = url.lowercase().trim()
        return _blockedLinks.value.any { lowerUrl.contains(it) }
    }

    fun isImageBlocked(url: String): Boolean {
        val lowerUrl = url.lowercase().trim()
        return _blockedImages.value.any { lowerUrl.contains(it) }
    }

    private fun saveAdFilters(filters: List<com.example.data.AdFilterSubscription>) {
        val array = org.json.JSONArray()
        for (f in filters) {
            val obj = org.json.JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("url", f.url)
            obj.put("lastUpdated", f.lastUpdated)
            obj.put("size", f.size)
            obj.put("enabled", f.enabled)
            array.put(obj)
        }
        prefs.edit().putString("ad_filter_subscriptions", array.toString()).apply()
    }

    private fun loadAdFilters(): List<com.example.data.AdFilterSubscription> {
        val jsonStr = prefs.getString("ad_filter_subscriptions", null) ?: return listOf(
            com.example.data.AdFilterSubscription("easylist", "EasyList", "https://easylist-downloads.adblockplus.org/easylist.txt", "01/07/2026", "1.95 MB", true),
            com.example.data.AdFilterSubscription("mobile_ads", "Mobile ads filter", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", "01/07/2026", "611.43 KB", true),
            com.example.data.AdFilterSubscription("oisd", "OISD Ads Filter", "https://small.oisd.nl", "01/07/2026", "824.12 KB", false)
        )
        val list = mutableListOf<com.example.data.AdFilterSubscription>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.data.AdFilterSubscription(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        url = obj.getString("url"),
                        lastUpdated = obj.getString("lastUpdated"),
                        size = obj.getString("size"),
                        enabled = obj.getBoolean("enabled")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun toggleAdFilter(id: String) {
        val updated = _adFilters.value.map {
            if (it.id == id) it.copy(enabled = !it.enabled) else it
        }
        _adFilters.value = updated
        saveAdFilters(updated)
        syncAdBlockerFilters()
    }

    fun addAdFilter(name: String, url: String) {
        val id = java.util.UUID.randomUUID().toString()
        val nextFilter = com.example.data.AdFilterSubscription(
            id = id,
            name = name,
            url = url,
            lastUpdated = "Never",
            size = "0 KB",
            enabled = true
        )
        val nextList = _adFilters.value + nextFilter
        _adFilters.value = nextList
        saveAdFilters(nextList)
        syncAdBlockerFilters()
    }

    fun updateAdFilter(id: String, name: String, url: String) {
        val updated = _adFilters.value.map {
            if (it.id == id) it.copy(name = name, url = url) else it
        }
        _adFilters.value = updated
        saveAdFilters(updated)
        syncAdBlockerFilters()
    }

    fun deleteAdFilter(id: String) {
        val updated = _adFilters.value.filter { it.id != id }
        _adFilters.value = updated
        saveAdFilters(updated)
        syncAdBlockerFilters()
    }

    fun syncAdBlockerFilters() {
        val enabledUrls = _adFilters.value.filter { it.enabled }.map { it.url }
        if (enabledUrls.isNotEmpty()) {
            AdBlocker.updateFilterLists(getApplication(), enabledUrls)
        } else {
            AdBlocker.updateFilterLists(getApplication(), emptyList())
        }
    }

    private val _copyUnblockDisabledDomains = MutableStateFlow<Set<String>>(
        prefs.getStringSet("copy_unblock_disabled_domains", emptySet()) ?: emptySet()
    )
    val copyUnblockDisabledDomains: StateFlow<Set<String>> = _copyUnblockDisabledDomains.asStateFlow()

    fun isCopyUnblockActiveForUrl(urlStr: String?): Boolean {
        if (urlStr.isNullOrEmpty()) return true
        val host = try {
            android.net.Uri.parse(urlStr).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
        if (host.isEmpty()) return true
        return !_copyUnblockDisabledDomains.value.contains(host)
    }

    fun toggleCopyUnblockForUrl(urlStr: String?) {
        if (urlStr.isNullOrEmpty()) return
        val host = try {
            android.net.Uri.parse(urlStr).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
        if (host.isEmpty()) return
        val currentSet = _copyUnblockDisabledDomains.value.toMutableSet()
        if (currentSet.contains(host)) {
            currentSet.remove(host)
        } else {
            currentSet.add(host)
        }
        _copyUnblockDisabledDomains.value = currentSet
        prefs.edit().putStringSet("copy_unblock_disabled_domains", currentSet).apply()
    }

    // --- Chrome Web Store Themes Support ---
    private val _chromeThemeActive = MutableStateFlow<Boolean>(prefs.getBoolean("chrome_theme_active", false))
    val chromeThemeActive: StateFlow<Boolean> = _chromeThemeActive.asStateFlow()

    private val _chromeThemeId = MutableStateFlow<String>(prefs.getString("chrome_theme_id", "") ?: "")
    val chromeThemeId: StateFlow<String> = _chromeThemeId.asStateFlow()

    private val _chromeThemeName = MutableStateFlow<String>(prefs.getString("chrome_theme_name", "Default") ?: "Default")
    val chromeThemeName: StateFlow<String> = _chromeThemeName.asStateFlow()

    private val _chromeThemeFrameColor = MutableStateFlow<Int>(prefs.getInt("chrome_theme_frame_color", android.graphics.Color.parseColor("#1E293B")))
    val chromeThemeFrameColor: StateFlow<Int> = _chromeThemeFrameColor.asStateFlow()

    private val _chromeThemeToolbarColor = MutableStateFlow<Int>(prefs.getInt("chrome_theme_toolbar_color", android.graphics.Color.parseColor("#0F172A")))
    val chromeThemeToolbarColor: StateFlow<Int> = _chromeThemeToolbarColor.asStateFlow()

    private val _chromeThemeTextColor = MutableStateFlow<Int>(prefs.getInt("chrome_theme_text_color", android.graphics.Color.WHITE))
    val chromeThemeTextColor: StateFlow<Int> = _chromeThemeTextColor.asStateFlow()

    private val _chromeThemeInactiveTextColor = MutableStateFlow<Int>(prefs.getInt("chrome_theme_inactive_text_color", android.graphics.Color.parseColor("#94A3B8")))
    val chromeThemeInactiveTextColor: StateFlow<Int> = _chromeThemeInactiveTextColor.asStateFlow()

    private val _chromeThemeNtpBgColor = MutableStateFlow<Int>(prefs.getInt("chrome_theme_ntp_bg_color", android.graphics.Color.parseColor("#0B0F19")))
    val chromeThemeNtpBgColor: StateFlow<Int> = _chromeThemeNtpBgColor.asStateFlow()

    private val _chromeThemeNtpTextColor = MutableStateFlow<Int>(prefs.getInt("chrome_theme_ntp_text_color", android.graphics.Color.WHITE))
    val chromeThemeNtpTextColor: StateFlow<Int> = _chromeThemeNtpTextColor.asStateFlow()

    private val _chromeThemeNtpBgPath = MutableStateFlow<String?>(prefs.getString("chrome_theme_ntp_bg_path", null))
    val chromeThemeNtpBgPath: StateFlow<String?> = _chromeThemeNtpBgPath.asStateFlow()

    fun resetChromeTheme() {
        val edit = prefs.edit()
        edit.putBoolean("chrome_theme_active", false)
        edit.remove("chrome_theme_id")
        edit.remove("chrome_theme_name")
        edit.remove("chrome_theme_frame_color")
        edit.remove("chrome_theme_toolbar_color")
        edit.remove("chrome_theme_text_color")
        edit.remove("chrome_theme_inactive_text_color")
        edit.remove("chrome_theme_ntp_bg_color")
        edit.remove("chrome_theme_ntp_text_color")
        edit.remove("chrome_theme_ntp_bg_path")
        edit.apply()

        // Delete downloaded image if it exists
        try {
            val file = java.io.File(getApplication<Application>().filesDir, "chrome_theme_bg.png")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _chromeThemeActive.value = false
        _chromeThemeId.value = ""
        _chromeThemeName.value = "Default"
        _chromeThemeFrameColor.value = android.graphics.Color.parseColor("#1E293B")
        _chromeThemeToolbarColor.value = android.graphics.Color.parseColor("#0F172A")
        _chromeThemeTextColor.value = android.graphics.Color.WHITE
        _chromeThemeInactiveTextColor.value = android.graphics.Color.parseColor("#94A3B8")
        _chromeThemeNtpBgColor.value = android.graphics.Color.parseColor("#0B0F19")
        _chromeThemeNtpTextColor.value = android.graphics.Color.WHITE
        _chromeThemeNtpBgPath.value = null
    }

    fun applyPresetTheme(
        id: String,
        name: String,
        frameColor: Int,
        toolbarColor: Int,
        textColor: Int,
        inactiveTextColor: Int,
        ntpBgColor: Int,
        ntpTextColor: Int
    ) {
        // Clear custom NTP background
        try {
            val file = java.io.File(getApplication<Application>().filesDir, "chrome_theme_bg.png")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val edit = prefs.edit()
        edit.putBoolean("chrome_theme_active", true)
        edit.putString("chrome_theme_id", id)
        edit.putString("chrome_theme_name", name)
        edit.putInt("chrome_theme_frame_color", frameColor)
        edit.putInt("chrome_theme_toolbar_color", toolbarColor)
        edit.putInt("chrome_theme_text_color", textColor)
        edit.putInt("chrome_theme_inactive_text_color", inactiveTextColor)
        edit.putInt("chrome_theme_ntp_bg_color", ntpBgColor)
        edit.putInt("chrome_theme_ntp_text_color", ntpTextColor)
        edit.remove("chrome_theme_ntp_bg_path")
        edit.apply()

        _chromeThemeActive.value = true
        _chromeThemeId.value = id
        _chromeThemeName.value = name
        _chromeThemeFrameColor.value = frameColor
        _chromeThemeToolbarColor.value = toolbarColor
        _chromeThemeTextColor.value = textColor
        _chromeThemeInactiveTextColor.value = inactiveTextColor
        _chromeThemeNtpBgColor.value = ntpBgColor
        _chromeThemeNtpTextColor.value = ntpTextColor
        _chromeThemeNtpBgPath.value = null
    }

    fun extractChromeThemeId(urlOrId: String): String? {
        val trimmed = urlOrId.trim()
        if (trimmed.length == 32 && trimmed.all { it in 'a'..'z' }) {
            return trimmed
        }
        val pattern = java.util.regex.Pattern.compile("([a-z]{32})")
        val matcher = pattern.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    fun installChromeThemeByUrlOrId(urlOrId: String, onResult: (Boolean, String) -> Unit) {
        val themeId = extractChromeThemeId(urlOrId)
        if (themeId == null) {
            onResult(false, "Invalid Chrome Web Store Theme URL or ID. Please check and try again.")
            return
        }
        installChromeTheme(themeId, onResult)
    }

    fun installChromeTheme(themeId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Download the crx file
                val urlString = "https://clients2.google.com/service/update2/crx?response=redirect&prodversion=110.0&acceptformat=crx2,crx3&x=id%3D${themeId}%26uc"
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                connection.connect()

                var responseCode = connection.responseCode
                var stream = connection.inputStream

                if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP || responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM) {
                    val newUrl = connection.getHeaderField("Location")
                    val conn2 = java.net.URL(newUrl).openConnection() as java.net.HttpURLConnection
                    conn2.connect()
                    responseCode = conn2.responseCode
                    stream = conn2.inputStream
                }

                if (responseCode !in 200..299) {
                    throw Exception("HTTP Error $responseCode from theme server")
                }

                processCrxStream(stream, themeId, onResult)
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch {
                    onResult(false, e.localizedMessage ?: "Failed to download theme")
                }
            }
        }
    }

    private fun processCrxStream(
        inputStream: java.io.InputStream,
        themeId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val bytes = inputStream.readBytes()
            if (bytes.size < 4) {
                throw Exception("Invalid theme file size downloaded")
            }

            var zipBytes = bytes
            // Check magic number "Cr24"
            if (bytes[0] == 0x43.toByte() && bytes[1] == 0x72.toByte() && bytes[2] == 0x32.toByte() && bytes[3] == 0x34.toByte()) {
                val version = (bytes[4].toInt() and 0xFF) or
                              ((bytes[5].toInt() and 0xFF) shl 8) or
                              ((bytes[6].toInt() and 0xFF) shl 16) or
                              ((bytes[7].toInt() and 0xFF) shl 24)

                val zipOffset = if (version == 3) {
                    val headerLength = (bytes[8].toInt() and 0xFF) or
                                       ((bytes[9].toInt() and 0xFF) shl 8) or
                                       ((bytes[10].toInt() and 0xFF) shl 16) or
                                       ((bytes[11].toInt() and 0xFF) shl 24)
                    12 + headerLength
                } else if (version == 2) {
                    val pkLen = (bytes[8].toInt() and 0xFF) or
                                 ((bytes[9].toInt() and 0xFF) shl 8) or
                                 ((bytes[10].toInt() and 0xFF) shl 16) or
                                 ((bytes[11].toInt() and 0xFF) shl 24)
                    val sigLen = (bytes[12].toInt() and 0xFF) or
                                  ((bytes[13].toInt() and 0xFF) shl 8) or
                                  ((bytes[14].toInt() and 0xFF) shl 16) or
                                  ((bytes[15].toInt() and 0xFF) shl 24)
                    16 + pkLen + sigLen
                } else {
                    0
                }
                if (zipOffset > 0 && zipOffset < bytes.size) {
                    zipBytes = bytes.copyOfRange(zipOffset, bytes.size)
                }
            }

            // Parse ZIP bytes
            val bais = java.io.ByteArrayInputStream(zipBytes)
            val zis = java.util.zip.ZipInputStream(bais)
            var entry = zis.nextEntry

            var manifestJsonString: String? = null
            val images = mutableMapOf<String, ByteArray>()

            while (entry != null) {
                val name = entry.name
                if (name == "manifest.json") {
                    manifestJsonString = zis.readBytes().toString(Charsets.UTF_8)
                } else if (name.startsWith("images/") || name.contains(".png") || name.contains(".jpg") || name.contains(".jpeg")) {
                    images[name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()

            if (manifestJsonString == null) {
                throw Exception("No manifest.json found in the theme archive")
            }

            parseAndApplyTheme(themeId, manifestJsonString, images, onResult)

        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                onResult(false, e.localizedMessage ?: "Failed to extract theme zip")
            }
        }
    }

    private fun parseAndApplyTheme(
        themeId: String,
        jsonString: String,
        images: Map<String, ByteArray>,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val rootObj = org.json.JSONObject(jsonString)
            val name = rootObj.optString("name", "Unnamed Theme")
            val themeObj = rootObj.optJSONObject("theme") ?: throw Exception("Not a valid Chrome theme (missing 'theme' object)")

            val colorsObj = themeObj.optJSONObject("colors")

            fun parseColor(key: String, default: Int): Int {
                if (colorsObj == null) return default
                val arr = colorsObj.optJSONArray(key) ?: return default
                if (arr.length() >= 3) {
                    val r = arr.getInt(0)
                    val g = arr.getInt(1)
                    val b = arr.getInt(2)
                    return android.graphics.Color.rgb(r, g, b)
                }
                return default
            }

            val frameColor = parseColor("frame", android.graphics.Color.parseColor("#1E293B"))
            val toolbarColor = parseColor("toolbar", android.graphics.Color.parseColor("#0F172A"))
            val textColor = parseColor("tab_text", android.graphics.Color.WHITE)
            val inactiveTextColor = parseColor("tab_background_text", android.graphics.Color.parseColor("#94A3B8"))
            val ntpBgColor = parseColor("ntp_background", android.graphics.Color.parseColor("#0B0F19"))
            val ntpTextColor = parseColor("ntp_text", android.graphics.Color.WHITE)

            var ntpBgPath: String? = null
            val imagesObj = themeObj.optJSONObject("images")
            if (imagesObj != null) {
                val ntpBgKey = imagesObj.optString("theme_ntp_background", "")
                if (ntpBgKey.isNotEmpty() && images.containsKey(ntpBgKey)) {
                    val context = getApplication<Application>().applicationContext
                    val file = java.io.File(context.filesDir, "chrome_theme_bg.png")
                    file.writeBytes(images[ntpBgKey]!!)
                    ntpBgPath = file.absolutePath
                }
            }

            val edit = prefs.edit()
            edit.putBoolean("chrome_theme_active", true)
            edit.putString("chrome_theme_id", themeId)
            edit.putString("chrome_theme_name", name)
            edit.putInt("chrome_theme_frame_color", frameColor)
            edit.putInt("chrome_theme_toolbar_color", toolbarColor)
            edit.putInt("chrome_theme_text_color", textColor)
            edit.putInt("chrome_theme_inactive_text_color", inactiveTextColor)
            edit.putInt("chrome_theme_ntp_bg_color", ntpBgColor)
            edit.putInt("chrome_theme_ntp_text_color", ntpTextColor)
            if (ntpBgPath != null) {
                edit.putString("chrome_theme_ntp_bg_path", ntpBgPath)
            } else {
                edit.remove("chrome_theme_ntp_bg_path")
            }
            edit.apply()

            viewModelScope.launch {
                _chromeThemeActive.value = true
                _chromeThemeId.value = themeId
                _chromeThemeName.value = name
                _chromeThemeFrameColor.value = frameColor
                _chromeThemeToolbarColor.value = toolbarColor
                _chromeThemeTextColor.value = textColor
                _chromeThemeInactiveTextColor.value = inactiveTextColor
                _chromeThemeNtpBgColor.value = ntpBgColor
                _chromeThemeNtpTextColor.value = ntpTextColor
                _chromeThemeNtpBgPath.value = ntpBgPath

                onResult(true, "Successfully installed theme: $name")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                onResult(false, "Failed to parse theme: " + e.localizedMessage)
            }
        }
    }

    // --- Video Download & Trim Feature ---
    private val _trimProgressState = MutableStateFlow<TrimProgress?>(null)
    val trimProgressState: StateFlow<TrimProgress?> = _trimProgressState.asStateFlow()

    fun downloadAndTrimVideo(
        context: Context,
        videoUrl: String,
        originalFilename: String,
        startMs: Long,
        endMs: Long
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val finalFilename = if (originalFilename.isBlank() || !originalFilename.contains(".")) "${System.currentTimeMillis()}.mp4" else originalFilename
            val baseName = finalFilename.substringBeforeLast(".")
            val ext = finalFilename.substringAfterLast(".", "mp4")
            val targetFilename = "${baseName}_trimmed_${startMs / 1000}s_${endMs / 1000}s.$ext"

            _trimProgressState.value = TrimProgress(
                filename = targetFilename,
                phase = "Downloading",
                progress = 0f,
                message = "Downloading video from source..."
            )

            // Insert initial pending download
            val initialDownload = com.example.data.DownloadEntry(
                filename = targetFilename,
                url = videoUrl,
                status = "Downloading",
                size = "Pending"
            )
            val dbId = repository.insertDownload(initialDownload)

            val tempSourceFile = java.io.File(context.cacheDir, "temp_source_${System.currentTimeMillis()}.mp4")
            val tempDestFile = java.io.File(context.cacheDir, "temp_trimmed_${System.currentTimeMillis()}.mp4")

            try {
                // 1. Download source video to cacheDir
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(videoUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to connect to video server (HTTP ${response.code})")
                    }

                    val body = response.body ?: throw Exception("Empty response body from video server")
                    val contentLength = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = java.io.FileOutputStream(tempSourceFile)

                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                            _trimProgressState.value = TrimProgress(
                                filename = targetFilename,
                                phase = "Downloading",
                                progress = progress * 0.8f, // Reserve last 20% for trimming
                                message = "Downloading: ${(progress * 100).toInt()}%"
                            )
                        } else {
                            _trimProgressState.value = TrimProgress(
                                filename = targetFilename,
                                phase = "Downloading",
                                progress = 0.4f,
                                message = "Downloading: ${String.format("%.2f MB", totalBytesRead.toFloat() / (1024 * 1024))}"
                            )
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                }

                // 2. Perform native trimming
                _trimProgressState.value = TrimProgress(
                    filename = targetFilename,
                    phase = "Trimming",
                    progress = 0.85f,
                    message = "Trimming video piece..."
                )

                val trimSuccess = VideoTrimmerHelper.trimMp4(
                    sourceFile = tempSourceFile,
                    outputFile = tempDestFile,
                    startMs = startMs,
                    endMs = endMs
                )

                if (!trimSuccess) {
                    throw Exception("Video cropping / trimming operation failed")
                }

                // 3. Move trimmed file to public Downloads folder
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val finalPublicFile = java.io.File(downloadsDir, targetFilename)
                
                // Copy/move processed file to public downloads
                tempDestFile.copyTo(finalPublicFile, overwrite = true)

                // Format nice readable size
                val sizeBytes = finalPublicFile.length()
                val readableSize = when {
                    sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes.toFloat() / (1024 * 1024))
                    sizeBytes >= 1024 -> String.format("%.2f KB", sizeBytes.toFloat() / 1024)
                    else -> "$sizeBytes bytes"
                }

                // Update download entry status in DB
                repository.updateDownload(
                    initialDownload.copy(
                        id = dbId,
                        status = "Completed",
                        size = readableSize
                    )
                )

                _trimProgressState.value = TrimProgress(
                    filename = targetFilename,
                    phase = "Success",
                    progress = 1.0f,
                    message = "Video cropped and saved to Downloads!"
                )

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Cropped video saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                repository.updateDownload(
                    initialDownload.copy(
                        id = dbId,
                        status = "Failed",
                        size = "0 KB"
                    )
                )

                _trimProgressState.value = TrimProgress(
                    filename = targetFilename,
                    phase = "Failed",
                    progress = 0f,
                    message = "Error: ${e.message}"
                )

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Trim failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                // Delete temp files
                try {
                    if (tempSourceFile.exists()) tempSourceFile.delete()
                    if (tempDestFile.exists()) tempDestFile.delete()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun clearTrimProgress() {
        _trimProgressState.value = null
    }

    fun setLowPowerModeEnabled(enabled: Boolean) {
        _lowPowerModeEnabled.value = enabled
        prefs.edit().putBoolean("low_power_mode_enabled", enabled).apply()
    }

    fun setBypassPaywallsEnabled(enabled: Boolean) {
        _bypassPaywallsEnabled.value = enabled
        prefs.edit().putBoolean("bypass_paywalls_enabled", enabled).apply()
    }

    fun setFullScreenReading(enabled: Boolean) {
        _fullScreenReading.value = enabled
    }

    fun toggleTabSortMode() {
        _tabSortMode.value = if (_tabSortMode.value == "Default") "ReadTime" else "Default"
    }

    fun updateTabReadTime(tabId: Long, wordCount: Int) {
        val minutes = if (wordCount <= 0) 1 else kotlin.math.ceil(wordCount.toDouble() / 200.0).toInt().coerceAtLeast(1)
        val updated = _tabReadTimes.value.toMutableMap()
        updated[tabId] = minutes
        _tabReadTimes.value = updated
    }

    fun toggleDesktopModeForTab(tabId: Long, context: android.content.Context) {
        val updated = _tabDesktopModes.value.toMutableMap()
        val isCurrentlyDesktop = updated[tabId] ?: false
        val newMode = !isCurrentlyDesktop
        updated[tabId] = newMode
        _tabDesktopModes.value = updated
        
        // Apply UA to WebView
        val wv = WebViewPool.getOrCreateWebView(context, tabId, this)
        wv.settings.apply {
            userAgentString = if (newMode) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            } else {
                null
            }
            useWideViewPort = newMode
            loadWithOverviewMode = newMode
        }
        wv.reload()
    }

    fun activateReaderMode(title: String, content: String) {
        _readerTitle.value = title
        _readerContent.value = content
        _isReaderModeActive.value = true
    }

    fun deactivateReaderMode() {
        _isReaderModeActive.value = false
    }

    fun setReaderTextSize(size: Int) {
        _readerTextSize.value = size
        prefs.edit().putInt("reader_text_size", size).apply()
    }

    fun setReaderTheme(theme: String) {
        _readerTheme.value = theme
        prefs.edit().putString("reader_theme", theme).apply()
    }

    fun setReaderFontFamily(family: String) {
        _readerFontFamily.value = family
        prefs.edit().putString("reader_font_family", family).apply()
    }

    fun oneTapClearEverything(context: android.content.Context) {
        viewModelScope.launch {
            // Clear database tables
            repository.clearAllHistory()
            repository.clearAllTabs()
            _tabReadTimes.value = emptyMap()
            _tabDesktopModes.value = emptyMap()
            
            // Clear WebView caches, cookies, databases
            android.webkit.WebStorage.getInstance().deleteAllData()
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            
            // Create a default tab
            addTab()
            
            android.widget.Toast.makeText(context, "All tabs, cache and history cleared successfully!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BrowserViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class SavedPassword(
    val id: Long,
    val site: String,
    val username: String,
    val password: String
)

data class TrimProgress(
    val filename: String,
    val phase: String, // "Downloading", "Trimming", "Success", "Failed"
    val progress: Float, // 0.0 to 1.0
    val message: String
)
