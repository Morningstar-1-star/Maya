package com.example.ui

import android.content.Context
import android.text.format.Formatter
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.BrowserTab
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

// --- 1. DUPLICATE TAB ALERT DIALOG ---
@Composable
fun DuplicateTabAlertDialog(
    viewModel: BrowserViewModel,
    onRequest: DuplicateTabRequest,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CopyAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
        title = {
            Text(
                text = "Duplicate Tab Detected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "A tab is already open with a duplicate page:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = onRequest.existingTabTitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                        Text(
                            text = onRequest.url,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "Would you like to switch to it, open a duplicate anyway, or replace the old one?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.selectTab(onRequest.existingTabId)
                        viewModel.highlightTab(onRequest.existingTabId)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch to Existing Tab")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.addTabForce(onRequest.url)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Anyway", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.closeTab(onRequest.existingTabId)
                            viewModel.addTabForce(onRequest.url)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Replace Old", fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}


// --- 2. PERMISSION DASHBOARD SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePermissionDashboardSheet(
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val currentTabId by viewModel.activeTabId.collectAsState()
    val allTabs by viewModel.allTabs.collectAsState()
    val activeTab = remember(allTabs, currentTabId) { allTabs.find { it.id == currentTabId } }
    val currentDomain = remember(activeTab) {
        try {
            val uri = URI(activeTab?.url ?: "")
            val host = uri.host ?: "website.com"
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            "website.com"
        }
    }

    val permissionTypes = listOf("Camera", "Microphone", "Location", "Notifications", "Clipboard", "File access", "Downloads", "Popups", "Autoplay")

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Website Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDomain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            var selectedTabIdx by remember { mutableStateOf(0) }
            TabRow(selectedTabIndex = selectedTabIdx) {
                Tab(selected = selectedTabIdx == 0, onClick = { selectedTabIdx = 0 }) {
                    Box(modifier = Modifier.padding(12.dp)) { Text("Manage Settings", fontWeight = FontWeight.Bold) }
                }
                Tab(selected = selectedTabIdx == 1, onClick = { selectedTabIdx = 1 }) {
                    Box(modifier = Modifier.padding(12.dp)) { Text("Chronological Timeline", fontWeight = FontWeight.Bold) }
                }
            }

            if (selectedTabIdx == 0) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(permissionTypes) { type ->
                        val state = BrowserFeaturesManager.getPermissionState(currentDomain, type)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = when(type) {
                                            "Camera" -> Icons.Default.Videocam
                                            "Microphone" -> Icons.Default.Mic
                                            "Location" -> Icons.Default.LocationOn
                                            "Notifications" -> Icons.Default.Notifications
                                            "Clipboard" -> Icons.Default.ContentPaste
                                            "File access" -> Icons.Default.FolderOpen
                                            "Downloads" -> Icons.Default.Download
                                            "Popups" -> Icons.Default.OpenInNew
                                            else -> Icons.Default.PlayArrow
                                        },
                                        contentDescription = type,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(text = type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "Status: $state",
                                            fontSize = 11.sp,
                                            color = if (state == "Allow") Color(0xFF10B981) else if (state == "Block") Color(0xFFEF4444) else Color.Gray
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssistChip(
                                        onClick = {
                                            BrowserFeaturesManager.updatePermission(context, currentDomain, type, "Allow")
                                        },
                                        label = { Text("Allow", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (state == "Allow") Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent
                                        )
                                    )
                                    AssistChip(
                                        onClick = {
                                            BrowserFeaturesManager.updatePermission(context, currentDomain, type, "Ask")
                                        },
                                        label = { Text("Ask", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (state == "Ask") MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                        )
                                    )
                                    AssistChip(
                                        onClick = {
                                            BrowserFeaturesManager.updatePermission(context, currentDomain, type, "Block")
                                        },
                                        label = { Text("Block", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (state == "Block") Color(0xFFEF4444).copy(alpha = 0.2f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        permissionTypes.forEach { type ->
                            BrowserFeaturesManager.updatePermission(context, currentDomain, type, "Ask")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant Revoke All")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (BrowserFeaturesManager.permissionTimeline.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No requests logged yet.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(BrowserFeaturesManager.permissionTimeline) { entry ->
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "${entry.permissionType} - ${entry.action}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = entry.domain, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(text = timeStr, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- 3. SPLIT SCREEN WEB VIEW ---
@Composable
fun SplitScreenTabWebView(
    tabId: Long,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val splitState = BrowserFeaturesManager.splitTabStates[tabId] ?: SplitState(tabId)

    val webView1 = remember(tabId) {
        WebViewPool.getOrCreateWebView(context, tabId * 1000 + 1, viewModel)
    }
    val webView2 = remember(tabId) {
        WebViewPool.getOrCreateWebView(context, tabId * 1000 + 2, viewModel)
    }

    LaunchedEffect(splitState.url1) {
        if (webView1.url != splitState.url1) {
            webView1.loadUrl(splitState.url1)
        }
    }

    LaunchedEffect(splitState.url2) {
        if (webView2.url != splitState.url2) {
            webView2.loadUrl(splitState.url2)
        }
    }

    var dividerRatio by remember { mutableStateOf(splitState.ratio) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        if (splitState.isVertical) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel
                Column(
                    modifier = Modifier
                        .weight(dividerRatio)
                        .fillMaxHeight()
                ) {
                    SplitPanelHeader(1, webView1, splitState.url1) { url ->
                        BrowserFeaturesManager.updateSplitUrl(tabId, 1, url)
                    }
                    AndroidView(
                        factory = { webView1 },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }

                // Vertical Draggable Divider
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount.x / containerWidth.toPx()
                                dividerRatio = (dividerRatio + delta).coerceIn(0.2f, 0.8f)
                                BrowserFeaturesManager.setSplitRatio(tabId, dividerRatio)
                            }
                        }
                )

                // Right Panel
                Column(
                    modifier = Modifier
                        .weight(1f - dividerRatio)
                        .fillMaxHeight()
                ) {
                    SplitPanelHeader(2, webView2, splitState.url2) { url ->
                        BrowserFeaturesManager.updateSplitUrl(tabId, 2, url)
                    }
                    AndroidView(
                        factory = { webView2 },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Panel
                Column(
                    modifier = Modifier
                        .weight(dividerRatio)
                        .fillMaxWidth()
                ) {
                    SplitPanelHeader(1, webView1, splitState.url1) { url ->
                        BrowserFeaturesManager.updateSplitUrl(tabId, 1, url)
                    }
                    AndroidView(
                        factory = { webView1 },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }

                // Horizontal Draggable Divider
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount.y / containerHeight.toPx()
                                dividerRatio = (dividerRatio + delta).coerceIn(0.2f, 0.8f)
                                BrowserFeaturesManager.setSplitRatio(tabId, dividerRatio)
                            }
                        }
                )

                // Bottom Panel
                Column(
                    modifier = Modifier
                        .weight(1f - dividerRatio)
                        .fillMaxWidth()
                ) {
                    SplitPanelHeader(2, webView2, splitState.url2) { url ->
                        BrowserFeaturesManager.updateSplitUrl(tabId, 2, url)
                    }
                    AndroidView(
                        factory = { webView2 },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }

        // Float Control Bar for panel actions (Swap, orientation, close)
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { BrowserFeaturesManager.swapSplitPanels(tabId) }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Panels")
                }
                IconButton(onClick = { BrowserFeaturesManager.setSplitOrientation(tabId, !splitState.isVertical) }) {
                    Icon(if (splitState.isVertical) Icons.Default.ViewStream else Icons.Default.ViewWeek, contentDescription = "Toggle orientation")
                }
                IconButton(onClick = { 
                    BrowserFeaturesManager.toggleSplitScreen(tabId, splitState.url1)
                }) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Close Split (Full Screen Left)")
                }
            }
        }
    }
}

@Composable
fun SplitPanelHeader(
    panelIdx: Int,
    webView: WebView,
    currentUrl: String,
    onUrlGo: (String) -> Unit
) {
    var textInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(42.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { if (webView.canGoBack()) webView.goBack() }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { webView.reload() }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(16.dp))
            }
            
            androidx.compose.foundation.text.BasicTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Go
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onGo = { onUrlGo(textInput) }
                )
            )
            Text(
                text = "P$panelIdx",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}


// --- 4. SMART READER VIEW ---
@Composable
fun SmartReaderView(
    tabId: Long,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val content = BrowserFeaturesManager.readerContentMap[tabId] ?: Triple("Web Article", "", emptyList())
    val settings by BrowserFeaturesManager.readerSettings
    
    val wordsCount = remember(content) { content.third.sumOf { it.split("\\s+".toRegex()).size } }
    val readingTimeMins = remember(wordsCount) { (wordsCount / 200).coerceAtLeast(1) }

    val scope = rememberCoroutineScope()
    var aiSummary by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }

    var translatedActive by remember { mutableStateOf(false) }
    var translatedText by remember { mutableStateOf<List<String>>(emptyList()) }
    var translationLoading by remember { mutableStateOf(false) }

    val bg = when(settings.theme) {
        "Dark" -> Color(0xFF1E293B)
        "Sepia" -> Color(0xFFFDF6E3)
        "Solarized" -> Color(0xFFF5F2EB)
        else -> Color.White
    }
    
    val textPrimary = when(settings.theme) {
        "Dark" -> Color.White
        "Sepia" -> Color(0xFF586E75)
        "Solarized" -> Color(0xFF073642)
        else -> Color.Black
    }

    Box(modifier = modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { BrowserFeaturesManager.toggleReaderMode(tabId, null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text("Reader Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("$readingTimeMins min read • $wordsCount words", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            if (settings.isTtsPlaying) {
                                BrowserFeaturesManager.stopTTS()
                            } else {
                                val body = content.third
                                BrowserFeaturesManager.speakReaderContent(context, body)
                            }
                        }) {
                            Icon(if (settings.isTtsPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute, contentDescription = "TTS")
                        }

                        IconButton(onClick = {
                            if (aiSummary.isNotEmpty()) {
                                aiSummary = ""
                            } else {
                                aiLoading = true
                                scope.launch {
                                    val fullBody = content.third.joinToString("\n\n")
                                    aiSummary = viewModel.getGeminiExplanation(fullBody, "summary")
                                    aiLoading = false
                                }
                            }
                        }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Summary")
                        }

                        IconButton(onClick = {
                            if (translatedActive) {
                                translatedActive = false
                            } else {
                                translationLoading = true
                                scope.launch {
                                    val fullBody = content.third.joinToString("\n\n")
                                    val translatedResult = viewModel.getGeminiExplanation(fullBody, "translate")
                                    translatedText = translatedResult.split("\n\n")
                                    translatedActive = true
                                    translationLoading = false
                                }
                            }
                        }) {
                            Icon(Icons.Default.Translate, contentDescription = "Translate")
                        }
                    }
                }
            }

            // Reader Area
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Settings Controls
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Aesthetic Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Light", "Dark", "Sepia", "Solarized").forEach { themeName ->
                                        TextButton(
                                            onClick = { BrowserFeaturesManager.readerSettings.value = settings.copy(theme = themeName) },
                                            colors = ButtonDefaults.textButtonColors(
                                                containerColor = if (settings.theme == themeName) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            ),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(themeName, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Text Size", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { BrowserFeaturesManager.readerSettings.value = settings.copy(fontSizeSp = (settings.fontSizeSp - 2).coerceAtLeast(12f)) }) {
                                        Icon(Icons.Default.Remove, contentDescription = "Smaller")
                                    }
                                    Text("${settings.fontSizeSp.toInt()} sp", fontSize = 12.sp)
                                    IconButton(onClick = { BrowserFeaturesManager.readerSettings.value = settings.copy(fontSizeSp = (settings.fontSizeSp + 2).coerceAtMost(32f)) }) {
                                        Icon(Icons.Default.Add, contentDescription = "Larger")
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Summary Area
                if (aiLoading) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("AI is composing summary...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else if (aiSummary.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("AI Executive Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(text = aiSummary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Translation loading
                if (translationLoading) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("Translating page text...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Article Info
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = content.first,
                            fontSize = (settings.fontSizeSp + 6).sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            lineHeight = (settings.fontSizeSp + 10).sp
                        )
                        if (content.second.isNotEmpty()) {
                            Text(
                                text = "By ${content.second}",
                                fontSize = 13.sp,
                                color = textPrimary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        HorizontalDivider(color = textPrimary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                    }
                }

                // Paragraphs
                val bodyTextList = if (translatedActive && translatedText.isNotEmpty()) translatedText else content.third
                items(bodyTextList.size) { idx ->
                    val text = bodyTextList[idx]
                    val isSpoken = settings.ttsActiveIndex == idx
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSpoken) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = settings.fontSizeSp.sp,
                            color = textPrimary,
                            lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}


// --- 5. SMART AUTO REFRESH DIALOG ---
@Composable
fun SmartAutoRefreshDialog(
    tabId: Long,
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentRule = BrowserFeaturesManager.autoRefreshRules[tabId] ?: AutoRefreshRule(tabId)
    
    var intervalSecInput by remember { mutableIntStateOf(currentRule.intervalSeconds) }
    var wifiOnly by remember { mutableStateOf(currentRule.wifiOnly) }
    var chargingOnly by remember { mutableStateOf(currentRule.chargingOnly) }
    var visibleOnly by remember { mutableStateOf(currentRule.visibleOnly) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Auto Refresh", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Configure custom timers and criteria for auto reloading the tab contents.", fontSize = 12.sp, color = Color.Gray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Refresh Interval", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { intervalSecInput = (intervalSecInput - 5).coerceAtLeast(5) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Less")
                        }
                        Text("$intervalSecInput s", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(onClick = { intervalSecInput = (intervalSecInput + 5).coerceAtMost(3600) }) {
                            Icon(Icons.Default.Add, contentDescription = "More")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Wi-Fi Only", fontSize = 14.sp)
                    }
                    Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Charging Only", fontSize = 14.sp)
                    }
                    Switch(checked = chargingOnly, onCheckedChange = { chargingOnly = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Visible Only", fontSize = 14.sp)
                    }
                    Switch(checked = visibleOnly, onCheckedChange = { visibleOnly = it })
                }

                if (currentRule.enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentRule.isPaused) currentRule.pauseReason else "Status: Running periodically",
                            color = if (currentRule.isPaused) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRule = AutoRefreshRule(
                        tabId = tabId,
                        enabled = true,
                        intervalSeconds = intervalSecInput,
                        wifiOnly = wifiOnly,
                        chargingOnly = chargingOnly,
                        visibleOnly = visibleOnly
                    )
                    BrowserFeaturesManager.startAutoRefresh(context, tabId, newRule) {
                        // Refresh trigger! Call reload on target web view
                        val wv = WebViewPool.getWebView(tabId)
                        wv?.reload()
                    }
                    onDismiss()
                }
            ) {
                Text("Start Rule")
            }
        },
        dismissButton = {
            if (currentRule.enabled) {
                TextButton(
                    onClick = {
                        BrowserFeaturesManager.stopAutoRefresh(tabId)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Rule")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}


// --- 6. SITE ANALYTICS & CONTROLS SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDashboardAnalyticsSheet(
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val currentTabId by viewModel.activeTabId.collectAsState()
    val allTabs by viewModel.allTabs.collectAsState()
    val activeTab = remember(allTabs, currentTabId) { allTabs.find { it.id == currentTabId } }
    
    val currentDomain = remember(activeTab) {
        try {
            val uri = URI(activeTab?.url ?: "")
            val host = uri.host ?: "website.com"
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            "website.com"
        }
    }

    val analytics = remember(currentDomain) { BrowserFeaturesManager.getOrCreateAnalytics(currentDomain) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Website Control Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDomain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            var selectedSection by remember { mutableStateOf(0) }
            TabRow(selectedTabIndex = selectedSection) {
                Tab(selected = selectedSection == 0, onClick = { selectedSection = 0 }) {
                    Box(modifier = Modifier.padding(10.dp)) { Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
                Tab(selected = selectedSection == 1, onClick = { selectedSection = 1 }) {
                    Box(modifier = Modifier.padding(10.dp)) { Text("Controls", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }

            if (selectedSection == 0) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("PERFORMANCE METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard("CPU Usage", "${analytics.cpuPercent}%", Icons.Default.DeveloperMode, Modifier.weight(1f))
                            MetricCard("RAM Usage", "${analytics.ramMb} MB", Icons.Default.SdCard, Modifier.weight(1f))
                            MetricCard("FPS Rate", "${analytics.fps} Hz", Icons.Default.Speed, Modifier.weight(1f))
                        }
                    }

                    item {
                        Text("NETWORK & STORAGE DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DetailRow("Connection SSL", analytics.sslCertificate, if (analytics.isHttps) Color(0xFF10B981) else Color.Red)
                                DetailRow("Cached Page Size", "${analytics.pageSizeKb} KB")
                                DetailRow("Load Time Speed", "${analytics.loadTimeMs} ms")
                                DetailRow("Total Network Requests", "${analytics.requestsCount}")
                                DetailRow("Stored Cookies Count", "${analytics.cookiesCount}")
                            }
                        }
                    }

                    item {
                        Text("PRIVACY SHIELD STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Column {
                                        Text("Trackers Blocked", fontWeight = FontWeight.Bold)
                                        Text("Intelligent defense shield active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    }
                                }
                                Text("${analytics.trackersBlocked}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                var jsOn by remember { mutableStateOf(BrowserFeaturesManager.siteScriptEnabled[currentDomain] ?: true) }
                                ControlSwitchRow("Execute JavaScript", "Enable or disable web scripts", jsOn) {
                                    jsOn = it
                                    BrowserFeaturesManager.siteScriptEnabled[currentDomain] = it
                                    val wv = WebViewPool.getWebView(currentTabId ?: 0)
                                    if (wv != null) BrowserFeaturesManager.applySiteControls(wv, currentDomain)
                                }
                                HorizontalDivider()
                                var imgOn by remember { mutableStateOf(BrowserFeaturesManager.siteImagesEnabled[currentDomain] ?: true) }
                                ControlSwitchRow("Render Images", "Automatically load photos", imgOn) {
                                    imgOn = it
                                    BrowserFeaturesManager.siteImagesEnabled[currentDomain] = it
                                    val wv = WebViewPool.getWebView(currentTabId ?: 0)
                                    if (wv != null) BrowserFeaturesManager.applySiteControls(wv, currentDomain)
                                }
                                HorizontalDivider()
                                var cssOn by remember { mutableStateOf(BrowserFeaturesManager.siteCssEnabled[currentDomain] ?: true) }
                                ControlSwitchRow("Apply Site CSS", "Inject custom visual styles", cssOn) {
                                    cssOn = it
                                    BrowserFeaturesManager.siteCssEnabled[currentDomain] = it
                                    val wv = WebViewPool.getWebView(currentTabId ?: 0)
                                    if (wv != null) BrowserFeaturesManager.applySiteControls(wv, currentDomain)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Storage Management", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val cookieManager = CookieManager.getInstance()
                                            cookieManager.removeAllCookies(null)
                                            cookieManager.flush()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Clear Cookies", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val storage = WebStorage.getInstance()
                                            storage.deleteAllData()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Clear Cache", fontSize = 11.sp)
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

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: Any,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun ControlSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// Dummy Icon classes because material icon doesn't contain Cpu or Memory directly
