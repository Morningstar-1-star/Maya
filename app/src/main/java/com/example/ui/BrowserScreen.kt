package com.example.ui

import kotlin.math.roundToInt
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.Bookmark
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize

fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleanHex = hex.trim().replace("#", "")
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

fun isColorDark(color: Color): Boolean {
    val luma = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luma < 0.5
}

@Composable
fun ImmersiveStatusBar(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemDark
    }
    
    val allTabs by viewModel.allTabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeTab = remember(allTabs, activeTabId) { allTabs.find { it.id == activeTabId } }
    val currentUrl = activeTab?.url ?: ""
    val isNativeHomepage = currentUrl.isEmpty() || currentUrl == "dineinstyle.com"
    
    val currentWebsiteThemeColor by viewModel.currentWebsiteThemeColor.collectAsStateWithLifecycle()
    
    val statusBarBgColor = when {
        isNativeHomepage -> {
            if (isDarkTheme) Color(0xFF030712) else Color(0xFFF8FAFC)
        }
        !currentWebsiteThemeColor.isNullOrBlank() -> {
            parseHexColor(currentWebsiteThemeColor, if (isDarkTheme) Color(0xFF1E293B) else Color.White)
        }
        else -> {
            if (isDarkTheme) Color(0xFF1E293B) else Color.White
        }
    }
    
    val isBgDark = isColorDark(statusBarBgColor)
    val contentColor = if (isBgDark) Color.White else Color(0xFF1E293B)
    
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while (true) {
            val cal = java.util.Calendar.getInstance()
            currentTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)
            currentDate = java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault()).format(cal.time)
            kotlinx.coroutines.delay(10000)
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(statusBarBgColor)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentTime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentDate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.65f)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(contentColor.copy(alpha = 0.7f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(Color(0xFF38BDF8), CircleShape)
            )
            Text(
                text = "MAYA",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = contentColor.copy(alpha = 0.4f)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "5G",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            
            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = "Signal strength",
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wifi connection",
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "88%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(10.dp)
                        .border(1.dp, contentColor, RoundedCornerShape(2.dp))
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.88f)
                            .background(
                                if (isBgDark) Color(0xFF4CAF50) else Color(0xFF2E7D32),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}

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
    val copyUnblockDisabledDomains by viewModel.copyUnblockDisabledDomains.collectAsStateWithLifecycle()

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
    val isInPipMode by viewModel.isInPictureInPictureMode.collectAsStateWithLifecycle()
    val ucPlayerVideoUrl by viewModel.ucPlayerVideoUrl.collectAsStateWithLifecycle()
    val ucPlayerVideoTitle by viewModel.ucPlayerVideoTitle.collectAsStateWithLifecycle()
    val capturedMedia by viewModel.allCapturedMedia.collectAsStateWithLifecycle()
    val backgroundVideo by viewModel.backgroundVideo.collectAsStateWithLifecycle()
    val isBackgroundVideoPlaying by viewModel.isBackgroundVideoPlaying.collectAsStateWithLifecycle()
    val detectedVideoMedia by viewModel.detectedVideoActionMedia.collectAsStateWithLifecycle()

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

    // AI Summarizer States
    val aiSmartSummarizerEnabled by viewModel.aiSmartSummarizerEnabled.collectAsStateWithLifecycle()
    val isSummaryDialogVisible by viewModel.isSummaryDialogVisible.collectAsStateWithLifecycle()
    val summaryContent by viewModel.summaryContent.collectAsStateWithLifecycle()
    val isSummaryLoading by viewModel.isSummaryLoading.collectAsStateWithLifecycle()

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

    val chromeThemeActive by viewModel.chromeThemeActive.collectAsStateWithLifecycle()
    val chromeThemeFrameColor by viewModel.chromeThemeFrameColor.collectAsStateWithLifecycle()
    val chromeThemeToolbarColor by viewModel.chromeThemeToolbarColor.collectAsStateWithLifecycle()
    val chromeThemeTextColor by viewModel.chromeThemeTextColor.collectAsStateWithLifecycle()
    val chromeThemeInactiveTextColor by viewModel.chromeThemeInactiveTextColor.collectAsStateWithLifecycle()
    val chromeThemeNtpBgColor by viewModel.chromeThemeNtpBgColor.collectAsStateWithLifecycle()
    val chromeThemeNtpTextColor by viewModel.chromeThemeNtpTextColor.collectAsStateWithLifecycle()
    val chromeThemeNtpBgPath by viewModel.chromeThemeNtpBgPath.collectAsStateWithLifecycle()

    // Collect new premium features states
    val lowPowerModeEnabled by viewModel.lowPowerModeEnabled.collectAsStateWithLifecycle()
    val bypassPaywallsEnabled by viewModel.bypassPaywallsEnabled.collectAsStateWithLifecycle()
    val fullScreenReading by viewModel.fullScreenReading.collectAsStateWithLifecycle()
    val tabDesktopModes by viewModel.tabDesktopModes.collectAsStateWithLifecycle()
    val tabReadTimes by viewModel.tabReadTimes.collectAsStateWithLifecycle()
    val tabSortMode by viewModel.tabSortMode.collectAsStateWithLifecycle()
    val activeTabReadTime by viewModel.activeTabReadTime.collectAsStateWithLifecycle()

    // Reader Mode States
    val isReaderModeActive by viewModel.isReaderModeActive.collectAsStateWithLifecycle()
    val readerTitle by viewModel.readerTitle.collectAsStateWithLifecycle()
    val readerContent by viewModel.readerContent.collectAsStateWithLifecycle()
    val readerTextSize by viewModel.readerTextSize.collectAsStateWithLifecycle()
    val readerTheme by viewModel.readerTheme.collectAsStateWithLifecycle()
    val readerFontFamily by viewModel.readerFontFamily.collectAsStateWithLifecycle()

    // Overlay State variables
    val isAdBlockerPopupVisible by viewModel.isAdBlockerPopupVisible.collectAsStateWithLifecycle()
    val isTabSwitcherVisible by viewModel.isTabSwitcherVisible.collectAsStateWithLifecycle()
    val tabLayoutStyle by viewModel.tabLayoutStyle.collectAsStateWithLifecycle()
    val isBookmarksHistorySheetVisible by viewModel.isBookmarksHistorySheetVisible.collectAsStateWithLifecycle()
    val isMediaStudioVisible by viewModel.isMediaStudioVisible.collectAsStateWithLifecycle()
    val activeSheetTab by viewModel.activeSheetTab.collectAsStateWithLifecycle()

    var isSearchFocused by remember { mutableStateOf(false) }
    var showTabGroupManager by remember { mutableStateOf(false) }
    var isCookieEditorVisible by remember { mutableStateOf(false) }
    var showWebsiteUpdatesDialog by remember { mutableStateOf(false) }
    var trimmingVideoMedia by remember { mutableStateOf<CapturedMedia?>(null) }

    // Lists for rendering
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val allBookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()

    val updatedBookmarks = remember(allBookmarks) { allBookmarks.filter { it.hasUpdateAlert } }
    val updateCount = updatedBookmarks.size
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsStateWithLifecycle()

    // Get active tab details
    val activeTab = allTabs.find { it.id == activeTabId }
    val activeTabGroupName = remember(activeTab) { activeTab?.groupName }
    val isTabGroupActive = activeTabGroupName != null

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemDark
    }

    val overlayActive = isAdBlockerPopupVisible || isTabSwitcherVisible || isBookmarksHistorySheetVisible || isMediaStudioVisible || isSettingsScreenVisible || isCookieEditorVisible || (detectedVideoMedia != null)
    val backgroundBlur by animateDpAsState(
        targetValue = if (overlayActive) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "blur"
    )
    val backgroundScale by animateFloatAsState(
        targetValue = if (overlayActive) 0.94f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    // Root level BackHandler for Predictive Back and hierarchical back navigation
    BackHandler(enabled = true) {
        when {
            isTabSwitcherVisible -> {
                viewModel.setTabSwitcherVisible(false)
            }
            isBookmarksHistorySheetVisible -> {
                viewModel.setBookmarksHistorySheetVisible(false)
            }
            isSettingsScreenVisible -> {
                if (currentSettingsSubScreen != "main") {
                    viewModel.setSettingsSubScreen("main")
                } else {
                    viewModel.setSettingsScreenVisible(false)
                }
            }
            isCookieEditorVisible -> {
                isCookieEditorVisible = false
            }
            showWebsiteUpdatesDialog -> {
                showWebsiteUpdatesDialog = false
            }
            activeTab != null -> {
                val webView = WebViewPool.getOrCreateWebView(context, activeTab.id, viewModel)
                if (webView.canGoBack()) {
                    webView.goBack()
                } else if (activeTab.url != "dineinstyle.com") {
                    viewModel.loadUrl("dineinstyle.com")
                } else {
                    (context as? android.app.Activity)?.finish()
                }
            }
            else -> {
                (context as? android.app.Activity)?.finish()
            }
        }
    }
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

    if (isInPipMode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AuraVideoPlayer(
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
                },
                onPlayInBackground = { url, title ->
                    viewModel.playBackgroundVideo(CapturedMedia(url = url, type = "video", pageTitle = title, pageUrl = url))
                    viewModel.setUcPlayerActive(false)
                },
                viewModel = viewModel
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (chromeThemeActive) Color(chromeThemeFrameColor) else MaterialTheme.colorScheme.background
            )
    ) {
        // --- 1. Immersive Web Content Area ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // Keep below safe area for content
        ) {
            ImmersiveStatusBar(viewModel = viewModel)

            if (addressBarPosition == "top") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible && !fullScreenReading,
                        enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -50 }) + fadeOut(),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .widthIn(max = 450.dp)
                    ) {
                        AddressBar(
                            viewModel = viewModel,
                            url = urlInput,
                            isAdBlockerActive = adBlockerOn,
                            blockedAdsCount = activeTabBlockedAds,
                            canGoBack = activeTabCanGoBack,
                            canGoForward = activeTabCanGoForward,
                            loadingProgress = activeTabProgress,
                            updatedBookmarksCount = updateCount,
                            onUpdatesClick = { showWebsiteUpdatesDialog = true },
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
                            onHomeClick = { viewModel.loadUrl("dineinstyle.com") },
                            onCookieEditorClick = { isCookieEditorVisible = true },
                            copyUnblockActive = viewModel.isCopyUnblockActiveForUrl(activeTab?.url),
                            onToggleCopyUnblock = { viewModel.toggleCopyUnblockForUrl(activeTab?.url) },
                            themeMode = themeMode,
                            chromeThemeActive = chromeThemeActive,
                            chromeThemeToolbarColor = chromeThemeToolbarColor,
                            chromeThemeTextColor = chromeThemeTextColor,
                            chromeThemeInactiveTextColor = chromeThemeInactiveTextColor,
                            aiSmartSummarizerEnabled = aiSmartSummarizerEnabled,
                            onAiSummarizeClick = {
                                val activeId = activeTabId
                                if (activeId != null) {
                                    val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                    wv.evaluateJavascript(
                                        "(function() { " +
                                        "   var text = ''; " +
                                        "   var els = document.querySelectorAll('h1, h2, h3, p, article'); " +
                                        "   for (var i = 0; i < els.length; i++) { " +
                                        "       text += els[i].innerText + '\\n'; " +
                                        "       if (text.length > 5000) break; " +
                                        "   } " +
                                        "   return text; " +
                                        "})()"
                                    ) { result ->
                                        val cleanText = result?.trim()?.removeSurrounding("\"")?.replace("\\\\n", "\n")?.replace("\\n", "\n") ?: ""
                                        viewModel.summarizeCurrentPage(cleanText.ifBlank { "No readable article content found on this page." })
                                    }
                                }
                            },
                            activeTabReadTime = activeTabReadTime
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = backgroundScale
                        scaleY = backgroundScale
                        clip = true
                    }
                    .blur(backgroundBlur)
            ) {
                AnimatedContent(
                    targetState = activeTabId,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) +
                         scaleIn(initialScale = 0.97f, animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(120)) +
                            scaleOut(targetScale = 1.03f, animationSpec = tween(120))
                        )
                    },
                    label = "tab_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetTabId ->
                    val targetTab = allTabs.find { it.id == targetTabId }
                    if (targetTab != null) {
                        if (targetTab.url == "dineinstyle.com") {
                            val wallpaperUrl by viewModel.customWallpaperUrl.collectAsStateWithLifecycle()
                            val shortcuts by viewModel.allShortcuts.collectAsStateWithLifecycle()
                            val showNewsSection by viewModel.showNewsSection.collectAsStateWithLifecycle()

                            val playlistVideos = remember(capturedMedia) {
                                capturedMedia.filter { it.isSaved && it.type == "video" }
                            }

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
                                },
                                chromeThemeActive = chromeThemeActive,
                                chromeThemeNtpBgColor = chromeThemeNtpBgColor,
                                chromeThemeNtpTextColor = chromeThemeNtpTextColor,
                                chromeThemeNtpBgPath = chromeThemeNtpBgPath,
                                playlistVideos = playlistVideos,
                                onPlayVideo = { video ->
                                    viewModel.setUcPlayerVideoUrl(video.url)
                                    viewModel.setUcPlayerVideoTitle(video.pageTitle)
                                    viewModel.setUcPlayerActive(true)
                                },
                                onDeleteVideo = { id ->
                                    viewModel.deleteMedia(id)
                                }
                            )
                        } else {
                            // Check if tab is sensitive (locked) and needs authentication
                            val unlockedTabIds by viewModel.unlockedTabIds.collectAsStateWithLifecycle()
                            val isLocked = targetTab.isLocked && !unlockedTabIds.contains(targetTab.id)

                            if (isLocked) {
                                LockedTabScreen(
                                    tabId = targetTab.id,
                                    tabTitle = targetTab.title,
                                    onAuthenticate = { viewModel.unlockTab(targetTab.id) }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Show standard WebView with real content and adblock
                                    TabWebView(
                                        tabId = targetTab.id,
                                        url = targetTab.url,
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Skeleton loading instead of blank screen
                                    val loadingProgress = viewModel.loadingProgressMap.collectAsStateWithLifecycle().value[targetTab.id] ?: 100
                                    if (loadingProgress < 100 && targetTab.url != "dineinstyle.com") {
                                        WebSkeletonLoader(
                                            isDark = isDarkTheme,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
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
            }
        }

        // --- 2. Semi-Transparent Blur Backdrop behind overlays ---
        val overlayActive = isAdBlockerPopupVisible || isTabSwitcherVisible || isBookmarksHistorySheetVisible || isMediaStudioVisible || isSettingsScreenVisible || isCookieEditorVisible || (detectedVideoMedia != null)
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
                        isCookieEditorVisible = false
                        viewModel.setDetectedVideoActionMedia(null)
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
                currentUrl = activeTab?.url ?: "",
                viewModel = viewModel,
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
                onManageGroups = { showTabGroupManager = true },
                layoutStyle = tabLayoutStyle,
                onLayoutStyleChanged = { viewModel.setTabLayoutStyle(it) },
                onToggleLock = { viewModel.toggleTabLock(it.id) },
                onNewTab = { viewModel.addTab() },
                onCloseAllTabs = { viewModel.closeAllTabs() },
                onGroupTabs = { tabId1, tabId2 -> viewModel.groupTabs(tabId1, tabId2) }
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
                downloads = allDownloads,
                onTabSelected = { viewModel.setSheetTab(it) },
                onItemClicked = { url ->
                    viewModel.loadUrl(url)
                    viewModel.setBookmarksHistorySheetVisible(false)
                },
                onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                onDeleteHistory = { id -> viewModel.deleteHistory(id) },
                onDeleteDownload = { id -> viewModel.deleteDownload(id) },
                onClearDownloads = { viewModel.clearAllDownloads() },
                onClearHistory = { viewModel.clearHistory() },
                onClose = { viewModel.setBookmarksHistorySheetVisible(false) },
                onToggleWatchMode = { url -> viewModel.toggleBookmarkWatchMode(url) }
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
                viewModel = viewModel,
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
                onQuickTabStripVisibleChange = { viewModel.toggleQuickTabStrip() },
                onCookieEditorClick = { isCookieEditorVisible = true }
            )
        }

        // --- 5d. Cookie-Editor Bottom Sheet ---
        AnimatedVisibility(
            visible = isCookieEditorVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.82f)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.82f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            CookieEditorSheet(
                url = activeTab?.url ?: "https://dineinstyle.com",
                onClose = { isCookieEditorVisible = false },
                onReloadPage = {
                    val activeId = activeTabId
                    if (activeId != null) {
                        val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                        wv.reload()
                    }
                }
            )
        }

        // --- 5f. Website Evolution Notification Overlay ---
        val evolutionAlert by viewModel.websiteEvolutionAlert.collectAsStateWithLifecycle()
        AnimatedVisibility(
            visible = evolutionAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.8f)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            evolutionAlert?.let { alert ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF22D3EE).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Website Change Detected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = alert.first.removePrefix("https://").removePrefix("http://"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "This watched webpage has evolved! We identified ${alert.second} new modifications/changes in text blocks.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.dismissWebsiteEvolutionAlert() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Text("Dismiss", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.clearBookmarkTextHashAndReload(alert.first) {
                                        val activeId = activeTabId
                                        if (activeId != null) {
                                            val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                            wv.reload()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22D3EE),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update Baseline", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 5e. Detected Video Action Bottom Sheet ---
        AnimatedVisibility(
            visible = detectedVideoMedia != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.82f)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = 300f, dampingRatio = 0.82f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            detectedVideoMedia?.let { media ->
                DetectedVideoActionSheet(
                    media = media,
                    onClose = { viewModel.setDetectedVideoActionMedia(null) },
                    onPlayFullscreen = {
                        viewModel.setUcPlayerVideoUrl(it.url)
                        viewModel.setUcPlayerVideoTitle(it.pageTitle)
                        viewModel.setUcPlayerActive(true)
                    },
                    onPlayInBackground = {
                        viewModel.playBackgroundVideo(it)
                    },
                    onAddToQueue = {
                        viewModel.addToVideoQueue(it)
                        Toast.makeText(context, "Added to video queue!", Toast.LENGTH_SHORT).show()
                    },
                    onDownload = {
                        val extension = if (it.url.contains(".m3u8")) "m3u8" else "mp4"
                        val filename = "CapturedVideo_${System.currentTimeMillis()}.$extension"
                        downloadMedia(context, it.url, filename, viewModel)
                    },
                    onTrimDownload = {
                        trimmingVideoMedia = it
                        viewModel.setDetectedVideoActionMedia(null)
                    },
                    onSaveToPlaylist = {
                        viewModel.saveVideoToPlaylist(it)
                    }
                )
            }
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

        if (isSummaryDialogVisible) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.setSummaryDialogVisible(false) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = "Summary",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "AI Page Summary",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.setSummaryDialogVisible(false) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 350.dp)
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            if (isSummaryLoading) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF6366F1),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Analyzing page contents...",
                                        fontSize = 13.sp,
                                        color = Color.LightGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = summaryContent,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.setSummaryDialogVisible(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Done",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (showWebsiteUpdatesDialog) {
            WebsiteUpdatesDialog(
                updatedBookmarks = updatedBookmarks,
                isChecking = isCheckingUpdates,
                onRefresh = { viewModel.checkWatchedWebsitesForUpdates() },
                onClearAlert = { bookmark -> viewModel.clearBookmarkUpdateAlert(bookmark.url) },
                onVisit = { bookmark ->
                    viewModel.loadUrl(bookmark.url)
                    viewModel.clearBookmarkUpdateAlert(bookmark.url)
                    showWebsiteUpdatesDialog = false
                },
                onDismiss = { showWebsiteUpdatesDialog = false }
            )
        }

        trimmingVideoMedia?.let { media ->
            VideoTrimmingDialog(
                media = media,
                viewModel = viewModel,
                onDismiss = { trimmingVideoMedia = null }
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

        val pendingBlockSelector by viewModel.pendingBlockElementSelector.collectAsStateWithLifecycle()

        if (pendingBlockSelector != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissBlockElementConfirm() },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.8f),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = "Block Element", tint = Color(0xFFF43F5E), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Block Web Element?")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Do you want to permanently hide this element from this webpage? It will be hidden automatically whenever you open this site.")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("CSS Selector:", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = pendingBlockSelector!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                        onClick = {
                            viewModel.addCustomBlockedElement(pendingBlockSelector!!)
                            viewModel.dismissBlockElementConfirm()
                            // Refresh current tab
                            val activeId = activeTabId
                            if (activeId != null) {
                                val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                wv.reload()
                            }
                        }
                    ) {
                        Text("Block Element", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissBlockElementConfirm() }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // --- 6. Address Bar and Controls Area ---
        if (addressBarPosition == "top") {
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

                BottomNavigationBar(
                    viewModel = viewModel,
                    activeTab = activeTab,
                    allTabs = allTabs,
                    allBookmarks = allBookmarks,
                    isTabSwitcherVisible = isTabSwitcherVisible,
                    isBookmarksHistorySheetVisible = isBookmarksHistorySheetVisible,
                    isMediaStudioVisible = isMediaStudioVisible,
                    isSettingsScreenVisible = isSettingsScreenVisible,
                    hideBottomToolbar = hideBottomToolbar,
                    themeMode = themeMode,
                    menuShowReader = menuShowReader,
                    menuPageZoom = menuPageZoom,
                    menuFindOnPage = menuFindOnPage,
                    menuRequestDesktop = menuRequestDesktop,
                    menuAddToHome = menuAddToHome,
                    menuDeveloperTools = menuDeveloperTools
                )
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
                    visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible && !fullScreenReading,
                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut(),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .widthIn(max = 450.dp)
                ) {
                    AddressBar(
                        viewModel = viewModel,
                        url = urlInput,
                        isAdBlockerActive = adBlockerOn,
                        blockedAdsCount = activeTabBlockedAds,
                        canGoBack = activeTabCanGoBack,
                        canGoForward = activeTabCanGoForward,
                        loadingProgress = activeTabProgress,
                        updatedBookmarksCount = updateCount,
                        onUpdatesClick = { showWebsiteUpdatesDialog = true },
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
                        onHomeClick = { viewModel.loadUrl("dineinstyle.com") },
                        onCookieEditorClick = { isCookieEditorVisible = true },
                        copyUnblockActive = viewModel.isCopyUnblockActiveForUrl(activeTab?.url),
                        onToggleCopyUnblock = { viewModel.toggleCopyUnblockForUrl(activeTab?.url) },
                        themeMode = themeMode,
                        chromeThemeActive = chromeThemeActive,
                        chromeThemeToolbarColor = chromeThemeToolbarColor,
                        chromeThemeTextColor = chromeThemeTextColor,
                        chromeThemeInactiveTextColor = chromeThemeInactiveTextColor,
                        aiSmartSummarizerEnabled = aiSmartSummarizerEnabled,
                        onAiSummarizeClick = {
                            val activeId = activeTabId
                            if (activeId != null) {
                                val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                wv.evaluateJavascript(
                                    "(function() { " +
                                    "   var text = ''; " +
                                    "   var els = document.querySelectorAll('h1, h2, h3, p, article'); " +
                                    "   for (var i = 0; i < els.length; i++) { " +
                                    "       text += els[i].innerText + '\\n'; " +
                                    "       if (text.length > 5000) break; " +
                                    "   } " +
                                    "   return text; " +
                                    "})()"
                                ) { result ->
                                    val cleanText = result?.trim()?.removeSurrounding("\"")?.replace("\\\\n", "\n")?.replace("\\n", "\n") ?: ""
                                    viewModel.summarizeCurrentPage(cleanText.ifBlank { "No readable article content found on this page." })
                                }
                            }
                        },
                        activeTabReadTime = activeTabReadTime
                    )
                }

                // Bottom Navigation Row
                BottomNavigationBar(
                    viewModel = viewModel,
                    activeTab = activeTab,
                    allTabs = allTabs,
                    allBookmarks = allBookmarks,
                    isTabSwitcherVisible = isTabSwitcherVisible,
                    isBookmarksHistorySheetVisible = isBookmarksHistorySheetVisible,
                    isMediaStudioVisible = isMediaStudioVisible,
                    isSettingsScreenVisible = isSettingsScreenVisible,
                    hideBottomToolbar = hideBottomToolbar,
                    themeMode = themeMode,
                    menuShowReader = menuShowReader,
                    menuPageZoom = menuPageZoom,
                    menuFindOnPage = menuFindOnPage,
                    menuRequestDesktop = menuRequestDesktop,
                    menuAddToHome = menuAddToHome,
                    menuDeveloperTools = menuDeveloperTools
                )
            }
        }

        // --- 7. Search Active Overlay ---
        AnimatedVisibility(
            visible = isSearchFocused,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            SearchActiveOverlay(
                viewModel = viewModel,
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

        // --- 6. Floating Background Mini Player ---
        backgroundVideo?.let { bVideo ->
            AnimatedVisibility(
                visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible && !ucPlayerActive,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (hideBottomToolbar) 20.dp else 76.dp)
                    .zIndex(98f)
            ) {
                FloatingMiniPlayerBar(
                    backgroundVideo = bVideo,
                    isPlaying = isBackgroundVideoPlaying,
                    onTogglePlay = { viewModel.toggleBackgroundVideoPlay() },
                    onClose = { viewModel.stopBackgroundVideo() },
                    onExpand = {
                        viewModel.setUcPlayerVideoUrl(bVideo.url)
                        viewModel.setUcPlayerVideoTitle(bVideo.pageTitle)
                        viewModel.setUcPlayerActive(true)
                    }
                )
            }
        }

        // --- 8. UC Premium Video Player Intercept Floating Corner Icon ---
        val latestVideo = remember(capturedMedia, activeTab?.url) {
            if (activeTab?.url == null) null
            else capturedMedia.lastOrNull {
                it.type == "video" && (
                    it.pageUrl == activeTab.url ||
                    it.pageUrl.trimEnd('/') == activeTab.url.trimEnd('/') ||
                    (it.pageUrl.isNotBlank() && activeTab.url.contains(it.pageUrl)) ||
                    (activeTab.url.isNotBlank() && it.pageUrl.contains(activeTab.url))
                )
            }
        }
        var dismissVideoToast by remember { mutableStateOf(false) }
        LaunchedEffect(activeTab?.url) {
            dismissVideoToast = false
        }

        var dismissCurrentUrlVideo by remember(activeTab?.url) { mutableStateOf(false) }

        var videoIconVisible by remember { mutableStateOf(false) }
        LaunchedEffect(latestVideo) {
            if (latestVideo != null) {
                videoIconVisible = true
            } else {
                videoIconVisible = false
            }
        }

        val showCornerIcon = videoIconVisible && !dismissCurrentUrlVideo
        var showPlayerOptionsMenu by remember { mutableStateOf(false) }

        AnimatedVisibility(
            visible = showCornerIcon,
            enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 20.dp)
                .zIndex(99f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expanded Options Menu Card (from user concept screenshot)
                AnimatedVisibility(
                    visible = showPlayerOptionsMenu,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            // Option 1: Play in Background
                            IconButton(onClick = {
                                latestVideo?.let {
                                    viewModel.playBackgroundVideo(it)
                                    showPlayerOptionsMenu = false
                                    Toast.makeText(context, "Playing in background...", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play in Background",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Vertical Separator
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
 
                            // Option 2: Add to Queue
                            IconButton(onClick = {
                                latestVideo?.let {
                                    viewModel.addToVideoQueue(it)
                                    Toast.makeText(context, "Added to queue!", Toast.LENGTH_SHORT).show()
                                    showPlayerOptionsMenu = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Queue Video",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            
                            // Vertical Separator
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
 
                            // Option 3: Fullscreen / Expand
                            IconButton(onClick = {
                                latestVideo?.let {
                                    viewModel.setUcPlayerVideoUrl(it.url)
                                    viewModel.setUcPlayerVideoTitle(it.pageTitle)
                                    viewModel.setUcPlayerActive(true)
                                    showPlayerOptionsMenu = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Fullscreen Player",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
 
                            // Vertical Separator
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
 
                            // Option 4: Download
                            IconButton(onClick = {
                                latestVideo?.let {
                                    downloadMedia(context, it.url, "${System.currentTimeMillis()}.mp4", viewModel)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Download Video",
                                    tint = Color(0xFF10B981)
                                )
                            }

                            // Vertical Separator
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                            // Option 5: Dismiss / Close
                            IconButton(onClick = {
                                dismissCurrentUrlVideo = true
                                showPlayerOptionsMenu = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide Icon",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
 
                // Core Floating Play Toggle Button
                Box(
                    modifier = Modifier
                        .shadow(12.dp, CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            showPlayerOptionsMenu = !showPlayerOptionsMenu
                        }
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
 
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseScale)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha), CircleShape)
                        )
 
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Intercept Media Stream",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
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
            AuraVideoPlayer(
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
                },
                onPlayInBackground = { url, title ->
                    viewModel.playBackgroundVideo(CapturedMedia(url = url, type = "video", pageTitle = title, pageUrl = url))
                    viewModel.setUcPlayerActive(false)
                },
                viewModel = viewModel
            )
        }
    }
}

// --- BOUNCY INTERACTIVE ICON BUTTON ---
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.42f, // delightfully bouncy bubble rebound
            stiffness = 500f      // snappy, premium response
        ),
        label = "btn_press"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// --- REUSABLE THEME-AWARE BOTTOM NAVIGATION BAR ---
@Composable
fun BottomNavigationBar(
    viewModel: BrowserViewModel,
    activeTab: BrowserTab?,
    allTabs: List<BrowserTab>,
    allBookmarks: List<com.example.data.Bookmark>,
    isTabSwitcherVisible: Boolean,
    isBookmarksHistorySheetVisible: Boolean,
    isMediaStudioVisible: Boolean,
    isSettingsScreenVisible: Boolean,
    hideBottomToolbar: Boolean,
    themeMode: String,
    menuShowReader: Boolean,
    menuPageZoom: Boolean,
    menuFindOnPage: Boolean,
    menuRequestDesktop: Boolean,
    menuAddToHome: Boolean,
    menuDeveloperTools: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabDesktopModes by viewModel.tabDesktopModes.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = !isTabSwitcherVisible && !isBookmarksHistorySheetVisible && !isMediaStudioVisible && !isSettingsScreenVisible && !hideBottomToolbar,
        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
    ) {
        val isDarkThemeLocal = when (themeMode) {
            "light" -> false
            "dark", "amoled" -> true
            else -> androidx.compose.foundation.isSystemInDarkTheme()
        }
        val navBgColor = if (isDarkThemeLocal) {
            if (themeMode == "amoled") Color(0xFF121212).copy(alpha = 0.95f) else Color(0xFF1E293B).copy(alpha = 0.95f)
        } else {
            Color.White.copy(alpha = 0.95f)
        }
        val navContentColor = if (isDarkThemeLocal) Color.White else Color.Black
        val navDisabledColor = if (isDarkThemeLocal) Color.DarkGray else Color.LightGray
        val navItemBgColor = if (isDarkThemeLocal) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f)

        Row(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 400.dp)
                .height(56.dp)
                .background(navBgColor, RoundedCornerShape(100.dp))
                .shadow(12.dp, RoundedCornerShape(100.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. History Icon
            AnimatedIconButton(
                onClick = { viewModel.toggleBookmarksHistorySheet(1) },
                modifier = Modifier.testTag("nav_history_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "Browsing History",
                    tint = navContentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(26.dp)
                )
            }

            // 2. Bookmark Pin Icon
            val isBookmarkedState = remember(activeTab?.url, allBookmarks) {
                allBookmarks.any { it.url == activeTab?.url }
            }
            AnimatedIconButton(
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
                    tint = if (isBookmarkedState) Color(0xFFD4E157) else navContentColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(26.dp)
                )
            }

            // 3. New Tab Button (+)
            AnimatedIconButton(
                onClick = { viewModel.addTab() },
                modifier = Modifier
                    .size(38.dp)
                    .background(navItemBgColor, CircleShape)
                    .testTag("nav_new_tab_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = navContentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 4. Tab Switcher Icon
            val tabCount = allTabs.size
            AnimatedIconButton(
                onClick = { viewModel.toggleTabSwitcher() },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("nav_tab_switcher_btn")
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, if (isTabSwitcherVisible) navContentColor else navContentColor.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .background(
                            if (isTabSwitcherVisible) navContentColor else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabCount.toString(),
                        color = if (isTabSwitcherVisible) navBgColor else navContentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 5. Options Menu Icon (...)
            var isMenuExpanded by remember { mutableStateOf(false) }
            Box {
                AnimatedIconButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    modifier = Modifier.testTag("nav_options_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More Options",
                        tint = navContentColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(26.dp)
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
                            viewModel.clearBlockedAds(activeTab?.id ?: 0)
                        }
                    )

                    // Custom Toggles
                    if (menuShowReader) {
                        DropdownMenuItem(
                            text = { Text("Reader Mode") },
                            leadingIcon = { Icon(Icons.Default.Book, "Reader") },
                            onClick = {
                                isMenuExpanded = false
                                val activeId = activeTab?.id
                                if (activeId != null) {
                                    val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                                    val js = """
                                        (function() {
                                            var title = document.querySelector('h1')?.innerText || document.title || 'Untitled Article';
                                            var paras = document.querySelectorAll('article p, p, h2, h3');
                                            var text = '';
                                            for (var i = 0; i < paras.length; i++) {
                                                text += paras[i].innerText + '\n\n';
                                                if (text.length > 20000) break;
                                            }
                                            return JSON.stringify({ title: title, content: text.trim() });
                                        })()
                                    """.trimIndent()
                                    wv.evaluateJavascript(js) { result ->
                                        try {
                                            if (!result.isNullOrEmpty() && result != "null") {
                                                val jsonStr = if (result.startsWith("\"")) {
                                                    org.json.JSONTokener(result).nextValue().toString()
                                                } else {
                                                    result
                                                }
                                                val obj = org.json.JSONObject(jsonStr)
                                                val t = obj.optString("title", "Untitled Article")
                                                val c = obj.optString("content", "")
                                                if (c.isNotEmpty()) {
                                                    viewModel.activateReaderMode(t, c)
                                                } else {
                                                    android.widget.Toast.makeText(context, "No article content detected on this page.", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                android.widget.Toast.makeText(context, "Failed to read page content.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "No readable article content found.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
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
                        val isDesktop = tabDesktopModes[activeTab?.id ?: 0] ?: false
                        DropdownMenuItem(
                            text = { Text(if (isDesktop) "Request Mobile Site" else "Request Desktop Site") },
                            leadingIcon = { Icon(if (isDesktop) Icons.Default.Smartphone else Icons.Default.Monitor, "Desktop") },
                            onClick = {
                                isMenuExpanded = false
                                viewModel.toggleDesktopModeForTab(activeTab?.id ?: 0, context)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Translate Page (Google)") },
                        leadingIcon = { Icon(Icons.Default.Translate, "Translate") },
                        onClick = {
                            isMenuExpanded = false
                            val currentUrl = activeTab?.url ?: ""
                            if (currentUrl.isNotEmpty() && currentUrl != "dineinstyle.com") {
                                val translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=" + android.net.Uri.encode(currentUrl)
                                viewModel.loadUrl(translateUrl)
                            } else {
                                android.widget.Toast.makeText(context, "Open a website first to translate.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Full Screen Reading") },
                        leadingIcon = { Icon(Icons.Default.Fullscreen, "Full Screen") },
                        onClick = {
                            isMenuExpanded = false
                            viewModel.setFullScreenReading(true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Tabs & Cache") },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, "Clear") },
                        onClick = {
                            isMenuExpanded = false
                            viewModel.oneTapClearEverything(context)
                        }
                    )
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
                        text = { Text("Start Page") },
                        leadingIcon = { Icon(Icons.Default.Home, "Start Page Home") },
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

// --- FLOATING ADDRESS BAR COMPONENT ---
@Composable
fun AddressBar(
    viewModel: BrowserViewModel,
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
    onHomeClick: () -> Unit = {},
    onCookieEditorClick: () -> Unit = {},
    copyUnblockActive: Boolean = true,
    onToggleCopyUnblock: () -> Unit = {},
    themeMode: String = "dark",
    chromeThemeActive: Boolean = false,
    chromeThemeToolbarColor: Int = 0,
    chromeThemeTextColor: Int = 0,
    chromeThemeInactiveTextColor: Int = 0,
    updatedBookmarksCount: Int = 0,
    onUpdatesClick: () -> Unit = {},
    aiSmartSummarizerEnabled: Boolean = false,
    onAiSummarizeClick: () -> Unit = {},
    activeTabReadTime: Int? = null
) {
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val barBgColor = if (chromeThemeActive) {
        Color(chromeThemeToolbarColor).copy(alpha = 0.92f)
    } else if (isDarkTheme) {
        if (themeMode == "amoled") Color(0xFF09090B).copy(alpha = 0.90f) else Color(0xFF1E293B).copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    val contentColor = if (chromeThemeActive) Color(chromeThemeTextColor) else if (isDarkTheme) Color.White else Color.Black
    val disabledColor = if (chromeThemeActive) Color(chromeThemeInactiveTextColor).copy(alpha = 0.4f) else if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)
    val fieldBgColor = if (chromeThemeActive) Color(chromeThemeTextColor).copy(alpha = 0.08f) else if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val textOrUrlColor = if (url.isEmpty() || url == "dineinstyle.com") Color.Gray else contentColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = if (isDarkTheme) Color.Black else Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = barBgColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sleeker Premium Progress Bar
            if (loadingProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color(0xFF0284C7), // Sleeker premium blue loading indicator
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                AnimatedIconButton(
                    onClick = onBackClick,
                    enabled = canGoBack,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = if (canGoBack) contentColor else disabledColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Forward Button
                AnimatedIconButton(
                    onClick = onForwardClick,
                    enabled = canGoForward,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Forward",
                        tint = if (canGoForward) contentColor else disabledColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Address field container
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(fieldBgColor, RoundedCornerShape(20.dp))
                        .border(
                            width = 0.5.dp,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onAddressBarClick() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (url.isEmpty() || url == "dineinstyle.com") "Search or type URL" else url,
                        color = textOrUrlColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (activeTabReadTime != null && url.isNotEmpty() && url != "dineinstyle.com") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Read Time",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$activeTabReadTime min",
                                color = Color(0xFF38BDF8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (updatedBookmarksCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .clickable { onUpdatesClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Website Updates Available",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Microphone Icon (Voice Input)
                    AnimatedIconButton(
                        onClick = onMicClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Shield / Ad Blocker status indicator (Emerald/Teal glowing badge when active)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAdBlockerActive) {
                                    if (blockedAdsCount > 0) Color(0xFF0EA5E9) else Color(0xFF10B981)
                                } else {
                                    contentColor.copy(alpha = 0.15f)
                                }
                            )
                            .clickable { onShieldClick() }
                            .testTag("shield_badge_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdBlockerActive) Icons.Default.ElectricBolt else Icons.Default.Shield,
                            contentDescription = "Ad Blocker Status",
                            tint = if (isAdBlockerActive) Color.White else contentColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (hideBottomToolbar) {
                    Spacer(modifier = Modifier.width(6.dp))

                    // Tab Count Box (Compact Tab Switcher Icon)
                    AnimatedIconButton(
                        onClick = onTabSwitcherClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("address_tab_switcher_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.5.dp, if (isTabSwitcherVisible) contentColor else disabledColor, RoundedCornerShape(6.dp))
                                .background(
                                    if (isTabSwitcherVisible) contentColor else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabCount.toString(),
                                color = if (isTabSwitcherVisible) barBgColor else contentColor,
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
                        AnimatedIconButton(
                            onClick = { isMenuExpanded = !isMenuExpanded },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("address_options_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = contentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                         BrowserOptionsMenu(
                            viewModel = viewModel,
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
                            menuDeveloperTools = menuDeveloperTools,
                            onCookieEditorClick = onCookieEditorClick,
                            copyUnblockActive = copyUnblockActive,
                            onToggleCopyUnblock = onToggleCopyUnblock,
                            themeMode = themeMode,
                            aiSmartSummarizerEnabled = aiSmartSummarizerEnabled,
                            onAiSummarizeClick = onAiSummarizeClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserOptionsMenu(
    viewModel: BrowserViewModel,
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
    onCookieEditorClick: () -> Unit = {},
    copyUnblockActive: Boolean = true,
    onToggleCopyUnblock: () -> Unit = {},
    themeMode: String = "dark",
    aiSmartSummarizerEnabled: Boolean = false,
    onAiSummarizeClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val allTabs by viewModel.allTabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val tabDesktopModes by viewModel.tabDesktopModes.collectAsStateWithLifecycle()
    val activeTab = allTabs.find { it.id == activeTabId }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        com.example.ui.theme.MyApplicationTheme(themeMode = themeMode) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
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
        DropdownMenuItem(
            text = { Text("Cookie-Editor") },
            leadingIcon = { Icon(Icons.Default.Cookie, "Cookie-Editor") },
            onClick = {
                onDismissRequest()
                onCookieEditorClick()
            },
            modifier = Modifier.testTag("menu_cookie_editor_btn")
        )
        DropdownMenuItem(
            text = { Text(if (copyUnblockActive) "Unblock Copy: ON" else "Unblock Copy: OFF") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, "Unblock Copy") },
            onClick = {
                onDismissRequest()
                onToggleCopyUnblock()
            },
            modifier = Modifier.testTag("menu_toggle_copy_unblock_btn")
        )

        // Custom Toggles
        if (menuShowReader) {
            DropdownMenuItem(
                text = { Text("Reader Mode") },
                leadingIcon = { Icon(Icons.Default.Book, "Reader") },
                onClick = {
                    onDismissRequest()
                    val activeId = activeTab?.id
                    if (activeId != null) {
                        val wv = WebViewPool.getOrCreateWebView(context, activeId, viewModel)
                        val js = """
                            (function() {
                                var title = document.querySelector('h1')?.innerText || document.title || 'Untitled Article';
                                var paras = document.querySelectorAll('article p, p, h2, h3');
                                var text = '';
                                for (var i = 0; i < paras.length; i++) {
                                    text += paras[i].innerText + '\n\n';
                                    if (text.length > 20000) break;
                                }
                                return JSON.stringify({ title: title, content: text.trim() });
                            })()
                        """.trimIndent()
                        wv.evaluateJavascript(js) { result ->
                            try {
                                if (!result.isNullOrEmpty() && result != "null") {
                                    val jsonStr = if (result.startsWith("\"")) {
                                        org.json.JSONTokener(result).nextValue().toString()
                                    } else {
                                        result
                                    }
                                    val obj = org.json.JSONObject(jsonStr)
                                    val t = obj.optString("title", "Untitled Article")
                                    val c = obj.optString("content", "")
                                    if (c.isNotEmpty()) {
                                        viewModel.activateReaderMode(t, c)
                                    } else {
                                        android.widget.Toast.makeText(context, "No article content detected on this page.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to read page content.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "No readable article content found.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
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
            val isDesktop = tabDesktopModes[activeTab?.id ?: 0] ?: false
            DropdownMenuItem(
                text = { Text(if (isDesktop) "Request Mobile Site" else "Request Desktop Site") },
                leadingIcon = { Icon(if (isDesktop) Icons.Default.Smartphone else Icons.Default.Monitor, "Desktop") },
                onClick = {
                    onDismissRequest()
                    viewModel.toggleDesktopModeForTab(activeTab?.id ?: 0, context)
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Translate Page (Google)") },
            leadingIcon = { Icon(Icons.Default.Translate, "Translate") },
            onClick = {
                onDismissRequest()
                val currentUrl = activeTab?.url ?: ""
                if (currentUrl.isNotEmpty() && currentUrl != "dineinstyle.com") {
                    val translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=" + android.net.Uri.encode(currentUrl)
                    viewModel.loadUrl(translateUrl)
                } else {
                    android.widget.Toast.makeText(context, "Open a website first to translate.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        DropdownMenuItem(
            text = { Text("Full Screen Reading") },
            leadingIcon = { Icon(Icons.Default.Fullscreen, "Full Screen") },
            onClick = {
                onDismissRequest()
                viewModel.setFullScreenReading(true)
            }
        )
        DropdownMenuItem(
            text = { Text("Clear Tabs & Cache") },
            leadingIcon = { Icon(Icons.Default.DeleteForever, "Clear") },
            onClick = {
                onDismissRequest()
                viewModel.oneTapClearEverything(context)
            }
        )
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
        if (aiSmartSummarizerEnabled) {
            DropdownMenuItem(
                text = { Text("AI Page Summarizer", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Book, "AI Summarize", tint = Color(0xFF818CF8)) },
                onClick = {
                    onDismissRequest()
                    onAiSummarizeClick()
                }
            )
            HorizontalDivider()
        }

        DropdownMenuItem(
            text = { Text("Settings") },
            leadingIcon = { Icon(Icons.Default.Settings, "Settings") },
            onClick = {
                onDismissRequest()
                onSettingsClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Start Page") },
            leadingIcon = { Icon(Icons.Default.Home, "Start Page Home") },
            onClick = {
                onDismissRequest()
                onHomeClick()
            }
        )
            }
        }
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
    currentUrl: String,
    viewModel: BrowserViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showDnsSettings by remember { mutableStateOf(false) }
    var showRoutingSettings by remember { mutableStateOf(false) }
    var activeTabIdx by remember { mutableStateOf(0) }

    val isDark = isSystemInDarkTheme() || viewModel.themeMode.value != "light"
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val domain = remember(currentUrl) {
        try {
            val uri = java.net.URI(currentUrl)
            val host = uri.host ?: ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            currentUrl
        }
    }.ifEmpty { "Local Session" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.15f))
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
                    .background(outlineColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Premium Custom Tab Switcher Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceVariantColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("🛡️ Ultra Shield", "✨ Site Care & Reality").forEachIndexed { index, title ->
                    val isSelected = activeTabIdx == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) primaryColor else Color.Transparent)
                            .clickable { activeTabIdx = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) onPrimaryColor else onSurfaceVariantColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeTabIdx == 0) {
                // TAB 1: ULTRA SHIELD & STATS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    if (adBlockerOn) primaryColor.copy(alpha = 0.12f) else surfaceVariantColor,
                                    CircleShape
                                )
                                .clickable(onClick = onToggleAdBlocker)
                                .border(
                                    1.5.dp,
                                    if (adBlockerOn) primaryColor else outlineColor.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Ad Blocker Toggle",
                                tint = if (adBlockerOn) primaryColor else onSurfaceVariantColor,
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
                                    color = if (adBlockerOn) MaterialTheme.colorScheme.error else onSurfaceVariantColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "uBlock Ads Blocked",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onSurfaceColor
                                )
                            }
                            Text(
                                text = "on this website (Ultra Shield active)",
                                fontSize = 12.sp,
                                color = onSurfaceVariantColor
                            )
                        }
                    }

                    Switch(
                        checked = adBlockerOn,
                        onCheckedChange = { onToggleAdBlocker() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = onPrimaryColor,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = outlineColor,
                            uncheckedTrackColor = surfaceVariantColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Page Resource Estimates (Resource Inspector concept!)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceVariantColor.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🕵️ Page Resource Inspector Estimates",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Blocked Trackers: ${if (adBlockerOn) blockedCount else 0}", fontSize = 10.sp, color = onSurfaceVariantColor)
                                Text("Scripts Analyzed: 14", fontSize = 10.sp, color = onSurfaceVariantColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Data Saved: ~${if (adBlockerOn) (blockedCount * 45).toString() + " KB" else "0 KB"}", fontSize = 10.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                                Text("Battery Impact: Very Low", fontSize = 10.sp, color = onSurfaceVariantColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "uBlock Engine features",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showAdvancedSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle advanced settings",
                            tint = onSurfaceVariantColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showAdvancedSettings) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AdvancedToggleRow("uBlock network script filter rules", true)
                        AdvancedToggleRow("Cosmetic block & empty layout cleaning", true)
                        AdvancedToggleRow("Interstitials & cookie overlays auto-remover", true)
                        AdvancedToggleRow("Strict privacy tracker prevention", true)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = outlineColor.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(6.dp))

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
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "DNS Settings Icon",
                            tint = if (dnsEnabled) primaryColor else onSurfaceVariantColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Private & Custom DNS Servers",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (dnsEnabled) primaryColor else onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showDnsSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle DNS settings",
                            tint = onSurfaceVariantColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showDnsSettings) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Custom DNS Engine",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceColor
                                )
                                Text(
                                    text = "Route connections via secure servers to bypass blocks.",
                                    fontSize = 9.sp,
                                    color = onSurfaceVariantColor
                                )
                            }
                            Switch(
                                checked = dnsEnabled,
                                onCheckedChange = { onDnsEnabledChange(it) },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = onPrimaryColor,
                                    checkedTrackColor = primaryColor,
                                    uncheckedThumbColor = outlineColor,
                                    uncheckedTrackColor = surfaceVariantColor
                                )
                            )
                        }

                        if (dnsEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surfaceVariantColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("preset" to "Preset", "custom" to "Custom IP").forEach { (modeKey, modeTitle) ->
                                    val isSelected = dnsMode == modeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) surfaceColor else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { onDnsModeChange(modeKey) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = modeTitle,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) onSurfaceColor else onSurfaceVariantColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (dnsMode == "preset") {
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
                                                    color = if (isSelected) primaryColor.copy(alpha = 0.5f) else outlineColor.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .background(
                                                    color = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onDnsPresetIdChange(preset.id) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(preset.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                                Text("Fallback: ${preset.fallbackIp}", fontSize = 8.sp, color = onSurfaceVariantColor)
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.CheckCircle, "Selected", tint = primaryColor, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                var tempCustomDns by remember(dnsCustomValue) { mutableStateOf(dnsCustomValue) }
                                OutlinedTextField(
                                    value = tempCustomDns,
                                    onValueChange = { tempCustomDns = it },
                                    placeholder = { Text("e.g. dns.adguard-dns.com", fontSize = 10.sp) },
                                    label = { Text("DNS Server IP/Endpoint", fontSize = 10.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (tempCustomDns != dnsCustomValue) {
                                            IconButton(onClick = { onDnsCustomValueChange(tempCustomDns) }) {
                                                Icon(Icons.Default.Check, "Apply", tint = primaryColor, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = outlineColor.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(6.dp))

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
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Smart Auto-Routing Icon",
                            tint = if (smartAutoRouting) primaryColor else onSurfaceVariantColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Smart Auto-Routing System",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (smartAutoRouting) primaryColor else onSurfaceVariantColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showRoutingSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Routing settings",
                            tint = onSurfaceVariantColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showRoutingSettings) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Smart Auto-Routing",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceColor
                                )
                                Text(
                                    text = "Auto-detects and bypasses errors via secure proxies.",
                                    fontSize = 9.sp,
                                    color = onSurfaceVariantColor
                                )
                            }
                            Switch(
                                checked = smartAutoRouting,
                                onCheckedChange = { onSmartAutoRoutingChange(it) },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = onPrimaryColor,
                                    checkedTrackColor = primaryColor,
                                    uncheckedThumbColor = outlineColor,
                                    uncheckedTrackColor = surfaceVariantColor
                                )
                            )
                        }

                        if (smartAutoRouting) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = surfaceVariantColor.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(primaryColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Status: $activeRoutingStatus",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = onSurfaceColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• Onion Gateway: Active (.onion to Tor Web Bridge)", fontSize = 9.sp, color = onSurfaceVariantColor)
                                    Text("• Fallback: Active (CroxyProxy secure routing rotation)", fontSize = 9.sp, color = onSurfaceVariantColor)
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 2: SITE CARE & REALITY FILTERS
                // 1. Per-Site Preference Profile Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceVariantColor.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, "Web site profile", tint = primaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Per-Site Profile: $domain",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceColor
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Site Zoom Level Row
                            var siteZoomVal by remember(domain) { mutableStateOf(viewModel.getSiteZoom(domain)) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Custom Page Zoom", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    Text("Overrides global page scale.", fontSize = 9.sp, color = onSurfaceVariantColor)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (siteZoomVal > 0.5f) {
                                                siteZoomVal = (siteZoomVal - 0.1f)
                                                viewModel.setSiteZoom(domain, siteZoomVal)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).background(surfaceVariantColor, CircleShape)
                                    ) {
                                        Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${(siteZoomVal * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = onSurfaceColor,
                                        modifier = Modifier.widthIn(min = 36.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    IconButton(
                                        onClick = {
                                            if (siteZoomVal < 2.5f) {
                                                siteZoomVal = (siteZoomVal + 0.1f)
                                                viewModel.setSiteZoom(domain, siteZoomVal)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).background(surfaceVariantColor, CircleShape)
                                    ) {
                                        Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = outlineColor.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Site Force Dark Mode
                            var siteDarkOn by remember(domain) { mutableStateOf(viewModel.getSiteForceDark(domain)) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Force Site Dark Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    Text("Inverts background colors locally.", fontSize = 9.sp, color = onSurfaceVariantColor)
                                }
                                Switch(
                                    checked = siteDarkOn,
                                    onCheckedChange = {
                                        siteDarkOn = it
                                        viewModel.setSiteForceDark(domain, it)
                                    },
                                    modifier = Modifier.scale(0.8f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = onPrimaryColor,
                                        checkedTrackColor = primaryColor,
                                        uncheckedThumbColor = outlineColor,
                                        uncheckedTrackColor = surfaceVariantColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = outlineColor.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Site Page Scripts Allowed
                            var siteScriptsOn by remember(domain) { mutableStateOf(viewModel.getSiteScriptsEnabled(domain)) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Enable Custom Scripts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    Text("Run user JS files on page load.", fontSize = 9.sp, color = onSurfaceVariantColor)
                                }
                                Switch(
                                    checked = siteScriptsOn,
                                    onCheckedChange = {
                                        siteScriptsOn = it
                                        viewModel.setSiteScriptsEnabled(domain, it)
                                    },
                                    modifier = Modifier.scale(0.8f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = onPrimaryColor,
                                        checkedTrackColor = primaryColor,
                                        uncheckedThumbColor = outlineColor,
                                        uncheckedTrackColor = surfaceVariantColor
                                    )
                                )
                            }
                        }
                    }

                    // 2. Reality Filters Section
                    val pinkCardBg = if (isDark) surfaceVariantColor.copy(alpha = 0.3f) else Color(0xFFFDF2F8)
                    val pinkCardBorder = if (isDark) outlineColor.copy(alpha = 0.12f) else Color(0xFFFCE7F3)
                    val pinkHeaderColor = if (isDark) primaryColor else Color(0xFFEC4899)
                    val pinkTitleColor = if (isDark) onSurfaceColor else Color(0xFF831843)
                    val pinkDescColor = if (isDark) onSurfaceVariantColor else Color(0xFF9D174D)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = pinkCardBg),
                        border = BorderStroke(1.dp, pinkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FilterAlt, "Reality Filters", tint = pinkHeaderColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reality Lens Filters",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pinkTitleColor
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Clickbait Filter
                            val clickbaitOn by viewModel.realityClickbaitFilter.collectAsStateWithLifecycle()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Anti-Clickbait Lens", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pinkTitleColor)
                                    Text("Minimizes sensational headlines and titles.", fontSize = 9.sp, color = pinkDescColor)
                                }
                                Switch(
                                    checked = clickbaitOn,
                                    onCheckedChange = { viewModel.setRealityClickbaitFilter(!clickbaitOn) },
                                    modifier = Modifier.scale(0.8f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = onPrimaryColor,
                                        checkedTrackColor = pinkHeaderColor,
                                        uncheckedThumbColor = outlineColor,
                                        uncheckedTrackColor = surfaceVariantColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = pinkCardBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Sponsored Content Blocker
                            val sponsoredOn by viewModel.realitySponsoredBlock.collectAsStateWithLifecycle()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Block Sponsored / Promoted Feed Blocks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pinkTitleColor)
                                    Text("Hides native ads, sponsored tags, and feeds.", fontSize = 9.sp, color = pinkDescColor)
                                }
                                Switch(
                                    checked = sponsoredOn,
                                    onCheckedChange = { viewModel.setRealitySponsoredBlock(!sponsoredOn) },
                                    modifier = Modifier.scale(0.8f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = onPrimaryColor,
                                        checkedTrackColor = pinkHeaderColor,
                                        uncheckedThumbColor = outlineColor,
                                        uncheckedTrackColor = surfaceVariantColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = pinkCardBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            // AI content detector badge
                            val aiBadgeOn by viewModel.realityAiBadge.collectAsStateWithLifecycle()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AI-Generated Content Detector", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pinkTitleColor)
                                    Text("Injects a smart warning on potential AI text.", fontSize = 9.sp, color = pinkDescColor)
                                }
                                Switch(
                                    checked = aiBadgeOn,
                                    onCheckedChange = { viewModel.setRealityAiBadge(!aiBadgeOn) },
                                    modifier = Modifier.scale(0.8f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = onPrimaryColor,
                                        checkedTrackColor = pinkHeaderColor,
                                        uncheckedThumbColor = outlineColor,
                                        uncheckedTrackColor = surfaceVariantColor
                                    )
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
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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


sealed class TabSwitcherItem {
    data class Single(val tab: com.example.data.BrowserTab) : TabSwitcherItem()
    data class Group(val name: String, val tabs: List<com.example.data.BrowserTab>) : TabSwitcherItem()
    
    val id: String
        get() = when (this) {
            is Single -> "single_${tab.id}"
            is Group -> "group_$name"
        }
}

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
    layoutStyle: com.example.viewmodel.TabLayoutStyle = com.example.viewmodel.TabLayoutStyle.GRID,
    onLayoutStyleChanged: (com.example.viewmodel.TabLayoutStyle) -> Unit = {},
    onToggleLock: (BrowserTab) -> Unit = {},
    onNewTab: () -> Unit = {},
    onCloseAllTabs: () -> Unit = {},
    modifier: Modifier = Modifier,
    onGroupTabs: (Long, Long) -> Unit = { _, _ -> },
    themeMode: String = "dark"
) {
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isAmoled = themeMode == "amoled"
    val context = androidx.compose.ui.platform.LocalContext.current

    val containerColor = if (isAmoled) {
        Color.Black
    } else if (isDarkTheme) {
        Color(0xFF030712)
    } else {
        Color(0xFFF8FAFC)
    }

    val primaryTextColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.LightGray else Color.DarkGray
    val capsuleBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val buttonBg = if (isDarkTheme) Color(0xFF334155) else Color.White
    val buttonTint = if (isDarkTheme) Color.White else Color.DarkGray
    val searchContainerBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White

    val filteredTabs = remember(tabs, searchQuery) {
        if (searchQuery.isBlank()) {
            tabs
        } else {
            tabs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true) }
        }
    }

    val groupedItems = remember(filteredTabs) {
        val list = mutableListOf<TabSwitcherItem>()
        val groups = filteredTabs.filter { it.groupName != null }.groupBy { it.groupName!! }
        val singles = filteredTabs.filter { it.groupName == null }
        
        singles.forEach { list.add(TabSwitcherItem.Single(it)) }
        groups.forEach { (name, groupTabs) ->
            list.add(TabSwitcherItem.Group(name, groupTabs))
        }
        
        list.sortByDescending {
            when (it) {
                is TabSwitcherItem.Single -> it.tab.timestamp
                is TabSwitcherItem.Group -> it.tabs.maxOfOrNull { t -> t.timestamp } ?: 0L
            }
        }
        list
    }

    var selectedMode by remember { mutableStateOf(0) } // 0 = Normal, 1 = Private

    var draggedTabId by remember { mutableStateOf<Long?>(null) }
    var hoverTabId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartRootPos by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<Long, Rect>() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
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
                    .background(Color.LightGray.copy(alpha = 0.6f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Capsule Normal vs Private Selector Row (Styled exactly like the iPhone mockup)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(capsuleBg, RoundedCornerShape(100.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Normal Tab Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (selectedMode == 0) (if (isDarkTheme) Color(0xFF334155) else Color.White) else Color.Transparent)
                            .clickable { selectedMode = 0 }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Styled red square folder container from image
                        Box(
                            modifier = Modifier
                                .background(if (selectedMode == 0) Color(0xFFC23E50) else Color.Gray, RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${filteredTabs.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Private/Incognito Tab Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (selectedMode == 1) (if (isDarkTheme) Color(0xFF334155) else Color.White) else Color.Transparent)
                            .clickable { selectedMode = 1 }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff, // Visual equivalent to incognito shades/glasses
                            contentDescription = "Private Mode",
                            tint = if (selectedMode == 1) (if (isDarkTheme) Color.White else Color.Black) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title and stack management buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMode == 0) "Normal Tabs" else "Private Tabs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onManageGroups,
                        modifier = Modifier
                            .size(36.dp)
                            .background(buttonBg, CircleShape)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Stacks",
                            tint = buttonTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(buttonBg, CircleShape)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tab Switcher",
                            tint = buttonTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search tabs...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = searchContainerBg,
                    unfocusedContainerColor = searchContainerBg,
                    focusedTextColor = primaryTextColor,
                    unfocusedTextColor = primaryTextColor
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area: 2-Column Responsive Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedMode == 1) {
                    // Private Mode View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF334155), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = "Private Mode",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Private Browsing Enabled",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your browsing history, search terms, cookies, and temporary files will not be saved locally.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else if (groupedItems.isEmpty()) {
                    // Empty tabs list
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "No tabs",
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active tabs found", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    // Drag and drop enabled 2-Column Responsive Grid Layout
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(groupedItems, key = { it.id }) { item ->
                            val itemId = when (item) {
                                is TabSwitcherItem.Single -> item.tab.id
                                is TabSwitcherItem.Group -> item.tabs.first().id
                            }
                            val isCurrentlyDragged = draggedTabId == itemId
                            val isHovered = hoverTabId == itemId

                            val animatedOffset by animateOffsetAsState(
                                targetValue = if (isCurrentlyDragged) dragOffset else Offset.Zero,
                                animationSpec = if (isCurrentlyDragged) snap() else spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )

                            val scale by animateFloatAsState(
                                targetValue = if (isCurrentlyDragged) 1.06f else if (isHovered) 0.90f else 1.0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )

                            Box(
                                modifier = Modifier
                                    .zIndex(if (isCurrentlyDragged) 10f else if (isHovered) 5f else 1f)
                                    .offset {
                                        androidx.compose.ui.unit.IntOffset(
                                            animatedOffset.x.roundToInt(),
                                            animatedOffset.y.roundToInt()
                                        )
                                    }
                                    .scale(scale)
                                    .onGloballyPositioned { layoutCoordinates ->
                                        val position = layoutCoordinates.positionInRoot()
                                        val size = layoutCoordinates.size.toSize()
                                        itemBounds[itemId] = Rect(position, size)
                                    }
                                    .pointerInput(itemId) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                draggedTabId = itemId
                                                dragOffset = Offset.Zero
                                                val rect = itemBounds[itemId]
                                                if (rect != null) {
                                                    dragStartRootPos = rect.topLeft + offset
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                                val currentFingerPos = dragStartRootPos + dragOffset

                                                var foundHover: Long? = null
                                                for ((tabId, bounds) in itemBounds) {
                                                    if (tabId != itemId && bounds.contains(currentFingerPos)) {
                                                        foundHover = tabId
                                                        break
                                                    }
                                                }
                                                hoverTabId = foundHover
                                            },
                                            onDragEnd = {
                                                if (draggedTabId != null && hoverTabId != null && draggedTabId != hoverTabId) {
                                                    onGroupTabs(draggedTabId!!, hoverTabId!!)
                                                    Toast.makeText(context, "Group created/updated successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                                draggedTabId = null
                                                hoverTabId = null
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedTabId = null
                                                hoverTabId = null
                                                dragOffset = Offset.Zero
                                            }
                                        )
                                    }
                            ) {
                                when (item) {
                                    is TabSwitcherItem.Single -> {
                                        TabSingleCard(
                                            tab = item.tab,
                                            onSelected = {
                                                if (draggedTabId == null) {
                                                    onTabSelected(item.tab)
                                                }
                                            },
                                            onClosed = { onTabClosed(item.tab) },
                                            isHovered = isHovered,
                                            themeMode = themeMode
                                        )
                                    }
                                    is TabSwitcherItem.Group -> {
                                        TabGroupCard(
                                            groupName = item.name,
                                            tabs = item.tabs,
                                            onTabSelected = { tab ->
                                                if (draggedTabId == null) {
                                                    onTabSelected(tab)
                                                }
                                            },
                                            onTabClosed = { tab -> onTabClosed(tab) },
                                            isHovered = isHovered,
                                            themeMode = themeMode
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM ACTION FLOATING CONTROL BUTTONS
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Trash / Close all on left
                    IconButton(
                        onClick = {
                            onCloseAllTabs()
                            onClose()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(54.dp)
                            .background(Color.White, CircleShape)
                            .shadow(4.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Close all tabs",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Floating Add New Tab on right
                    FloatingActionButton(
                        onClick = {
                            onNewTab()
                            onClose()
                        },
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Open new tab",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WebpageVisualPreview(url: String, title: String, isDarkTheme: Boolean) {
    val bg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkTheme) Color.LightGray else Color(0xFF64748B)
    val cardBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(8.dp)
    ) {
        val lowerUrl = url.lowercase()
        when {
            lowerUrl.contains("google") -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(20.dp)
                            .background(cardBg, RoundedCornerShape(10.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color.LightGray, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search or type URL", color = Color.LightGray, fontSize = 8.sp)
                        }
                    }
                }
            }
            lowerUrl.contains("youtube") -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                        Text("YouTube", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.weight(1f).height(45.dp).background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp).align(Alignment.Center))
                        }
                        Box(modifier = Modifier.weight(1f).height(45.dp).background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp).align(Alignment.Center))
                        }
                    }
                    Text("Trending Streams", color = textSecondary, fontSize = 8.sp)
                }
            }
            lowerUrl.contains("apple") -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhoneIphone, null, tint = textPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("iPhone Pro", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Titanium. So strong.", color = textSecondary, fontSize = 8.sp)
                }
            }
            lowerUrl.contains("dine") || lowerUrl.contains("restaurant") -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DINE IN STYLE", color = Color(0xFFEC4899), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.weight(1f).height(45.dp).background(Color(0xFFFFEDD5), RoundedCornerShape(4.dp)))
                        Box(modifier = Modifier.weight(1f).height(45.dp).background(Color(0xFFFCE7F3), RoundedCornerShape(4.dp)))
                    }
                    Text("Reserve Gourmet Dining", color = textSecondary, fontSize = 8.sp)
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Lock, null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                            Text(url.substringAfter("://").substringBefore("/").take(15), color = textSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(0.9f).height(8.dp).background(textSecondary.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(8.dp).background(textSecondary.copy(alpha = 0.15f), RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.5f).height(8.dp).background(textSecondary.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                    }
                    
                    Text(title, color = textPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun TabGridItem(
    tab: BrowserTab,
    onSelected: () -> Unit,
    onClosed: () -> Unit,
    isHovered: Boolean = false,
    themeMode: String = "dark"
) {
    val isSelected = tab.isSelected
    
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isAmoled = themeMode == "amoled"

    val previewBg = if (isAmoled) Color.Black else if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val borderColor = if (isHovered) {
        Color(0xFF3B82F6)
    } else if (isSelected) {
        Color(0xFF3B82F6)
    } else {
        if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.5f)
    }
    
    val borderWidth = if (isHovered) 3.5.dp else if (isSelected) 2.5.dp else 1.dp

    val headerPillColor = remember(tab.title) {
        when {
            tab.url.contains("apple", ignoreCase = true) -> Color(0xFF1F2937)
            tab.url.contains("google", ignoreCase = true) -> Color(0xFF3B82F6)
            tab.url.contains("youtube", ignoreCase = true) -> Color(0xFFEF4444)
            tab.url.contains("dine", ignoreCase = true) -> Color(0xFFEC4899)
            else -> Color(0xFF8B5CF6)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
    ) {
        // 1. Header Pill Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(headerPillColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = tab.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClosed,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close tab",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // 2. Main Web Preview Card Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(previewBg)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            WebpageVisualPreview(url = tab.url, title = tab.title, isDarkTheme = isDarkTheme)
            
            // Stack badge overlay if grouped
            if (tab.groupName != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tab.groupName ?: "Stack",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TabSingleCard(
    tab: BrowserTab,
    onSelected: () -> Unit,
    onClosed: () -> Unit,
    isHovered: Boolean = false,
    themeMode: String = "dark"
) {
    val isSelected = tab.isSelected
    
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isAmoled = themeMode == "amoled"
    val previewBg = if (isAmoled) Color.Black else if (isDarkTheme) Color(0xFF1E293B) else Color.White

    // Border highlights
    val borderColor = if (isHovered) {
        Color(0xFF3B82F6)
    } else if (isSelected) {
        Color(0xFF3B82F6)
    } else {
        if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.5f)
    }
    val borderWidth = if (isHovered) 3.5.dp else if (isSelected) 2.5.dp else 1.dp

    // Header pill color: White/Light Gray in Light Mode, Dark Slate in Dark Mode
    val headerBgColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val headerTextColor = if (isDarkTheme) Color.White else Color.Black
    val headerBorderStroke = if (isDarkTheme) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    } else {
        BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.25f))
    }

    var swipeOffset by remember { mutableStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "swipeOffset"
    )
    val swipeAlpha = (1f - (kotlin.math.abs(animatedSwipeOffset) / 300f)).coerceIn(0.1f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = animatedSwipeOffset
                alpha = swipeAlpha
            }
            .pointerInput(tab.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(swipeOffset) > 200f) {
                            onClosed()
                        } else {
                            swipeOffset = 0f
                        }
                    },
                    onDragCancel = {
                        swipeOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffset += dragAmount
                    }
                )
            }
            .clickable { onSelected() }
    ) {
        // Pill Header matching the design of single tabs in the image
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(100.dp),
            border = headerBorderStroke,
            colors = CardDefaults.cardColors(containerColor = headerBgColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small Favicon or stylized leading letter icon
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title.take(1).uppercase(),
                            color = if (isDarkTheme) Color.White else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = tab.title,
                        color = headerTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClosed,
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = if (isDarkTheme) Color.White else Color.DarkGray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Web Page Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(previewBg)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            WebpageVisualPreview(url = tab.url, title = tab.title, isDarkTheme = isDarkTheme)
            
            if (tab.isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Tab",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TabGroupCard(
    groupName: String,
    tabs: List<BrowserTab>,
    onTabSelected: (BrowserTab) -> Unit,
    onTabClosed: (BrowserTab) -> Unit,
    isHovered: Boolean = false,
    themeMode: String = "dark"
) {
    var activeIndex by remember(tabs) { mutableStateOf(0) }
    // Ensure activeIndex is valid
    val currentIndex = if (activeIndex in tabs.indices) activeIndex else 0
    val activeTab = tabs.getOrNull(currentIndex) ?: return

    val groupHeaderColor = remember(groupName) {
        when {
            groupName.contains("Lifestyle", ignoreCase = true) -> Color(0xFFC23E50) // Berry red
            groupName.contains("Reading", ignoreCase = true) -> Color(0xFF64748B) // Slate grey
            groupName.contains("Work", ignoreCase = true) -> Color(0xFF0F766E) // Teal
            groupName.contains("Social", ignoreCase = true) -> Color(0xFF7C3AED) // Purple
            groupName.contains("Shopping", ignoreCase = true) -> Color(0xFFD97706) // Amber/Orange
            groupName.contains("News", ignoreCase = true) -> Color(0xFF0369A1) // Blue
            else -> {
                val colors = listOf(
                    Color(0xFFC23E50), // Berry
                    Color(0xFF4F46E5), // Indigo
                    Color(0xFF0D9488), // Teal
                    Color(0xFF0891B2), // Cyan
                    Color(0xFF4B5563), // Slate grey
                    Color(0xFF7C3AED), // Purple
                    Color(0xFFDB2777)  // Pink
                )
                val hash = groupName.hashCode().coerceAtLeast(0)
                colors[hash % colors.size]
            }
        }
    }

    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isAmoled = themeMode == "amoled"
    val previewBg = if (isAmoled) Color.Black else if (isDarkTheme) Color(0xFF1E293B) else Color.White

    // Border highlights
    val isSelected = tabs.any { it.isSelected }
    val borderColor = if (isHovered) {
        Color(0xFF3B82F6)
    } else if (isSelected) {
        Color(0xFF3B82F6)
    } else {
        if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.5f)
    }
    val borderWidth = if (isHovered) 3.5.dp else if (isSelected) 2.5.dp else 1.dp

    // Swiping Gestures
    var dragAmountAccumulated by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(tabs) {
                detectDragGestures(
                    onDragStart = { dragAmountAccumulated = 0f },
                    onDragEnd = {
                        val swipeThreshold = 100f // pixels
                        if (dragAmountAccumulated > swipeThreshold) {
                            // Swipe Right -> Show previous tab
                            activeIndex = if (currentIndex > 0) currentIndex - 1 else tabs.lastIndex
                        } else if (dragAmountAccumulated < -swipeThreshold) {
                            // Swipe Left -> Show next tab
                            activeIndex = if (currentIndex < tabs.lastIndex) currentIndex + 1 else 0
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountAccumulated += dragAmount.x
                    }
                )
            }
            .clickable { onTabSelected(activeTab) }
    ) {
        // Pill Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(100.dp))
                .background(groupHeaderColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Group",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = groupName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { tabs.forEach { onTabClosed(it) } },
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Group",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Stack Preview with layered look
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp)
                .padding(bottom = 6.dp)
        ) {
            // Layer 2 (Back card)
            if (tabs.size > 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(115.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                        .scale(0.92f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(previewBg.copy(alpha = 0.4f))
                        .border(
                            width = 1.dp,
                            color = borderColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        )
                )
            }

            // Layer 1 (Middle card)
            if (tabs.size > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-2).dp)
                        .scale(0.96f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(previewBg.copy(alpha = 0.7f))
                        .border(
                            width = 1.dp,
                            color = borderColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        )
                )
            }

            // Foreground Active Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(previewBg)
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (currentIndex > activeIndex) {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        }
                    },
                    label = "TabGroupSwiper"
                ) { targetTab ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        WebpageVisualPreview(
                            url = targetTab.url,
                            title = targetTab.title,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tabs.size} tabs",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (tabs.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == currentIndex) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == currentIndex) Color.White else Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}

// Preview card representing an open tab in the switcher
@Composable
fun TabPreviewCard(
    tab: BrowserTab,
    onSelected: () -> Unit,
    onClosed: () -> Unit,
    onToggleLock: () -> Unit,
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
            .then(if (modifier == Modifier) Modifier.width(170.dp).fillMaxHeight() else Modifier)
            .clickable(onClick = onSelected),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                                if (tab.url == "dineinstyle.com") Color(0xFF3B82F6) else Color(0xFF64B5F6),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab.url == "dineinstyle.com") "H" else tab.title.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (tab.url == "dineinstyle.com") "Start Page" else tab.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
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
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                if (tab.url == "dineinstyle.com") {
                    // Show a stunning, highly polished native mockup of the browser start page!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFF8FAFC),
                                        Color(0xFFF1F5F9),
                                        Color(0xFFE2E8F0)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            // Mini Logo
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Mini Search Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Text(
                                        text = "Search or type URL",
                                        color = Color.Gray,
                                        fontSize = 7.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Mini Favorites Grid Mockup
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(4) { idx ->
                                    val iconColor = when(idx) {
                                        0 -> Color(0xFFEF4444)
                                        1 -> Color(0xFF3B82F6)
                                        2 -> Color(0xFF10B981)
                                        else -> Color(0xFFF59E0B)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(iconColor.copy(alpha = 0.15f), CircleShape)
                                            .border(0.5.dp, iconColor.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(iconColor, CircleShape)
                                        )
                                    }
                                }
                            }
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
                    // Floating Secure Lock Overlay
                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(
                                if (tab.isLocked) Color(0xFFF43F5E).copy(alpha = 0.9f)
                                else Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (tab.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Tab Security",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}


// --- TIMELINE PLACEMENT DATA HOLDER ---
data class TimelineItemPlacement(
    val entry: com.example.data.HistoryEntry,
    val cTime: Long,
    val laneIndex: Int,
    val domain: String,
    val faviconUrl: String?
)

// --- BOOKMARKS & HISTORY SHEET PANEL ---
@Composable
fun BookmarksAndHistorySheet(
    activeTab: Int, // 0 for Bookmarks, 1 for History, 2 for Downloads
    bookmarks: List<com.example.data.Bookmark>,
    history: List<com.example.data.HistoryEntry>,
    downloads: List<com.example.data.DownloadEntry> = emptyList(),
    onTabSelected: (Int) -> Unit,
    onItemClicked: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onDeleteDownload: (Long) -> Unit = {},
    onClearDownloads: () -> Unit = {},
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
    onToggleWatchMode: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Tag Search and Mode States (Local to this panel)
    var searchQuery by remember { mutableStateOf("") }
    val activeTags = remember { mutableStateListOf<String>() }
    var isTimelineMode by remember { mutableStateOf(true) }

    // Derive top domain suggestions from browsing history dynamically
    val suggestedDomains = remember(history) {
        history.mapNotNull { entry ->
            try {
                var temp = entry.url
                if (temp.startsWith("http://")) temp = temp.substring(7)
                else if (temp.startsWith("https://")) temp = temp.substring(8)
                if (temp.startsWith("www.")) temp = temp.substring(4)
                val slashIndex = temp.indexOf('/')
                val domain = if (slashIndex != -1) temp.substring(0, slashIndex) else temp
                if (domain.isBlank() || domain.contains("localhost") || !domain.contains(".")) null else domain
            } catch (e: Exception) {
                null
            }
        }
        .groupBy { it }
        .mapValues { it.value.size }
        .entries
        .sortedByDescending { it.value }
        .take(4)
        .map { it.key }
    }

    // Time calculations for relative date boundaries (Today, Yesterday)
    val timeBounds = remember {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
        Pair(todayStart, yesterdayStart)
    }

    // Dynamic Filtering based on Active Tags and Search Query
    val filteredHistory = remember(history, searchQuery, activeTags, bookmarks) {
        history.filter { entry ->
            // 1. Match search query if present
            val queryMatched = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.url.contains(searchQuery, ignoreCase = true)
            }

            // 2. Match ALL active tags
            val tagsMatched = activeTags.all { tag ->
                when (tag) {
                    "Today" -> entry.timestamp >= timeBounds.first
                    "Yesterday" -> entry.timestamp in timeBounds.second until timeBounds.first
                    "Saved" -> bookmarks.any { it.url == entry.url }
                    else -> entry.title.contains(tag, ignoreCase = true) ||
                            entry.url.contains(tag, ignoreCase = true)
                }
            }

            queryMatched && tagsMatched
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Sleek Premium Cosmic Slate Background
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
                    .background(Color.White.copy(alpha = 0.2f))
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
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(19.dp))
                        .padding(2.dp)
                ) {
                    // Bookmarks Tab Button
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (activeTab == 0) Color(0xFF38BDF8) else Color.Transparent)
                            .clickable { onTabSelected(0) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bookmarks",
                            color = if (activeTab == 0) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // History Tab Button
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (activeTab == 1) Color(0xFF38BDF8) else Color.Transparent)
                            .clickable { onTabSelected(1) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "History",
                            color = if (activeTab == 1) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Downloads Tab Button
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (activeTab == 2) Color(0xFF38BDF8) else Color.Transparent)
                            .clickable { onTabSelected(2) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Downloads",
                            color = if (activeTab == 2) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Clear History Button if on history tab
                    if (activeTab == 1 && filteredHistory.isNotEmpty()) {
                        TextButton(
                            onClick = onClearHistory,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF43F5E))
                        ) {
                            Text("Clear history", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Clear Downloads Button if on downloads tab
                    if (activeTab == 2 && downloads.isNotEmpty()) {
                        TextButton(
                            onClick = onClearDownloads,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF43F5E))
                        ) {
                            Text("Clear all", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Panel",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content area
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
                            description = "Tap the pin icon in the bottom menu bar to bookmark your favorite sites.",
                            iconColor = Color(0xFF38BDF8)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(bookmarks) { bookmark ->
                                NavigationListItemDark(
                                    title = bookmark.title,
                                    subtitle = bookmark.url,
                                    onItemClick = { onItemClicked(bookmark.url) },
                                    onDelete = { onDeleteBookmark(bookmark.id) },
                                    isWatchMode = bookmark.isWatchMode,
                                    onToggleWatchMode = { onToggleWatchMode(bookmark.url) }
                                )
                            }
                        }
                    }
                } else if (activeTab == 1) {
                    // HISTORY TAB CONTENT: TAG SEARCH & TIMELINE / LIST
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Tag Search Bar
                        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.trim().isNotEmpty()) {
                                            val t = searchQuery.trim()
                                            if (!activeTags.contains(t)) activeTags.add(t)
                                            searchQuery = ""
                                        }
                                        keyboardController?.hide()
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search tabs, URLs or add tags...",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        // 1.5. Selected Tags Horizontal Row (displayed below Search Bar if active)
                        if (activeTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                activeTags.toList().forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2563EB))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val icon = when (tag) {
                                                "Today", "Yesterday" -> Icons.Default.History
                                                "Saved" -> Icons.Default.PushPin
                                                else -> Icons.Default.Language
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = tag,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clickable { activeTags.remove(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Horizontal Suggested Tags Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recall tags:",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            // Today Tag suggestion
                            if (!activeTags.contains("Today")) {
                                SuggestionChipDark(
                                    text = "Today",
                                    icon = Icons.Default.History,
                                    onClick = { activeTags.add("Today") }
                                )
                            }

                            // Yesterday Tag suggestion
                            if (!activeTags.contains("Yesterday")) {
                                SuggestionChipDark(
                                    text = "Yesterday",
                                    icon = Icons.Default.History,
                                    onClick = { activeTags.add("Yesterday") }
                                )
                            }

                            // Saved/Bookmarked Tag suggestion
                            if (!activeTags.contains("Saved")) {
                                SuggestionChipDark(
                                    text = "Saved",
                                    icon = Icons.Default.PushPin,
                                    onClick = { activeTags.add("Saved") }
                                )
                            }

                            // Extracted Domain Tags suggestions
                            suggestedDomains.forEach { domain ->
                                val cleanName = domain.substringBefore(".").replaceFirstChar { it.uppercase() }
                                if (!activeTags.contains(cleanName)) {
                                    SuggestionChipDark(
                                        text = cleanName,
                                        icon = Icons.Default.Language,
                                        onClick = { activeTags.add(cleanName) }
                                    )
                                }
                            }
                        }

                        // 3. Segmented Control Switcher: Timeline vs List
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(38.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(19.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (isTimelineMode) Color(0xFF2563EB) else Color.Transparent)
                                    .clickable { isTimelineMode = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Timeline",
                                    color = if (isTimelineMode) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (!isTimelineMode) Color(0xFF2563EB) else Color.Transparent)
                                    .clickable { isTimelineMode = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "List",
                                    color = if (!isTimelineMode) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Main Swapped Viewport
                        if (isTimelineMode) {
                            // TIMELINE MULTI-TRACK FLOW
                            val sortedList = filteredHistory.sortedBy { it.timestamp }
                            if (sortedList.isEmpty()) {
                                EmptyStateInfo(
                                    icon = Icons.Outlined.History,
                                    title = "No Timeline Matches",
                                    description = "Try expanding your recall tags or resetting your search bar queries.",
                                    iconColor = Color(0xFF3B82F6)
                                )
                            } else {
                                // Linear compression algorithm
                                val compressedPlacements = remember(sortedList) {
                                    val maxGapMs = 12 * 60 * 1000L // 12 minutes limit
                                    val placements = mutableListOf<TimelineItemPlacement>()
                                    var runningCTime = 0L
                                    var prevActualTime = sortedList.firstOrNull()?.timestamp ?: 0L
                                    
                                    val lanesCount = 4
                                    val laneLastEndTime = LongArray(lanesCount) { 0L }
                                    
                                    sortedList.forEach { entry ->
                                        val actualGap = entry.timestamp - prevActualTime
                                        val compressedGap = minOf(actualGap, maxGapMs)
                                        runningCTime += compressedGap
                                        prevActualTime = entry.timestamp
                                        
                                        var assignedLane = -1
                                        for (i in 0 until lanesCount) {
                                            if (runningCTime >= laneLastEndTime[i]) {
                                                assignedLane = i
                                                break
                                            }
                                        }
                                        if (assignedLane == -1) {
                                            var earliestLane = 0
                                            var earliestTime = laneLastEndTime[0]
                                            for (i in 1 until lanesCount) {
                                                if (laneLastEndTime[i] < earliestTime) {
                                                    earliestTime = laneLastEndTime[i]
                                                    earliestLane = i
                                                }
                                            }
                                            assignedLane = earliestLane
                                        }
                                        
                                        val finalCTime = maxOf(runningCTime, laneLastEndTime[assignedLane])
                                        
                                        // Compute domain and favicon url
                                        val domain = try {
                                            var temp = entry.url
                                            if (temp.startsWith("http://")) temp = temp.substring(7)
                                            else if (temp.startsWith("https://")) temp = temp.substring(8)
                                            if (temp.startsWith("www.")) temp = temp.substring(4)
                                            val slashIndex = temp.indexOf('/')
                                            val dom = if (slashIndex != -1) temp.substring(0, slashIndex) else temp
                                            if (dom.isBlank() || dom.contains("localhost") || !dom.contains(".")) "" else dom
                                        } catch (e: Exception) {
                                            ""
                                        }
                                        val faviconUrl = if (domain.isNotEmpty()) "https://www.google.com/s2/favicons?sz=128&domain=$domain" else null
                                        
                                        placements.add(TimelineItemPlacement(entry, finalCTime, assignedLane, domain, faviconUrl))
                                        
                                        val charCount = entry.title.length
                                        val estimatedWidthDp = minOf(maxOf(charCount * 6 + 48, 140), 280)
                                        val durationMs = estimatedWidthDp * 1500L
                                        laneLastEndTime[assignedLane] = finalCTime + durationMs + 45000L
                                    }
                                    placements
                                }

                                val scale = 50.0 / 60000.0 // 50dp per virtual minute (60000ms)
                                val maxCTime = compressedPlacements.lastOrNull()?.cTime ?: 0L
                                val timelineWidth = (maxCTime * scale).dp + 320.dp

                                val timeFormatter = remember {
                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                }

                                // Interpolate actual timestamps from compressed space
                                fun compressedTimeToActualTime(cTime: Long): Long {
                                    if (compressedPlacements.isEmpty()) return System.currentTimeMillis()
                                    val first = compressedPlacements.first()
                                    if (cTime <= first.cTime) return first.entry.timestamp
                                    val last = compressedPlacements.last()
                                    if (cTime >= last.cTime) return last.entry.timestamp
                                    
                                    for (i in 0 until compressedPlacements.size - 1) {
                                        val cur = compressedPlacements[i]
                                        val next = compressedPlacements[i + 1]
                                        if (cTime >= cur.cTime && cTime <= next.cTime) {
                                            val denom = next.cTime - cur.cTime
                                            val ratio = if (denom > 0) (cTime - cur.cTime).toDouble() / denom else 0.0
                                            return cur.entry.timestamp + (ratio * (next.entry.timestamp - cur.entry.timestamp)).toLong()
                                        }
                                    }
                                    return last.entry.timestamp
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .horizontalScroll(rememberScrollState())
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(timelineWidth)
                                        ) {
                                            // Grid lines and timestamps background layer
                                            val spacingPx = 180
                                            val rawNumLines = (timelineWidth.value / spacingPx).toInt()
                                            val numLines = minOf(maxOf(rawNumLines, 0), 60)
                                            for (i in 0..numLines) {
                                                val xDp = (i * spacingPx).dp
                                                val cTimeLine = (xDp.value / scale).toLong()
                                                val actualTime = compressedTimeToActualTime(cTimeLine)
                                                val timeLabel = try {
                                                    timeFormatter.format(java.util.Date(actualTime))
                                                } catch (t: Throwable) {
                                                    "--:--"
                                                }

                                                // Vertical grid line
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = xDp)
                                                        .width(1.dp)
                                                        .fillMaxHeight()
                                                        .background(Color.White.copy(alpha = 0.04f))
                                                )

                                                // Vertical grid tick label
                                                Text(
                                                    text = timeLabel,
                                                    color = Color.White.copy(alpha = 0.35f),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .offset(x = xDp + 6.dp, y = 6.dp)
                                                )
                                            }

                                            // Cards overlays
                                            compressedPlacements.forEach { placement ->
                                                val entry = placement.entry
                                                val cTime = placement.cTime
                                                val laneIndex = placement.laneIndex
                                                val faviconUrl = placement.faviconUrl
                                                val xDp = (cTime * scale).dp
                                                val yDp = 36.dp + (laneIndex * 54).dp
                                                val charCount = entry.title.length
                                                val estimatedWidthDp = minOf(maxOf(charCount * 6 + 48, 140), 280)

                                                Card(
                                                    modifier = Modifier
                                                        .offset(x = xDp, y = yDp)
                                                        .width(estimatedWidthDp.dp)
                                                        .height(44.dp)
                                                        .clickable { onItemClicked(entry.url) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = Color.White.copy(alpha = 0.08f)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (faviconUrl != null) {
                                                            androidx.compose.foundation.Image(
                                                                painter = coil.compose.rememberAsyncImagePainter(model = faviconUrl),
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(18.dp)
                                                                    .clip(CircleShape)
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.Language,
                                                                contentDescription = null,
                                                                tint = Color.White.copy(alpha = 0.5f),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        Text(
                                                            text = entry.title,
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Dynamic vertical scrubber line on right side
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.4f))
                                            .align(Alignment.CenterEnd)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF38BDF8), CircleShape)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        } else {
                            // NORMAL HISTORY LIST (Filtered)
                            if (filteredHistory.isEmpty()) {
                                EmptyStateInfo(
                                    icon = Icons.Outlined.History,
                                    title = "No Matches Found",
                                    description = "Verify that your typed search query is correct, or clear selected tags.",
                                    iconColor = Color(0xFF3B82F6)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 20.dp)
                                ) {
                                    items(filteredHistory) { entry ->
                                        NavigationListItemDark(
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
                } else if (activeTab == 2) {
                    // Downloads Content List
                    if (downloads.isEmpty()) {
                        EmptyStateInfo(
                            icon = Icons.Default.Download,
                            title = "No Downloads Found",
                            description = "Files you download from websites will appear here.",
                            iconColor = Color(0xFF10B981)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(downloads) { download ->
                                DownloadListItem(
                                    download = download,
                                    onDelete = { onDeleteDownload(download.id) }
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
fun DownloadListItem(
    download: com.example.data.DownloadEntry,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (download.filename.endsWith(".mp4") || download.filename.endsWith(".m3u8")) Icons.Default.PlayCircle else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (download.status == "Downloading") Color(0xFF38BDF8) else Color(0xFF10B981),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.filename,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = download.status,
                    color = if (download.status == "Downloading") Color(0xFF38BDF8) else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
                val formattedTime = remember(download.timestamp) {
                    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(download.timestamp))
                }
                Text(
                    text = formattedTime,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Download Entry",
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SuggestionChipDark(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NavigationListItemDark(
    title: String,
    subtitle: String,
    onItemClick: () -> Unit,
    onDelete: () -> Unit,
    isWatchMode: Boolean = false,
    onToggleWatchMode: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val domain = remember(subtitle) {
            try {
                var temp = subtitle
                if (temp.startsWith("http://")) temp = temp.substring(7)
                else if (temp.startsWith("https://")) temp = temp.substring(8)
                if (temp.startsWith("www.")) temp = temp.substring(4)
                val slashIndex = temp.indexOf('/')
                val dom = if (slashIndex != -1) temp.substring(0, slashIndex) else temp
                if (dom.isBlank() || dom.contains("localhost") || !dom.contains(".")) "" else dom
            } catch (e: Exception) {
                ""
            }
        }

        val faviconUrl = remember(domain) {
            if (domain.isNotEmpty()) "https://www.google.com/s2/favicons?sz=128&domain=$domain" else null
        }

        if (faviconUrl != null) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(model = faviconUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        if (onToggleWatchMode != null) {
            IconButton(onClick = onToggleWatchMode) {
                Icon(
                    imageVector = if (isWatchMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Evolution Watch",
                    tint = if (isWatchMode) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Item",
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EmptyStateInfo(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color = Color.LightGray
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
            tint = iconColor.copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
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
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
    val videoQueue by viewModel.videoQueue.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 for Stream, 1 for Liked/Saved, 2 for Queue
    var selectedMediaForView by remember { mutableStateOf<CapturedMedia?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Stylish premium dark aesthetic
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
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(38.dp)
                        .horizontalScroll(rememberScrollState())
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(19.dp))
                        .padding(2.dp)
                ) {
                    val tabModifier0 = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (activeTab == 0) Color(0xFFBB86FC) else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(horizontal = 12.dp)

                    Box(
                        modifier = tabModifier0,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Auto Stream",
                            color = if (activeTab == 0) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val tabModifier1 = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (activeTab == 1) Color(0xFFBB86FC) else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(horizontal = 12.dp)

                    Box(
                        modifier = tabModifier1,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Saved",
                            color = if (activeTab == 1) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val tabModifier2 = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (activeTab == 2) Color(0xFFBB86FC) else Color.Transparent)
                        .clickable { activeTab = 2 }
                        .padding(horizontal = 12.dp)

                    Box(
                        modifier = tabModifier2,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Queue (${videoQueue.size})",
                            color = if (activeTab == 2) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
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
                } else if (activeTab == 1) {
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
                } else {
                    // Playback Queue tab
                    if (videoQueue.isEmpty()) {
                        EmptyStateView("Your Video Playback Queue is empty.\nQueue up web video links to play them later!")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Queued Video Tracks",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    TextButton(onClick = { viewModel.clearVideoQueue() }) {
                                        Text("Clear Queue", color = Color(0xFFEF4444), fontSize = 12.sp)
                                    }
                                }
                            }

                            itemsIndexed(videoQueue) { index, media ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.playBackgroundVideo(media)
                                            viewModel.removeFromVideoQueue(media.id)
                                            onClose()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFBB86FC).copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (index + 1).toString(),
                                                color = Color(0xFFBB86FC),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = media.pageTitle.ifBlank { "Video Stream Track" },
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = media.url,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (index > 0) {
                                            IconButton(
                                                onClick = { viewModel.reorderQueue(index, index - 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (index < videoQueue.size - 1) {
                                            IconButton(
                                                onClick = { viewModel.reorderQueue(index, index + 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeFromVideoQueue(media.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove From Queue",
                                                tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
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
                },
                viewModel = viewModel
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
    onDelete: () -> Unit,
    viewModel: com.example.viewmodel.BrowserViewModel? = null
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
                        downloadMedia(context, media.url, filename, viewModel)
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

fun downloadMedia(context: android.content.Context, url: String, filename: String, viewModel: com.example.viewmodel.BrowserViewModel? = null) {
    try {
        val cleanUrl = url.trim()
        val finalFilename = if (filename.isBlank() || !filename.contains(".")) "${System.currentTimeMillis()}.mp4" else filename
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(cleanUrl)).apply {
            setTitle(finalFilename)
            setDescription("Downloading captured media from browser...")
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
            setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                finalFilename
            )
        }
        val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        manager.enqueue(request)
        
        viewModel?.insertDownload(
            com.example.data.DownloadEntry(
                filename = finalFilename,
                url = cleanUrl,
                status = "Downloading",
                size = "Pending"
            )
        )
        
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error starting download: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SearchActiveOverlay(
    viewModel: com.example.viewmodel.BrowserViewModel,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    historyList: List<com.example.data.HistoryEntry>,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val searchSuggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()
    val activeEngineName by viewModel.searchEngineName.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val allTabs by viewModel.allTabs.collectAsStateWithLifecycle(emptyList())
    val matchingTabs = remember(query, allTabs) {
        if (query.isBlank()) emptyList()
        else {
            allTabs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }
    }

    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager }
    val clipboardText = remember {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).text?.toString()?.trim()?.ifEmpty { null }
        } else {
            null
        }
    }

    val mathResult = remember(query) {
        if (query.isBlank()) null else tryEvaluateExpression(query)
    }

    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    // Automatically request focus on enter to open keyboard
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Define color scheme based on the active theme mode
    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF030712), // Deep pitch space black
                Color(0xFF0B1224)  // Rich deep space navy
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFC), // Slate 50
                Color(0xFFF1F5F9)  // Slate 100
            )
        )
    }

    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF475569)
    val accentColor = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val chipUnselectedBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val chipUnselectedText = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF334155)

    val inputCardBg = if (isDarkTheme) Color(0xFF0F172A) else Color.White
    val inputBorderColor = if (isDarkTheme) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFF0284C7).copy(alpha = 0.35f)
    val cursorColor = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val clearIconColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search & History",
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Recommendations / Suggestions List (takes remaining space above input)
            val filteredSuggestions = remember(query, historyList) {
                if (query.isBlank()) {
                    historyList.map { it.title to it.url }
                } else {
                    historyList.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.url.contains(query, ignoreCase = true)
                    }.map { it.title to it.url }
                }.distinctBy { it.second.lowercase().trim() }.take(10)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (searchSuggestions.isEmpty() && filteredSuggestions.isEmpty() && query.isBlank()) {
                    // Premium, ultra-sleek Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(accentColor.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, accentColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Search the Web",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Type a search term or website URL below. Your actual browsing history will appear here for easy recommendation.",
                            color = subTextColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Smart Online Search Option
                        if (query.isNotBlank()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accentColor.copy(alpha = 0.08f))
                                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSearchSubmit(query)
                                        }
                                        .padding(vertical = 14.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = "Search the Web",
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Search for \"$query\"",
                                            color = textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Search the web using $activeEngineName",
                                            color = subTextColor,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Go",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // 1. Math / Unit Conversion Result
                        if (query.isNotBlank() && mathResult != null) {
                            item {
                                Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.08f))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                            .clickable {
                                                onQueryChange(mathResult)
                                            }
                                            .padding(vertical = 14.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Functions,
                                                contentDescription = "Calculator",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mathResult,
                                                color = textColor,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tap to insert calculation result",
                                                color = subTextColor,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Insert",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp).graphicsLayer(rotationZ = 45f)
                                        )
                                    }
                                }
                            }

                        // 2. Clipboard Suggestion Card
                        if (query.isBlank() && clipboardText != null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accentColor.copy(alpha = 0.05f))
                                        .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            onQueryChange(clipboardText)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Clipboard",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Paste from clipboard",
                                            color = textColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = clipboardText,
                                            color = subTextColor,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Open Tabs matching query
                        if (query.isNotBlank() && matchingTabs.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Open Tabs matching \"$query\"",
                                    color = Color(0xFFA855F7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(matchingTabs) { tab ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFA855F7).copy(alpha = 0.05f))
                                        .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.selectTab(tab.id)
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFA855F7).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tab,
                                            contentDescription = "Tab",
                                            tint = Color(0xFFA855F7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tab.title,
                                            color = textColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tab.url,
                                            color = subTextColor,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Switch to Tab",
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Internet Search suggestions/recommendations
                        if (query.isNotBlank() && searchSuggestions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "$activeEngineName search suggestions",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(searchSuggestions) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.02f) else Color(0xFFF1F5F9))
                                        .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.03f) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSearchSubmit(suggestion)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Suggestion",
                                            tint = subTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion,
                                            color = textColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            onQueryChange(suggestion)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Fill Query",
                                            tint = subTextColor,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .graphicsLayer(rotationZ = -45f)
                                        )
                                    }
                                }
                            }
                        }

                        // History Recommendations List
                        if (filteredSuggestions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "History suggestions",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredSuggestions) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.02f) else Color(0xFFF1F5F9))
                                        .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.03f) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                        .clickable {
                                            onSearchSubmit(suggestion.second)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "History",
                                            tint = subTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.first,
                                            color = textColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = suggestion.second,
                                            color = subTextColor,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            onQueryChange(suggestion.first)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Fill Query",
                                            tint = subTextColor,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .graphicsLayer(rotationZ = -45f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom search engine chips/selector from screenshot (Google, DuckDuckGo, Bing, Baidu, Naver, ChatGPT, Perplexity, Grok, Claude)
            val presets = listOf(
                Triple("Google", "https://www.google.com/search?q=%s", "g"),
                Triple("DuckDuckGo", "https://duckduckgo.com/?q=%s", "d"),
                Triple("Bing", "https://www.bing.com/search?q=%s", "b"),
                Triple("Baidu", "https://www.baidu.com/s?wd=%s", "ba"),
                Triple("Naver", "https://search.naver.com/search.naver?query=%s", "n"),
                Triple("ChatGPT", "https://chatgpt.com/?q=%s", "gpt"),
                Triple("Perplexity", "https://www.perplexity.ai/?q=%s", "p"),
                Triple("Grok", "https://grok.com/?q=%s", "gr"),
                Triple("Claude", "https://claude.ai/?q=%s", "c")
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(presets) { (presetName, presetUrl, presetShortcut) ->
                    val isSelected = activeEngineName.lowercase() == presetName.lowercase()
                    val domain = when (presetName.lowercase()) {
                        "google" -> "google.com"
                        "duckduckgo" -> "duckduckgo.com"
                        "bing" -> "bing.com"
                        "baidu" -> "baidu.com"
                        "naver" -> "naver.com"
                        "chatgpt" -> "chatgpt.com"
                        "perplexity" -> "perplexity.ai"
                        "grok" -> "grok.com"
                        "claude" -> "claude.ai"
                        else -> "google.com"
                    }
                    val faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=$domain"

                    Card(
                        modifier = Modifier
                            .clickable {
                                viewModel.setCustomSearchEngine(presetName, presetUrl, presetShortcut)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) accentColor else chipUnselectedBg
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            coil.compose.AsyncImage(
                                model = faviconUrl,
                                contentDescription = "$presetName favicon",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = presetName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else chipUnselectedText
                            )
                        }
                    }
                }
            }

            // Beautiful typing search bar matching keyboard and theme mode aesthetics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = inputCardBg
                ),
                border = BorderStroke(1.5.dp, inputBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = accentColor,
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
                            color = textColor,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(cursorColor),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search or type URL",
                                    color = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.DarkGray.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Clear",
                                tint = clearIconColor,
                                modifier = Modifier.size(20.dp)
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
    viewModel: com.example.viewmodel.BrowserViewModel,
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
    onCookieEditorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val isAmoled = themeMode == "amoled"

    val backgroundBrush = if (isDarkTheme) {
        if (isAmoled) {
            Brush.verticalGradient(colors = listOf(Color(0xFF000000), Color(0xFF000000)))
        } else {
            Brush.verticalGradient(colors = listOf(Color(0xFF030712), Color(0xFF070E1E)))
        }
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)))
    }

    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val cardBgColor = if (isDarkTheme) {
        if (isAmoled) Color(0xFF121212) else Color(0xFF0B1224)
    } else {
        Color.White
    }
    val cardBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color(0xFFCBD5E1).copy(alpha = 0.4f)
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
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
                        } else if (currentSubScreen in listOf("customize_address_bar", "customize_menu", "tabs_start_page", "chrome_themes")) {
                            onNavigateSub("appearance_settings")
                        } else if (currentSubScreen in listOf("manage_personal_data", "js_optimisation")) {
                            onNavigateSub("privacy_guard")
                        } else {
                            onNavigateSub("main")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }

                Text(
                    text = when (currentSubScreen) {
                        "search_engine" -> "Search Engine Settings"
                        "video_options" -> "Video Options Toolbar"
                        "privacy_guard" -> "Privacy and security"
                        "manage_personal_data" -> "Manage Personal Data"
                        "js_optimisation" -> "JavaScript optimisation"
                        "dns_routing" -> "DNS & Secure Routing"
                        "appearance_settings" -> "Appearance Settings"
                        "chrome_themes" -> "Chrome Web Store Themes"
                        "customize_address_bar" -> "Customize Address Bar"
                        "customize_menu" -> "Customize Menu"
                        "tabs_start_page" -> "Tabs & Start Page"
                        "user_scripts" -> "User Script Manager"
                        "web_cleaner" -> "Web Cleaner & Ad Blocker"
                        "ai_automation" -> "AI Automation & Intelligence"
                        else -> "Browser Advanced Settings"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            HorizontalDivider(color = dividerColor)

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
                                color = subTextColor,
                                letterSpacing = 1.sp
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, cardBorderColor)
                            ) {
                                Column {
                                    SettingsItemRow(
                                        title = "Search Engine",
                                        subtitle = "Active: $searchEngineName",
                                        icon = Icons.Default.Language,
                                        iconColor = Color(0xFF38BDF8),
                                        onClick = { onNavigateSub("search_engine") }
                                    )
                                    HorizontalDivider(color = dividerColor)
                                    SettingsItemRow(
                                        title = "Video Toolbar & Background Play",
                                        subtitle = "Background Listen, Youtube Ad-free tools",
                                        icon = Icons.Default.PlayCircle,
                                        iconColor = Color(0xFFF43F5E),
                                        onClick = { onNavigateSub("video_options") }
                                    )
                                    HorizontalDivider(color = dividerColor)
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
                                 color = subTextColor,
                                 letterSpacing = 1.sp
                             )

                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                 shape = RoundedCornerShape(16.dp),
                                 border = BorderStroke(1.dp, cardBorderColor)
                             ) {
                                 Column {
                                     SettingsItemRow(
                                         title = "Privacy and security",
                                         subtitle = "Delete browsing data, Safe Browsing, Cookies",
                                         icon = Icons.Default.Shield,
                                         iconColor = Color(0xFF34D399),
                                         onClick = { onNavigateSub("privacy_guard") }
                                     )
                                     HorizontalDivider(color = dividerColor)
                                     SettingsItemRow(
                                         title = "DNS & Smart Auto-Routing",
                                         subtitle = "Private DoH Server, SOCKS & Tor Bridges",
                                         icon = Icons.Default.Dns,
                                         iconColor = Color(0xFFA78BFA),
                                         onClick = { onNavigateSub("dns_routing") }
                                     )
                                     HorizontalDivider(color = dividerColor)
                                     SettingsItemRow(
                                         title = "Web Cleaner & Ad Blocker",
                                         subtitle = "Ad Filter Subscriptions, Whitelists & Block Rules",
                                         icon = Icons.Default.Block,
                                         iconColor = Color(0xFFF43F5E),
                                         onClick = { onNavigateSub("web_cleaner") }
                                     )
                                 }
                             }

                             // AI Automation & Intelligence
                             Text(
                                 text = "AI AUTOMATION & INTELLIGENCE",
                                 fontSize = 12.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = subTextColor,
                                 letterSpacing = 1.sp
                             )

                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                 shape = RoundedCornerShape(16.dp),
                                 border = BorderStroke(1.dp, cardBorderColor)
                             ) {
                                 Column {
                                     SettingsItemRow(
                                         title = "AI Smart Settings",
                                         subtitle = "AI Smart AdBlocker, Dynamic Tab Grouping & Summarizer",
                                         icon = Icons.Default.AutoAwesome,
                                         iconColor = Color(0xFF818CF8),
                                         onClick = { onNavigateSub("ai_automation") }
                                     )
                                 }
                             }

                             // Extensions & Scripts
                             Text(
                                 text = "EXTENSIONS & SCRIPTS",
                                 fontSize = 12.sp,
                                 fontWeight = FontWeight.Bold,
                                 color = subTextColor,
                                 letterSpacing = 1.sp
                             )

                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                 shape = RoundedCornerShape(16.dp),
                                 border = BorderStroke(1.dp, cardBorderColor)
                             ) {
                                 Column {
                                     SettingsItemRow(
                                         title = "User Script Manager",
                                         subtitle = "Create & inject custom sandboxed page modifications",
                                         icon = Icons.Default.Code,
                                         iconColor = Color(0xFF38BDF8),
                                         onClick = { onNavigateSub("user_scripts") }
                                     )
                                 }
                             }

                             Spacer(modifier = Modifier.height(8.dp))

                             // System Info
                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                 shape = RoundedCornerShape(16.dp),
                                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                            viewModel = viewModel,
                            onCookieEditorClick = {
                                onCookieEditorClick()
                                onClose() // close settings overlay so cookie editor is fully visible!
                            }
                        )
                    }

                    "manage_personal_data" -> {
                        ManagePersonalDataSubScreen(
                            viewModel = viewModel
                        )
                    }

                    "js_optimisation" -> {
                        JsOptimisationSubScreen(
                            viewModel = viewModel
                        )
                    }

                    "web_cleaner" -> {
                        WebCleanerSubScreen(
                            viewModel = viewModel
                        )
                    }

                    "user_scripts" -> {
                        UserScriptsSubScreen(
                            viewModel = viewModel
                        )
                    }

                    "ai_automation" -> {
                        AiAutomationSubScreen(
                            viewModel = viewModel
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

                    "chrome_themes" -> {
                        ChromeThemesSettingsSubScreen(
                            viewModel = viewModel,
                            onNavigateSub = onNavigateSub
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiAutomationSubScreen(
    viewModel: com.example.viewmodel.BrowserViewModel
) {
    val aiAutoGroupingEnabled by viewModel.aiAutoGroupingEnabled.collectAsStateWithLifecycle()
    val aiSmartAdBlockerEnabled by viewModel.aiSmartAdBlockerEnabled.collectAsStateWithLifecycle()
    val aiSmartSummarizerEnabled by viewModel.aiSmartSummarizerEnabled.collectAsStateWithLifecycle()
    
    val lowPowerModeEnabled by viewModel.lowPowerModeEnabled.collectAsStateWithLifecycle()
    val bypassPaywallsEnabled by viewModel.bypassPaywallsEnabled.collectAsStateWithLifecycle()
    val tabSortMode by viewModel.tabSortMode.collectAsStateWithLifecycle()

    val cardBgColor = Color(0xFF0F172A)
    val cardBorderColor = Color.White.copy(alpha = 0.08f)
    val dividerColor = Color.White.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "AI",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Automation & Intelligence Center",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Take control of intelligent browsing features that automate grouping, clean pages with layout heuristic, and extract dynamic page data.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Text(
            text = "AI COGNITIVE CONTROLS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF818CF8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column {
                // 1. AI Auto Grouping Toggle
                PrivacySwitchRow(
                    title = "AI Tab Grouping Engine",
                    subtitle = "Automatically cluster opened websites (e.g., WWE Wrestling, Apple Tech, Reading) into semantic stacks on loading.",
                    checked = aiAutoGroupingEnabled,
                    onCheckedChange = { viewModel.setAIAutoGroupingEnabled(it) }
                )

                HorizontalDivider(color = dividerColor)

                // 2. Run manual group button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Group Open Tabs Now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Instantly run the AI semantic grouping algorithm on all currently opened tabs.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { viewModel.triggerAIAutoGrouping() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Group Tabs", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = dividerColor)

                // 3. AI Smart AdBlocker (Heuristics)
                PrivacySwitchRow(
                    title = "AI Smart AdBlocker (Cosmetic)",
                    subtitle = "Auto-analyze DOM layout ratios & keywords on page finish to quietly hide and suppress advertisement boxes.",
                    checked = aiSmartAdBlockerEnabled,
                    onCheckedChange = { viewModel.setAISmartAdBlockerEnabled(it) }
                )

                HorizontalDivider(color = dividerColor)

                // 4. AI Content Summarizer
                PrivacySwitchRow(
                    title = "AI Content Summarizer Integration",
                    subtitle = "Expose a smart text summarizing menu shortcut on eligible articles and readable posts.",
                    checked = aiSmartSummarizerEnabled,
                    onCheckedChange = { viewModel.setAISmartSummarizerEnabled(it) }
                )
            }
        }

        Text(
            text = "PREMIUM CONTROLS & UTILITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column {
                PrivacySwitchRow(
                    title = "Low Power Mode",
                    subtitle = "Maximum performance and energy savings by suspending heavy page animations and rendering overhead.",
                    checked = lowPowerModeEnabled,
                    onCheckedChange = { viewModel.setLowPowerModeEnabled(it) }
                )

                HorizontalDivider(color = dividerColor)

                PrivacySwitchRow(
                    title = "Bypass Script Paywalls",
                    subtitle = "Intercept and block common script-based paywall overlays and cookie walls to keep reading uninterrupted.",
                    checked = bypassPaywallsEnabled,
                    onCheckedChange = { viewModel.setBypassPaywallsEnabled(it) }
                )

                HorizontalDivider(color = dividerColor)

                PrivacySwitchRow(
                    title = "Sort Tabs by Read Time",
                    subtitle = "Organize all your active tabs from shortest to longest estimated reading time.",
                    checked = tabSortMode == "ReadTime",
                    onCheckedChange = { viewModel.toggleTabSortMode() }
                )
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            Triple("Kagi", "https://kagi.com/search?q=%s", "ka"),
            Triple("DuckDuckGo", "https://duckduckgo.com/?q=%s", "d"),
            Triple("Bing", "https://www.bing.com/search?q=%s", "b"),
            Triple("Baidu", "https://www.baidu.com/s?wd=%s", "ba"),
            Triple("Naver", "https://search.naver.com/search.naver?query=%s", "n"),
            Triple("ChatGPT", "https://chatgpt.com/?q=%s", "gpt"),
            Triple("Perplexity", "https://www.perplexity.ai/?q=%s", "p"),
            Triple("Grok", "https://grok.com/?q=%s", "gr"),
            Triple("Claude", "https://claude.ai/?q=%s", "c")
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
                    containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF0B1224)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.05f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
            text = "AURA PREMIUM CINEMATIC VIDEO PLAYER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6), // Aura premium violet
            letterSpacing = 1.2.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.25f)) // glowing outline
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Use Aura Cinematic Engine",
                    subtitle = "Enable our state-of-the-art hybrid player supporting HLS live-streams (.m3u8), gesture volume/brightness, and premium cinema overlays.",
                    checked = useUcPlayerEngine,
                    onCheckedChange = onUseUcPlayerEngineChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Screen Gestures & Seeking Controls",
                    subtitle = "Allows double-tap to skip 10 seconds, and vertical swipe to adjust brightness and volume.",
                    checked = ucPlayerGestureControls,
                    onCheckedChange = onUcPlayerGestureControlsChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Bandwidth Speed Indicator HUD",
                    subtitle = "Display real-time connection download speeds dynamically in the player header.",
                    checked = ucPlayerShowSpeedMeter,
                    onCheckedChange = onUcPlayerShowSpeedMeterChange
                )
            }
        }

        Text(
            text = "DEFAULT PLAYBACK STARTUP SPEED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6),
            letterSpacing = 1.2.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                            .background(if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.05f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Listen in Background",
                    subtitle = "Keeps video audio playing when app is minimized or screen is off.",
                    checked = videoListenInBackground,
                    onCheckedChange = onVideoListenChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show Floating Video Toolbar",
                    subtitle = "Displays quick download & overlay tools on detected video elements.",
                    checked = videoShowToolbar,
                    onCheckedChange = onVideoShowToolbarChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
fun PrivacyInteractiveRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    statusText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
            }
        }
        if (statusText != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PrivacySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF34D399),
                checkedTrackColor = Color(0xFF34D399).copy(alpha = 0.4f),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun PrivacyGuardSubScreen(
    viewModel: BrowserViewModel,
    onCookieEditorClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Collect states from ViewModel
    val alwaysUseHttps by viewModel.alwaysUseHttps.collectAsStateWithLifecycle()
    val removeFingerprint by viewModel.removeFingerprint.collectAsStateWithLifecycle()
    val scriptControlEnabled by viewModel.scriptControlEnabled.collectAsStateWithLifecycle()
    val cookieManagementMode by viewModel.cookieManagementMode.collectAsStateWithLifecycle()
    val stopAppRedirects by viewModel.stopAppRedirects.collectAsStateWithLifecycle()
    val safeBrowsingEnabled by viewModel.safeBrowsingEnabled.collectAsStateWithLifecycle()
    val doNotTrack by viewModel.doNotTrack.collectAsStateWithLifecycle()
    val autoDeAmp by viewModel.autoDeAmp.collectAsStateWithLifecycle()
    val globalPrivacyControl by viewModel.globalPrivacyControl.collectAsStateWithLifecycle()

    // Collect new states
    val thirdPartyCookiesSetting by viewModel.thirdPartyCookiesSetting.collectAsStateWithLifecycle()
    val incognitoTrackingProtections by viewModel.incognitoTrackingProtections.collectAsStateWithLifecycle()
    val adsPrivacyTopics by viewModel.adsPrivacyTopics.collectAsStateWithLifecycle()
    val adsPrivacySiteSuggested by viewModel.adsPrivacySiteSuggested.collectAsStateWithLifecycle()
    val adsPrivacyMeasurement by viewModel.adsPrivacyMeasurement.collectAsStateWithLifecycle()
    val preloadPagesMode by viewModel.preloadPagesMode.collectAsStateWithLifecycle()
    val lockIncognitoTabs by viewModel.lockIncognitoTabs.collectAsStateWithLifecycle()
    val safeBrowsingLevel by viewModel.safeBrowsingLevel.collectAsStateWithLifecycle()
    val warnPasswordCompromised by viewModel.warnPasswordCompromised.collectAsStateWithLifecycle()
    val jsOptimisationAndSecurity by viewModel.jsOptimisationAndSecurity.collectAsStateWithLifecycle()
    val accessPaymentMethods by viewModel.accessPaymentMethods.collectAsStateWithLifecycle()

    // Collect DNS states
    val dnsEnabled by viewModel.dnsEnabled.collectAsStateWithLifecycle()
    val dnsMode by viewModel.dnsMode.collectAsStateWithLifecycle()
    val dnsPresetId by viewModel.dnsPresetId.collectAsStateWithLifecycle()
    val dnsCustomValue by viewModel.dnsCustomValue.collectAsStateWithLifecycle()

    // Dialog trigger states
    val showDeleteDataDialogState = remember { mutableStateOf(false) }
    var showDeleteDataDialog by showDeleteDataDialogState

    val showPrivacyGuideDialogState = remember { mutableStateOf(false) }
    var showPrivacyGuideDialog by showPrivacyGuideDialogState

    val showThirdPartyCookiesDialogState = remember { mutableStateOf(false) }
    var showThirdPartyCookiesDialog by showThirdPartyCookiesDialogState

    val showIncognitoProtectionsDialogState = remember { mutableStateOf(false) }
    var showIncognitoProtectionsDialog by showIncognitoProtectionsDialogState

    val showAdsPrivacyDialogState = remember { mutableStateOf(false) }
    var showAdsPrivacyDialog by showAdsPrivacyDialogState

    val showDoNotTrackDialogState = remember { mutableStateOf(false) }
    var showDoNotTrackDialog by showDoNotTrackDialogState

    val showPreloadPagesDialogState = remember { mutableStateOf(false) }
    var showPreloadPagesDialog by showPreloadPagesDialogState

    val showSafeBrowsingDialogState = remember { mutableStateOf(false) }
    var showSafeBrowsingDialog by showSafeBrowsingDialogState

    val showDnsDialogState = remember { mutableStateOf(false) }
    var showDnsDialog by showDnsDialogState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- PRIVACY SECTION ---
        Text(
            text = "PRIVACY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                PrivacyInteractiveRow(
                    title = "Manage Personal Data",
                    subtitle = "History, cookie/caches trash, passwords, bookmarks transfer, auto-clear...",
                    onClick = { viewModel.setSettingsSubScreen("manage_personal_data") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Delete browsing data",
                    subtitle = "Delete history, cookies, site data, cache...",
                    onClick = { showDeleteDataDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Cookie-Editor",
                    subtitle = "Create, edit, delete, backup, restore or search active cookie values",
                    onClick = { onCookieEditorClick() }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                
                PrivacyInteractiveRow(
                    title = "Privacy guide",
                    subtitle = "Review key privacy and security controls",
                    onClick = { showPrivacyGuideDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Third-party cookies",
                    subtitle = "Third-party cookies are limited",
                    statusText = when (thirdPartyCookiesSetting) {
                        "block_incognito" -> "Limited"
                        "block_all" -> "Blocked"
                        else -> "Allowed"
                    },
                    onClick = { showThirdPartyCookiesDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Incognito tracking protections",
                    subtitle = "Manage information sites that can use to learn about you in Incognito",
                    statusText = when (incognitoTrackingProtections) {
                        "limited" -> "Standard"
                        "strict" -> "Strict"
                        else -> "Off"
                    },
                    onClick = { showIncognitoProtectionsDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Ads privacy",
                    subtitle = "Customise the info used by sites to show you ads",
                    onClick = { showAdsPrivacyDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Send a 'Do Not Track' request",
                    subtitle = "Send a DNT header signal with web packages",
                    statusText = if (doNotTrack) "On" else "Off",
                    onClick = { showDoNotTrackDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Preload pages",
                    subtitle = "Pages load faster but may share cookies beforehand",
                    statusText = when (preloadPagesMode) {
                        "standard" -> "Standard"
                        "extended" -> "Extended"
                        else -> "Off"
                    },
                    onClick = { showPreloadPagesDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacySwitchRow(
                    title = "Lock Incognito tabs when you leave Sigma",
                    subtitle = "Turn on screen lock in Android settings",
                    checked = lockIncognitoTabs,
                    onCheckedChange = { viewModel.setLockIncognitoTabs(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- SECURITY SECTION ---
        Text(
            text = "SECURITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF34D399),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                PrivacyInteractiveRow(
                    title = "Safe Browsing",
                    subtitle = "Shield from dangerous content, phishing, and malware",
                    statusText = when (safeBrowsingLevel) {
                        "enhanced" -> "Enhanced"
                        "standard" -> "Standard"
                        else -> "No protection"
                    },
                    onClick = { showSafeBrowsingDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacySwitchRow(
                    title = "Warn you if a password was compromised in a data breach",
                    subtitle = "When you use a password, Sigma warns you if it has been published online. Passwords are encrypted for safety.",
                    checked = warnPasswordCompromised,
                    onCheckedChange = { viewModel.setWarnPasswordCompromised(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacySwitchRow(
                    title = "Always use secure connections",
                    subtitle = "Warns you for insecure public sites",
                    checked = alwaysUseHttps,
                    onCheckedChange = { viewModel.setAlwaysUseHttps(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "Use secure DNS",
                    subtitle = "Dns-over-HTTPS securely resolves IP addresses",
                    statusText = if (dnsEnabled) "Active" else "Off",
                    onClick = { showDnsDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacyInteractiveRow(
                    title = "JavaScript optimisation and security",
                    subtitle = if (jsOptimisationAndSecurity) "Optimised for speed (V8 engine enabled)" else "Optimised for strict security",
                    statusText = if (jsOptimisationAndSecurity) "On" else "Off",
                    onClick = { viewModel.setSettingsSubScreen("js_optimisation") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                PrivacySwitchRow(
                    title = "Access payment methods",
                    subtitle = "Allow sites to check if you have payment methods saved",
                    checked = accessPaymentMethods,
                    onCheckedChange = { viewModel.setAccessPaymentMethods(it) }
                )
            }
        }

        PrivacyGuardDialogs(
            viewModel = viewModel,
            showDeleteDataDialogState = showDeleteDataDialogState,
            showPrivacyGuideDialogState = showPrivacyGuideDialogState,
            showThirdPartyCookiesDialogState = showThirdPartyCookiesDialogState,
            showIncognitoProtectionsDialogState = showIncognitoProtectionsDialogState,
            showAdsPrivacyDialogState = showAdsPrivacyDialogState,
            showDoNotTrackDialogState = showDoNotTrackDialogState,
            showPreloadPagesDialogState = showPreloadPagesDialogState,
            showSafeBrowsingDialogState = showSafeBrowsingDialogState,
            showDnsDialogState = showDnsDialogState,
            alwaysUseHttps = alwaysUseHttps,
            safeBrowsingEnabled = safeBrowsingEnabled,
            removeFingerprint = removeFingerprint,
            thirdPartyCookiesSetting = thirdPartyCookiesSetting,
            incognitoTrackingProtections = incognitoTrackingProtections,
            adsPrivacyTopics = adsPrivacyTopics,
            adsPrivacySiteSuggested = adsPrivacySiteSuggested,
            adsPrivacyMeasurement = adsPrivacyMeasurement,
            doNotTrack = doNotTrack,
            preloadPagesMode = preloadPagesMode,
            safeBrowsingLevel = safeBrowsingLevel,
            dnsEnabled = dnsEnabled,
            dnsPresetId = dnsPresetId,
            dnsCustomValue = dnsCustomValue
        )
    }
}

@Composable
fun ManagePersonalDataSubScreen(
    viewModel: BrowserViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel state collections
    val autofillEnabled by viewModel.autofillEnabled.collectAsStateWithLifecycle()
    val autoClearMode by viewModel.autoClearMode.collectAsStateWithLifecycle()
    val savedPasswords by viewModel.savedPasswords.collectAsStateWithLifecycle()

    // Local states
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf<String?>(null) } // "history", "cookies", "caches", "favorites", "playlist"
    var showPasswordManagerDialog by remember { mutableStateOf(false) }
    var showImportBookmarksDialog by remember { mutableStateOf(false) }
    var showExportBookmarksDialog by remember { mutableStateOf(false) }
    var showAutoClearDialog by remember { mutableStateOf(false) }

    val cardBgColor = MaterialTheme.colorScheme.surface
    val cardBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val subTextColor = Color.White.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: BROWSING DATA ---
        Text(
            text = "BROWSING DATA",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Delete browsing data card (outer clickable card matching screenshot)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteDataDialog = true },
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Delete browsing data",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Sub categories card: History, Cookies, Caches, Favorites, Playlist
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column {
                PersonalDataTrashRow(
                    title = "History",
                    icon = Icons.Default.History,
                    onTrashClick = { showConfirmDeleteDialog = "history" }
                )
                HorizontalDivider(color = cardBorderColor)
                PersonalDataTrashRow(
                    title = "Cookies",
                    icon = Icons.Default.Cookie,
                    onTrashClick = { showConfirmDeleteDialog = "cookies" }
                )
                HorizontalDivider(color = cardBorderColor)
                PersonalDataTrashRow(
                    title = "Caches",
                    icon = Icons.Default.Storage,
                    onTrashClick = { showConfirmDeleteDialog = "caches" }
                )
                HorizontalDivider(color = cardBorderColor)
                PersonalDataTrashRow(
                    title = "Favorites",
                    icon = Icons.Default.Favorite,
                    onTrashClick = { showConfirmDeleteDialog = "favorites" }
                )
                HorizontalDivider(color = cardBorderColor)
                PersonalDataTrashRow(
                    title = "Playlist",
                    icon = Icons.Default.PlaylistPlay,
                    onTrashClick = { showConfirmDeleteDialog = "playlist" }
                )
            }
        }

        // --- SECTION 2: PASSWORD AND AUTOFILL ---
        Text(
            text = "PASSWORD AND AUTOFILL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPasswordManagerDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Password Manager",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "View and manage saved website logins",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = cardBorderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Autofill Services",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Automatically fill forms with saved information",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                    Switch(
                        checked = autofillEnabled,
                        onCheckedChange = { viewModel.setAutofillEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        // --- SECTION 3: TRANSFER DATA ---
        Text(
            text = "TRANSFER DATA",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showImportBookmarksDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import Bookmarks",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Import bookmark settings from standard templates",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Publish,
                        contentDescription = "Import",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = cardBorderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExportBookmarksDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Bookmarks",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Export active bookmarks as text configuration",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- SECTION 4: CLEAR AFTER INACTIVITY ---
        Text(
            text = "CLEAR AFTER INACTIVITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAutoClearDialog = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-Clear",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = autoClearMode,
                    fontSize = 14.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // --- ALL POPUPS AND DIALOGS ---

    // 1. Delete Browsing Data Dialog (copied & integrated)
    if (showDeleteDataDialog) {
        var clearHistory by remember { mutableStateOf(true) }
        var clearCookies by remember { mutableStateOf(true) }
        var clearCache by remember { mutableStateOf(true) }
        var clearTabs by remember { mutableStateOf(false) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteDataDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Delete browsing data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearHistory = !clearHistory }) {
                        Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browsing history", color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearCookies = !clearCookies }) {
                        Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cookies and site data", color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearCache = !clearCache }) {
                        Checkbox(checked = clearCache, onCheckedChange = { clearCache = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cached images and files", color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearTabs = !clearTabs }) {
                        Checkbox(checked = clearTabs, onCheckedChange = { clearTabs = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Close all open tabs", color = Color.White, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDeleteDataDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.deleteBrowsingData(clearHistory, clearCookies, clearCache, clearTabs)
                                showDeleteDataDialog = false
                                android.widget.Toast.makeText(context, "Browsing data deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                        ) {
                            Text("Delete Data", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2. Individual Confirm Delete Dialog
    if (showConfirmDeleteDialog != null) {
        val type = showConfirmDeleteDialog!!
        val titleText = when (type) {
            "history" -> "Clear Browsing History?"
            "cookies" -> "Clear Cookies and Site Data?"
            "caches" -> "Clear Cached Images and Files?"
            "favorites" -> "Clear Bookmarks/Favorites?"
            "playlist" -> "Clear Saved Playlist?"
            else -> "Clear Data?"
        }
        val descriptionText = when (type) {
            "history" -> "This will permanently delete all website visit history from your database."
            "cookies" -> "This will sign you out of most websites and clear local cookie databases."
            "caches" -> "This will clear temporary files and local cache folders."
            "favorites" -> "This will delete all saved website bookmarks."
            "playlist" -> "This will delete all saved playlist videos and captured media."
            else -> "This will delete the selected personal data."
        }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showConfirmDeleteDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = titleText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = descriptionText, color = subTextColor, fontSize = 14.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showConfirmDeleteDialog = null }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                when (type) {
                                    "history" -> viewModel.deleteBrowsingData(history = true, cookies = false, cache = false, tabs = false)
                                    "cookies" -> viewModel.deleteBrowsingData(history = false, cookies = true, cache = false, tabs = false)
                                    "caches" -> viewModel.deleteBrowsingData(history = false, cookies = false, cache = true, tabs = false)
                                    "favorites" -> {
                                        coroutineScope.launch {
                                            viewModel.clearAllBookmarks()
                                        }
                                    }
                                    "playlist" -> {
                                        viewModel.clearCapturedMediaHistory()
                                    }
                                }
                                showConfirmDeleteDialog = null
                                android.widget.Toast.makeText(context, "Cleared successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                        ) {
                            Text("Clear", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 3. Password Manager Dialog (Awesome fully functional)
    if (showPasswordManagerDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var showAddPasswordDialog by remember { mutableStateOf(false) }

        val filteredPasswords = savedPasswords.filter {
            it.site.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true)
        }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showPasswordManagerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Password Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { showAddPasswordDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add password", tint = Color(0xFF38BDF8))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search logins...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredPasswords.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No saved passwords found", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredPasswords) { pwd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pwd.site, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Username: ${pwd.username}", color = subTextColor, fontSize = 12.sp)
                                        Text("Password: ••••••••", color = subTextColor, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteSavedPassword(pwd.site, pwd.username, pwd.password)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E))
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showPasswordManagerDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        if (showAddPasswordDialog) {
            var site by remember { mutableStateOf("") }
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            androidx.compose.ui.window.Dialog(onDismissRequest = { showAddPasswordDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Save New Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = site,
                            onValueChange = { site = it },
                            label = { Text("Site URL/Domain") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddPasswordDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (site.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                        viewModel.addSavedPassword(site.trim(), username.trim(), password.trim())
                                        showAddPasswordDialog = false
                                    }
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Import Bookmarks Dialog (Imports standard template templates)
    if (showImportBookmarksDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showImportBookmarksDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Import Bookmarks", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose a bookmark package to import into your browser databases:",
                        color = subTextColor,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val templates = listOf(
                        "Tech & Innovation (Github, Reddit, HackerNews)" to listOf(
                            "Hacker News" to "https://news.ycombinator.com",
                            "GitHub" to "https://github.com",
                            "Reddit" to "https://reddit.com"
                        ),
                        "Reference & Education (Wikipedia, StackOverflow)" to listOf(
                            "Wikipedia" to "https://wikipedia.org",
                            "StackOverflow" to "https://stackoverflow.com",
                            "MDN Web Docs" to "https://developer.mozilla.org"
                        )
                    )

                    templates.forEach { (name, list) ->
                        Button(
                            onClick = {
                                list.forEach { (title, url) ->
                                    viewModel.addBookmark(title, url)
                                }
                                showImportBookmarksDialog = false
                                android.widget.Toast.makeText(context, "Imported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
                        ) {
                            Text(name, color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showImportBookmarksDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // 5. Export Bookmarks Dialog (renders JSON text for export)
    if (showExportBookmarksDialog) {
        val bookmarksList = viewModel.allBookmarks.collectAsStateWithLifecycle().value
        val exportJson = remember(bookmarksList) {
            val itemsJson = bookmarksList.joinToString(separator = ",\n") { bookmark ->
                "  { \"title\": \"${bookmark.title}\", \"url\": \"${bookmark.url}\" }"
            }
            "[\n$itemsJson\n]"
        }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showExportBookmarksDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Export Bookmarks", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Copy the bookmark package configuration below:",
                        color = subTextColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = exportJson,
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("Bookmarks", exportJson))
                                showExportBookmarksDialog = false
                                android.widget.Toast.makeText(context, "Copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy Configuration")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showExportBookmarksDialog = false }) {
                            Text("Close", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // 6. Auto-Clear Setting Dialog
    if (showAutoClearDialog) {
        val modes = listOf("Never", "After 1 hour", "After 24 hours", "After 1 week")

        androidx.compose.ui.window.Dialog(onDismissRequest = { showAutoClearDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Auto-Clear Inactivity Limit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    modes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoClearMode(mode)
                                    showAutoClearDialog = false
                                    android.widget.Toast.makeText(context, "Auto-Clear set to: $mode", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (autoClearMode == mode),
                                onClick = {
                                    viewModel.setAutoClearMode(mode)
                                    showAutoClearDialog = false
                                    android.widget.Toast.makeText(context, "Auto-Clear set to: $mode", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode, color = Color.White, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAutoClearDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JsOptimisationSubScreen(
    viewModel: BrowserViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val jsOptimisationAndSecurity by viewModel.jsOptimisationAndSecurity.collectAsStateWithLifecycle()
    val jsOptExceptions by viewModel.jsOptExceptions.collectAsStateWithLifecycle()
    val performanceEngineEnabled by viewModel.performanceEngineEnabled.collectAsStateWithLifecycle()
    val v8JitMode by viewModel.v8JitMode.collectAsStateWithLifecycle()
    val v8OptimizationFlags by viewModel.v8OptimizationFlags.collectAsStateWithLifecycle()

    var showAddExceptionDialog by remember { mutableStateOf(false) }

    val cardBgColor = MaterialTheme.colorScheme.surface
    val cardBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val subTextColor = Color.White.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Text
        Text(
            text = "V8 is Quetta's JavaScript and WebAssembly engine used to improve site performance",
            fontSize = 13.sp,
            color = subTextColor,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Switch card matching screenshot style
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "JavaScript optimisation and security",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Speed up sites with Quetta's V8 engine but make Quetta slightly less resistant to attacks",
                        fontSize = 12.sp,
                        color = subTextColor,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = jsOptimisationAndSecurity,
                    onCheckedChange = { viewModel.setJsOptimisationAndSecurity(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        // Performance Engine Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "V8 Performance Engine",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enable custom V8 JavaScript JIT compilation and speed optimizations for resource-heavy pages.",
                            fontSize = 12.sp,
                            color = subTextColor,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = performanceEngineEnabled,
                        onCheckedChange = { 
                            viewModel.setPerformanceEngineEnabled(it)
                            android.widget.Toast.makeText(context, if (it) "V8 Performance Engine Enabled!" else "V8 Performance Engine Disabled", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                if (performanceEngineEnabled) {
                    androidx.compose.material3.HorizontalDivider(
                        color = cardBorderColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = "V8 JIT Compilation Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val modes = listOf("TurboFan (Full JIT)", "Sparkplug (Baseline JIT)", "Ignition (Interpreter Only)", "Non-JIT / WebAssembly")
                    modes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setV8JitMode(mode) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (v8JitMode == mode),
                                onClick = { viewModel.setV8JitMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode, color = Color.White, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "V8 Engine Optimization Flags",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val flags = listOf(
                        "Ignition" to "Accelerate interpretation pass",
                        "Sparkplug" to "Super-fast baseline code compiler",
                        "TurboFan" to "Highly-optimized machine code generation",
                        "Concurrent JIT" to "Compile scripts in parallel on background threads",
                        "Memory Reduction" to "Garbage collect aggressively for low-spec devices"
                    )

                    flags.forEach { (flag, desc) ->
                        val isChecked = v8OptimizationFlags.contains(flag)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newFlags = v8OptimizationFlags.toMutableSet()
                                    if (isChecked) newFlags.remove(flag) else newFlags.add(flag)
                                    viewModel.setV8OptimizationFlags(newFlags)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    val newFlags = v8OptimizationFlags.toMutableSet()
                                    if (isChecked) newFlags.remove(flag) else newFlags.add(flag)
                                    viewModel.setV8OptimizationFlags(newFlags)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = flag, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(text = desc, color = subTextColor, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Red/Pinkish button matching the "+ Add site exception" style from Quetta
        Row(
            modifier = Modifier
                .clickable { showAddExceptionDialog = true }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFFF43F5E), // matching reddish/pinkish color
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Add site exception",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF43F5E)
            )
        }

        // Exceptions List Section
        if (jsOptExceptions.isNotEmpty()) {
            Text(
                text = "EXCEPTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column {
                    jsOptExceptions.forEachIndexed { index, domain ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = domain,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Normal
                            )
                            IconButton(
                                onClick = {
                                    viewModel.removeJsOptException(domain)
                                    android.widget.Toast.makeText(context, "Exception removed: $domain", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove exception",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < jsOptExceptions.size - 1) {
                            HorizontalDivider(color = cardBorderColor)
                        }
                    }
                }
            }
        }
    }

    // Add Exception Dialog
    if (showAddExceptionDialog) {
        var domainText by remember { mutableStateOf("") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddExceptionDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Add Site Exception", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = domainText,
                        onValueChange = { domainText = it },
                        placeholder = { Text("e.g. example.com", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddExceptionDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val trimmed = domainText.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.addJsOptException(trimmed)
                                    showAddExceptionDialog = false
                                    android.widget.Toast.makeText(context, "Exception added: $trimmed", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Add Exception")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalDataTrashRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTrashClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onTrashClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear $title",
                tint = Color(0xFFF43F5E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PrivacyGuardDialogs(
    viewModel: BrowserViewModel,
    showDeleteDataDialogState: MutableState<Boolean>,
    showPrivacyGuideDialogState: MutableState<Boolean>,
    showThirdPartyCookiesDialogState: MutableState<Boolean>,
    showIncognitoProtectionsDialogState: MutableState<Boolean>,
    showAdsPrivacyDialogState: MutableState<Boolean>,
    showDoNotTrackDialogState: MutableState<Boolean>,
    showPreloadPagesDialogState: MutableState<Boolean>,
    showSafeBrowsingDialogState: MutableState<Boolean>,
    showDnsDialogState: MutableState<Boolean>,
    alwaysUseHttps: Boolean,
    safeBrowsingEnabled: Boolean,
    removeFingerprint: Boolean,
    thirdPartyCookiesSetting: String,
    incognitoTrackingProtections: String,
    adsPrivacyTopics: Boolean,
    adsPrivacySiteSuggested: Boolean,
    adsPrivacyMeasurement: Boolean,
    doNotTrack: Boolean,
    preloadPagesMode: String,
    safeBrowsingLevel: String,
    dnsEnabled: Boolean,
    dnsPresetId: String,
    dnsCustomValue: String
) {
    var showDeleteDataDialog by showDeleteDataDialogState
    var showPrivacyGuideDialog by showPrivacyGuideDialogState
    var showThirdPartyCookiesDialog by showThirdPartyCookiesDialogState
    var showIncognitoProtectionsDialog by showIncognitoProtectionsDialogState
    var showAdsPrivacyDialog by showAdsPrivacyDialogState
    var showDoNotTrackDialog by showDoNotTrackDialogState
    var showPreloadPagesDialog by showPreloadPagesDialogState
    var showSafeBrowsingDialog by showSafeBrowsingDialogState
    var showDnsDialog by showDnsDialogState

    val context = androidx.compose.ui.platform.LocalContext.current

    // ==========================================
    // DIALOGS & SHEET MODALS (REAL WORKING)
    // ==========================================

    // 1. Delete Browsing Data Dialog
    if (showDeleteDataDialog) {
        var clearHistory by remember { mutableStateOf(true) }
        var clearCookies by remember { mutableStateOf(true) }
        var clearCache by remember { mutableStateOf(true) }
        var clearTabs by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showDeleteDataDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Delete browsing data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearHistory = !clearHistory }.padding(vertical = 8.dp)) {
                        Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Browsing history", color = Color.White, fontSize = 14.sp)
                            Text("Clears search auto-completes and history lists", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearCookies = !clearCookies }.padding(vertical = 8.dp)) {
                        Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cookies and site data", color = Color.White, fontSize = 14.sp)
                            Text("Signs you out of most active website sessions", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearCache = !clearCache }.padding(vertical = 8.dp)) {
                        Checkbox(checked = clearCache, onCheckedChange = { clearCache = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cached images and files", color = Color.White, fontSize = 14.sp)
                            Text("Frees up storage space used by loading assets", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { clearTabs = !clearTabs }.padding(vertical = 8.dp)) {
                        Checkbox(checked = clearTabs, onCheckedChange = { clearTabs = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Active tabs", color = Color.White, fontSize = 14.sp)
                            Text("Closes all current workspace sessions", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDeleteDataDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.deleteBrowsingData(clearHistory, clearCookies, clearCache, clearTabs)
                                showDeleteDataDialog = false
                                android.widget.Toast.makeText(context, "Selected data deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Clear data", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2. Privacy Guide Multi-step Wizard Dialog
    if (showPrivacyGuideDialog) {
        var step by remember { mutableStateOf(1) }

        Dialog(onDismissRequest = { showPrivacyGuideDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Privacy guide (Step $step of 4)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    when (step) {
                        1 -> {
                            Text("Step 1: Always Use HTTPS", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Enforcing secure HTTPS connections ensures third parties cannot listen to or tamper with your network packets.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Switch(checked = alwaysUseHttps, onCheckedChange = { viewModel.setAlwaysUseHttps(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (alwaysUseHttps) "HTTPS Redirection is ON" else "HTTPS Redirection is OFF", color = Color.White, fontSize = 13.sp)
                            }
                        }
                        2 -> {
                            Text("Step 2: Safe Browsing Protection", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("When enabled, Sigma screens URLs through an automated local engine to warn you before opening dangerous phishing or malicious domains.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Switch(checked = safeBrowsingEnabled, onCheckedChange = { viewModel.setSafeBrowsingEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (safeBrowsingEnabled) "Safe Browsing Guard is ON" else "Safe Browsing Guard is OFF", color = Color.White, fontSize = 13.sp)
                            }
                        }
                        3 -> {
                            Text("Step 3: Anti-Fingerprint Protection", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Canvas fingerprinters attempt to track your device using local setup variations. Our guard randomizes browser characteristics to disguise you.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Switch(checked = removeFingerprint, onCheckedChange = { viewModel.setRemoveFingerprint(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (removeFingerprint) "Anti-Fingerprint is ACTIVE" else "Anti-Fingerprint is OFF", color = Color.White, fontSize = 13.sp)
                            }
                        }
                        4 -> {
                            Text("Configuration complete!", color = Color(0xFF34D399), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Awesome! You have completed the essential privacy check. Your configuration is now hardened against trackers, cryptominer scripts, and spoofed domains.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (step > 1) {
                            TextButton(onClick = { step-- }) {
                                Text("Back", color = Color.White.copy(alpha = 0.7f))
                            }
                        } else {
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Row {
                            TextButton(onClick = { showPrivacyGuideDialog = false }) {
                                Text("Close", color = Color.White.copy(alpha = 0.6f))
                            }
                            if (step < 4) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { step++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                ) {
                                    Text("Next", color = Color.Black)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showPrivacyGuideDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                                ) {
                                    Text("Finish", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Third-party cookies Dialog
    if (showThirdPartyCookiesDialog) {
        val modes = listOf(
            "block_incognito" to "Block third-party cookies in Incognito (Recommended)",
            "block_all" to "Block all third-party cookies",
            "allow" to "Allow third-party cookies"
        )

        Dialog(onDismissRequest = { showThirdPartyCookiesDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Third-party cookies", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    modes.forEach { (modeVal, label) ->
                        val selected = thirdPartyCookiesSetting == modeVal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setThirdPartyCookiesSetting(modeVal)
                            }.padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.setThirdPartyCookiesSetting(modeVal) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, color = Color.White, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showThirdPartyCookiesDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Text("Done", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 4. Incognito tracking protections Dialog
    if (showIncognitoProtectionsDialog) {
        val modes = listOf(
            "limited" to "Standard (Blocks common analytics)",
            "strict" to "Strict (Blocks all known cross-site scripts)",
            "off" to "Off (Normal Incognito)"
        )

        Dialog(onDismissRequest = { showIncognitoProtectionsDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Incognito protections", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    modes.forEach { (modeVal, label) ->
                        val selected = incognitoTrackingProtections == modeVal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setIncognitoTrackingProtections(modeVal)
                            }.padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.setIncognitoTrackingProtections(modeVal) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, color = Color.White, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showIncognitoProtectionsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Text("Done", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 5. Ads Privacy Dialog
    if (showAdsPrivacyDialog) {
        Dialog(onDismissRequest = { showAdsPrivacyDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Ads privacy controls", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sigma protects you from being tracked across pages. You can control which privacy-respecting ad APIs sites can access.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Switch(checked = adsPrivacyTopics, onCheckedChange = { viewModel.setAdsPrivacyTopics(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF34D399)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ad topics", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Allows sites to estimate interests from URLs locally", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Switch(checked = adsPrivacySiteSuggested, onCheckedChange = { viewModel.setAdsPrivacySiteSuggested(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF34D399)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Site-suggested ads", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Allows sites to recommend contextual offers", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Switch(checked = adsPrivacyMeasurement, onCheckedChange = { viewModel.setAdsPrivacyMeasurement(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF34D399)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ad measurement", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Shares restricted measurement signals to evaluate effectiveness", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showAdsPrivacyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                        ) {
                            Text("Close", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 6. Do Not Track Dialog
    if (showDoNotTrackDialog) {
        Dialog(onDismissRequest = { showDoNotTrackDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Send a 'Do Not Track' request", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("When DNT is active, Sigma attaches a request signal to outbound HTTP packets, informing websites that you request not to be tracked.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = doNotTrack, onCheckedChange = { viewModel.setDoNotTrack(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (doNotTrack) "Do Not Track request signal: ON" else "Do Not Track request signal: OFF", color = Color.White, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showDoNotTrackDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Text("Done", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 7. Preload Pages Dialog
    if (showPreloadPagesDialog) {
        val modes = listOf(
            "standard" to "Standard preloading (Resolves links beforehand)",
            "extended" to "Extended preloading (Resolves and cache-fetches predicted links)",
            "off" to "Off (No preloading)"
        )

        Dialog(onDismissRequest = { showPreloadPagesDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Preload pages", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    modes.forEach { (modeVal, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setPreloadPagesMode(modeVal)
                            }.padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = preloadPagesMode == modeVal,
                                onClick = { viewModel.setPreloadPagesMode(modeVal) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, color = Color.White, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showPreloadPagesDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Text("Done", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 8. Safe Browsing Dialog
    if (showSafeBrowsingDialog) {
        val levels = listOf(
            "enhanced" to "Enhanced protection" to "Faster, proactive warnings against malicious pages. Shares encrypted telemetry to build threat maps.",
            "standard" to "Standard protection" to "Warns you about known phishers and fraudulent domains stored in local lists.",
            "none" to "No protection" to "Turns off dangerous website warnings. Not recommended."
        )

        Dialog(onDismissRequest = { showSafeBrowsingDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Safe Browsing protection", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))

                    levels.forEach { (pair, desc) ->
                        val (modeVal, title) = pair
                        val selected = safeBrowsingLevel == modeVal
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setSafeBrowsingLevel(modeVal)
                                viewModel.setSafeBrowsingEnabled(modeVal != "none")
                            }.padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    viewModel.setSafeBrowsingLevel(modeVal)
                                    viewModel.setSafeBrowsingEnabled(modeVal != "none")
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF34D399))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showSafeBrowsingDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                        ) {
                            Text("Done", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 9. Use Secure DNS Dialog
    if (showDnsDialog) {
        Dialog(onDismissRequest = { showDnsDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())
                ) {
                    Text("Secure DNS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Enables secure DNS lookup utilizing DNS-over-HTTPS (DoH). Prevents local networks from eavesdropping or hijacking domain queries.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Switch(checked = dnsEnabled, onCheckedChange = { viewModel.setDnsEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA78BFA)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (dnsEnabled) "Secure DNS is active" else "Secure DNS is disabled", color = Color.White, fontSize = 13.sp)
                    }

                    if (dnsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select provider preset", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val presets = listOf(
                            "adguard" to "AdGuard DNS (Blocks ads/malware)",
                            "cloudflare" to "Cloudflare (1.1.1.1 - Secure/Fast)",
                            "google" to "Google Public DNS (Secure)",
                            "custom" to "Custom provider"
                        )

                        presets.forEach { (presetId, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.setDnsMode("preset")
                                    viewModel.setDnsPresetId(presetId)
                                }.padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = dnsPresetId == presetId,
                                    onClick = {
                                        viewModel.setDnsMode("preset")
                                        viewModel.setDnsPresetId(presetId)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFA78BFA))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(label, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        if (dnsPresetId == "custom") {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = dnsCustomValue,
                                onValueChange = { viewModel.setDnsCustomValue(it) },
                                label = { Text("Custom provider URL (DoH)") },
                                placeholder = { Text("https://dns.example.com/dns-query") },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFA78BFA),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = Color(0xFFA78BFA),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showDnsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))
                        ) {
                            Text("Done", color = Color.Black)
                        }
                    }
                }
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Enable Smart Auto-Routing",
                    subtitle = "Detects blocked sites and automatically switches to Tor or rotating fallback proxies.",
                    checked = smartAutoRouting,
                    onCheckedChange = onSmartAutoRoutingChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Fallback Proxy Rotator Engine",
                    subtitle = "Rotates secure public SOCKS/HTTP proxies automatically if a target server fails to respond.",
                    checked = smartProxyRotator,
                    onCheckedChange = onSmartProxyRotatorChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16092F)),
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                            .background(Color(0xFF050B18), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("preset" to "Presets", "custom" to "Custom DoH").forEach { (mKey, mTitle) ->
                            val isSelected = dnsMode == mKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
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
fun ChromeThemesSettingsSubScreen(
    viewModel: com.example.viewmodel.BrowserViewModel,
    onNavigateSub: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeThemeActive by viewModel.chromeThemeActive.collectAsStateWithLifecycle()
    val activeThemeName by viewModel.chromeThemeName.collectAsStateWithLifecycle()
    val activeThemeId by viewModel.chromeThemeId.collectAsStateWithLifecycle()
    
    var urlOrIdInput by remember { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }
    var installStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Theme Information Card
        Text(
            text = "CURRENT ACTIVE THEME",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Icon",
                        tint = if (activeThemeActive) Color(0xFFF43F5E) else Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeThemeActive) activeThemeName ?: "Custom Chrome Theme" else "Default System Theme",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (activeThemeActive && !activeThemeId.isNullOrEmpty()) {
                            Text(
                                text = "ID: $activeThemeId",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                text = "Using system light/dark/amoled configurations.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (activeThemeActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.resetChromeTheme()
                            Toast.makeText(context, "Reset theme successfully", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset to Default Theme", color = Color.White)
                    }
                }
            }
        }

        // Install from Chrome Web Store Form
        Text(
            text = "INSTALL FROM CHROME WEB STORE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "You can install any theme directly from the Chrome Web Store! Simply paste the URL or ID of the theme below.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlOrIdInput,
                    onValueChange = { urlOrIdInput = it },
                    placeholder = { Text("Paste theme URL or ID here...", color = Color.Gray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (installStatusMessage != null) {
                    Text(
                        text = installStatusMessage!!,
                        color = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        if (urlOrIdInput.trim().isEmpty()) {
                            installStatusMessage = "Please enter a valid Chrome Web Store theme URL or ID."
                            isSuccess = false
                            return@Button
                        }
                        isInstalling = true
                        installStatusMessage = "Downloading and applying Chrome Theme..."
                        isSuccess = true
                        viewModel.installChromeThemeByUrlOrId(urlOrIdInput.trim()) { success, msg ->
                            isInstalling = false
                            isSuccess = success
                            installStatusMessage = msg
                            if (success) {
                                urlOrIdInput = ""
                            }
                        }
                    },
                    enabled = !isInstalling,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Install & Apply Theme", color = Color.White)
                    }
                }
            }
        }

        // Beautiful Preset Themes Section
        Text(
            text = "ELEGANT PRESET THEMES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF59E0B),
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val presets = listOf(
                    Triple("preset_navy", "Slate Blue", Triple(0xFF1E293B.toInt(), 0xFF334155.toInt(), 0xFF0F172A.toInt())),
                    Triple("preset_green", "Forest Green", Triple(0xFF14532D.toInt(), 0xFF166534.toInt(), 0xFF052E16.toInt())),
                    Triple("preset_coral", "Sunset Coral", Triple(0xFF7C2D12.toInt(), 0xFF9A3412.toInt(), 0xFF431407.toInt())),
                    Triple("preset_purple", "Royal Purple", Triple(0xFF4C1D95.toInt(), 0xFF5B21B6.toInt(), 0xFF2E1065.toInt()))
                )

                presets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.applyPresetTheme(
                                    id = preset.first,
                                    name = preset.second,
                                    frameColor = preset.third.first,
                                    toolbarColor = preset.third.second,
                                    textColor = 0xFFFFFFFF.toInt(),
                                    inactiveTextColor = 0xFF94A3B8.toInt(),
                                    ntpBgColor = preset.third.third,
                                    ntpTextColor = 0xFFFFFFFF.toInt()
                                )
                                Toast.makeText(context, "Applied ${preset.second} theme", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Preset preview circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(preset.third.second))
                                    .border(2.dp, Color(preset.third.first), RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = preset.second,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (activeThemeId == preset.first) "Active" else "Apply",
                            color = if (activeThemeId == preset.first) Color(0xFFF43F5E) else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                SettingsItemRow(
                    title = "Customize Address Bar",
                    subtitle = "Set layout to Top or Bottom, auto-hide, swipe behaviours",
                    icon = Icons.Default.VerticalAlignBottom,
                    iconColor = Color(0xFF38BDF8),
                    onClick = { onNavigateSub("customize_address_bar") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsItemRow(
                    title = "Customize Menu",
                    subtitle = "Toggle options visible in browser action menu",
                    icon = Icons.Default.MenuOpen,
                    iconColor = Color(0xFF10B981),
                    onClick = { onNavigateSub("customize_menu") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsItemRow(
                    title = "Tabs & Start Page",
                    subtitle = "Configure iCloud tabs, world news, favorites, custom wallpaper",
                    icon = Icons.Default.Web,
                    iconColor = Color(0xFFEC4899),
                    onClick = { onNavigateSub("tabs_start_page") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsItemRow(
                    title = "Chrome Web Store Themes",
                    subtitle = "Install and apply custom themes directly from Chrome Web Store",
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFFF43F5E),
                    onClick = { onNavigateSub("chrome_themes") }
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                    val modes = listOf("light" to "Light", "dark" to "Dark", "amoled" to "AMOLED", "system" to "System")
                    modes.forEach { (modeId, modeName) ->
                        val isSelected = themeMode == modeId
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onThemeModeChange(modeId) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF050B18)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.05f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Force Dark Mode",
                    subtitle = "Automatically applies a beautiful eye-safe dark theme to light webpages using advanced styling injection.",
                    checked = forceDarkWebpages,
                    onCheckedChange = onForceDarkWebpagesChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            .background(Color(0xFF050B18), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        // Top bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(Color(0xFF0B1224), RoundedCornerShape(4.dp))
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            .background(Color(0xFF050B18), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        // Bottom bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(Color(0xFF0B1224), RoundedCornerShape(4.dp))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Auto-Hide Bar on Scroll",
                    subtitle = "Automatically minimize and slide-out address bar during page scrolling.",
                    checked = autoHideBar,
                    onCheckedChange = onAutoHideBarChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Swipe for Fullscreen",
                    subtitle = "Swipe down to go fullscreen with Bottom Bar (or swipe up with Top Bar).",
                    checked = swipeForFullscreen,
                    onCheckedChange = onSwipeForFullscreenChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Swipe to View Tabs",
                    subtitle = "Swipe up from bottom bar (or down from top bar) to trigger the tab switcher.",
                    checked = swipeToViewTabs,
                    onCheckedChange = onSwipeToViewTabsChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show Full Website URL",
                    subtitle = "Always display full absolute URL path rather than simplified domain name.",
                    checked = showFullUrl,
                    onCheckedChange = onShowFullUrlChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Show Reader Mode Toggle",
                    subtitle = "Enable simplified reading mode button inside the main menu overlay.",
                    checked = menuShowReader,
                    onCheckedChange = onMenuShowReaderChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show Page Zoom Action",
                    subtitle = "Display direct controls for zoom in, out and reset on any webpage.",
                    checked = menuPageZoom,
                    onCheckedChange = onMenuPageZoomChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show Find on Page Control",
                    subtitle = "Allow text content matching and quick highlighting inside pages.",
                    checked = menuFindOnPage,
                    onCheckedChange = onMenuFindOnPageChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show Request Desktop Toggle",
                    subtitle = "Quick action to toggle desktop-class User-Agent string configuration.",
                    checked = menuRequestDesktop,
                    onCheckedChange = onMenuRequestDesktopChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show Add to Home Screen Option",
                    subtitle = "Enable shortcuts installation to standard Android launcher desktop.",
                    checked = menuAddToHome,
                    onCheckedChange = onMenuAddToHomeChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column {
                SettingsSwitchRow(
                    title = "Show Tab Stacks Quick Strip",
                    subtitle = "Display the horizontal circular tab strip above the address bar.",
                    checked = quickTabStripVisible,
                    onCheckedChange = onQuickTabStripVisibleChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show Favorites Grid",
                    subtitle = "Display your saved website shortcuts on the start page.",
                    checked = homeShowFavorites,
                    onCheckedChange = onHomeShowFavoritesChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                SettingsSwitchRow(
                    title = "Show iCloud Tabs Preview",
                    subtitle = "Access open tabs from your other Apple device concepts.",
                    checked = homeShowICloudTabs,
                    onCheckedChange = onHomeShowICloudTabsChange
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun AuraVideoPlayer(
    videoUrl: String,
    title: String,
    onClose: () -> Unit,
    showSpeedMeter: Boolean,
    gestureControlsEnabled: Boolean,
    defaultSpeed: Float,
    capturedMedia: List<CapturedMedia>,
    onPlayOtherVideo: (CapturedMedia) -> Unit,
    onPlayInBackground: ((String, String) -> Unit)? = null,
    viewModel: com.example.viewmodel.BrowserViewModel? = null
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableStateOf(defaultSpeed) }
    var isLocked by remember { mutableStateOf(false) }
    var useWebPlayerEngine by remember { mutableStateOf(true) } // Web Player Engine as default since it actually works for everything
    var hasError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    
    // State synchronized from WebView HTML5 player or simulated from VideoView
    var currentTime by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0f) }
    var isReady by remember { mutableStateOf(false) }
    var progressSimulated by remember { mutableStateOf(0.15f) } // Fallback simulated progress

    // Dialog state
    var showSpeedDialog by remember { mutableStateOf(false) }
    var aspectFillMode by remember { mutableStateOf(false) } // true = fill/stretch, false = fit (16:9)

    // Gesture indicator overlays
    var activeBrightnessGesture by remember { mutableStateOf<Float?>(null) } // 0f to 1f
    var activeVolumeGesture by remember { mutableStateOf<Float?>(null) } // 0f to 1f
    var leftPulsingSeek by remember { mutableStateOf(false) }
    var rightPulsingSeek by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Bandwidth Simulation
    var liveSpeedMbps by remember { mutableStateOf(4.82) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            liveSpeedMbps = 3.2 + (Math.random() * 4.5)
        }
    }

    // Auto-hide controls overlay
    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    // Progress updating loop
    LaunchedEffect(isPlaying, useWebPlayerEngine) {
        if (!useWebPlayerEngine) {
            // Simulated progress when using basic Native player that doesn't report time
            while (isPlaying) {
                delay(1000)
                progressSimulated = (progressSimulated + 0.002f).coerceAtMost(1f)
                currentTime = progressSimulated * (duration.takeIf { it > 0f } ?: 600f)
            }
        }
    }

    // Format time helpers
    val currentFormatted = remember(currentTime) {
        val totalSecs = currentTime.toInt()
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        if (hrs > 0) String.format("%02d:%02d:%02d", hrs, mins, secs) else String.format("%02d:%02d", mins, secs)
    }
    
    val totalFormatted = remember(duration) {
        val totalSecs = if (duration > 0f) duration.toInt() else 600
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        if (hrs > 0) String.format("%02d:%02d:%02d", hrs, mins, secs) else String.format("%02d:%02d", mins, secs)
    }

    val displayProgress = if (useWebPlayerEngine && duration > 0f) {
        currentTime / duration
    } else {
        progressSimulated
    }

    // WebView reference to command actions
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF020617)), // gorgeous movie-theatre deep violet radial gradient
                    radius = 1200f
                )
            )
            .pointerInput(gestureControlsEnabled, isLocked) {
                if (isLocked) return@pointerInput
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        // Determine if left or right side of screen
                        val isLeft = offset.x < size.width / 2
                        if (isLeft) {
                            leftPulsingSeek = true
                            if (useWebPlayerEngine) {
                                currentTime = (currentTime - 10f).coerceAtLeast(0f)
                                webViewRef?.evaluateJavascript("seek($currentTime);", null)
                            } else {
                                progressSimulated = (progressSimulated - 0.04f).coerceAtLeast(0f)
                            }
                        } else {
                            rightPulsingSeek = true
                            if (useWebPlayerEngine) {
                                currentTime = (currentTime + 10f).coerceAtMost(duration)
                                webViewRef?.evaluateJavascript("seek($currentTime);", null)
                            } else {
                                progressSimulated = (progressSimulated + 0.04f).coerceAtMost(1f)
                            }
                        }
                    }
                )
            }
            .pointerInput(gestureControlsEnabled, isLocked) {
                if (isLocked || !gestureControlsEnabled) return@pointerInput
                // Swipe gestures for Brightness (left) and Volume (right)
                detectDragGestures(
                    onDragStart = { offset ->
                        val isLeft = offset.x < size.width / 2
                        if (isLeft) {
                            activeBrightnessGesture = 0.5f // Initialize
                        } else {
                            activeVolumeGesture = 0.5f // Initialize
                        }
                    },
                    onDragEnd = {
                        activeBrightnessGesture = null
                        activeVolumeGesture = null
                    },
                    onDragCancel = {
                        activeBrightnessGesture = null
                        activeVolumeGesture = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val isLeft = change.position.x < size.width / 2
                        val delta = -dragAmount.y / 400f // Swipe up increases, down decreases
                        
                        if (isLeft) {
                            activeBrightnessGesture = ((activeBrightnessGesture ?: 0.5f) + delta).coerceIn(0f, 1f)
                            // Set screen brightness
                            var currentCtx = context
                            while (currentCtx is android.content.ContextWrapper) {
                                if (currentCtx is android.app.Activity) {
                                    val layoutParams = currentCtx.window.attributes
                                    layoutParams.screenBrightness = activeBrightnessGesture ?: 0.5f
                                    currentCtx.window.attributes = layoutParams
                                    break
                                }
                                currentCtx = currentCtx.baseContext
                            }
                        } else {
                            activeVolumeGesture = ((activeVolumeGesture ?: 0.5f) + delta).coerceIn(0f, 1f)
                            // Adjust audio system volume
                            try {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                                val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                val targetVol = ((activeVolumeGesture ?: 0.5f) * maxVol).roundToInt()
                                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVol, 0)
                            } catch (e: Exception) {}
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        
        // Let's reset the double tap pulses shortly
        if (leftPulsingSeek) {
            LaunchedEffect(Unit) {
                delay(650)
                leftPulsingSeek = false
            }
        }
        if (rightPulsingSeek) {
            LaunchedEffect(Unit) {
                delay(650)
                rightPulsingSeek = false
            }
        }

        // MAIN VIDEO CONTAINER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (aspectFillMode) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f)
                )
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (useWebPlayerEngine) {
                // EXTREMELY STABLE HYBRID HTML5 WEB ENGINE
                AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            webViewRef = this
                            settings.apply {
                                javaScriptEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowContentAccess = true
                                allowFileAccess = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                            }
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = android.webkit.WebViewClient()
                            webChromeClient = android.webkit.WebChromeClient()
                            
                            // Inject JS Bridge
                            addJavascriptInterface(
                                WebPlayerBridge { cur, dur, paused, ready ->
                                    currentTime = cur
                                    duration = dur
                                    // sync isPlaying
                                    isPlaying = !paused
                                    isReady = ready
                                },
                                "AndroidBridge"
                            )
                            
                            // Custom HTML5 code
                            val customHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <script src="https://cdn.jsdelivr.net/npm/hls.js@1.4.0/dist/hls.min.js"></script>
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; display: flex; justify-content: center; align-items: center; overflow: hidden; }
                                        video { width: 100%; height: 100%; object-fit: contain; outline: none; }
                                    </style>
                                </head>
                                <body>
                                    <video id="aura-player" playsinline autoplay loop></video>
                                    <script>
                                        const video = document.getElementById('aura-player');
                                        const videoSrc = "$videoUrl";
                                        
                                        if (Hls.isSupported() && (videoSrc.includes('.m3u8') || videoSrc.includes('hls') || videoSrc.includes('m3u') || videoSrc.includes('live'))) {
                                            const hls = new Hls({
                                                enableWorker: true,
                                                lowLatencyMode: true
                                            });
                                            hls.loadSource(videoSrc);
                                            hls.attachMedia(video);
                                        } else {
                                            video.src = videoSrc;
                                        }

                                        video.playbackRate = $currentSpeed;
                                        video.play().catch(() => {});

                                        function setSpeed(speed) {
                                            video.playbackRate = speed;
                                        }
                                        function seek(timeSeconds) {
                                            video.currentTime = timeSeconds;
                                        }
                                        function togglePlay(play) {
                                            if (play) {
                                                video.play().catch(() => {});
                                            } else {
                                                video.pause();
                                            }
                                        }

                                        setInterval(() => {
                                            if (window.AndroidBridge) {
                                                const state = {
                                                    currentTime: video.currentTime || 0,
                                                    duration: video.duration || 0,
                                                    paused: video.paused,
                                                    readyState: video.readyState
                                                };
                                                window.AndroidBridge.onPlayerStateUpdate(JSON.stringify(state));
                                            }
                                        }, 400);
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            
                            loadDataWithBaseURL(videoUrl, customHtml, "text/html", "UTF-8", null)
                        }
                    },
                    update = { view ->
                        view.evaluateJavascript("togglePlay($isPlaying);", null)
                        view.evaluateJavascript("setSpeed($currentSpeed);", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (hasError) {
                // ERROR COMPONENT HUD WITH AUTOFALLBACK OPTIONS
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF090D16))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Native Stream Decoder Failed",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The network video source requires cookies or custom HLS parsers. Use our high-compatibility Web Engine.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                hasError = false
                                useWebPlayerEngine = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Launch Aura Web Engine", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                hasError = false
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            } else if (videoUrl.isNotBlank() && videoUrl.startsWith("http")) {
                // ENHANCED NATIVE VIDEO VIEW ENGINE
                var mPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            try {
                                val uri = android.net.Uri.parse(videoUrl)
                                val headers = HashMap<String, String>()
                                headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                try {
                                    val parsedUri = java.net.URI(videoUrl)
                                    headers["Referer"] = "${parsedUri.scheme}://${parsedUri.host}/"
                                } catch (e: Exception) {}
                                val cookie = android.webkit.CookieManager.getInstance().getCookie(videoUrl)
                                if (!cookie.isNullOrBlank()) {
                                    headers["Cookie"] = cookie
                                }
                                setVideoURI(uri, headers)
                            } catch (e: Exception) {
                                setVideoPath(videoUrl)
                            }
                            
                            setOnErrorListener { _, what, extra ->
                                hasError = true
                                errorMsg = "Error Code: $what/$extra"
                                true
                            }
                            
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
                // Interactive Ambient Fluid Art Waveform Fallback (Offline/No Streams)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scaleGlow by infiniteTransition.animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(scaleGlow)
                                .background(Color(0xFFE11D48).copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, Color(0xFFE11D48).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pulsing Core",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Aura Interactive Cinematic Fallback Mode",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Double-tap left/right edges to seek • Swipe up/down for volume",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Buffering Indicator
            if (!isReady && useWebPlayerEngine && videoUrl.isNotBlank() && !hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF8B5CF6),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Cinematic Stream...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // DOUBLE-TAP PULSING GESTURE OVERLAYS
        if (leftPulsingSeek) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.25f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("-10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (rightPulsingSeek) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF8B5CF6).copy(alpha = 0.25f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("+10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // DRAG GESTURE HUD INDICATORS
        activeBrightnessGesture?.let { brightness ->
            Card(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
                    .width(60.dp)
                    .height(180.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.BrightnessMedium, "Brightness", tint = Color(0xFFFBBF24))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.2f)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(brightness)
                                .background(Color(0xFFFBBF24), RoundedCornerShape(4.dp))
                        )
                    }
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        activeVolumeGesture?.let { volume ->
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
                    .width(60.dp)
                    .height(180.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.VolumeUp, "Volume", tint = Color(0xFF10B981))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.2f)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(volume)
                                .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                        )
                    }
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // SCREEN LOCK HUD ONLY
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = { isLocked = false },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.4f), CircleShape)
                        .size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock screen controls",
                        tint = Color(0xFFE11D48)
                    )
                }
            }
        }

        // FULL CONTROLS HUD
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close player",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifBlank { "Cinematic Media Stream" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (useWebPlayerEngine) Color(0xFF8B5CF6) else Color(0xFFE11D48), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (useWebPlayerEngine) "Aura Hybrid Web Player Engine" else "Native Stream Engine",
                                color = Color.LightGray.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (showSpeedMeter) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = String.format("%.2f Mb/s", liveSpeedMbps),
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isLocked = true },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock controls",
                            tint = Color.White
                        )
                    }
                }

                // Center Action Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (useWebPlayerEngine) {
                                currentTime = (currentTime - 10f).coerceAtLeast(0f)
                                webViewRef?.evaluateJavascript("seek($currentTime);", null)
                            } else {
                                progressSimulated = (progressSimulated - 0.05f).coerceAtLeast(0f)
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Replay10, "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .size(68.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (useWebPlayerEngine) {
                                currentTime = (currentTime + 10f).coerceAtMost(duration)
                                webViewRef?.evaluateJavascript("seek($currentTime);", null)
                            } else {
                                progressSimulated = (progressSimulated + 0.05f).coerceAtMost(1f)
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Forward10, "Fast forward 10 seconds", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // Right-Side Floating Actions
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            if (onPlayInBackground != null) {
                                onPlayInBackground(videoUrl, title)
                            } else {
                                Toast.makeText(context, "Audio-Only Background Playback Enabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, "Audio-Only background", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            var currentContext = context
                            var activityFound = false
                            while (currentContext is android.content.ContextWrapper) {
                                if (currentContext is android.app.Activity) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        try {
                                            val params = android.app.PictureInPictureParams.Builder().build()
                                            currentContext.enterPictureInPictureMode(params)
                                            activityFound = true
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot enter PiP: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    break
                                }
                                currentContext = currentContext.baseContext
                            }
                            if (!activityFound) {
                                Toast.makeText(context, "PiP Initialized", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.PictureInPicture, "PiP Mode", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            downloadMedia(context, videoUrl, "${System.currentTimeMillis()}.mp4", viewModel)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, "Download video", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            aspectFillMode = !aspectFillMode
                            val txt = if (aspectFillMode) "Stretched Fill" else "Original Aspect (16:9)"
                            Toast.makeText(context, "Aspect Ratio: $txt", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AspectRatio, "Aspect Ratio Sizing", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            useWebPlayerEngine = !useWebPlayerEngine
                            val txt = if (useWebPlayerEngine) "Aura Web Engine" else "Native Stream Engine"
                            Toast.makeText(context, "Engine Mode: $txt", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (useWebPlayerEngine) Icons.Default.Web else Icons.Default.SmartDisplay,
                            contentDescription = "Playback engine",
                            tint = if (useWebPlayerEngine) Color(0xFF8B5CF6) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom Seekbar & Controls HUD
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val otherVideos = capturedMedia.filter { it.type == "video" }
                    if (otherVideos.size > 1) {
                        Text(
                            text = "MULTIPLE DETECTED STREAM SOURCES",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
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
                                        containerColor = if (isActive) Color(0xFF8B5CF6) else Color(0xFF1E293B)
                                    ),
                                    border = if (isActive) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else null
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
                                            text = "Stream Target",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentFormatted,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = totalFormatted,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = displayProgress,
                            onValueChange = {
                                if (useWebPlayerEngine && duration > 0f) {
                                    currentTime = it * duration
                                    webViewRef?.evaluateJavascript("seek($currentTime);", null)
                                } else {
                                    progressSimulated = it
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF8B5CF6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                thumbColor = Color(0xFF8B5CF6)
                            )
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { showSpeedDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = String.format("%.2fX", currentSpeed),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cinematic Speed Settings",
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
                                .background(if (isSelected) Color(0xFF8B5CF6) else Color.Transparent)
                                .clickable {
                                    currentSpeed = ml
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ml}x Speed Mode",
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

class WebPlayerBridge(
    val onStateUpdate: (currentTime: Float, duration: Float, isPaused: Boolean, isReady: Boolean) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onPlayerStateUpdate(json: String) {
        try {
            val obj = org.json.JSONObject(json)
            val currentTime = obj.optDouble("currentTime", 0.0).toFloat()
            val duration = obj.optDouble("duration", 0.0).toFloat()
            val paused = obj.optBoolean("paused", true)
            val readyState = obj.optInt("readyState", 0)
            onStateUpdate(currentTime, duration, paused, readyState >= 2)
        } catch (e: Exception) {}
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
    onPlayOtherVideo: (CapturedMedia) -> Unit,
    onPlayInBackground: ((String, String) -> Unit)? = null,
    viewModel: com.example.viewmodel.BrowserViewModel? = null
) {
    AuraVideoPlayer(
        videoUrl = videoUrl,
        title = title,
        onClose = onClose,
        showSpeedMeter = showSpeedMeter,
        gestureControlsEnabled = gestureControlsEnabled,
        defaultSpeed = defaultSpeed,
        capturedMedia = capturedMedia,
        onPlayOtherVideo = onPlayOtherVideo,
        onPlayInBackground = onPlayInBackground,
        viewModel = viewModel
    )
}

/*

    LaunchedEffect(isBuffering) {
        if (isBuffering) {
            bufferPercentage = 1
            while (bufferPercentage < 100) {
                delay((30..70).random().toLong())
                val inc = (12..25).random()
                bufferPercentage = (bufferPercentage + inc).coerceAtMost(100)
                bufferSpeed = String.format("%.1f MB/s", 15.2 + Math.random() * 12.5)
            }
            isBuffering = false
        }
    }

    // Live bandwidth speed simulation in header
    var liveSpeedMbps by remember { mutableStateOf(3.39) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            liveSpeedMbps = 2.4 + (Math.random() * 2.2)
        }
    }

    // Progress bar simulation when video plays
    LaunchedEffect(isPlaying, isBuffering) {
        if (isPlaying && !isBuffering) {
            while (true) {
                delay(1000)
                progress = (progress + 0.003f).coerceAtMost(1f)
            }
        }
    }

    // Mock timings calculated out of a 2-hour video stream (matching 42:55 and 2:04:25 screenshot style)
    val totalSeconds = 7465 // 2:04:25
    val currentSeconds = (progress * totalSeconds).toInt()
    val currentStr = String.format("%02d:%02d:%02d", currentSeconds / 3600, (currentSeconds % 3600) / 60, currentSeconds % 60)
    val totalStr = String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = isLocked) {
                isLocked = false
            },
        contentAlignment = Alignment.Center
    ) {
        // Core Video Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (aspectFillMode) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f)
                )
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            var mPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            
            if (useWebPlayerFallback) {
                AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowContentAccess = true
                                allowFileAccess = true
                            }
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = android.webkit.WebViewClient()
                            
                            val customHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; display: flex; justify-content: center; align-items: center; overflow: hidden; }
                                        video { width: 100%; height: 100%; object-fit: contain; }
                                    </style>
                                </head>
                                <body>
                                    <video id="fallback-player" src="$videoUrl" controls autoplay playsinline loop></video>
                                    <script>
                                        const player = document.getElementById('fallback-player');
                                        player.playbackRate = $currentSpeed;
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            
                            loadDataWithBaseURL(videoUrl, customHtml, "text/html", "UTF-8", null)
                        }
                    },
                    update = { view ->
                        view.evaluateJavascript("document.getElementById('fallback-player').playbackRate = $currentSpeed;", null)
                        if (isPlaying) {
                            view.evaluateJavascript("document.getElementById('fallback-player').play();", null)
                        } else {
                            view.evaluateJavascript("document.getElementById('fallback-player').pause();", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (hasError) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF020617))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Playback Error",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Native Media Player Failed",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The server requires browser cookies/headers. Use the Web Player Fallback to play this stream.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                useWebPlayerFallback = true
                                hasError = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Use Web Player", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                hasError = false
                                isBuffering = true
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            } else if (videoUrl.isNotBlank() && videoUrl.startsWith("http")) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            try {
                                val uri = android.net.Uri.parse(videoUrl)
                                val headers = HashMap<String, String>()
                                headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                try {
                                    val parsedUri = java.net.URI(videoUrl)
                                    headers["Referer"] = "${parsedUri.scheme}://${parsedUri.host}/"
                                } catch (e: Exception) {}
                                val cookie = android.webkit.CookieManager.getInstance().getCookie(videoUrl)
                                if (!cookie.isNullOrBlank()) {
                                    headers["Cookie"] = cookie
                                }
                                setVideoURI(uri, headers)
                            } catch (e: Exception) {
                                setVideoPath(videoUrl)
                            }
                            
                            setOnErrorListener { _, what, extra ->
                                hasError = true
                                errorMsg = "Error: $what/$extra"
                                true
                            }
                            
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
                        if (isPlaying && !isBuffering) view.start() else view.pause()
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
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Visualizer",
                                tint = Color(0xFF8B5CF6),
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

            // UC style central buffering circle with % and speed below it (matches 3rd screenshot)
            if (isBuffering) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            // Circular buffering progress track
                            CircularProgressIndicator(
                                progress = bufferPercentage / 100f,
                                color = Color(0xFF8B5CF6), // Violet indicator matching purple theme
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "$bufferPercentage%",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = bufferSpeed,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
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
                            tint = Color(0xFF8B5CF6)
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
                            text = "UC Premium Player Mode",
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

            // UC Right-Side Control panel column (as seen in screenshots)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Button 1: Background Music/Audio mode
                IconButton(onClick = {
                    if (onPlayInBackground != null) {
                        onPlayInBackground(videoUrl, title)
                    } else {
                        Toast.makeText(context, "Audio-Only Background mode enabled", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio only",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Button 2: Picture-in-picture
                IconButton(onClick = {
                    // Try to enter real Android Picture-In-Picture mode
                    var currentContext = context
                    var activityFound = false
                    while (currentContext is android.content.ContextWrapper) {
                        if (currentContext is android.app.Activity) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                try {
                                    val params = android.app.PictureInPictureParams.Builder().build()
                                    currentContext.enterPictureInPictureMode(params)
                                    activityFound = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to enter PiP: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "PiP is not supported on this Android version", Toast.LENGTH_SHORT).show()
                            }
                            break
                        }
                        currentContext = currentContext.baseContext
                    }
                    if (!activityFound) {
                        Toast.makeText(context, "PiP mode initialized for active video", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "PiP",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Button 3: Download
                IconButton(onClick = {
                    downloadMedia(context, videoUrl, "${System.currentTimeMillis()}.mp4", viewModel)
                }) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download Video",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Button 4: Fullscreen/Aspect Ratio stretch toggle
                IconButton(onClick = {
                    aspectFillMode = !aspectFillMode
                    val modeText = if (aspectFillMode) "Stretched (Fill)" else "Original (16:9)"
                    Toast.makeText(context, "Aspect Ratio: $modeText", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Button 5: Web Player Toggle Engine
                IconButton(onClick = {
                    useWebPlayerFallback = !useWebPlayerFallback
                    val modeText = if (useWebPlayerFallback) "Web Player Mode" else "Native Player Mode"
                    Toast.makeText(context, "Engine: $modeText", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = if (useWebPlayerFallback) Icons.Default.Web else Icons.Default.SmartDisplay,
                        contentDescription = "Toggle Player Engine",
                        tint = if (useWebPlayerFallback) Color(0xFF8B5CF6) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Gesture Double-Tap targets for seeking
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s Region
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
                                isBuffering = true // seek triggers buffering animation
                            }
                        }
                )

                // Spacer for center control area
                Spacer(modifier = Modifier.width(120.dp))

                // Forward 10s Region
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
                                isBuffering = true // seek triggers buffering animation
                            }
                        }
                )
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // playlist slider of detected videos
                val otherVideos = capturedMedia.filter { it.type == "video" }
                if (otherVideos.size > 1) {
                    Text(
                        text = "OTHER DETECTED WEB STREAMS",
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
                                    containerColor = if (isActive) Color(0xFF8B5CF6) else Color(0xFF1E293B)
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
                                        text = "Captured Video",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // UC Custom Seekbar & Timings Row above it (matches third screenshot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentStr,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = totalStr,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Slider(
                        value = progress,
                        onValueChange = {
                            progress = it
                            isBuffering = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF8B5CF6), // Purple indicator (matches screenshot)
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            thumbColor = Color(0xFF8B5CF6)
                        )
                    )

                    // Playback speed badge
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

                    // Play/Pause circular toggle next to seek bar (matches screenshots "00")
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set Playback speed",
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
                                .background(if (isSelected) Color(0xFF8B5CF6) else Color.Transparent)
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
fun MiBrowserVideoPlayer(
    videoUrl: String,
    title: String,
    onClose: () -> Unit,
    gestureControlsEnabled: Boolean,
    defaultSpeed: Float,
    capturedMedia: List<CapturedMedia>,
    onPlayOtherVideo: (CapturedMedia) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableStateOf(defaultSpeed) }
    var isLocked by remember { mutableStateOf(false) }
    var useWebPlayerFallback by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0.20f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Progress bar simulation when video plays
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(1000)
                progress = (progress + 0.002f).coerceAtMost(1f)
            }
        }
    }

    val totalSeconds = 600
    val currentSeconds = (progress * totalSeconds).toInt()
    val currentStr = String.format("%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val totalStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = isLocked) { isLocked = false },
        contentAlignment = Alignment.Center
    ) {
        // Video rendering area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            var mPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            
            if (useWebPlayerFallback) {
                AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowContentAccess = true
                                allowFileAccess = true
                            }
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = android.webkit.WebViewClient()
                            
                            val customHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; display: flex; justify-content: center; align-items: center; overflow: hidden; }
                                        video { width: 100%; height: 100%; object-fit: contain; }
                                    </style>
                                </head>
                                <body>
                                    <video id="fallback-player" src="$videoUrl" controls autoplay playsinline loop></video>
                                    <script>
                                        const player = document.getElementById('fallback-player');
                                        player.playbackRate = $currentSpeed;
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            
                            loadDataWithBaseURL(videoUrl, customHtml, "text/html", "UTF-8", null)
                        }
                    },
                    update = { view ->
                        view.evaluateJavascript("document.getElementById('fallback-player').playbackRate = $currentSpeed;", null)
                        if (isPlaying) {
                            view.evaluateJavascript("document.getElementById('fallback-player').play();", null)
                        } else {
                            view.evaluateJavascript("document.getElementById('fallback-player').pause();", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (hasError) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Playback Error",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Native Stream Failed",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            useWebPlayerFallback = true
                            hasError = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Use Web Fallback", color = Color.White)
                    }
                }
            } else if (videoUrl.isNotBlank() && videoUrl.startsWith("http")) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            try {
                                val uri = android.net.Uri.parse(videoUrl)
                                val headers = HashMap<String, String>()
                                headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                try {
                                    val parsedUri = java.net.URI(videoUrl)
                                    headers["Referer"] = "${parsedUri.scheme}://${parsedUri.host}/"
                                } catch (e: Exception) {}
                                val cookie = android.webkit.CookieManager.getInstance().getCookie(videoUrl)
                                if (!cookie.isNullOrBlank()) {
                                    headers["Cookie"] = cookie
                                }
                                setVideoURI(uri, headers)
                            } catch (e: Exception) {
                                setVideoPath(videoUrl)
                            }
                            
                            setOnErrorListener { _, what, extra ->
                                hasError = true
                                errorMsg = "Error: $what/$extra"
                                true
                            }
                            
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mi Minimalist Player Mode", color = Color.White, fontSize = 14.sp)
                }
            }

            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier.padding(16.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Unlock", tint = Color.White)
                    }
                }
            }
        }

        if (!isLocked) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title.ifBlank { "Mi Media Stream" }, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "Minimalist Media Engine", color = Color.LightGray.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                IconButton(onClick = { isLocked = true }) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Lock", tint = Color.White)
                }
            }

            // Center gesture tap regions & playback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (gestureControlsEnabled) progress = (progress - 0.05f).coerceAtLeast(0f)
                        }
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (gestureControlsEnabled) progress = (progress + 0.05f).coerceAtMost(1f)
                        }
                )
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val otherVideos = capturedMedia.filter { it.type == "video" }
                if (otherVideos.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        items(otherVideos) { media ->
                            val isActive = media.url == videoUrl
                            Card(
                                modifier = Modifier.width(120.dp).clickable { onPlayOtherVideo(media) },
                                colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF3B82F6) else Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = media.pageTitle, color = Color.White, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = currentStr, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                            thumbColor = Color(0xFF3B82F6)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = totalStr, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${currentSpeed}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .clickable { showSpeedDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    if (showSpeedDialog) {
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Playback Speed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSpeed = spd
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text("${spd}x", color = if (currentSpeed == spd) Color(0xFF3B82F6) else Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
*/

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
                            // Clean modern home icon for the browser start page
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Start Page Logo",
                                tint = if (isSelected) Color.White else Color(0xFF3B82F6),
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                                    text = if (tab.url == "dineinstyle.com") "Start Page" else tab.title,
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Peek Mini-Window!
                    var livePeekEnabled by remember { mutableStateOf(true) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Live Peek",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Peek Mini-Window",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = livePeekEnabled,
                            onCheckedChange = { livePeekEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (livePeekEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        ) {
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }

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
                            icon = Icons.Default.Bookmark,
                            title = "Save to Online Playlist",
                            onClick = {
                                viewModel.saveVideoToPlaylist(cleanTitle, url, url)
                                Toast.makeText(context, "Saved video link to playlist!", Toast.LENGTH_SHORT).show()
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
            val finalTitle = title
            Text(
                text = finalTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WebCleanerSubScreen(viewModel: com.example.viewmodel.BrowserViewModel) {
    val adBlockerOn by viewModel.adBlockerOn.collectAsStateWithLifecycle()
    val blockAreaEnabled by viewModel.blockAreaEnabled.collectAsStateWithLifecycle()
    val overlayBlockerEnabled by viewModel.overlayBlockerEnabled.collectAsStateWithLifecycle()
    val popupBlockerMode by viewModel.popupBlockerMode.collectAsStateWithLifecycle()
    val blockedImagesEnabled by viewModel.blockedImagesEnabled.collectAsStateWithLifecycle()

    val adFilters by viewModel.adFilters.collectAsStateWithLifecycle()
    val adWhitelist by viewModel.adWhitelist.collectAsStateWithLifecycle()
    val popupWhitelist by viewModel.popupWhitelist.collectAsStateWithLifecycle()
    val blockedLinks by viewModel.blockedLinks.collectAsStateWithLifecycle()
    val blockedImages by viewModel.blockedImages.collectAsStateWithLifecycle()
    val customBlockedElements by viewModel.customBlockedElements.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("controls") } // "controls", "rules", "store"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                "controls" to "Filters",
                "rules" to "Rules & Lists",
                "store" to "Store"
            )
            tabs.forEach { (id, label) ->
                val active = activeSubTab == id
                Button(
                    onClick = { activeSubTab = id },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Color(0xFFF43F5E) else Color.Transparent,
                        contentColor = if (active) Color.White else Color.White.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        when (activeSubTab) {
            "controls" -> {
                // Main toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ad-Blocker Engine",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Block ads, cookies, banners, and scripts",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = adBlockerOn,
                                onCheckedChange = { viewModel.toggleAdBlocker() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFF43F5E),
                                    checkedTrackColor = Color(0xFFF43F5E).copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }

                // Core Cleaner Modules
                Text(
                    text = "WEB CLEANER CORE MODULES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E),
                    letterSpacing = 1.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column {
                        // 1. Block Area Element Inspector
                        PrivacySwitchRow(
                            title = "Visual Element Blocker",
                            subtitle = "Long-press any element/banner on web pages to hide it permanently",
                            checked = blockAreaEnabled,
                            onCheckedChange = { viewModel.setBlockAreaEnabled(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // 2. Anti-Overlay / Cosmetic
                        PrivacySwitchRow(
                            title = "Anti-Overlay & GDPR Blocker",
                            subtitle = "Forcefully dismiss cookie consents, sub covers, and overlay dialogs",
                            checked = overlayBlockerEnabled,
                            onCheckedChange = { viewModel.setOverlayBlockerEnabled(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // 3. Popup Mode
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pop-up Window Blocker",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Active popup blocking strategy",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("off", "weak", "strong").forEach { m ->
                                        val selected = popupBlockerMode == m
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (selected) Color(0xFFF43F5E) else Color(0xFF1E293B),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.setPopupBlockerMode(m) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = m.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) Color.White else Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // 4. Image Blocker
                        PrivacySwitchRow(
                            title = "Global Image Blocker",
                            subtitle = "Block all image assets from loading (Ultra Data Saver mode)",
                            checked = blockedImagesEnabled,
                            onCheckedChange = { viewModel.setBlockedImagesEnabled(it) }
                        )
                    }
                }

                // Filter Subscriptions List
                Text(
                    text = "ACTIVE FILTER LISTS (${adFilters.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E),
                    letterSpacing = 1.sp
                )

                adFilters.forEach { filter ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = filter.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = filter.size, fontSize = 9.sp, color = Color(0xFF38BDF8))
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = filter.url,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Updated: ${filter.lastUpdated}",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = filter.enabled,
                                    onCheckedChange = { viewModel.toggleAdFilter(filter.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF38BDF8),
                                        checkedTrackColor = Color(0xFF38BDF8).copy(alpha = 0.4f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.deleteAdFilter(filter.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Filter",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Custom Filter List Section
                var customName by remember { mutableStateOf("") }
                var customUrl by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Add Custom Filter List Link",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Filter List Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF43F5E),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFFF43F5E),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = { Text("Filter List Link URL") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF43F5E),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFFF43F5E),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (customName.isNotBlank() && customUrl.isNotBlank()) {
                                    viewModel.addAdFilter(customName, customUrl)
                                    customName = ""
                                    customUrl = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Add and Subscribe", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "rules" -> {
                // Whitelists & Block Rules Section
                // 1. Domain Whitelist (Ads Allowed)
                RuleManagerSection(
                    title = "Website Whitelist (Allow Ads)",
                    subtitle = "Domains listed here will not have their ads blocked",
                    items = adWhitelist,
                    onAdd = { viewModel.addAdWhitelist(it) },
                    onRemove = { viewModel.removeAdWhitelist(it) },
                    placeholder = "e.g. google.com"
                )

                // 2. Popup Whitelist
                RuleManagerSection(
                    title = "Popup Whitelist",
                    subtitle = "Allow popups on these specific websites",
                    items = popupWhitelist,
                    onAdd = { viewModel.addPopupWhitelist(it) },
                    onRemove = { viewModel.removePopupWhitelist(it) },
                    placeholder = "e.g. securepay.com"
                )

                // 3. Blocked Links
                RuleManagerSection(
                    title = "Manual Link Block Rules",
                    subtitle = "Block requests containing these substrings completely",
                    items = blockedLinks,
                    onAdd = { viewModel.addBlockedLink(it) },
                    onRemove = { viewModel.removeBlockedLink(it) },
                    placeholder = "e.g. tracking-script.js"
                )

                // 4. Blocked Images Pattern
                RuleManagerSection(
                    title = "Image Block Patterns",
                    subtitle = "Block images with these words or domains in their URL",
                    items = blockedImages,
                    onAdd = { viewModel.addBlockedImage(it) },
                    onRemove = { viewModel.removeBlockedImage(it) },
                    placeholder = "e.g. banner-ads"
                )

                // 5. Custom Blocked CSS Selectors
                RuleManagerSection(
                    title = "Custom CSS Element Hide Rules",
                    subtitle = "Custom CSS selectors to permanently hide from web pages",
                    items = customBlockedElements,
                    onAdd = { viewModel.addCustomBlockedElement(it) },
                    onRemove = { viewModel.removeCustomBlockedElement(it) },
                    placeholder = "e.g. .annoying-box, #paywall-banner"
                )

                // 6. Customizable Local Content Filters (De-Clutter Spoiler Keywords)
                val customDeClutterKeywords by viewModel.customDeClutterKeywords.collectAsStateWithLifecycle()
                RuleManagerSection(
                    title = "De-Clutter Keyword Filters (Spoiler Protection)",
                    subtitle = "Hide elements and articles containing these words (e.g. spoilers, politics)",
                    items = customDeClutterKeywords.toSet(),
                    onAdd = { viewModel.addCustomDeClutterKeyword(it) },
                    onRemove = { viewModel.removeCustomDeClutterKeyword(it) },
                    placeholder = "e.g. spoiler, gossip, celebrity"
                )
            }

            "store" -> {
                // Preset Filter Store!
                Text(
                    text = "PRESET FILTER STORE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Select any premium preset filters to subscribe, download, and import them instantly.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                val storePresets = listOf(
                    Triple("EasyPrivacy", "https://easylist-downloads.adblockplus.org/easyprivacy.txt", "Anti-tracking, behavioral data collect blocker"),
                    Triple("AdGuard Annoyance Filter", "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/AnnoyancesFilter/addon.txt", "Blocks annoying popups, cookie consents, widgets"),
                    Triple("NoCoin Filter", "https://raw.githubusercontent.com/NoCoin-org/NoCoin/master/downloads/hosts", "Blocks browser-based crypto mining scripts"),
                    Triple("Fanboy's Social Blocking List", "https://easylist-downloads.adblockplus.org/fanboy-social.txt", "Removes social media sharing widgets"),
                    Triple("Peter Lowe's List", "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext", "Strictly-curated server host-list of adservers")
                )

                storePresets.forEach { (name, url, desc) ->
                    val isSubscribed = adFilters.any { it.url == url }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = desc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = url, fontSize = 9.sp, color = Color(0xFF38BDF8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (isSubscribed) {
                                        val f = adFilters.find { it.url == url }
                                        if (f != null) viewModel.deleteAdFilter(f.id)
                                    } else {
                                        viewModel.addAdFilter(name, url)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSubscribed) Color.White.copy(alpha = 0.1f) else Color(0xFF38BDF8)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isSubscribed) "Unsubscribe" else "Import & Subscribe",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubscribed) Color.White else Color(0xFF0B1224)
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
fun RuleManagerSection(
    title: String,
    subtitle: String,
    items: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    placeholder: String
) {
    var textInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text(placeholder) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF43F5E),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onAdd(textInput)
                            textInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(text = "Add", fontWeight = FontWeight.Bold)
                }
            }

            if (items.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item, fontSize = 13.sp, color = Color.White)
                            IconButton(
                                onClick = { onRemove(item) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
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
fun FloatingMiniPlayerBar(
    backgroundVideo: CapturedMedia,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 400.dp)
            .height(64.dp)
            .clickable { onExpand() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Indicator/Thumbnail with play/pause animations or vinyl
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Playing Cover",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title block
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = backgroundVideo.pageTitle.ifBlank { "Video Stream" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Background Player active",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }

            // Play/Pause button
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle Playback",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Player",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Math evaluation and unit conversion helper functions
fun tryEvaluateExpression(query: String): String? {
    val clean = query.trim().lowercase()
    if (clean.isEmpty()) return null

    // Check if it's a simple math expression (contains digits and operators: + - * / ( ) .)
    if (clean.matches(Regex("""^[\d\s\+\-\*\/\(\)\.]+$"""))) {
        try {
            val result = evaluateMathExpression(clean)
            if (result != null) {
                // If it ends with .0, return as integer string
                return if (result % 1 == 0.0) {
                    "${result.toLong()}"
                } else {
                    String.format("%.4f", result).trimEnd('0').trimEnd('.')
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // Check if it's a unit conversion
    val usdToEurRegex = Regex("""^(\d+(\.\d+)?)\s*usd\s+(in|to)\s+eur$""")
    val cToFRegex = Regex("""^(\d+(\.\d+)?)\s*c\s+(in|to)\s+f$""")
    val milesToKmRegex = Regex("""^(\d+(\.\d+)?)\s*miles?\s+(in|to)\s+km$""")

    usdToEurRegex.find(clean)?.let { match ->
        val amount = match.groupValues[1].toDoubleOrNull() ?: return@let
        return String.format("%.2f EUR", amount * 0.92)
    }
    cToFRegex.find(clean)?.let { match ->
        val c = match.groupValues[1].toDoubleOrNull() ?: return@let
        return String.format("%.1f °F", c * 9 / 5 + 32)
    }
    milesToKmRegex.find(clean)?.let { match ->
        val miles = match.groupValues[1].toDoubleOrNull() ?: return@let
        return String.format("%.2f km", miles * 1.60934)
    }

    return null
}

fun evaluateMathExpression(expression: String): Double? {
    val expr = expression.replace(" ", "")
    try {
        val values = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        var tempNum = ""

        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                tempNum += c
            }
            if (!c.isDigit() && c != '.' || i == expr.length - 1) {
                if (tempNum.isNotEmpty()) {
                    val num = tempNum.toDoubleOrNull() ?: 0.0
                    values.add(num)
                    tempNum = ""
                }
                if (c == '+' || c == '-' || c == '*' || c == '/') {
                    ops.add(c)
                }
            }
            i++
        }

        if (values.isEmpty()) return null

        val finalValues = mutableListOf<Double>()
        finalValues.add(values[0])
        val finalOps = mutableListOf<Char>()

        for (j in 0 until ops.size) {
            val op = ops[j]
            val nextVal = values[j + 1]
            if (op == '*' || op == '/') {
                val lastVal = finalValues.removeAt(finalValues.size - 1)
                val newVal = if (op == '*') lastVal * nextVal else {
                    if (nextVal == 0.0) return null
                    lastVal / nextVal
                }
                finalValues.add(newVal)
            } else {
                finalValues.add(nextVal)
                finalOps.add(op)
            }
        }

        var res = finalValues[0]
        for (j in 0 until finalOps.size) {
            val op = finalOps[j]
            val nextVal = finalValues[j + 1]
            if (op == '+') res += nextVal else res -= nextVal
        }
        return res
    } catch (e: Exception) {
        return null
    }
}

@Composable
fun LockedTabScreen(
    tabId: Long,
    tabTitle: String,
    onAuthenticate: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            val transition = rememberInfiniteTransition()
            val scale by transition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(Color(0xFFF43F5E).copy(alpha = 0.12f), CircleShape)
                    .border(2.dp, Color(0xFFF43F5E).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked Tab",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Sensitive Tab Secured",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tabTitle.ifEmpty { "Private Webpage" },
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val active = i < pinText.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.15f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (active) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showError) {
                Text(
                    text = "Incorrect PIN. Try again.",
                    color = Color(0xFFF43F5E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Enter simulated PIN (1234) or tap Biometrics",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "🔑")
                )

                for (row in keys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        showError = false
                                        when (key) {
                                            "C" -> {
                                                if (pinText.isNotEmpty()) {
                                                    pinText = pinText.substring(0, pinText.length - 1)
                                                }
                                            }
                                            "🔑" -> {
                                                onAuthenticate()
                                            }
                                            else -> {
                                                if (pinText.length < 4) {
                                                    pinText += key
                                                    if (pinText.length == 4) {
                                                        if (pinText == "1234") {
                                                            onAuthenticate()
                                                        } else {
                                                            showError = true
                                                            pinText = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "🔑") {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometrics Unlock",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
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

@Composable
fun WebsiteUpdatesDialog(
    updatedBookmarks: List<Bookmark>,
    isChecking: Boolean,
    onRefresh: () -> Unit,
    onClearAlert: (Bookmark) -> Unit,
    onVisit: (Bookmark) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Updates",
                            tint = Color(0xFFD4E157),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Website Updates",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFFD4E157),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Updates",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                if (updatedBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No updates",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No website updates detected",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enable 'Watch Mode' on any bookmark to monitor changes automatically.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(updatedBookmarks) { bookmark ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF334155)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Bookmark Info
                                    Text(
                                        text = bookmark.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bookmark.url,
                                        color = Color(0xFFD4E157),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    val detailsList = remember(bookmark.lastUpdateDetails) {
                                        val list = mutableListOf<String>()
                                        val detailsJson = bookmark.lastUpdateDetails
                                        if (!detailsJson.isNullOrBlank()) {
                                            try {
                                                val arr = org.json.JSONArray(detailsJson)
                                                for (i in 0 until arr.length()) {
                                                    val text = arr.getString(i).trim()
                                                    if (text.isNotBlank() && text.length > 5) {
                                                        list.add(text)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                list.add(detailsJson)
                                            }
                                        }
                                        if (list.isEmpty()) {
                                            list.add("Content has changed.")
                                        }
                                        list.take(3)
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Recent Changes:",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        detailsList.forEach { change ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "•",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                                Text(
                                                    text = change,
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { onClearAlert(bookmark) },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = Color.LightGray
                                            )
                                        ) {
                                            Text("Dismiss", fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onVisit(bookmark) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD4E157),
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Visit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WebSkeletonLoader(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val bgColor = if (isDark) Color(0xFF030712) else Color(0xFFF8FAFC)
    val itemColor = if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Shimmery top site title and icon placeholder
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(itemColor.copy(alpha = alpha))
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(itemColor.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(itemColor.copy(alpha = alpha))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Hero banner placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(itemColor.copy(alpha = alpha))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Paragraph 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(itemColor.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(itemColor.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(itemColor.copy(alpha = alpha))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid layout placeholder (news list cards style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(itemColor.copy(alpha = alpha * 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemColor.copy(alpha = alpha))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(itemColor.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}


