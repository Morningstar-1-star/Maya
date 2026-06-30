package com.example.ui

import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import android.webkit.WebView
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BrowserTab
import com.example.data.CapturedMedia
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.scale
import androidx.compose.ui.viewinterop.AndroidView
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel State variables
    val allTabs by viewModel.allTabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val urlInput by viewModel.currentUrlInput.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val adBlockerOn by viewModel.adBlockerOn.collectAsStateWithLifecycle()
    val blockedAdsMap by viewModel.blockedAdsMap.collectAsStateWithLifecycle()

    // Custom DNS State collectors
    val dnsEnabled by viewModel.dnsEnabled.collectAsStateWithLifecycle()
    val dnsMode by viewModel.dnsMode.collectAsStateWithLifecycle()
    val dnsPresetId by viewModel.dnsPresetId.collectAsStateWithLifecycle()
    val dnsCustomValue by viewModel.dnsCustomValue.collectAsStateWithLifecycle()

    // Settings States
    val isSettingsScreenVisible by viewModel.isSettingsScreenVisible.collectAsStateWithLifecycle()
    val currentSettingsSubScreen by viewModel.currentSettingsSubScreen.collectAsStateWithLifecycle()
    val smartAutoRouting by viewModel.smartAutoRouting.collectAsStateWithLifecycle()
    val smartProxyRotator by viewModel.smartProxyRotator.collectAsStateWithLifecycle()
    val smartTorActive by viewModel.smartTorActive.collectAsStateWithLifecycle()
    val activeRoutingStatus by viewModel.activeRoutingStatus.collectAsStateWithLifecycle()
    val searchEngineName by viewModel.searchEngineName.collectAsStateWithLifecycle()
    val searchEngineUrl by viewModel.searchEngineUrl.collectAsStateWithLifecycle()
    val searchEngineShortcut by viewModel.searchEngineShortcut.collectAsStateWithLifecycle()
    val videoListenInBackground by viewModel.videoListenInBackground.collectAsStateWithLifecycle()
    val videoShowToolbar by viewModel.videoShowToolbar.collectAsStateWithLifecycle()
    val videoShowMenu by viewModel.videoShowMenu.collectAsStateWithLifecycle()
    val videoYoutubeOption by viewModel.videoYoutubeOption.collectAsStateWithLifecycle()

    // UC Premium Video Player States
    val useUcPlayerEngine by viewModel.useUcPlayerEngine.collectAsStateWithLifecycle()
    val ucPlayerGestureControls by viewModel.ucPlayerGestureControls.collectAsStateWithLifecycle()
    val ucPlayerShowSpeedMeter by viewModel.ucPlayerShowSpeedMeter.collectAsStateWithLifecycle()
    val ucPlayerDefaultSpeed by viewModel.ucPlayerDefaultSpeed.collectAsStateWithLifecycle()
    val ucPlayerActive by viewModel.ucPlayerActive.collectAsStateWithLifecycle()
    val ucPlayerVideoUrl by viewModel.ucPlayerVideoUrl.collectAsStateWithLifecycle()
    val ucPlayerVideoTitle by viewModel.ucPlayerVideoTitle.collectAsStateWithLifecycle()
    val capturedMedia by viewModel.allCapturedMedia.collectAsStateWithLifecycle()

    val alwaysUseHttps by viewModel.alwaysUseHttps.collectAsStateWithLifecycle()
    val removeFingerprint by viewModel.removeFingerprint.collectAsStateWithLifecycle()
    val scriptControlEnabled by viewModel.scriptControlEnabled.collectAsStateWithLifecycle()
    val cookieManagementMode by viewModel.cookieManagementMode.collectAsStateWithLifecycle()
    val stopAppRedirects by viewModel.stopAppRedirects.collectAsStateWithLifecycle()
    val safeBrowsingEnabled by viewModel.safeBrowsingEnabled.collectAsStateWithLifecycle()
    val doNotTrack by viewModel.doNotTrack.collectAsStateWithLifecycle()
    val autoDeAmp by viewModel.autoDeAmp.collectAsStateWithLifecycle()
    val globalPrivacyControl by viewModel.globalPrivacyControl.collectAsStateWithLifecycle()

    // Appearance & Accessibility States
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val webZoomLevel by viewModel.webZoomLevel.collectAsStateWithLifecycle()
    val forceDarkWebpages by viewModel.forceDarkWebpages.collectAsStateWithLifecycle()
    val webTextZoom by viewModel.webTextZoom.collectAsStateWithLifecycle()
    val hideDistractingItems by viewModel.hideDistractingItems.collectAsStateWithLifecycle()

    val addressBarPosition by viewModel.addressBarPosition.collectAsStateWithLifecycle()
    val autoHideBar by viewModel.autoHideBar.collectAsStateWithLifecycle()
    val swipeForFullscreen by viewModel.swipeForFullscreen.collectAsStateWithLifecycle()
    val swipeToViewTabs by viewModel.swipeToViewTabs.collectAsStateWithLifecycle()
    val showFullUrl by viewModel.showFullUrl.collectAsStateWithLifecycle()
    val hideBottomToolbar by viewModel.hideBottomToolbar.collectAsStateWithLifecycle()

    val menuShowReader by viewModel.menuShowReader.collectAsStateWithLifecycle()
    val menuPageZoom by viewModel.menuPageZoom.collectAsStateWithLifecycle()
    val menuFindOnPage by viewModel.menuFindOnPage.collectAsStateWithLifecycle()
    val menuRequestDesktop by viewModel.menuRequestDesktop.collectAsStateWithLifecycle()
    val menuAddToHome by viewModel.menuAddToHome.collectAsStateWithLifecycle()
    val menuDeveloperTools by viewModel.menuDeveloperTools.collectAsStateWithLifecycle()

    val homeShowFavorites by viewModel.homeShowFavorites.collectAsStateWithLifecycle()
    val homeShowICloudTabs by viewModel.homeShowICloudTabs.collectAsStateWithLifecycle()
    val showNewsSection by viewModel.showNewsSection.collectAsStateWithLifecycle()

    // Overlay State variables
    val isAdBlockerPopupVisible by viewModel.isAdBlockerPopupVisible.collectAsStateWithLifecycle()
    val isTabSwitcherVisible by viewModel.isTabSwitcherVisible.collectAsStateWithLifecycle()
    val isBookmarksHistorySheetVisible by viewModel.isBookmarksHistorySheetVisible.collectAsStateWithLifecycle()
    val isMediaStudioVisible by viewModel.isMediaStudioVisible.collectAsStateWithLifecycle()
    val activeSheetTab by viewModel.activeSheetTab.collectAsStateWithLifecycle()

    var isSearchFocused by remember { mutableStateOf(false) }
    var showTabGroupManager by remember { mutableStateOf(false) }

    // Lists for rendering
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val allBookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()

    // Get active tab details
    val activeTab = allTabs.find { it.id == activeTabId }
    val activeTabGroupName = remember(activeTab) { activeTab?.groupName }
    val isTabGroupActive = activeTabGroupName != null
    val tabsInActiveGroupCount = remember(allTabs, activeTabGroupName) {
        if (activeTabGroupName != null) {
            allTabs.count { it.groupName == activeTabGroupName }
        } else {
            0
        }
    }
    val showQuickTabStrip = !isTabSwitcherVisible && 
            !isBookmarksHistorySheetVisible && 
            !isMediaStudioVisible && 
            !isSettingsScreenVisible && 
            viewModel.quickTabStripVisible.collectAsStateWithLifecycle().value && 
            isTabGroupActive && 
            tabsInActiveGroupCount > 1
    val activeTabProgress = activeTabId?.let { viewModel.loadingProgressMap.collectAsStateWithLifecycle().value[it] } ?: 100
    
    // Always allow back navigation if we are not on the homepage, so we can return to the homepage
    val activeTabCanGoBack = activeTabId?.let { id ->
        val webViewCanGoBack = viewModel.canGoBackMap.collectAsStateWithLifecycle().value[id] ?: false
        webViewCanGoBack || (activeTab?.url != "dineinstyle.com")
    } ?: false
    
    val activeTabCanGoForward = activeTabId?.let { viewModel.canGoForwardMap.collectAsStateWithLifecycle().value[it] } ?: false
    val activeTabBlockedAds = activeTabId?.let { blockedAdsMap[it] } ?: 0

    // Voice search setup
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                viewModel.updateUrlInput(spokenText)
                viewModel.loadUrl(spokenText)
            }
        }
    )

    fun startVoiceSearch() {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search or speak URL")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            // Speech recognizer not available, fallback to beautiful typing
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Immersive Web Content Area ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // Keep below safe area for content
        ) {
            if (activeTab != null) {
                if (activeTab.url == "dineinstyle.com") {
                    val wallpaperUrl by viewModel.customWallpaperUrl.collectAsStateWithLifecycle()
                    val shortcuts by viewModel.allShortcuts.collectAsStateWithLifecycle()
                    val showNewsSection by viewModel.showNewsSection.collectAsStateWithLifecycle()

                    // Show beautiful, animated native page
                    MockDineInStylePage(
                        wallpaperUrl = wallpaperUrl,
                        shortcuts = shortcuts,
                        showNewsSection = showNewsSection,
                        onShowNewsSectionChange = { show ->
                            viewModel.updateShowNewsSection(show)
                        },
                        onShortcutClicked = { url ->
                            viewModel.loadUrl(url)
                        },
                        onAddShortcut = { title, url, iconUrl ->
                            viewModel.addShortcut(title, url, iconUrl)
                        },
                        onDeleteShortcut = { id ->
                            viewModel.deleteShortcut(id)
                        },
                        onUpdateShortcut = { id, title, url, iconUrl ->
                            viewModel.updateShortcut(id, title, url, iconUrl)
                        },
                        onWallpaperChanged = { url ->
                            viewModel.updateWallpaperUrl(url)
                        },
                        onProductClicked = { productName ->
                            viewModel.loadUrl("https://www.google.com/search?q=buy+$productName")
                        }
                    )
                } else {
                    // Show standard WebView with real content and adblock
                    TabWebView(
                        tabId = activeTab.id,
                        url = activeTab.url,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Empty state if tabs are loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // --- 2. Semi-Transparent Blur Backdrop behind overlays ---
        val overlayActive = isAdBlockerPopupVisible || isTabSwitcherVisible || isBookmarksHistorySheetVisible || isMediaStudioVisible || isSettingsScreenVisible
        AnimatedVisibility(
            visible = overlayActive,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.setAdBlockerPopupVisible(false)
                        viewModel.setTabSwitcherVisible(false)
                        viewModel.setBookmarksHistorySheetVisible(false)
                        viewModel.setMediaStudioVisible(false)
                        viewModel.setSettingsScreenVisible(false)
                        focusManager.clearFocus()
                    }
            )
        }

        // --- 3. Ad Blocker Panel (Screen 2 Layout) ---
        AnimatedVisibility(
            visible = isAdBlockerPopupVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 450.dp)
        ) {
            AdBlockerPanel(
                adBlockerOn = adBlockerOn,
                blockedCount = activeTabBlockedAds,
                onToggleAdBlocker = { viewModel.toggleAdBlocker() },
                dnsEnabled = dnsEnabled,
                dnsMode = dnsMode,
                dnsPresetId = dnsPresetId,
                dnsCustomValue = dnsCustomValue,
                onDnsEnabledChange = { viewModel.setDnsEnabled(it) },
                onDnsModeChange = { viewModel.setDnsMode(it) },
                onDnsPresetIdChange = { viewModel.setDnsPresetId(it) },
                onDnsCustomValueChange = { viewModel.setDnsCustomValue(it) },
                smartAutoRouting = smartAutoRouting,
                onSmartAutoRoutingChange = { viewModel.setSmartAutoRouting(it) },
                activeRoutingStatus = activeRoutingStatus,
                onClose = { viewModel.setAdBlockerPopupVisible(false) }
            )
        }

        // --- 4. Tab Switcher Drawer (Screen 3 Layout) ---
        AnimatedVisibility(
            visible = isTabSwitcherVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.82f)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.82f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            TabSwitcherLayout(
                tabs = allTabs,
                searchQuery = searchQuery,
                onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                onTabSelected = { viewModel.selectTab(it.id) },
                onTabClosed = { viewModel.closeTab(it.id) },
                onClose = { viewModel.setTabSwitcherVisible(false) },
                onManageGroups = { showTabGroupManager = true }
            )
        }

        // --- 5. Bookmarks & History Bottom Sheet ---
        AnimatedVisibility(
            visible = isBookmarksHistorySheetVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.82f)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.82f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BookmarksAndHistorySheet(
                activeTab = activeSheetTab,
                bookmarks = allBookmarks,
                history = allHistory,
                onTabSelected = { viewModel.setSheetTab(it) },
                onItemClicked = { url ->
                    viewModel.loadUrl(url)
                    viewModel.setBookmarksHistorySheetVisible(false)
                },
                onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                onDeleteHistory = { id -> viewModel.deleteHistory(id) },
                onClearHistory = { viewModel.clearHistory() },
                onClose = { viewModel.setBookmarksHistorySheetVisible(false) }
            )
        }

        // --- 5b. Media Studio Center Sheet ---
        AnimatedVisibility(
            visible = isMediaStudioVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.82f)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 350f, dampingRatio = 0.82f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            MediaStudioSheet(
                viewModel = viewModel,
                onClose = { viewModel.setMediaStudioVisible(false) }
            )
        }

        // --- 5c. Settings Fullscreen Overlay ---
        AnimatedVisibility(
            visible = isSettingsScreenVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.82f)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.82f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            SettingsOverlay(
                isVisible = isSettingsScreenVisible,
                currentSubScreen = currentSettingsSubScreen,
                onClose = { viewModel.setSettingsScreenVisible(false) },
                onNavigateSub = { viewModel.setSettingsSubScreen(it) },
                searchEngineName = searchEngineName,
                searchEngineUrl = searchEngineUrl,
                searchEngineShortcut = searchEngineShortcut,
                onSaveSearchEngine = { name, url, shortcut -> viewModel.setCustomSearchEngine(name, url, shortcut) },
                videoListenInBackground = videoListenInBackground,
                onVideoListenChange = { viewModel.setVideoListenInBackground(it) },
                videoShowToolbar = videoShowToolbar,
                onVideoShowToolbarChange = { viewModel.setVideoShowToolbar(it) },
                videoShowMenu = videoShowMenu,
                onVideoShowMenuChange = { viewModel.setVideoShowMenu(it) },
                videoYoutubeOption = videoYoutubeOption,
                onVideoYoutubeOptionChange = { viewModel.setVideoYoutubeOption(it) },
                useUcPlayerEngine = useUcPlayerEngine,
                onUseUcPlayerEngineChange = { viewModel.setUseUcPlayerEngine(it) },
                ucPlayerGestureControls = ucPlayerGestureControls,
                onUcPlayerGestureControlsChange = { viewModel.setUcPlayerGestureControls(it) },
                ucPlayerShowSpeedMeter = ucPlayerShowSpeedMeter,
                onUcPlayerShowSpeedMeterChange = { viewModel.setUcPlayerShowSpeedMeter(it) },
                ucPlayerDefaultSpeed = ucPlayerDefaultSpeed,
                onUcPlayerDefaultSpeedChange = { viewModel.setUcPlayerDefaultSpeed(it) },
                alwaysUseHttps = alwaysUseHttps,
                onAlwaysUseHttpsChange = { viewModel.setAlwaysUseHttps(it) },
                removeFingerprint = removeFingerprint,
                onRemoveFingerprintChange = { viewModel.setRemoveFingerprint(it) },
                scriptControlEnabled = scriptControlEnabled,
                onScriptControlChange = { viewModel.setScriptControlEnabled(it) },
                cookieManagementMode = cookieManagementMode,
                onCookieManagementChange = { viewModel.setCookieManagementMode(it) },
                stopAppRedirects = stopAppRedirects,
                onStopAppRedirectsChange = { viewModel.setStopAppRedirects(it) },
                safeBrowsingEnabled = safeBrowsingEnabled,
                onSafeBrowsingChange = { viewModel.setSafeBrowsingEnabled(it) },
                doNotTrack = doNotTrack,
                onDoNotTrackChange = { viewModel.setDoNotTrack(it) },
                autoDeAmp = autoDeAmp,
                onAutoDeAmpChange = { viewModel.setAutoDeAmp(it) },
                globalPrivacyControl = globalPrivacyControl,
                onGlobalPrivacyControlChange = { viewModel.setGlobalPrivacyControl(it) },
                dnsEnabled = dnsEnabled,
                dnsMode = dnsMode,
                dnsPresetId = dnsPresetId,
                dnsCustomValue = dnsCustomValue,
                onDnsEnabledChange = { viewModel.setDnsEnabled(it) },
                onDnsModeChange = { viewModel.setDnsMode(it) },
                onDnsPresetIdChange = { viewModel.setDnsPresetId(it) },
                onDnsCustomValueChange = { viewModel.setDnsCustomValue(it) },
                smartAutoRouting = smartAutoRouting,
                onSmartAutoRoutingChange = { viewModel.setSmartAutoRouting(it) },
                smartProxyRotator = smartProxyRotator,
                onSmartProxyRotatorChange = { viewModel.setSmartProxyRotator(it) },
                smartTorActive = smartTorActive,
                onSmartTorActiveChange = { viewModel.setSmartTorActive(it) },
                activeRoutingStatus = activeRoutingStatus,
                themeMode = themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                webZoomLevel = webZoomLevel,
                onWebZoomLevelChange = { viewModel.setWebZoomLevel(it) },
                forceDarkWebpages = forceDarkWebpages,
                onForceDarkWebpagesChange = { viewModel.setForceDarkWebpages(it) },
                webTextZoom = webTextZoom,
                onWebTextZoomChange = { viewModel.setWebTextZoom(it) },
                hideDistractingItems = hideDistractingItems,
                onHideDistractingItemsChange = { viewModel.setHideDistractingItems(it) },
                addressBarPosition = addressBarPosition,
                onAddressBarPositionChange = { viewModel.setAddressBarPosition(it) },
                autoHideBar = autoHideBar,
                onAutoHideBarChange = { viewModel.setAutoHideBar(it) },
                swipeForFullscreen = swipeForFullscreen,
                onSwipeForFullscreenChange = { viewModel.setSwipeForFullscreen(it) },
                swipeToViewTabs = swipeToViewTabs,
                onSwipeToViewTabsChange = { viewModel.setSwipeToViewTabs(it) },
                showFullUrl = showFullUrl,
                onShowFullUrlChange = { viewModel.setShowFullUrl(it) },
                hideBottomToolbar = hideBottomToolbar,
                onHideBottomToolbarChange = { viewModel.setHideBottomToolbar(it) },
                menuShowReader = menuShowReader,
                onMenuShowReaderChange = { viewModel.setMenuShowReader(it) },
                menuPageZoom = menuPageZoom,
                onMenuPageZoomChange = { viewModel.setMenuPageZoom(it) },
                menuFindOnPage = menuFindOnPage,
                onMenuFindOnPageChange = { viewModel.setMenuFindOnPage(it) },
                menuRequestDesktop = menuRequestDesktop,
                onMenuRequestDesktopChange = { viewModel.setMenuRequestDesktop(it) },
                menuAddToHome = menuAddToHome,
                onMenuAddToHomeChange = { viewModel.setMenuAddToHome(it) },
                menuDeveloperTools = menuDeveloperTools,
                onMenuDeveloperToolsChange = { viewModel.setMenuDeveloperTools(it) },
                homeShowFavorites = homeShowFavorites,
                onHomeShowFavoritesChange = { viewModel.setHomeShowFavorites(it) },
                homeShowICloudTabs = homeShowICloudTabs,
                onHomeShowICloudTabsChange = { viewModel.setHomeShowICloudTabs(it) },
                homeShowNews = showNewsSection,
                onHomeShowNewsChange = { viewModel.updateShowNewsSection(it) },
                quickTabStripVisible = viewModel.quickTabStripVisible.collectAsStateWithLifecycle().value,
                onQuickTabStripVisibleChange = { viewModel.toggleQuickTabStrip() }
            )
        }

        if (showTabGroupManager) {
            TabGroupManagerDialog(
                tabs = allTabs,
                activeTabId = activeTabId,
                onSetGroup = { tabId, groupName -> viewModel.setTabGroup(tabId, groupName) },
                onDeleteGroup = { groupName -> viewModel.deleteGroupTabs(groupName) },
                onDismiss = { showTabGroupManager = false }
            )
        }

        val linkContextMenuUrl by viewModel.linkContextMenuUrl.collectAsStateWithLifecycle()
        val linkContextMenuText by viewModel.linkContextMenuText.collectAsStateWithLifecycle()

        if (linkContextMenuUrl != null) {
            LinkContextMenuDialog(
                url = linkContextMenuUrl!!,
                text = linkContextMenuText ?: "",
                viewModel = viewModel,
                onDismiss = { viewModel.hideLinkContextMenu() }
            )
        }

        // --- 6. Address Bar and Controls Area ---
        if (addressBarPosition == "top") {
            // Address Bar at the top!
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible,
                    enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -50 }) + fadeOut(),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .widthIn(max = 450.dp)
                ) {
                    AddressBar(
                        url = urlInput,
                        isAdBlockerActive = adBlockerOn,
                        blockedAdsCount = activeTabBlockedAds,
                        canGoBack = activeTabCanGoBack,
                        canGoForward = activeTabCanGoForward,
                        loadingProgress = activeTabProgress,
                        onUrlSubmit = {
                            focusManager.clearFocus()
                            viewModel.setUserTyping(false)
                            viewModel.loadUrl(it)
                        },
                        onUrlChange = {
                            viewModel.setUserTyping(true)
                            viewModel.updateUrlInput(it)
                        },
                        onAddressBarClick = {
                            viewModel.updateSearchQuery(if (urlInput == "dineinstyle.com") "" else urlInput)
                            isSearchFocused = true
                        },
                        onBackClick = {
                            val activeId = activeTabId
                            if (activeId != null) {
                                val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                if (wv.canGoBack()) {
                                    wv.goBack()
                                } else {
                                    // Go back to homepage
                                    viewModel.loadUrl("dineinstyle.com")
                                }
                            }
                        },
                        onForwardClick = {
                            val activeId = activeTabId
                            if (activeId != null) {
                                val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                if (wv.canGoForward()) wv.goForward()
                            }
                        },
                        onShieldClick = {
                            viewModel.toggleAdBlockerPopup()
                        },
                        onMicClick = {
                            startVoiceSearch()
                        },
                        hideBottomToolbar = hideBottomToolbar,
                        tabCount = allTabs.size,
                        onTabSwitcherClick = { viewModel.toggleTabSwitcher() },
                        onNewTabClick = { viewModel.addTab() },
                        isTabSwitcherVisible = isTabSwitcherVisible,
                        menuShowReader = menuShowReader,
                        menuPageZoom = menuPageZoom,
                        menuFindOnPage = menuFindOnPage,
                        menuRequestDesktop = menuRequestDesktop,
                        menuAddToHome = menuAddToHome,
                        menuDeveloperTools = menuDeveloperTools,
                        onBookmarksClick = { viewModel.toggleBookmarksHistorySheet(0) },
                        onHistoryClick = { viewModel.toggleBookmarksHistorySheet(1) },
                        onMediaStudioClick = { viewModel.toggleMediaStudio() },
                        onClearCacheClick = {
                            WebView(context).clearCache(true)
                            viewModel.clearBlockedAds(activeTabId ?: 0)
                        },
                        onSettingsClick = { viewModel.setSettingsScreenVisible(true) },
                        onHomeClick = { viewModel.loadUrl("dineinstyle.com") }
                    )
                }
            }

            // Bottom Navigation Row at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Tab Strip (above bottom navigation row)
                AnimatedVisibility(
                    visible = showQuickTabStrip,
                    enter = slideInVertically(
                        initialOffsetY = { 80 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { 80 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    QuickTabStrip(
                        tabs = allTabs,
                        activeTabId = activeTabId,
                        onTabSelected = { tab -> viewModel.selectTab(tab.id) },
                        onTabClosed = { id -> viewModel.closeTab(id) },
                        onAddTab = { groupName -> 
                            if (groupName != null) {
                                viewModel.addTabToGroup(groupName)
                            } else {
                                viewModel.addTab()
                            }
                        },
                        onManageGroups = { showTabGroupManager = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                AnimatedVisibility(
                    visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible && !hideBottomToolbar,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 400.dp)
                            .height(56.dp)
                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(100.dp))
                            .shadow(12.dp, RoundedCornerShape(100.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. History Icon
                        IconButton(
                            onClick = { viewModel.toggleBookmarksHistorySheet(1) },
                            modifier = Modifier.testTag("nav_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Browsing History",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 2. Bookmark Pin Icon
                        val isBookmarkedState = remember(activeTab?.url, allBookmarks) {
                            allBookmarks.any { it.url == activeTab?.url }
                        }
                        IconButton(
                            onClick = {
                                activeTab?.let {
                                    viewModel.toggleBookmark(it.title, it.url)
                                }
                            },
                            modifier = Modifier.testTag("nav_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarkedState) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Website",
                                tint = if (isBookmarkedState) Color(0xFFD4E157) else Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 3. New Tab Button (+)
                        IconButton(
                            onClick = { viewModel.addTab() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.06f), CircleShape)
                                .testTag("nav_new_tab_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Tab",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 4. Tab Switcher Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { viewModel.toggleTabSwitcher() }
                                .testTag("nav_tab_switcher_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            val tabCount = allTabs.size
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(1.5.dp, if (isTabSwitcherVisible) Color.Black else Color.DarkGray, RoundedCornerShape(6.dp))
                                    .background(
                                        if (isTabSwitcherVisible) Color.Black else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabCount.toString(),
                                    color = if (isTabSwitcherVisible) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 5. Options Menu Icon (...)
                        var isMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { isMenuExpanded = !isMenuExpanded },
                                modifier = Modifier.testTag("nav_options_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More Options",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Bookmarks") },
                                    leadingIcon = { Icon(Icons.Default.Book, "Bookmarks") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.toggleBookmarksHistorySheet(0)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("History") },
                                    leadingIcon = { Icon(Icons.Default.History, "History") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.toggleBookmarksHistorySheet(1)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Media Studio Center") },
                                    leadingIcon = { Icon(Icons.Default.Collections, "Media Studio") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.toggleMediaStudio()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear All Cache") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, "Clear cache") },
                                    onClick = {
                                        isMenuExpanded = false
                                        WebView(context).clearCache(true)
                                        viewModel.clearBlockedAds(activeTabId ?: 0)
                                    }
                                )

                                // Custom Toggles
                                if (menuShowReader) {
                                    DropdownMenuItem(
                                        text = { Text("Reader Mode") },
                                        leadingIcon = { Icon(Icons.Default.Book, "Reader") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuPageZoom) {
                                    DropdownMenuItem(
                                        text = { Text("Zoom Controls") },
                                        leadingIcon = { Icon(Icons.Default.Add, "Zoom") },
                                        onClick = {
                                            isMenuExpanded = false
                                            viewModel.setSettingsScreenVisible(true)
                                        }
                                    )
                                }
                                if (menuFindOnPage) {
                                    DropdownMenuItem(
                                        text = { Text("Find on Page") },
                                        leadingIcon = { Icon(Icons.Default.Search, "Find") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuRequestDesktop) {
                                    DropdownMenuItem(
                                        text = { Text("Request Desktop") },
                                        leadingIcon = { Icon(Icons.Default.Home, "Desktop") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuAddToHome) {
                                    DropdownMenuItem(
                                        text = { Text("Add to Home") },
                                        leadingIcon = { Icon(Icons.Default.Add, "Add to Home") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuDeveloperTools) {
                                    DropdownMenuItem(
                                        text = { Text("Developer Tools") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, "Developer") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }

                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, "Settings") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.setSettingsScreenVisible(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dine In Style") },
                                    leadingIcon = { Icon(Icons.Default.Home, "Dine In Style Home") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.loadUrl("dineinstyle.com")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // BOTH Address Bar and Bottom Navigation Row at the bottom!
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Tab Strip (above bottom address bar & controls)
                AnimatedVisibility(
                    visible = showQuickTabStrip,
                    enter = slideInVertically(
                        initialOffsetY = { 80 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { 80 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    QuickTabStrip(
                        tabs = allTabs,
                        activeTabId = activeTabId,
                        onTabSelected = { tab -> viewModel.selectTab(tab.id) },
                        onTabClosed = { id -> viewModel.closeTab(id) },
                        onAddTab = { groupName -> 
                            if (groupName != null) {
                                viewModel.addTabToGroup(groupName)
                            } else {
                                viewModel.addTab()
                            }
                        },
                        onManageGroups = { showTabGroupManager = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Floating Address Bar
                AnimatedVisibility(
                    visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut(),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .widthIn(max = 450.dp)
                ) {
                    AddressBar(
                        url = urlInput,
                        isAdBlockerActive = adBlockerOn,
                        blockedAdsCount = activeTabBlockedAds,
                        canGoBack = activeTabCanGoBack,
                        canGoForward = activeTabCanGoForward,
                        loadingProgress = activeTabProgress,
                        onUrlSubmit = {
                            focusManager.clearFocus()
                            viewModel.setUserTyping(false)
                            viewModel.loadUrl(it)
                        },
                        onUrlChange = {
                            viewModel.setUserTyping(true)
                            viewModel.updateUrlInput(it)
                        },
                        onAddressBarClick = {
                            viewModel.updateSearchQuery(if (urlInput == "dineinstyle.com") "" else urlInput)
                            isSearchFocused = true
                        },
                        onBackClick = {
                            val activeId = activeTabId
                            if (activeId != null) {
                                val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                if (wv.canGoBack()) {
                                    wv.goBack()
                                } else {
                                    // Go back to homepage
                                    viewModel.loadUrl("dineinstyle.com")
                                }
                            }
                        },
                        onForwardClick = {
                            val activeId = activeTabId
                            if (activeId != null) {
                                val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                if (wv.canGoForward()) wv.goForward()
                            }
                        },
                        onShieldClick = {
                            viewModel.toggleAdBlockerPopup()
                        },
                        onMicClick = {
                            startVoiceSearch()
                        },
                        hideBottomToolbar = hideBottomToolbar,
                        tabCount = allTabs.size,
                        onTabSwitcherClick = { viewModel.toggleTabSwitcher() },
                        onNewTabClick = { viewModel.addTab() },
                        isTabSwitcherVisible = isTabSwitcherVisible,
                        menuShowReader = menuShowReader,
                        menuPageZoom = menuPageZoom,
                        menuFindOnPage = menuFindOnPage,
                        menuRequestDesktop = menuRequestDesktop,
                        menuAddToHome = menuAddToHome,
                        menuDeveloperTools = menuDeveloperTools,
                        onBookmarksClick = { viewModel.toggleBookmarksHistorySheet(0) },
                        onHistoryClick = { viewModel.toggleBookmarksHistorySheet(1) },
                        onMediaStudioClick = { viewModel.toggleMediaStudio() },
                        onClearCacheClick = {
                            WebView(context).clearCache(true)
                            viewModel.clearBlockedAds(activeTabId ?: 0)
                        },
                        onSettingsClick = { viewModel.setSettingsScreenVisible(true) },
                        onHomeClick = { viewModel.loadUrl("dineinstyle.com") }
                    )
                }

                // Bottom Navigation Row
                AnimatedVisibility(
                    visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible && !hideBottomToolbar,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 400.dp)
                            .height(56.dp)
                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(100.dp))
                            .shadow(12.dp, RoundedCornerShape(100.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. History Icon
                        IconButton(
                            onClick = { viewModel.toggleBookmarksHistorySheet(1) },
                            modifier = Modifier.testTag("nav_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Browsing History",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 2. Bookmark Pin Icon
                        val isBookmarkedState = remember(activeTab?.url, allBookmarks) {
                            allBookmarks.any { it.url == activeTab?.url }
                        }
                        IconButton(
                            onClick = {
                                activeTab?.let {
                                    viewModel.toggleBookmark(it.title, it.url)
                                }
                            },
                            modifier = Modifier.testTag("nav_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarkedState) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Website",
                                tint = if (isBookmarkedState) Color(0xFFD4E157) else Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 3. New Tab Button (+)
                        IconButton(
                            onClick = { viewModel.addTab() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.06f), CircleShape)
                                .testTag("nav_new_tab_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Tab",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 4. Tab Switcher Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { viewModel.toggleTabSwitcher() }
                                .testTag("nav_tab_switcher_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            val tabCount = allTabs.size
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(1.5.dp, if (isTabSwitcherVisible) Color.Black else Color.DarkGray, RoundedCornerShape(6.dp))
                                    .background(
                                        if (isTabSwitcherVisible) Color.Black else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabCount.toString(),
                                    color = if (isTabSwitcherVisible) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 5. Options Menu Icon (...)
                        var isMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { isMenuExpanded = !isMenuExpanded },
                                modifier = Modifier.testTag("nav_options_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More Options",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Bookmarks") },
                                    leadingIcon = { Icon(Icons.Default.Book, "Bookmarks") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.toggleBookmarksHistorySheet(0)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("History") },
                                    leadingIcon = { Icon(Icons.Default.History, "History") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.toggleBookmarksHistorySheet(1)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Media Studio Center") },
                                    leadingIcon = { Icon(Icons.Default.Collections, "Media Studio") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.toggleMediaStudio()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear All Cache") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, "Clear cache") },
                                    onClick = {
                                        isMenuExpanded = false
                                        WebView(context).clearCache(true)
                                        viewModel.clearBlockedAds(activeTabId ?: 0)
                                    }
                                )

                                // Custom Toggles
                                if (menuShowReader) {
                                    DropdownMenuItem(
                                        text = { Text("Reader Mode") },
                                        leadingIcon = { Icon(Icons.Default.Book, "Reader") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuPageZoom) {
                                    DropdownMenuItem(
                                        text = { Text("Zoom Controls") },
                                        leadingIcon = { Icon(Icons.Default.Add, "Zoom") },
                                        onClick = {
                                            isMenuExpanded = false
                                            viewModel.setSettingsScreenVisible(true)
                                        }
                                    )
                                }
                                if (menuFindOnPage) {
                                    DropdownMenuItem(
                                        text = { Text("Find on Page") },
                                        leadingIcon = { Icon(Icons.Default.Search, "Find") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuRequestDesktop) {
                                    DropdownMenuItem(
                                        text = { Text("Request Desktop") },
                                        leadingIcon = { Icon(Icons.Default.Home, "Desktop") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuAddToHome) {
                                    DropdownMenuItem(
                                        text = { Text("Add to Home") },
                                        leadingIcon = { Icon(Icons.Default.Add, "Add to Home") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }
                                if (menuDeveloperTools) {
                                    DropdownMenuItem(
                                        text = { Text("Developer Tools") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, "Developer") },
                                        onClick = { isMenuExpanded = false }
                                    )
                                }

                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, "Settings") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.setSettingsScreenVisible(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dine In Style") },
                                    leadingIcon = { Icon(Icons.Default.Home, "Dine In Style Home") },
                                    onClick = {
                                        isMenuExpanded = false
                                        viewModel.loadUrl("dineinstyle.com")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 7. Search Active Overlay ---
        AnimatedVisibility(
            visible = isSearchFocused,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            SearchActiveOverlay(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearchSubmit = {
                    isSearchFocused = false
                    focusManager.clearFocus()
                    viewModel.setUserTyping(false)
                    viewModel.loadUrl(it)
                },
                onDismiss = {
                    isSearchFocused = false
                    focusManager.clearFocus()
                },
                historyList = allHistory
            )
        }

        // --- 8. UC Premium Video Player Intercept Floating Corner Icon ---
        val latestVideo = capturedMedia.lastOrNull { it.type == "video" }
        var dismissVideoToast by remember { mutableStateOf(false) }
        LaunchedEffect(activeTab?.url) {
            dismissVideoToast = false
        }

        var videoIconVisible by remember { mutableStateOf(false) }
        LaunchedEffect(latestVideo) {
            if (latestVideo != null) {
                videoIconVisible = true
                delay(8000)
                videoIconVisible = false
            } else {
                videoIconVisible = false
            }
        }

        val showCornerIcon = latestVideo != null && !dismissVideoToast && !ucPlayerActive && useUcPlayerEngine && videoIconVisible

        AnimatedVisibility(
            visible = showCornerIcon,
            enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 20.dp)
                .zIndex(99f)
        ) {
            Box(
                modifier = Modifier
                    .shadow(12.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFE11D48), Color(0xFF4F46E5))
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        latestVideo?.let {
                            viewModel.setUcPlayerVideoUrl(it.url)
                            viewModel.setUcPlayerVideoTitle(it.pageTitle)
                            viewModel.setUcPlayerActive(true)
                        }
                    }
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF0F172A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .background(Color(0xFFE11D48).copy(alpha = pulseAlpha), CircleShape)
                    )

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "UC Premium Player Intercept",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(start = 2.dp)
                    )
                }
            }
        }

        // --- 9. UC Fullscreen Premium Video Player Overlay ---
        AnimatedVisibility(
            visible = ucPlayerActive,
            enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f) // Draw on top of absolutely everything
        ) {
            UcPremiumVideoPlayer(
                videoUrl = ucPlayerVideoUrl,
                title = ucPlayerVideoTitle,
                onClose = { viewModel.setUcPlayerActive(false) },
                showSpeedMeter = ucPlayerShowSpeedMeter,
                gestureControlsEnabled = ucPlayerGestureControls,
                defaultSpeed = ucPlayerDefaultSpeed,
                capturedMedia = capturedMedia,
                onPlayOtherVideo = { media ->
                    viewModel.setUcPlayerVideoUrl(media.url)
                    viewModel.setUcPlayerVideoTitle(media.pageTitle)
                }
            )
        }
    }
}

// --- FLOATING ADDRESS BAR COMPONENT ---
@Composable
fun AddressBar(
    url: String,
    isAdBlockerActive: Boolean,
    blockedAdsCount: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    loadingProgress: Int,
    onUrlSubmit: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onAddressBarClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onShieldClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideBottomToolbar: Boolean = false,
    tabCount: Int = 1,
    onTabSwitcherClick: () -> Unit = {},
    onNewTabClick: () -> Unit = {},
    isTabSwitcherVisible: Boolean = false,
    menuShowReader: Boolean = true,
    menuPageZoom: Boolean = true,
    menuFindOnPage: Boolean = true,
    menuRequestDesktop: Boolean = true,
    menuAddToHome: Boolean = false,
    menuDeveloperTools: Boolean = false,
    onBookmarksClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onMediaStudioClick: () -> Unit = {},
    onClearCacheClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Loading Progress Bar
            if (loadingProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = Color(0xFFD4E157),
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    enabled = canGoBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = if (canGoBack) Color.Black else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Forward Button
                IconButton(
                    onClick = onForwardClick,
                    enabled = canGoForward,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Forward",
                        tint = if (canGoForward) Color.Black else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Address field container
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(19.dp))
                        .clip(RoundedCornerShape(19.dp))
                        .clickable { onAddressBarClick() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (url.isEmpty() || url == "dineinstyle.com") "Search or type URL" else url,
                        color = if (url.isEmpty() || url == "dineinstyle.com") Color.Gray else Color.Black,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Microphone Icon (Voice Input)
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Shield / Ad Blocker status indicator (Blue circle badge with check/bolt inside)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isAdBlockerActive && blockedAdsCount > 0) Color(0xFF1E88E5) else Color.DarkGray.copy(alpha = 0.3f),
                                CircleShape
                            )
                            .clickable(onClick = onShieldClick)
                            .testTag("shield_badge_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdBlockerActive) Icons.Default.ElectricBolt else Icons.Default.Shield,
                            contentDescription = "Ad Blocker Status",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (hideBottomToolbar) {
                    Spacer(modifier = Modifier.width(6.dp))

                    // Tab Count Box (Compact Tab Switcher Icon)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onTabSwitcherClick() }
                            .testTag("address_tab_switcher_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.5.dp, if (isTabSwitcherVisible) Color.Black else Color.DarkGray, RoundedCornerShape(6.dp))
                                .background(
                                    if (isTabSwitcherVisible) Color.Black else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabCount.toString(),
                                color = if (isTabSwitcherVisible) Color.White else Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 3-Dots Options Menu
                    var isMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { isMenuExpanded = !isMenuExpanded },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("address_options_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        BrowserOptionsMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            onBookmarksClick = onBookmarksClick,
                            onHistoryClick = onHistoryClick,
                            onMediaStudioClick = onMediaStudioClick,
                            onClearCacheClick = onClearCacheClick,
                            onSettingsClick = onSettingsClick,
                            onHomeClick = onHomeClick,
                            onNewTabClick = onNewTabClick,
                            menuShowReader = menuShowReader,
                            menuPageZoom = menuPageZoom,
                            menuFindOnPage = menuFindOnPage,
                            menuRequestDesktop = menuRequestDesktop,
                            menuAddToHome = menuAddToHome,
                            menuDeveloperTools = menuDeveloperTools
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onBookmarksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMediaStudioClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNewTabClick: (() -> Unit)? = null,
    menuShowReader: Boolean = true,
    menuPageZoom: Boolean = true,
    menuFindOnPage: Boolean = true,
    menuRequestDesktop: Boolean = true,
    menuAddToHome: Boolean = false,
    menuDeveloperTools: Boolean = false,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        if (onNewTabClick != null) {
            DropdownMenuItem(
                text = { Text("New Tab") },
                leadingIcon = { Icon(Icons.Default.Add, "New Tab") },
                onClick = {
                    onDismissRequest()
                    onNewTabClick()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Bookmarks") },
            leadingIcon = { Icon(Icons.Default.Book, "Bookmarks") },
            onClick = {
                onDismissRequest()
                onBookmarksClick()
            }
        )
        DropdownMenuItem(
            text = { Text("History") },
            leadingIcon = { Icon(Icons.Default.History, "History") },
            onClick = {
                onDismissRequest()
                onHistoryClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Media Studio Center") },
            leadingIcon = { Icon(Icons.Default.Collections, "Media Studio") },
            onClick = {
                onDismissRequest()
                onMediaStudioClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Clear All Cache") },
            leadingIcon = { Icon(Icons.Default.DeleteSweep, "Clear cache") },
            onClick = {
                onDismissRequest()
                onClearCacheClick()
            }
        )

        // Custom Toggles
        if (menuShowReader) {
            DropdownMenuItem(
                text = { Text("Reader Mode") },
                leadingIcon = { Icon(Icons.Default.Book, "Reader") },
                onClick = { onDismissRequest() }
            )
        }
        if (menuPageZoom) {
            DropdownMenuItem(
                text = { Text("Zoom Controls") },
                leadingIcon = { Icon(Icons.Default.Add, "Zoom") },
                onClick = {
                    onDismissRequest()
                    onSettingsClick()
                }
            )
        }
        if (menuFindOnPage) {
            DropdownMenuItem(
                text = { Text("Find on Page") },
                leadingIcon = { Icon(Icons.Default.Search, "Find") },
                onClick = { onDismissRequest() }
            )
        }
        if (menuRequestDesktop) {
            DropdownMenuItem(
                text = { Text("Request Desktop") },
                leadingIcon = { Icon(Icons.Default.Home, "Desktop") },
                onClick = { onDismissRequest() }
            )
        }
        if (menuAddToHome) {
            DropdownMenuItem(
                text = { Text("Add to Home") },
                leadingIcon = { Icon(Icons.Default.Add, "Add to Home") },
                onClick = { onDismissRequest() }
            )
        }
        if (menuDeveloperTools) {
            DropdownMenuItem(
                text = { Text("Developer Tools") },
                leadingIcon = { Icon(Icons.Default.Refresh, "Developer") },
                onClick = { onDismissRequest() }
            )
        }

        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Settings") },
            leadingIcon = { Icon(Icons.Default.Settings, "Settings") },
            onClick = {
                onDismissRequest()
                onSettingsClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Dine In Style") },
            leadingIcon = { Icon(Icons.Default.Home, "Dine In Style Home") },
            onClick = {
                onDismissRequest()
                onHomeClick()
            }
        )
    }
}

// Simple single-line text field for the address bar
@Composable
fun BasicTextFieldWithoutLabel(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = { onDone() }
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = Color.Black,
            fontSize = 14.sp
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// --- AD BLOCKER PANEL COMPONENT (Screen 2 popup) ---
@Composable
fun AdBlockerPanel(
    adBlockerOn: Boolean,
    blockedCount: Int,
    onToggleAdBlocker: () -> Unit,
    dnsEnabled: Boolean,
    dnsMode: String,
    dnsPresetId: String,
    dnsCustomValue: String,
    onDnsEnabledChange: (Boolean) -> Unit,
    onDnsModeChange: (String) -> Unit,
    onDnsPresetIdChange: (String) -> Unit,
    onDnsCustomValueChange: (String) -> Unit,
    smartAutoRouting: Boolean,
    onSmartAutoRoutingChange: (Boolean) -> Unit,
    activeRoutingStatus: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showDnsSettings by remember { mutableStateOf(false) }
    var showRoutingSettings by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle/Line indicator
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Power Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Power Button Circle Icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                if (adBlockerOn) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                                CircleShape
                            )
                            .clickable(onClick = onToggleAdBlocker)
                            .border(
                                1.5.dp,
                                if (adBlockerOn) Color(0xFF81C784) else Color.LightGray.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Ad Blocker Toggle",
                            tint = if (adBlockerOn) Color(0xFF4CAF50) else Color.DarkGray,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (adBlockerOn) "$blockedCount" else "0",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (adBlockerOn) Color(0xFFE53935) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "uBlock Ads Blocked",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "on this website (Ultra Shield active)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Switch control
                Switch(
                    checked = adBlockerOn,
                    onCheckedChange = { onToggleAdBlocker() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFE53935)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advisory Caption
            Text(
                text = "uBlock-powered cosmetic cleaner and network filter are active.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Advanced Options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedSettings = !showAdvancedSettings }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "uBlock Engine features",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showAdvancedSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle advanced settings",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showAdvancedSettings) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AdvancedToggleRow("uBlock network script filter rules", true)
                    AdvancedToggleRow("Cosmetic block & empty layout cleaning", true)
                    AdvancedToggleRow("Interstitials & cookie overlays auto-remover", true)
                    AdvancedToggleRow("Strict privacy tracker prevention", true)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Private & Custom DNS Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDnsSettings = !showDnsSettings }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "DNS Settings Icon",
                        tint = if (dnsEnabled) Color(0xFF4CAF50) else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Private & Custom DNS Servers",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (dnsEnabled) Color(0xFF4CAF50) else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showDnsSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle DNS settings",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showDnsSettings) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Toggle switch row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Custom DNS Engine",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Route connections via secure servers to bypass blocks and trackers.",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = dnsEnabled,
                            onCheckedChange = { onDnsEnabledChange(it) },
                            modifier = Modifier.scale(0.85f),
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF4CAF50)
                            )
                        )
                    }

                    if (dnsEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Mode Selector: Preset vs Custom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("preset" to "Preset Providers", "custom" to "Custom IP/DoH").forEach { (modeKey, modeTitle) ->
                                val isSelected = dnsMode == modeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onDnsModeChange(modeKey) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modeTitle,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (dnsMode == "preset") {
                            // Preset Providers list
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                com.example.data.DnsManager.presets.forEach { preset ->
                                    val isSelected = dnsPresetId == preset.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                color = if (isSelected) Color(0xFFE8F5E9) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onDnsPresetIdChange(preset.id) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = preset.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "Host: ${preset.fallbackIp} (DoH active)",
                                                fontSize = 9.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Custom Server IP / URL field
                            var tempCustomDns by remember(dnsCustomValue) { mutableStateOf(dnsCustomValue) }
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = tempCustomDns,
                                    onValueChange = { tempCustomDns = it },
                                    placeholder = { Text("e.g. dns.adguard-dns.com or 1.1.1.1", fontSize = 11.sp) },
                                    label = { Text("DNS IP, Domain, or DoH Endpoint", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (tempCustomDns != dnsCustomValue) {
                                            IconButton(
                                                onClick = { onDnsCustomValueChange(tempCustomDns) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Apply custom DNS",
                                                    tint = Color(0xFF4CAF50)
                                                )
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter a raw DNS IP (e.g. 1.0.0.1) or an HTTPS DNS-over-HTTPS URL (e.g. https://cloudflare-dns.com/dns-query).",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Private & Custom Smart Routing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRoutingSettings = !showRoutingSettings }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Smart Auto-Routing Icon",
                        tint = if (smartAutoRouting) Color(0xFFE53935) else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Smart Auto-Routing System",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (smartAutoRouting) Color(0xFFE53935) else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showRoutingSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Routing settings",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showRoutingSettings) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Toggle switch row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Smart Auto-Routing",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Auto-detects blocked sites and onion URLs to route securely.",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = smartAutoRouting,
                            onCheckedChange = { onSmartAutoRoutingChange(it) },
                            modifier = Modifier.scale(0.85f),
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFFE53935)
                            )
                        )
                    }

                    if (smartAutoRouting) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Status: $activeRoutingStatus",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• Onion Gateway: Active (Proxies .onion via Tor Web Bridge)",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "• Fallback Engine: Active (Bypasses errors via CroxyProxy rotation)",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                             }
                         }
                     }
                 }
             }
         }
    }
}

@Composable
fun AdvancedToggleRow(
    title: String,
    initialChecked: Boolean
) {
    var checked by remember { mutableStateOf(initialChecked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFFE53935)
            )
        )
    }
}

// Simple modifier extension to scale switches
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
)


// --- TAB SWITCHER BOTTOM SHEET (Screen 3 Layout) ---
@Composable
fun TabSwitcherLayout(
    tabs: List<BrowserTab>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onTabSelected: (BrowserTab) -> Unit,
    onTabClosed: (BrowserTab) -> Unit,
    onClose: () -> Unit,
    onManageGroups: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTabs = remember(tabs, searchQuery) {
        if (searchQuery.isBlank()) {
            tabs
        } else {
            tabs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true) }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F9F9)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp)
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray.copy(alpha = 0.5f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Card list title & close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Open Tabs (${tabs.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF0F172A))
                            .clickable { onManageGroups() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Stacks",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Stacks",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close switcher",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab previews row (Horizontal list matching mockup exactly!)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTabs, key = { it.id }) { tab ->
                    TabPreviewCard(
                        tab = tab,
                        onSelected = { onTabSelected(tab) },
                        onClosed = { onTabClosed(tab) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar ("Search tabs")
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search tabs", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .testTag("tab_switcher_search_field"),
                shape = RoundedCornerShape(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Preview card representing an open tab in the switcher
@Composable
fun TabPreviewCard(
    tab: BrowserTab,
    onSelected: () -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = tab.isSelected
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, Color(0xFF1E88E5))
    } else {
        BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
    }

    Card(
        modifier = modifier
            .width(170.dp)
            .fillMaxHeight()
            .clickable(onClick = onSelected),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Header (Favicon, Title, Close Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small Favicon Placeholder
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (tab.url == "dineinstyle.com") Color(0xFFFFB74D) else Color(0xFF64B5F6),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab.url == "dineinstyle.com") "D" else tab.title.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (tab.url == "dineinstyle.com") "Dine in Style" else tab.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClosed,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Tab Web Preview Body (Sleek card thumbnail representation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFEEEEEE))
            ) {
                if (tab.url == "dineinstyle.com") {
                    // Show a beautiful mini mockup of Dine In Style
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=400"
                        ),
                        contentDescription = "Mini Dine In Style Page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Minimal header overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Autumn '23",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Shop Now",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Show a stunning modern abstract card for regular sites
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3).copy(alpha = 0.2f),
                                        Color(0xFF00BCD4).copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Web Site",
                                tint = Color.Gray.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tab.url.removePrefix("https://").removePrefix("http://").take(30),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- BOOKMARKS & HISTORY SHEET PANEL ---
@Composable
fun BookmarksAndHistorySheet(
    activeTab: Int, // 0 for Bookmarks, 1 for History
    bookmarks: List<com.example.data.Bookmark>,
    history: List<com.example.data.HistoryEntry>,
    onTabSelected: (Int) -> Unit,
    onItemClicked: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray.copy(alpha = 0.5f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation selector for sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(19.dp))
                        .padding(2.dp)
                ) {
                    val tabModifier0 = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (activeTab == 0) Color.Black else Color.Transparent)
                        .clickable { onTabSelected(0) }
                        .padding(horizontal = 16.dp)

                    // Bookmarks Tab Button
                    Box(
                        modifier = tabModifier0,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bookmarks",
                            color = if (activeTab == 0) Color.White else Color.DarkGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val tabModifier1 = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (activeTab == 1) Color.Black else Color.Transparent)
                        .clickable { onTabSelected(1) }
                        .padding(horizontal = 16.dp)

                    // History Tab Button
                    Box(
                        modifier = tabModifier1,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "History",
                            color = if (activeTab == 1) Color.White else Color.DarkGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Clear History Button if on history tab
                    if (activeTab == 1 && history.isNotEmpty()) {
                        TextButton(
                            onClick = onClearHistory,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Clear all", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Panel",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeTab == 0) {
                    // Bookmarks Content List
                    if (bookmarks.isEmpty()) {
                        EmptyStateInfo(
                            icon = Icons.Default.PushPin,
                            title = "No Bookmarks Yet",
                            description = "Tap the pin icon in the bottom menu bar to bookmark your favorite sites."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(bookmarks) { bookmark ->
                                NavigationListItem(
                                    title = bookmark.title,
                                    subtitle = bookmark.url,
                                    onItemClick = { onItemClicked(bookmark.url) },
                                    onDelete = { onDeleteBookmark(bookmark.id) }
                                )
                            }
                        }
                    }
                } else {
                    // History Content List
                    if (history.isEmpty()) {
                        EmptyStateInfo(
                            icon = Icons.Outlined.History,
                            title = "No Browsing History",
                            description = "When you visit websites, they will appear here in chronological order."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(history) { entry ->
                                NavigationListItem(
                                    title = entry.title,
                                    subtitle = entry.url,
                                    onItemClick = { onItemClicked(entry.url) },
                                    onDelete = { onDeleteHistory(entry.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateInfo(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun NavigationListItem(
    title: String,
    subtitle: String,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.Black.copy(alpha = 0.04f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Webpage Icon",
                tint = Color.DarkGray,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Item",
                tint = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// --- MEDIA STUDIO CENTER COMPOSABLES ---
@Composable
fun MediaStudioSheet(
    viewModel: BrowserViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val capturedMedia by viewModel.allCapturedMedia.collectAsStateWithLifecycle()
    val likedSavedMedia by viewModel.likedSavedMedia.collectAsStateWithLifecycle()
    val useUcPlayerEngine by viewModel.useUcPlayerEngine.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 for Stream, 1 for Liked/Saved
    var selectedMediaForView by remember { mutableStateOf<CapturedMedia?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212) // Stylish premium dark aesthetic
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Clear Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = "Media Studio",
                        tint = Color(0xFFBB86FC),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Media Studio Center",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { viewModel.clearCapturedMediaHistory() }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(19.dp))
                        .padding(2.dp)
                ) {
                    val tabModifier0 = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (activeTab == 0) Color(0xFFBB86FC) else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(horizontal = 16.dp)

                    Box(
                        modifier = tabModifier0,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Auto Media Stream",
                            color = if (activeTab == 0) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val tabModifier1 = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (activeTab == 1) Color(0xFFBB86FC) else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(horizontal = 16.dp)

                    Box(
                        modifier = tabModifier1,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Liked & Saved",
                            color = if (activeTab == 1) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (activeTab == 0) {
                    // Auto Media Stream with chronological categorization
                    val now = System.currentTimeMillis()
                    val msInDay = 24 * 60 * 60 * 1000L

                    val within24h = capturedMedia.filter { now - it.timestamp <= msInDay }
                    val within3days = capturedMedia.filter { (now - it.timestamp > msInDay) && (now - it.timestamp <= 3 * msInDay) }
                    val within7days = capturedMedia.filter { (now - it.timestamp > 3 * msInDay) && (now - it.timestamp <= 7 * msInDay) }

                    if (capturedMedia.isEmpty()) {
                        EmptyStateView("No media captured yet.\nBrowse websites to capture images & videos automatically!")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (within24h.isNotEmpty()) {
                                item {
                                    MediaSectionHeader("Last 24 Hours", within24h.size)
                                }
                                item {
                                    MediaGridSection(within24h) { selectedMediaForView = it }
                                }
                            }

                            if (within3days.isNotEmpty()) {
                                item {
                                    MediaSectionHeader("Last 3 Days", within3days.size)
                                }
                                item {
                                    MediaGridSection(within3days) { selectedMediaForView = it }
                                }
                            }

                            if (within7days.isNotEmpty()) {
                                item {
                                    MediaSectionHeader("Last Week", within7days.size)
                                }
                                item {
                                    MediaGridSection(within7days) { selectedMediaForView = it }
                                }
                            }
                        }
                    }
                } else {
                    // Liked & Saved tab
                    if (likedSavedMedia.isEmpty()) {
                        EmptyStateView("No liked or saved media yet.\nHeart or save images inside the full-screen preview to pin them!")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                MediaGridSection(likedSavedMedia) { selectedMediaForView = it }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen viewer overlay
    selectedMediaForView?.let { media ->
        if (media.type == "video" && useUcPlayerEngine) {
            LaunchedEffect(media) {
                viewModel.setUcPlayerVideoUrl(media.url)
                viewModel.setUcPlayerVideoTitle(media.pageTitle)
                viewModel.setUcPlayerActive(true)
                selectedMediaForView = null
                onClose() // Close Media Studio Center sheet to let user see player
            }
        } else {
            MediaViewerDialog(
                media = media,
                onDismiss = { selectedMediaForView = null },
                onToggleLike = { viewModel.toggleLikeMedia(media.id) },
                onToggleSave = { viewModel.toggleSaveMedia(media.id) },
                onDelete = {
                    viewModel.deleteMedia(media.id)
                    selectedMediaForView = null
                }
            )
        }
    }
}

@Composable
fun MediaSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = "$count items",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun MediaGridSection(mediaList: List<CapturedMedia>, onClick: (CapturedMedia) -> Unit) {
    val rows = mediaList.chunked(3)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { media ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onClick(media) }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = media.url),
                            contentDescription = media.pageTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (media.type == "video") {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Bottom gradient with source website details
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = media.pageTitle,
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Heart icon indicator if liked
                        if (media.isLiked) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Liked",
                                tint = Color.Red,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
                // Fill the empty spaces of the row to keep equal sizing
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "No Media",
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun MediaViewerDialog(
    media: CapturedMedia,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isLikedState by remember { mutableStateOf(media.isLiked) }
    var isSavedState by remember { mutableStateOf(media.isSaved) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header with page Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.pageTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = media.pageUrl,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Media Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (media.type == "video") {
                        // Standard Video Player
                        AndroidView(
                            factory = { ctx ->
                                android.widget.VideoView(ctx).apply {
                                    setVideoPath(media.url)
                                    val mediaController = android.widget.MediaController(ctx)
                                    mediaController.setAnchorView(this)
                                    setMediaController(mediaController)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // High quality image
                        Image(
                            painter = rememberAsyncImagePainter(model = media.url),
                            contentDescription = "Image preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Like Button
                    IconButton(onClick = {
                        isLikedState = !isLikedState
                        onToggleLike()
                    }) {
                        Icon(
                            imageVector = if (isLikedState) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLikedState) Color.Red else Color.White
                        )
                    }

                    // 2. Save Button
                    IconButton(onClick = {
                        isSavedState = !isSavedState
                        onToggleSave()
                    }) {
                        Icon(
                            imageVector = if (isSavedState) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSavedState) Color(0xFFBB86FC) else Color.White
                        )
                    }

                    // 3. Download Button
                    IconButton(onClick = {
                        val extension = if (media.type == "video") "mp4" else "jpg"
                        val filename = "CapturedMedia_${System.currentTimeMillis()}.$extension"
                        downloadMedia(context, media.url, filename)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download to Device",
                            tint = Color.White
                        )
                    }

                    // 4. Share Button
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Shared Media")
                            putExtra(android.content.Intent.EXTRA_TEXT, media.url)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media URL"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }

                    // 5. Delete Button
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

fun downloadMedia(context: android.content.Context, url: String, filename: String) {
    try {
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("Downloading file from Media Studio")
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                filename
            )
        }
        val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        manager.enqueue(request)
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error starting download: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SearchActiveOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    historyList: List<com.example.data.HistoryEntry>,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // Automatically request focus on enter to open keyboard
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Sleek dark slate
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // Float nicely above the keyboard!
        ) {
            // Top cancel header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search & History",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFBB86FC), fontSize = 15.sp)
                }
            }

            // Recommendations / Suggestions List (takes remaining space above input)
            val filteredSuggestions = remember(query, historyList) {
                val list = if (query.isBlank()) {
                    historyList.map { it.title to it.url }
                } else {
                    historyList.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.url.contains(query, ignoreCase = true)
                    }.map { it.title to it.url }
                }

                // If empty or short, supplement with popular defaults
                if (list.size < 5) {
                    val defaults = listOf(
                        "google images" to "https://images.google.com",
                        "yandex" to "https://yandex.com",
                        "ind vs ireland" to "https://www.google.com/search?q=ind+vs+ireland",
                        "steam" to "https://store.steampowered.com",
                        "fifa world cup 2026" to "https://www.google.com/search?q=fifa+world+cup+2026",
                        "vegamovies" to "https://www.google.com/search?q=vegamovies",
                        "wwe" to "https://www.google.com/search?q=wwe"
                    )
                    val filteredDefaults = if (query.isBlank()) {
                        defaults
                    } else {
                        defaults.filter { it.first.contains(query, ignoreCase = true) }
                    }
                    (list + filteredDefaults).distinctBy { it.first.lowercase() }.take(10)
                } else {
                    list.take(10)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSuggestions) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSearchSubmit(suggestion.second)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = suggestion.first,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(
                            onClick = {
                                onQueryChange(suggestion.first)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Fill Query",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer(rotationZ = -45f)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            }

            // Beautiful typing search bar matching keyboard
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Simple editable text field
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotBlank()) {
                                    onSearchSubmit(query)
                                }
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFBB86FC)),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search or type URL",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsOverlay(
    isVisible: Boolean,
    currentSubScreen: String,
    onClose: () -> Unit,
    onNavigateSub: (String) -> Unit,
    // Search Engine
    searchEngineName: String,
    searchEngineUrl: String,
    searchEngineShortcut: String,
    onSaveSearchEngine: (String, String, String) -> Unit,
    // Video Options
    videoListenInBackground: Boolean,
    onVideoListenChange: (Boolean) -> Unit,
    videoShowToolbar: Boolean,
    onVideoShowToolbarChange: (Boolean) -> Unit,
    videoShowMenu: Boolean,
    onVideoShowMenuChange: (Boolean) -> Unit,
    videoYoutubeOption: String,
    onVideoYoutubeOptionChange: (String) -> Unit,
    // UC Premium Video Player settings
    useUcPlayerEngine: Boolean,
    onUseUcPlayerEngineChange: (Boolean) -> Unit,
    ucPlayerGestureControls: Boolean,
    onUcPlayerGestureControlsChange: (Boolean) -> Unit,
    ucPlayerShowSpeedMeter: Boolean,
    onUcPlayerShowSpeedMeterChange: (Boolean) -> Unit,
    ucPlayerDefaultSpeed: Float,
    onUcPlayerDefaultSpeedChange: (Float) -> Unit,
    // Privacy Guard
    alwaysUseHttps: Boolean,
    onAlwaysUseHttpsChange: (Boolean) -> Unit,
    removeFingerprint: Boolean,
    onRemoveFingerprintChange: (Boolean) -> Unit,
    scriptControlEnabled: Boolean,
    onScriptControlChange: (Boolean) -> Unit,
    cookieManagementMode: String,
    onCookieManagementChange: (String) -> Unit,
    stopAppRedirects: Boolean,
    onStopAppRedirectsChange: (Boolean) -> Unit,
    safeBrowsingEnabled: Boolean,
    onSafeBrowsingChange: (Boolean) -> Unit,
    doNotTrack: Boolean,
    onDoNotTrackChange: (Boolean) -> Unit,
    autoDeAmp: Boolean,
    onAutoDeAmpChange: (Boolean) -> Unit,
    globalPrivacyControl: Boolean,
    onGlobalPrivacyControlChange: (Boolean) -> Unit,
    // Custom DNS Settings
    dnsEnabled: Boolean,
    dnsMode: String,
    dnsPresetId: String,
    dnsCustomValue: String,
    onDnsEnabledChange: (Boolean) -> Unit,
    onDnsModeChange: (String) -> Unit,
    onDnsPresetIdChange: (String) -> Unit,
    onDnsCustomValueChange: (String) -> Unit,
    // Smart Auto-Routing
    smartAutoRouting: Boolean,
    onSmartAutoRoutingChange: (Boolean) -> Unit,
    smartProxyRotator: Boolean,
    onSmartProxyRotatorChange: (Boolean) -> Unit,
    smartTorActive: Boolean,
    onSmartTorActiveChange: (Boolean) -> Unit,
    activeRoutingStatus: String,
    // Appearance & Accessibility
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    webZoomLevel: Float,
    onWebZoomLevelChange: (Float) -> Unit,
    forceDarkWebpages: Boolean,
    onForceDarkWebpagesChange: (Boolean) -> Unit,
    webTextZoom: Float,
    onWebTextZoomChange: (Float) -> Unit,
    hideDistractingItems: Boolean,
    onHideDistractingItemsChange: (Boolean) -> Unit,
    // Customize Settings
    addressBarPosition: String,
    onAddressBarPositionChange: (String) -> Unit,
    autoHideBar: Boolean,
    onAutoHideBarChange: (Boolean) -> Unit,
    swipeForFullscreen: Boolean,
    onSwipeForFullscreenChange: (Boolean) -> Unit,
    swipeToViewTabs: Boolean,
    onSwipeToViewTabsChange: (Boolean) -> Unit,
    showFullUrl: Boolean,
    onShowFullUrlChange: (Boolean) -> Unit,
    hideBottomToolbar: Boolean = false,
    onHideBottomToolbarChange: (Boolean) -> Unit = {},
    menuShowReader: Boolean,
    onMenuShowReaderChange: (Boolean) -> Unit,
    menuPageZoom: Boolean,
    onMenuPageZoomChange: (Boolean) -> Unit,
    menuFindOnPage: Boolean,
    onMenuFindOnPageChange: (Boolean) -> Unit,
    menuRequestDesktop: Boolean,
    onMenuRequestDesktopChange: (Boolean) -> Unit,
    menuAddToHome: Boolean,
    onMenuAddToHomeChange: (Boolean) -> Unit,
    menuDeveloperTools: Boolean,
    onMenuDeveloperToolsChange: (Boolean) -> Unit,
    homeShowFavorites: Boolean,
    onHomeShowFavoritesChange: (Boolean) -> Unit,
    homeShowICloudTabs: Boolean,
    onHomeShowICloudTabsChange: (Boolean) -> Unit,
    homeShowNews: Boolean,
    onHomeShowNewsChange: (Boolean) -> Unit,
    quickTabStripVisible: Boolean = true,
    onQuickTabStripVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate 900 - ultra polished premium dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentSubScreen == "main") {
                            onClose()
                        } else if (currentSubScreen in listOf("customize_address_bar", "customize_menu", "tabs_start_page")) {
                            onNavigateSub("appearance_settings")
                        } else {
                            onNavigateSub("main")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = when (currentSubScreen) {
                        "search_engine" -> "Search Engine Settings"
                        "video_options" -> "Video Options Toolbar"
                        "privacy_guard" -> "Privacy Guard Control"
                        "dns_routing" -> "DNS & Secure Routing"
                        "appearance_settings" -> "Appearance Settings"
                        "customize_address_bar" -> "Customize Address Bar"
                        "customize_menu" -> "Customize Menu"
                        "tabs_start_page" -> "Tabs & Start Page"
                        else -> "Browser Advanced Settings"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Sub-screen selector
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (currentSubScreen) {
                    "main" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Category: General Settings
                            Text(
                                text = "GENERAL PREFERENCES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    SettingsItemRow(
                                        title = "Search Engine",
                                        subtitle = "Active: $searchEngineName",
                                        icon = Icons.Default.Language,
                                        iconColor = Color(0xFF38BDF8),
                                        onClick = { onNavigateSub("search_engine") }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    SettingsItemRow(
                                        title = "Video Toolbar & Background Play",
                                        subtitle = "Background Listen, Youtube Ad-free tools",
                                        icon = Icons.Default.PlayCircle,
                                        iconColor = Color(0xFFF43F5E),
                                        onClick = { onNavigateSub("video_options") }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    SettingsItemRow(
                                        title = "Appearance & Accessibility",
                                        subtitle = "Theme Mode, Web Zoom, Font Scale, Ad Hider",
                                        icon = Icons.Default.Palette,
                                        iconColor = Color(0xFFF59E0B),
                                        onClick = { onNavigateSub("appearance_settings") }
                                    )
                                }
                            }

                            // Category: Privacy & Security
                            Text(
                                text = "PRIVACY GUARD SYSTEM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                letterSpacing = 1.sp
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    SettingsItemRow(
                                        title = "Core Shield Control",
                                        subtitle = "HTTPS Force, Fingerprint Protection, Scripts",
                                        icon = Icons.Default.Shield,
                                        iconColor = Color(0xFF34D399),
                                        onClick = { onNavigateSub("privacy_guard") }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    SettingsItemRow(
                                        title = "DNS & Smart Auto-Routing",
                                        subtitle = "Private DoH Server, SOCKS & Tor Bridges",
                                        icon = Icons.Default.Dns,
                                        iconColor = Color(0xFFA78BFA),
                                        onClick = { onNavigateSub("dns_routing") }
                                    )
                                }
                            }

                            // System Info
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Ultra Shield Premium Browser",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Version 2.4.0 (Secure Sandbox Build)",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "search_engine" -> {
                        SearchEngineSubScreen(
                            currentName = searchEngineName,
                            currentUrl = searchEngineUrl,
                            currentShortcut = searchEngineShortcut,
                            onSave = { name, url, shortcut ->
                                onSaveSearchEngine(name, url, shortcut)
                                onNavigateSub("main")
                            }
                        )
                    }

                    "video_options" -> {
                        VideoOptionsSubScreen(
                            videoListenInBackground = videoListenInBackground,
                            onVideoListenChange = onVideoListenChange,
                            videoShowToolbar = videoShowToolbar,
                            onVideoShowToolbarChange = onVideoShowToolbarChange,
                            videoShowMenu = videoShowMenu,
                            onVideoShowMenuChange = onVideoShowMenuChange,
                            videoYoutubeOption = videoYoutubeOption,
                            onVideoYoutubeOptionChange = onVideoYoutubeOptionChange,
                            useUcPlayerEngine = useUcPlayerEngine,
                            onUseUcPlayerEngineChange = onUseUcPlayerEngineChange,
                            ucPlayerGestureControls = ucPlayerGestureControls,
                            onUcPlayerGestureControlsChange = onUcPlayerGestureControlsChange,
                            ucPlayerShowSpeedMeter = ucPlayerShowSpeedMeter,
                            onUcPlayerShowSpeedMeterChange = onUcPlayerShowSpeedMeterChange,
                            ucPlayerDefaultSpeed = ucPlayerDefaultSpeed,
                            onUcPlayerDefaultSpeedChange = onUcPlayerDefaultSpeedChange
                        )
                    }

                    "privacy_guard" -> {
                        PrivacyGuardSubScreen(
                            alwaysUseHttps = alwaysUseHttps,
                            onAlwaysUseHttpsChange = onAlwaysUseHttpsChange,
                            removeFingerprint = removeFingerprint,
                            onRemoveFingerprintChange = onRemoveFingerprintChange,
                            scriptControlEnabled = scriptControlEnabled,
                            onScriptControlChange = onScriptControlChange,
                            cookieManagementMode = cookieManagementMode,
                            onCookieManagementChange = onCookieManagementChange,
                            stopAppRedirects = stopAppRedirects,
                            onStopAppRedirectsChange = onStopAppRedirectsChange,
                            safeBrowsingEnabled = safeBrowsingEnabled,
                            onSafeBrowsingChange = onSafeBrowsingChange,
                            doNotTrack = doNotTrack,
                            onDoNotTrackChange = onDoNotTrackChange,
                            autoDeAmp = autoDeAmp,
                            onAutoDeAmpChange = onAutoDeAmpChange,
                            globalPrivacyControl = globalPrivacyControl,
                            onGlobalPrivacyControlChange = onGlobalPrivacyControlChange
                        )
                    }

                    "dns_routing" -> {
                        DnsRoutingSubScreen(
                            dnsEnabled = dnsEnabled,
                            dnsMode = dnsMode,
                            dnsPresetId = dnsPresetId,
                            dnsCustomValue = dnsCustomValue,
                            onDnsEnabledChange = onDnsEnabledChange,
                            onDnsModeChange = onDnsModeChange,
                            onDnsPresetIdChange = onDnsPresetIdChange,
                            onDnsCustomValueChange = onDnsCustomValueChange,
                            smartAutoRouting = smartAutoRouting,
                            onSmartAutoRoutingChange = onSmartAutoRoutingChange,
                            smartProxyRotator = smartProxyRotator,
                            onSmartProxyRotatorChange = onSmartProxyRotatorChange,
                            smartTorActive = smartTorActive,
                            onSmartTorActiveChange = onSmartTorActiveChange,
                            activeRoutingStatus = activeRoutingStatus
                        )
                    }

                    "appearance_settings" -> {
                        AppearanceSettingsSubScreen(
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            webZoomLevel = webZoomLevel,
                            onWebZoomLevelChange = onWebZoomLevelChange,
                            forceDarkWebpages = forceDarkWebpages,
                            onForceDarkWebpagesChange = onForceDarkWebpagesChange,
                            webTextZoom = webTextZoom,
                            onWebTextZoomChange = onWebTextZoomChange,
                            hideDistractingItems = hideDistractingItems,
                            onHideDistractingItemsChange = onHideDistractingItemsChange,
                            onNavigateSub = onNavigateSub
                        )
                    }

                    "customize_address_bar" -> {
                        CustomizeAddressBarSubScreen(
                            addressBarPosition = addressBarPosition,
                            onAddressBarPositionChange = onAddressBarPositionChange,
                            autoHideBar = autoHideBar,
                            onAutoHideBarChange = onAutoHideBarChange,
                            swipeForFullscreen = swipeForFullscreen,
                            onSwipeForFullscreenChange = onSwipeForFullscreenChange,
                            swipeToViewTabs = swipeToViewTabs,
                            onSwipeToViewTabsChange = onSwipeToViewTabsChange,
                            showFullUrl = showFullUrl,
                            onShowFullUrlChange = onShowFullUrlChange,
                            hideBottomToolbar = hideBottomToolbar,
                            onHideBottomToolbarChange = onHideBottomToolbarChange
                        )
                    }

                    "customize_menu" -> {
                        CustomizeMenuSubScreen(
                            menuShowReader = menuShowReader,
                            onMenuShowReaderChange = onMenuShowReaderChange,
                            menuPageZoom = menuPageZoom,
                            onMenuPageZoomChange = onMenuPageZoomChange,
                            menuFindOnPage = menuFindOnPage,
                            onMenuFindOnPageChange = onMenuFindOnPageChange,
                            menuRequestDesktop = menuRequestDesktop,
                            onMenuRequestDesktopChange = onMenuRequestDesktopChange,
                            menuAddToHome = menuAddToHome,
                            onMenuAddToHomeChange = onMenuAddToHomeChange,
                            menuDeveloperTools = menuDeveloperTools,
                            onMenuDeveloperToolsChange = onMenuDeveloperToolsChange
                        )
                    }

                    "tabs_start_page" -> {
                        TabsAndStartPageSubScreen(
                            homeShowFavorites = homeShowFavorites,
                            onHomeShowFavoritesChange = onHomeShowFavoritesChange,
                            homeShowICloudTabs = homeShowICloudTabs,
                            onHomeShowICloudTabsChange = onHomeShowICloudTabsChange,
                            homeShowNews = homeShowNews,
                            onHomeShowNewsChange = onHomeShowNewsChange,
                            quickTabStripVisible = quickTabStripVisible,
                            onQuickTabStripVisibleChange = onQuickTabStripVisibleChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SearchEngineSubScreen(
    currentName: String,
    currentUrl: String,
    currentShortcut: String,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var url by remember { mutableStateOf(currentUrl) }
    var shortcut by remember { mutableStateOf(currentShortcut) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PRESET PROVIDERS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp
        )

        val presets = listOf(
            Triple("Google", "https://www.google.com/search?q=%s", "g"),
            Triple("DuckDuckGo", "https://duckduckgo.com/?q=%s", "d"),
            Triple("Bing", "https://www.bing.com/search?q=%s", "b"),
            Triple("Baidu", "https://www.baidu.com/s?wd=%s", "ba"),
            Triple("Naver", "https://search.naver.com/search.naver?query=%s", "n")
        )

        presets.forEach { (presetName, presetUrl, presetShortcut) ->
            val isSelected = currentName == presetName
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSave(presetName, presetUrl, presetShortcut)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = presetName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = presetUrl,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "CUSTOM ENGINE PROPERTIES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Engine Name", color = Color.Gray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Query URL (with %s in place of query)", color = Color.Gray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shortcut,
                    onValueChange = { shortcut = it },
                    label = { Text("Engine Shortcut", color = Color.Gray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onSave(name, url, shortcut) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Apply & Save Engine", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun VideoOptionsSubScreen(
    videoListenInBackground: Boolean,
    onVideoListenChange: (Boolean) -> Unit,
    videoShowToolbar: Boolean,
    onVideoShowToolbarChange: (Boolean) -> Unit,
    videoShowMenu: Boolean,
    onVideoShowMenuChange: (Boolean) -> Unit,
    videoYoutubeOption: String,
    onVideoYoutubeOptionChange: (String) -> Unit,
    // UC Player settings
    useUcPlayerEngine: Boolean,
    onUseUcPlayerEngineChange: (Boolean) -> Unit,
    ucPlayerGestureControls: Boolean,
    onUcPlayerGestureControlsChange: (Boolean) -> Unit,
    ucPlayerShowSpeedMeter: Boolean,
    onUcPlayerShowSpeedMeterChange: (Boolean) -> Unit,
    ucPlayerDefaultSpeed: Float,
    onUcPlayerDefaultSpeedChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "UC PREMIUM VIDEO PLAYER ENGINE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE11D48), // Rose 600 - High energy premium primary
            letterSpacing = 1.2.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.25f)) // glowing outline
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Always Intercept with UC Player",
                    subtitle = "Automatically load online videos in the custom gesture-supported overlay player.",
                    checked = useUcPlayerEngine,
                    onCheckedChange = onUseUcPlayerEngineChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Lock Screen & Gesture Controls",
                    subtitle = "Allows double-tap to seek, swipe to volume/brightness, and locks control UI.",
                    checked = ucPlayerGestureControls,
                    onCheckedChange = onUcPlayerGestureControlsChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Live Network Speed Indicator",
                    subtitle = "Display real-time fluctuating bandwidth speed in the premium video overlay.",
                    checked = ucPlayerShowSpeedMeter,
                    onCheckedChange = onUseUcPlayerEngineChange // Sync'd or trigger state
                )
                if (ucPlayerShowSpeedMeter != useUcPlayerEngine) {
                    // Let's toggle correctly
                    LaunchedEffect(useUcPlayerEngine) {
                        onUcPlayerShowSpeedMeterChange(useUcPlayerEngine)
                    }
                }
            }
        }

        Text(
            text = "DEFAULT PLAYBACK STARTUP SPEED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE11D48),
            letterSpacing = 1.2.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val speedOptions = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
                speedOptions.forEach { speed ->
                    val isSelected = ucPlayerDefaultSpeed == speed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFE11D48) else Color.White.copy(alpha = 0.05f))
                            .clickable { onUcPlayerDefaultSpeedChange(speed) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "FLOATING VIDEO & MEDIA CONTROLS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF43F5E),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Listen in Background",
                    subtitle = "Keeps video audio playing when app is minimized or screen is off.",
                    checked = videoListenInBackground,
                    onCheckedChange = onVideoListenChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Floating Video Toolbar",
                    subtitle = "Displays quick download & overlay tools on detected video elements.",
                    checked = videoShowToolbar,
                    onCheckedChange = onVideoShowToolbarChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Fullscreen Video Menu",
                    subtitle = "Allows direct background looping, Sizing, and speed controls.",
                    checked = videoShowMenu,
                    onCheckedChange = onVideoShowMenuChange
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "YOUTUBE PLAYER EXPERIENCE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF43F5E),
            letterSpacing = 1.sp
        )

        val options = listOf("Standard Ad-Free", "PiP Player Mode", "Strict Privacy Proxy", "Premium Player Engine")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = videoYoutubeOption == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVideoYoutubeOptionChange(option) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onVideoYoutubeOptionChange(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF43F5E))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyGuardSubScreen(
    alwaysUseHttps: Boolean,
    onAlwaysUseHttpsChange: (Boolean) -> Unit,
    removeFingerprint: Boolean,
    onRemoveFingerprintChange: (Boolean) -> Unit,
    scriptControlEnabled: Boolean,
    onScriptControlChange: (Boolean) -> Unit,
    cookieManagementMode: String,
    onCookieManagementChange: (String) -> Unit,
    stopAppRedirects: Boolean,
    onStopAppRedirectsChange: (Boolean) -> Unit,
    safeBrowsingEnabled: Boolean,
    onSafeBrowsingChange: (Boolean) -> Unit,
    doNotTrack: Boolean,
    onDoNotTrackChange: (Boolean) -> Unit,
    autoDeAmp: Boolean,
    onAutoDeAmpChange: (Boolean) -> Unit,
    globalPrivacyControl: Boolean,
    onGlobalPrivacyControlChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BROWSER PROTECTION GUARD",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF34D399),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Always Use HTTPS",
                    subtitle = "Force upgrades connections to secure HTTPS and alerts on non-secure sites.",
                    checked = alwaysUseHttps,
                    onCheckedChange = onAlwaysUseHttpsChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Anti-Fingerprint Protection",
                    subtitle = "Rotates common safe user-agent configurations to prevent tracker canvas fingerprinters.",
                    checked = removeFingerprint,
                    onCheckedChange = onRemoveFingerprintChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Force Script Interception",
                    subtitle = "Inspects and cleans page-scripts. Disable to disable Javascript execution completely.",
                    checked = scriptControlEnabled,
                    onCheckedChange = onScriptControlChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Block App Store Redirections",
                    subtitle = "Prevents annoying automatic redirects to third-party stores like Play Store.",
                    checked = stopAppRedirects,
                    onCheckedChange = onStopAppRedirectsChange
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "COOKIE MANAGEMENT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF34D399),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf(
                    "allow" to "Allow All Cookies",
                    "block_all" to "Block All Cookies (May break websites)",
                    "block_third_party" to "Block Third-Party Trackers & Cookies"
                )
                modes.forEach { (modeVal, modeTitle) ->
                    val isSelected = cookieManagementMode == modeVal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCookieManagementChange(modeVal) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onCookieManagementChange(modeVal) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = modeTitle,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "ADVANCED ENHANCED TRACKING PROTECTION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF34D399),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Enable Safe Browsing Engine",
                    subtitle = "Examines loaded elements with local database of known phishers.",
                    checked = safeBrowsingEnabled,
                    onCheckedChange = onSafeBrowsingChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Send 'Do Not Track' Header",
                    subtitle = "Includes DNT request signals on all outgoing packages.",
                    checked = doNotTrack,
                    onCheckedChange = onDoNotTrackChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Auto De-AMP Redirection",
                    subtitle = "Decentralizes and strips Google AMP routing pages to native canonical URL structures.",
                    checked = autoDeAmp,
                    onCheckedChange = onAutoDeAmpChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Global Privacy Control (GPC)",
                    subtitle = "Broadcasting GPC signals notifying sites that you opt-out of personal data sharing.",
                    checked = globalPrivacyControl,
                    onCheckedChange = onGlobalPrivacyControlChange
                )
            }
        }
    }
}

@Composable
fun DnsRoutingSubScreen(
    dnsEnabled: Boolean,
    dnsMode: String,
    dnsPresetId: String,
    dnsCustomValue: String,
    onDnsEnabledChange: (Boolean) -> Unit,
    onDnsModeChange: (String) -> Unit,
    onDnsPresetIdChange: (String) -> Unit,
    onDnsCustomValueChange: (String) -> Unit,
    smartAutoRouting: Boolean,
    onSmartAutoRoutingChange: (Boolean) -> Unit,
    smartProxyRotator: Boolean,
    onSmartProxyRotatorChange: (Boolean) -> Unit,
    smartTorActive: Boolean,
    onSmartTorActiveChange: (Boolean) -> Unit,
    activeRoutingStatus: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SMART ROUTING & TOR BRIDGE ENGINE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA78BFA),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Enable Smart Auto-Routing",
                    subtitle = "Detects blocked sites and automatically switches to Tor or rotating fallback proxies.",
                    checked = smartAutoRouting,
                    onCheckedChange = onSmartAutoRoutingChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Fallback Proxy Rotator Engine",
                    subtitle = "Rotates secure public SOCKS/HTTP proxies automatically if a target server fails to respond.",
                    checked = smartProxyRotator,
                    onCheckedChange = onSmartProxyRotatorChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Tor Gateway Bridge (Automatic)",
                    subtitle = "Directly resolves and bridges .onion addresses using secure distributed Tor gateway relays.",
                    checked = smartTorActive,
                    onCheckedChange = onSmartTorActiveChange
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1065)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Smart Core Status: $activeRoutingStatus",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "PRIVATE SECURE DNS SERVER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA78BFA),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Secure DNS Engine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Shield DNS queries using secure DoH endpoints.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = dnsEnabled,
                        onCheckedChange = onDnsEnabledChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFA78BFA))
                    )
                }

                if (dnsEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("preset" to "Presets", "custom" to "Custom DoH").forEach { (mKey, mTitle) ->
                            val isSelected = dnsMode == mKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onDnsModeChange(mKey) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                            }
                        }
                    }

                    if (dnsMode == "preset") {
                        Spacer(modifier = Modifier.height(6.dp))
                        com.example.data.DnsManager.presets.forEach { preset ->
                            val isSelected = dnsPresetId == preset.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSelected) Color(0xFF2E1065) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFA78BFA).copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDnsPresetIdChange(preset.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "IP: ${preset.fallbackIp}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFFA78BFA)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = dnsCustomValue,
                            onValueChange = onDnsCustomValueChange,
                            label = { Text("DNS Domain or DoH Query URL", color = Color.Gray) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceSettingsSubScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    webZoomLevel: Float,
    onWebZoomLevelChange: (Float) -> Unit,
    forceDarkWebpages: Boolean,
    onForceDarkWebpagesChange: (Boolean) -> Unit,
    webTextZoom: Float,
    onWebTextZoomChange: (Float) -> Unit,
    hideDistractingItems: Boolean,
    onHideDistractingItemsChange: (Boolean) -> Unit,
    onNavigateSub: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Customizations (Top priority)
        Text(
            text = "CUSTOM PREFERENCES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsItemRow(
                    title = "Customize Address Bar",
                    subtitle = "Set layout to Top or Bottom, auto-hide, swipe behaviours",
                    icon = Icons.Default.VerticalAlignBottom,
                    iconColor = Color(0xFF38BDF8),
                    onClick = { onNavigateSub("customize_address_bar") }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsItemRow(
                    title = "Customize Menu",
                    subtitle = "Toggle options visible in browser action menu",
                    icon = Icons.Default.MenuOpen,
                    iconColor = Color(0xFF10B981),
                    onClick = { onNavigateSub("customize_menu") }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsItemRow(
                    title = "Tabs & Start Page",
                    subtitle = "Configure iCloud tabs, world news, favorites, custom wallpaper",
                    icon = Icons.Default.Web,
                    iconColor = Color(0xFFEC4899),
                    onClick = { onNavigateSub("tabs_start_page") }
                )
            }
        }

        // Section: Theme Mode
        Text(
            text = "THEME MODE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Theme Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Switch between light, dark or follow your system settings.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("light" to "Light", "dark" to "Dark", "system" to "System")
                    modes.forEach { (modeId, modeName) ->
                        val isSelected = themeMode == modeId
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onThemeModeChange(modeId) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = modeName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Website & Font Sizing
        Text(
            text = "WEBSITE ZOOM & FONTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. Website Zoom Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Website Zoom Level",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${(webZoomLevel * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
                Text(
                    text = "Controls the default magnification scaling of webpages.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onWebZoomLevelChange(maxOf(0.5f, webZoomLevel - 0.1f)) }) {
                        Icon(Icons.Default.Remove, "Decrease Zoom", tint = Color.White)
                    }
                    Slider(
                        value = webZoomLevel,
                        onValueChange = { onWebZoomLevelChange(it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF38BDF8),
                            thumbColor = Color(0xFF38BDF8)
                        )
                    )
                    IconButton(onClick = { onWebZoomLevelChange(minOf(2.0f, webZoomLevel + 0.1f)) }) {
                        Icon(Icons.Default.Add, "Increase Zoom", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // 2. Text Scaling Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Font Scale (Text Zoom)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${(webTextZoom * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
                Text(
                    text = "Scales the font sizes of text on pages for readability.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onWebTextZoomChange(maxOf(0.5f, webTextZoom - 0.1f)) }) {
                        Icon(Icons.Default.Remove, "Decrease Text Size", tint = Color.White)
                    }
                    Slider(
                        value = webTextZoom,
                        onValueChange = { onWebTextZoomChange(it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF38BDF8),
                            thumbColor = Color(0xFF38BDF8)
                        )
                    )
                    IconButton(onClick = { onWebTextZoomChange(minOf(2.0f, webTextZoom + 0.1f)) }) {
                        Icon(Icons.Default.Add, "Increase Text Size", tint = Color.White)
                    }
                }
            }
        }

        // Section: Accessibility Controls
        Text(
            text = "ACCESSIBILITY & CLEANING",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Force Dark Mode",
                    subtitle = "Automatically applies a beautiful eye-safe dark theme to light webpages using advanced styling injection.",
                    checked = forceDarkWebpages,
                    onCheckedChange = onForceDarkWebpagesChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Hide Distracting Items",
                    subtitle = "Instantly clears clutters, newsletters, comments, social bars and banners for a polished focused reading.",
                    checked = hideDistractingItems,
                    onCheckedChange = onHideDistractingItemsChange
                )
            }
        }
    }
}

@Composable
fun CustomizeAddressBarSubScreen(
    addressBarPosition: String,
    onAddressBarPositionChange: (String) -> Unit,
    autoHideBar: Boolean,
    onAutoHideBarChange: (Boolean) -> Unit,
    swipeForFullscreen: Boolean,
    onSwipeForFullscreenChange: (Boolean) -> Unit,
    swipeToViewTabs: Boolean,
    onSwipeToViewTabsChange: (Boolean) -> Unit,
    showFullUrl: Boolean,
    onShowFullUrlChange: (Boolean) -> Unit,
    hideBottomToolbar: Boolean = false,
    onHideBottomToolbarChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ADDRESS BAR POSITION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar Card Option
            val isTop = addressBarPosition == "top"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAddressBarPositionChange("top") }
                    .border(
                        width = 2.dp,
                        color = if (isTop) Color(0xFF38BDF8) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Visual Mockup of Top Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        // Top bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .align(Alignment.TopCenter)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Top Bar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Safari style", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Bottom Bar Card Option
            val isBottom = addressBarPosition == "bottom"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAddressBarPositionChange("bottom") }
                    .border(
                        width = 2.dp,
                        color = if (isBottom) Color(0xFF38BDF8) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Visual Mockup of Bottom Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        // Bottom bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .align(Alignment.BottomCenter)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Bottom Bar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Modern layout", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Text(
            text = "LAYOUT OPTIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Auto-Hide Bar on Scroll",
                    subtitle = "Automatically minimize and slide-out address bar during page scrolling.",
                    checked = autoHideBar,
                    onCheckedChange = onAutoHideBarChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Swipe for Fullscreen",
                    subtitle = "Swipe down to go fullscreen with Bottom Bar (or swipe up with Top Bar).",
                    checked = swipeForFullscreen,
                    onCheckedChange = onSwipeForFullscreenChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Swipe to View Tabs",
                    subtitle = "Swipe up from bottom bar (or down from top bar) to trigger the tab switcher.",
                    checked = swipeToViewTabs,
                    onCheckedChange = onSwipeToViewTabsChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Full Website URL",
                    subtitle = "Always display full absolute URL path rather than simplified domain name.",
                    checked = showFullUrl,
                    onCheckedChange = onShowFullUrlChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Hide Bottom Toolbar",
                    subtitle = "Hide the bottom bar and move its features to a 3-dots menu in the Address Bar.",
                    checked = hideBottomToolbar,
                    onCheckedChange = onHideBottomToolbarChange
                )
            }
        }
    }
}

@Composable
fun CustomizeMenuSubScreen(
    menuShowReader: Boolean,
    onMenuShowReaderChange: (Boolean) -> Unit,
    menuPageZoom: Boolean,
    onMenuPageZoomChange: (Boolean) -> Unit,
    menuFindOnPage: Boolean,
    onMenuFindOnPageChange: (Boolean) -> Unit,
    menuRequestDesktop: Boolean,
    onMenuRequestDesktopChange: (Boolean) -> Unit,
    menuAddToHome: Boolean,
    onMenuAddToHomeChange: (Boolean) -> Unit,
    menuDeveloperTools: Boolean,
    onMenuDeveloperToolsChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BROWSER ACTION MENU ITEMS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Show Reader Mode Toggle",
                    subtitle = "Enable simplified reading mode button inside the main menu overlay.",
                    checked = menuShowReader,
                    onCheckedChange = onMenuShowReaderChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Page Zoom Action",
                    subtitle = "Display direct controls for zoom in, out and reset on any webpage.",
                    checked = menuPageZoom,
                    onCheckedChange = onMenuPageZoomChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Find on Page Control",
                    subtitle = "Allow text content matching and quick highlighting inside pages.",
                    checked = menuFindOnPage,
                    onCheckedChange = onMenuFindOnPageChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Request Desktop Toggle",
                    subtitle = "Quick action to toggle desktop-class User-Agent string configuration.",
                    checked = menuRequestDesktop,
                    onCheckedChange = onMenuRequestDesktopChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Add to Home Screen Option",
                    subtitle = "Enable shortcuts installation to standard Android launcher desktop.",
                    checked = menuAddToHome,
                    onCheckedChange = onMenuAddToHomeChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Developer Console Tools",
                    subtitle = "Gain direct inspection of console log output, JS execution and DOM.",
                    checked = menuDeveloperTools,
                    onCheckedChange = onMenuDeveloperToolsChange
                )
            }
        }
    }
}

@Composable
fun TabsAndStartPageSubScreen(
    homeShowFavorites: Boolean,
    onHomeShowFavoritesChange: (Boolean) -> Unit,
    homeShowICloudTabs: Boolean,
    onHomeShowICloudTabsChange: (Boolean) -> Unit,
    homeShowNews: Boolean,
    onHomeShowNewsChange: (Boolean) -> Unit,
    quickTabStripVisible: Boolean = true,
    onQuickTabStripVisibleChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "START PAGE & TABS PREFERENCES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Show Tab Stacks Quick Strip",
                    subtitle = "Display the horizontal circular tab strip above the address bar.",
                    checked = quickTabStripVisible,
                    onCheckedChange = onQuickTabStripVisibleChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show Favorites Grid",
                    subtitle = "Display your saved website shortcuts on the start page.",
                    checked = homeShowFavorites,
                    onCheckedChange = onHomeShowFavoritesChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show iCloud Tabs Preview",
                    subtitle = "Access open tabs from your other Apple device concepts.",
                    checked = homeShowICloudTabs,
                    onCheckedChange = onHomeShowICloudTabsChange
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "Show World News Feed",
                    subtitle = "Stay updated with highly refined global topics right on home.",
                    checked = homeShowNews,
                    onCheckedChange = onHomeShowNewsChange
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White
            )
        )
    }
}

@Composable
fun UcPremiumVideoPlayer(
    videoUrl: String,
    title: String,
    onClose: () -> Unit,
    showSpeedMeter: Boolean,
    gestureControlsEnabled: Boolean,
    defaultSpeed: Float,
    capturedMedia: List<CapturedMedia>,
    onPlayOtherVideo: (CapturedMedia) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableStateOf(defaultSpeed) }
    var isLocked by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.35f) } // Seek bar slider position
    var showSpeedDialog by remember { mutableStateOf(false) }
    
    // Live bandwidth speed simulation
    var liveSpeedMbps by remember { mutableStateOf(3.39) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1200)
            liveSpeedMbps = 2.4 + (Math.random() * 2.2)
        }
    }

    // Progress bar simulation when video plays
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                progress = (progress + 0.003f).coerceAtMost(1f)
            }
        }
    }

    // Mock timings calculated out of a 5-minute video stream
    val totalSeconds = 300 
    val currentSeconds = (progress * totalSeconds).toInt()
    val currentStr = String.format("%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val totalStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.98f))
            .clickable(enabled = isLocked) {
                isLocked = false
            },
        contentAlignment = Alignment.Center
    ) {
        // Core Video Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            var mPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            // Standard Android VideoView wrapper for real URL playing
            if (videoUrl.isNotBlank() && videoUrl.startsWith("http")) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            setVideoPath(videoUrl)
                            setOnPreparedListener { mp ->
                                mPlayer = mp
                                mp.isLooping = true
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        mp.playbackParams = mp.playbackParams.setSpeed(currentSpeed)
                                    }
                                } catch (e: Exception) {}
                                if (isPlaying) start() else pause()
                            }
                        }
                    },
                    update = { view ->
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                mPlayer?.let { mp ->
                                    mp.playbackParams = mp.playbackParams.setSpeed(currentSpeed)
                                }
                            }
                        } catch (e: Exception) {}
                        if (isPlaying) view.start() else view.pause()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Interactive Visual Waveform Fallback Mode if video stream offline
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFE11D48).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Visualizer",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "UC Hardware Acceleration Mode Active",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Double Tap Sides to Seek 10s • Locked State Prevents Taps",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Lock Shield HUD (When control interaction is locked)
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock Controls",
                            tint = Color(0xFFE11D48)
                        )
                    }
                }
            }
        }

        // FULL INTERACTION CONTROL HUD (Hidden when locked)
        if (!isLocked) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifBlank { "Online Stream Web Video" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "UC Premium Player",
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    if (showSpeedMeter) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.60f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = String.format("%.2f Mb/s", liveSpeedMbps),
                                color = Color(0xFF10B981), // Emerald 500
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(onClick = { isLocked = true }) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock controls",
                            tint = Color.White
                        )
                    }
                }
            }

            // Gesture Double-Tap targets & Center play overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s Gesture Region
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (gestureControlsEnabled) {
                                progress = (progress - 0.04f).coerceAtLeast(0f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Empty region to intercept left taps
                }

                // Center Play/Pause Circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Forward 10s Gesture Region
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (gestureControlsEnabled) {
                                progress = (progress + 0.04f).coerceAtMost(1f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Empty region to intercept right taps
                }
            }

            // Bottom Navigation, Seekbar, playlist slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // playlist slider of detected videos
                val otherVideos = capturedMedia.filter { it.type == "video" }
                if (otherVideos.size > 1) {
                    Text(
                        text = "OTHER CAPTURED STREAMS ON THIS WEB PAGE",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(otherVideos) { media ->
                            val isActive = media.url == videoUrl
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { onPlayOtherVideo(media) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) Color(0xFFE11D48) else Color(0xFF1E293B)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = media.pageTitle,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Source Stream",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Seekbar & Timing Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentStr,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFFF97316), // Orange
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            thumbColor = Color(0xFFF97316)
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = totalStr,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Speed Badge clicker
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { showSpeedDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format("%.2fX", currentSpeed),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Simulated Download / Sync Option
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Save video offline",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set Web Speed Multiplier",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    val multipliers = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
                    multipliers.forEach { ml ->
                        val isSelected = currentSpeed == ml
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFE11D48) else Color.Transparent)
                                .clickable {
                                    currentSpeed = ml
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ml}x Playback",
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, "Selected", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickTabStrip(
    tabs: List<BrowserTab>,
    activeTabId: Long?,
    onTabSelected: (BrowserTab) -> Unit,
    onTabClosed: (Long) -> Unit,
    onAddTab: (String?) -> Unit,
    onManageGroups: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = remember(tabs, activeTabId) { tabs.find { it.id == activeTabId } }
    val activeGroupName = activeTab?.groupName

    // Filter tabs to current group, if grouped. If not grouped, we show all ungrouped tabs.
    val filteredTabs = remember(tabs, activeGroupName) {
        if (activeGroupName != null) {
            tabs.filter { it.groupName == activeGroupName }
        } else {
            tabs.filter { it.groupName == null }
        }
    }

    // Gentle pulsing animation for the active tab's glowing ring/indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth(0.96f)
            .widthIn(max = 430.dp)
            .height(58.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.98f),
                        Color(0xFFF8FAFC).copy(alpha = 0.96f)
                    )
                ),
                RoundedCornerShape(100.dp)
            )
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE2E8F0),
                            Color(0xFFCBD5E1).copy(alpha = 0.5f)
                        )
                    )
                ),
                RoundedCornerShape(100.dp)
            )
            .shadow(12.dp, RoundedCornerShape(100.dp), spotColor = Color(0xFF0F172A).copy(alpha = 0.15f))
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Action: Stacks Manage Icon
        Box(contentAlignment = Alignment.Center) {
            IconButton(
                onClick = onManageGroups,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (activeGroupName != null) Color(0xFFE2E8F0) else Color(0xFFF1F5F9).copy(alpha = 0.8f),
                        CircleShape
                    )
                    .border(
                        BorderStroke(1.dp, if (activeGroupName != null) Color(0xFFCBD5E1) else Color.Transparent),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (activeGroupName != null) Icons.Default.FolderOpen else Icons.Default.Layers,
                    contentDescription = "Tab Groups",
                    tint = if (activeGroupName != null) Color(0xFF0F172A) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Active Group Name/Badge if present
        if (activeGroupName != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF10B981).copy(alpha = 0.15f))
                        )
                    )
                    .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)), RoundedCornerShape(100.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                    )
                    Text(
                        text = activeGroupName,
                        color = Color(0xFF1E293B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 55.dp)
                    )
                }
            }
        }

        // Horizontal Row of Tab Circles (Favicons/logos/letters)
        LazyRow(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(filteredTabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTabId

                // Elastic spring animations for tab size, translation, and padding
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 0.90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "tabScale"
                )

                val translationY by animateFloatAsState(
                    targetValue = if (isSelected) -4f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "tabTranslation"
                )

                val closeButtonScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "closeScale"
                )

                // Define clean domain name
                val cleanDomain = remember(tab.url) {
                    try {
                        var temp = tab.url.trim()
                        if (temp.startsWith("http://")) temp = temp.substring(7)
                        else if (temp.startsWith("https://")) temp = temp.substring(8)
                        if (temp.startsWith("www.")) temp = temp.substring(4)
                        val slashIndex = temp.indexOf('/')
                        val domain = if (slashIndex != -1) temp.substring(0, slashIndex) else temp
                        if (domain.isBlank() || domain.contains("localhost") || !domain.contains(".")) "" else domain
                    } catch (e: Exception) {
                        ""
                    }
                }

                val faviconUrl = remember(cleanDomain) {
                    if (cleanDomain.isNotEmpty()) {
                        "https://www.google.com/s2/favicons?sz=128&domain=$cleanDomain"
                    } else {
                        null
                    }
                }

                val isDineInStyle = remember(tab.url) { tab.url.contains("dineinstyle.com") }

                // Aesthetic color theme based on URL/Title
                val tabColor = remember(tab.title, tab.url) {
                    val colors = listOf(
                        Color(0xFF3B82F6), // Blue
                        Color(0xFF10B981), // Green
                        Color(0xFFF59E0B), // Amber
                        Color(0xFF8B5CF6), // Purple
                        Color(0xFFEC4899), // Pink
                        Color(0xFF06B6D4), // Cyan
                        Color(0xFFF97316), // Orange
                    )
                    val titleHash = tab.title.hashCode().coerceAtLeast(0)
                    colors[titleHash % colors.size]
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.translationY = translationY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing outer border for selected tab
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                Color(0xFF3B82F6).copy(alpha = pulseGlowAlpha),
                                                Color(0xFFEC4899).copy(alpha = pulseGlowAlpha),
                                                Color(0xFF3B82F6).copy(alpha = pulseGlowAlpha)
                                            )
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                    }

                    // Main circular tab button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    if (isDineInStyle) {
                                        Brush.linearGradient(colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                                    } else {
                                        Brush.linearGradient(colors = listOf(tabColor, tabColor.copy(alpha = 0.8f)))
                                    }
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            tabColor.copy(alpha = 0.12f),
                                            tabColor.copy(alpha = 0.05f)
                                        )
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color.White else tabColor.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDineInStyle) {
                            // High-end custom gourmet brand icon for dineinstyle.com
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = "Dine In Style Logo",
                                tint = if (isSelected) Color.White else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (faviconUrl != null) {
                            // Dynamic real website logo loaded securely from Google Favicon service via Coil
                            val painter = rememberAsyncImagePainter(model = faviconUrl)
                            val painterState = painter.state
                            val isLoadingOrError = painterState is AsyncImagePainter.State.Loading ||
                                    painterState is AsyncImagePainter.State.Error

                            if (isLoadingOrError) {
                                // Dynamic crisp letter fallback if image is loading or offline
                                Text(
                                    text = tab.title.take(1).uppercase(),
                                    color = if (isSelected) Color.White else tabColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Image(
                                    painter = painter,
                                    contentDescription = "Website logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                )
                            }
                        } else {
                            // Fallback icons depending on content
                            val isHome = tab.url.isBlank() || tab.url == "about:blank" || tab.title.contains("Home", ignoreCase = true)
                            if (isHome) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = if (isSelected) Color.White else tabColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = tab.title.take(1).uppercase(),
                                    color = if (isSelected) Color.White else tabColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Close tab (X) badge hovering beautifully on top right with bouncy scale animation
                    if (closeButtonScale > 0.01f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .graphicsLayer {
                                    this.scaleX = closeButtonScale
                                    this.scaleY = closeButtonScale
                                }
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable { onTabClosed(tab.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = Color.White,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }

                    // Breathing dot indicator right beneath the active tab
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 5.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }
            }
        }

        // Right Action: Add Tab inside current group/view
        IconButton(
            onClick = { onAddTab(activeGroupName) },
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                    ),
                    CircleShape
                )
                .shadow(2.dp, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add tab to group",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TabGroupManagerDialog(
    tabs: List<BrowserTab>,
    activeTabId: Long?,
    onSetGroup: (Long, String?) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newGroupNameInput by remember { mutableStateOf("") }
    
    // Group tabs by groupName
    val groupedTabs = remember(tabs) {
        tabs.groupBy { it.groupName ?: "Ungrouped" }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tab Stacks & Groups",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Organize your tabs into neat stacks",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Create new group input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newGroupNameInput,
                        onValueChange = { newGroupNameInput = it },
                        placeholder = { Text("Group Name (e.g. Work, Social)", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F172A),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    Button(
                        onClick = {
                            if (newGroupNameInput.isNotBlank() && activeTabId != null) {
                                onSetGroup(activeTabId, newGroupNameInput.trim())
                                newGroupNameInput = ""
                            }
                        },
                        enabled = newGroupNameInput.isNotBlank() && activeTabId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Stack", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // List of current Groups & their tabs
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedTabs.forEach { (groupName, groupTabs) ->
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (groupName == "Ungrouped") Color.Gray else Color(0xFF3B82F6)
                                                    )
                                            )
                                            Text(
                                                text = groupName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "(${groupTabs.size} tabs)",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        if (groupName != "Ungrouped") {
                                            IconButton(
                                                onClick = { onDeleteGroup(groupName) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    "Delete Group",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Display tabs inside this group
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        groupTabs.forEach { tab ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (tab.id == activeTabId) Color(0xFFF1F5F9) else Color.Transparent)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (tab.url == "dineinstyle.com") "Dine in Style Home" else tab.title,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF334155),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                if (groupName != "Ungrouped") {
                                                    // Button to Ungroup this tab
                                                    Text(
                                                        text = "Ungroup",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF64748B),
                                                        modifier = Modifier
                                                            .clickable { onSetGroup(tab.id, null) }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                } else {
                                                    // Button to add to active stack
                                                    Text(
                                                        text = "Add to Group",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF3B82F6),
                                                        modifier = Modifier
                                                            .clickable { 
                                                                // Find first non-ungrouped group name, or "General"
                                                                val firstGroup = groupedTabs.keys.firstOrNull { it != "Ungrouped" } ?: "General"
                                                                onSetGroup(tab.id, firstGroup)
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getFirstLetterFromUrl(url: String): String {
    return try {
        val uri = java.net.URI(url)
        val host = uri.host ?: ""
        val domain = if (host.startsWith("www.")) host.substring(4) else host
        domain.firstOrNull()?.uppercase()?.toString() ?: "W"
    } catch (e: Exception) {
        "W"
    }
}

@Composable
fun PreviewPageDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Page Preview",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = url,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }
                
                // WebView hosting the previewed URL
                Box(modifier = Modifier.weight(1f)) {
                    val context = LocalContext.current
                    val webView = remember {
                        android.webkit.WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            webViewClient = android.webkit.WebViewClient()
                        }
                    }
                    LaunchedEffect(url) {
                        webView.loadUrl(url)
                    }
                    AndroidView(
                        factory = { webView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun LinkContextMenuDialog(
    url: String,
    text: String,
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cleanTitle = text.ifEmpty { 
        try {
            val uri = java.net.URI(url)
            val host = uri.host ?: ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            url
        }
    }

    var showPreview by remember { mutableStateOf(false) }

    if (showPreview) {
        PreviewPageDialog(url = url, onDismiss = { 
            showPreview = false
            onDismiss()
        })
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(24.dp, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circle Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getFirstLetterFromUrl(url),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = url,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Options list
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinkContextMenuItem(
                            icon = Icons.Default.Add,
                            title = "Open in new tab",
                            onClick = {
                                viewModel.addTab(url)
                                onDismiss()
                            }
                        )
                        LinkContextMenuItem(
                            icon = Icons.Default.FolderOpen,
                            title = "Open in new tab in group",
                            onClick = {
                                viewModel.openLinkInNewTabInGroup(url)
                                onDismiss()
                            }
                        )
                        LinkContextMenuItem(
                            icon = Icons.Default.VisibilityOff,
                            title = "Open in New Private Tab",
                            onClick = {
                                viewModel.addTab(url)
                                Toast.makeText(context, "Opened in Private Tab", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                        LinkContextMenuItem(
                            icon = Icons.Default.OpenInNew,
                            title = "Preview page",
                            onClick = {
                                showPreview = true
                            }
                        )
                        LinkContextMenuItem(
                            icon = Icons.Default.Link,
                            title = "Copy Link",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied Link", url)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                        LinkContextMenuItem(
                            icon = Icons.Default.ContentCopy,
                            title = "Copy Text",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied Text", cleanTitle)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                        LinkContextMenuItem(
                            icon = Icons.Default.Download,
                            title = "Download link",
                            onClick = {
                                try {
                                    val request = DownloadManager.Request(Uri.parse(url))
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    downloadManager.enqueue(request)
                                    Toast.makeText(context, "Downloading file...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to start download", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        )
                        LinkContextMenuItem(
                            icon = Icons.Default.Share,
                            title = "Share...",
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, url)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LinkContextMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

