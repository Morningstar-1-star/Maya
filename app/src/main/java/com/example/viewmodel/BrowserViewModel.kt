package com.example.viewmodel

import android.app.Application
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
import com.example.data.DnsManager
import com.example.data.AdBlocker
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository

    // Base database flows
    val allTabs: StateFlow<List<BrowserTab>>
    val allHistory: StateFlow<List<HistoryEntry>>
    val allBookmarks: StateFlow<List<Bookmark>>
    val allShortcuts: StateFlow<List<HomepageShortcut>>
    val allCapturedMedia: StateFlow<List<CapturedMedia>>
    val likedSavedMedia: StateFlow<List<CapturedMedia>>

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

    private val _currentUrlInput = MutableStateFlow("")
    val currentUrlInput: StateFlow<String> = _currentUrlInput.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _adBlockerOn = MutableStateFlow(true)
    val adBlockerOn: StateFlow<Boolean> = _adBlockerOn.asStateFlow()

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

    private val _quickTabStripVisible = MutableStateFlow(true)
    val quickTabStripVisible: StateFlow<Boolean> = _quickTabStripVisible.asStateFlow()

    // Overlay visibility
    private val _isAdBlockerPopupVisible = MutableStateFlow(false)
    val isAdBlockerPopupVisible: StateFlow<Boolean> = _isAdBlockerPopupVisible.asStateFlow()

    private val _isTabSwitcherVisible = MutableStateFlow(false)
    val isTabSwitcherVisible: StateFlow<Boolean> = _isTabSwitcherVisible.asStateFlow()

    private val _isBookmarksHistorySheetVisible = MutableStateFlow(false)
    val isBookmarksHistorySheetVisible: StateFlow<Boolean> = _isBookmarksHistorySheetVisible.asStateFlow()

    private val _isMediaStudioVisible = MutableStateFlow(false)
    val isMediaStudioVisible: StateFlow<Boolean> = _isMediaStudioVisible.asStateFlow()

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

    init {
        AdBlocker.initialize(application)
        val database = AppDatabase.getDatabase(application)
        repository = BrowserRepository(database.browserDao())

        allTabs = repository.allTabs.stateIn(
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
            title = "Autumn '23 Collection",
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
            }
        }
    }

    fun addTab(url: String = "dineinstyle.com") {
        val cleanUrl = if (url == "dineinstyle.com") url else AdBlocker.cleanTrackingParameters(url)
        viewModelScope.launch {
            repository.deselectAllTabs()
            val newTab = BrowserTab(
                title = if (cleanUrl == "dineinstyle.com") "Autumn '23 Collection" else getDomainName(cleanUrl),
                url = cleanUrl,
                isSelected = true
            )
            val newId = repository.insertTab(newTab)
            _activeTabId.value = newId
            _currentUrlInput.value = cleanUrl
            _isTabSwitcherVisible.value = false // Close tab switcher when tab is added
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

    fun addTabToGroup(groupName: String, url: String = "dineinstyle.com") {
        val cleanUrl = if (url == "dineinstyle.com") url else AdBlocker.cleanTrackingParameters(url)
        viewModelScope.launch {
            repository.deselectAllTabs()
            val newTab = BrowserTab(
                title = if (cleanUrl == "dineinstyle.com") "Autumn '23 Collection" else getDomainName(cleanUrl),
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
        _searchQuery.value = query
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
