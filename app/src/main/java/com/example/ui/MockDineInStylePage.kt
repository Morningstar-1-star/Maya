package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.CapturedMedia
import com.example.data.HomepageShortcut
import com.example.data.VaultItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MockDineInStylePage(
    modifier: Modifier = Modifier,
    wallpaperUrl: String? = null,
    shortcuts: List<HomepageShortcut> = emptyList(),
    onShortcutClicked: (String) -> Unit = {},
    onAddShortcut: (String, String, String?) -> Unit = { _, _, _ -> },
    onDeleteShortcut: (Long) -> Unit = {},
    onUpdateShortcut: (Long, String, String, String?) -> Unit = { _, _, _, _ -> },
    showNewsSection: Boolean = true,
    onShowNewsSectionChange: (Boolean) -> Unit = {},
    onWallpaperChanged: (String?) -> Unit = {},
    onProductClicked: (String) -> Unit = {},
    chromeThemeActive: Boolean = false,
    chromeThemeNtpBgColor: Int = 0,
    chromeThemeNtpTextColor: Int = 0,
    chromeThemeNtpBgPath: String? = null,
    playlistVideos: List<CapturedMedia> = emptyList(),
    onPlayVideo: (CapturedMedia) -> Unit = {},
    onDeleteVideo: (Long) -> Unit = {},
    allVaultItems: List<VaultItem> = emptyList(),
    onSeeAllVaultClick: () -> Unit = {},
    onDeleteVaultItem: (Long) -> Unit = {},
    themeMode: String = "amoled"
) {
    val isDark = themeMode != "light"
    
    val effectiveTextColor = if (isDark) Color.White else Color.Black
    val effectiveSubTextColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val cardBg = if (isDark) Color(0xFF121212) else Color.White
    val cardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
    val pillBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF4F4F5)
    val accentColor = if (isDark) Color.White else Color.Black
    val onAccentColor = if (isDark) Color.Black else Color.White

    var isAddShortcutDialogOpen by remember { mutableStateOf(false) }
    var isEditShortcutDialogOpen by remember { mutableStateOf(false) }
    var shortcutToEdit by remember { mutableStateOf<HomepageShortcut?>(null) }
    var isWallpaperDialogOpen by remember { mutableStateOf(false) }

    var searchInput by remember { mutableStateOf("") }
    var selectedSearchEngine by remember { mutableStateOf("Google") }

    val hasBgImage = wallpaperUrl != null || (chromeThemeActive && chromeThemeNtpBgPath != null)
    val bgModel = if (chromeThemeActive && chromeThemeNtpBgPath != null) chromeThemeNtpBgPath else wallpaperUrl

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else Color.White)
    ) {
        // Subtle background wallpaper with elegant dark gradient overlay
        if (hasBgImage && bgModel != null) {
            AsyncImage(
                model = bgModel,
                contentDescription = "Custom Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        }

        // Main scrollable Start Page content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 120.dp)
        ) {
            // --- 1. BRAND HEADER & SEARCH BAR ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Browser Logo & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 18.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = pillBg,
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Browser Logo",
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Maya Browser",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = effectiveTextColor,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Fast, private & secure web",
                                fontSize = 12.sp,
                                color = effectiveSubTextColor,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Modern Integrated Search Bar Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color.Black.copy(alpha = 0.12f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Engine Switcher Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = pillBg,
                                modifier = Modifier
                                    .clickable {
                                        selectedSearchEngine = when (selectedSearchEngine) {
                                            "Google" -> "DuckDuckGo"
                                            "DuckDuckGo" -> "Bing"
                                            else -> "Google"
                                        }
                                    }
                                    .padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Engine",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedSearchEngine,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = effectiveTextColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Search Text Input
                            TextField(
                                value = searchInput,
                                onValueChange = { searchInput = it },
                                placeholder = {
                                    Text(
                                        text = "Search or type web address...",
                                        fontSize = 14.sp,
                                        color = effectiveSubTextColor.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = effectiveTextColor,
                                    unfocusedTextColor = effectiveTextColor
                                )
                            )

                            // Submit / Go button
                            if (searchInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val query = searchInput.trim()
                                        val targetUrl = if (query.startsWith("http://") || query.startsWith("https://") || (query.contains(".") && !query.contains(" "))) {
                                            if (!query.startsWith("http://") && !query.startsWith("https://")) "https://$query" else query
                                        } else {
                                            when (selectedSearchEngine) {
                                                "DuckDuckGo" -> "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                                                "Bing" -> "https://www.bing.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                                                else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                                            }
                                        }
                                        onShortcutClicked(targetUrl)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Search",
                                                tint = onAccentColor,
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

            // --- 2. PRIVACY & PERFORMANCE SHIELDS CARD ---
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = pillBg,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Protection",
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Privacy Shields Active",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = effectiveTextColor
                                    )
                                    Text(
                                        text = "Ads, trackers & cryptominers blocked",
                                        fontSize = 11.sp,
                                        color = effectiveSubTextColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = pillBg
                            ) {
                                Text(
                                    text = "SECURE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats counters row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill(
                                count = "99.8%",
                                label = "Fast Engine",
                                icon = Icons.Default.Speed,
                                tint = accentColor,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatPill(
                                count = "Active",
                                label = "Anti-Tracking",
                                icon = Icons.Default.Security,
                                tint = accentColor,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatPill(
                                count = "HTTPS",
                                label = "Encrypted",
                                icon = Icons.Default.Lock,
                                tint = accentColor,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- 2.5 FEATURE HUBS (Category Lists, Private Vault & Telegram Media Saver) ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Lists & Vault Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSeeAllVaultClick() },
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = pillBg,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ViewList,
                                        contentDescription = "Lists",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lists & Vault",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = effectiveTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Categories & Media",
                                    fontSize = 10.sp,
                                    color = effectiveSubTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Telegram Hub Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSeeAllVaultClick() },
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = pillBg,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Telegram",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Telegram Hub",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = effectiveTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Channels & Media",
                                    fontSize = 10.sp,
                                    color = effectiveSubTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. FAVORITES / QUICK ACCESS GRID ---
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Access",
                            color = effectiveTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+ Add Site",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isAddShortcutDialogOpen = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { isWallpaperDialogOpen = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = "Wallpaper",
                                    tint = effectiveSubTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CleanFavoritesGrid(
                        shortcuts = shortcuts,
                        wallpaperActive = wallpaperUrl != null,
                        isDark = isDark,
                        onShortcutClicked = onShortcutClicked,
                        onDeleteShortcut = onDeleteShortcut,
                        onEditShortcut = {
                            shortcutToEdit = it
                            isEditShortcutDialogOpen = true
                        },
                        onAddClick = { isAddShortcutDialogOpen = true }
                    )
                }
            }

            // --- 4. REAL SAVED VIDEOS (Only shown if user actually saved media) ---
            if (playlistVideos.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Saved Videos",
                                    color = effectiveTextColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${playlistVideos.size} videos",
                                color = effectiveSubTextColor,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            playlistVideos.take(5).forEach { video ->
                                RealSavedVideoItem(
                                    video = video,
                                    isDark = isDark,
                                    effectiveTextColor = effectiveTextColor,
                                    effectiveSubTextColor = effectiveSubTextColor,
                                    onPlayVideo = onPlayVideo,
                                    onDeleteVideo = onDeleteVideo
                                )
                            }
                        }
                    }
                }
            }

            // --- 5. PRIVATE VAULT GLANCE (Only shown if items exist) ---
            if (allVaultItems.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Private Vault",
                                    color = effectiveTextColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "View All",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSeeAllVaultClick() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(allVaultItems.take(6)) { vaultItem ->
                                Surface(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { onSeeAllVaultClick() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(70.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(pillBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (vaultItem.url.isNotBlank()) {
                                                AsyncImage(
                                                    model = vaultItem.url,
                                                    contentDescription = vaultItem.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = vaultItem.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = effectiveTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

    // Add Shortcut Dialog
    if (isAddShortcutDialogOpen) {
        CleanAddShortcutDialog(
            onDismiss = { isAddShortcutDialogOpen = false },
            onAdd = { title, url ->
                onAddShortcut(title, url, null)
                isAddShortcutDialogOpen = false
            }
        )
    }

    // Edit Shortcut Dialog
    if (isEditShortcutDialogOpen && shortcutToEdit != null) {
        CleanEditShortcutDialog(
            shortcut = shortcutToEdit!!,
            onDismiss = {
                isEditShortcutDialogOpen = false
                shortcutToEdit = null
            },
            onSave = { id, title, url ->
                onUpdateShortcut(id, title, url, null)
                isEditShortcutDialogOpen = false
                shortcutToEdit = null
            },
            onDelete = { id ->
                onDeleteShortcut(id)
                isEditShortcutDialogOpen = false
                shortcutToEdit = null
            }
        )
    }

    // Wallpaper Dialog
    if (isWallpaperDialogOpen) {
        CleanWallpaperDialog(
            currentWallpaper = wallpaperUrl,
            onDismiss = { isWallpaperDialogOpen = false },
            onSelectWallpaper = {
                onWallpaperChanged(it)
                isWallpaperDialogOpen = false
            }
        )
    }
}

@Composable
private fun StatPill(
    count: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = if (isDark) 0.12f else 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569),
                maxLines = 1
            )
        }
    }
}

@Composable
fun CleanFavoritesGrid(
    shortcuts: List<HomepageShortcut>,
    wallpaperActive: Boolean,
    isDark: Boolean,
    onShortcutClicked: (String) -> Unit,
    onDeleteShortcut: (Long) -> Unit,
    onEditShortcut: (HomepageShortcut) -> Unit,
    onAddClick: () -> Unit
) {
    // Standard default popular shortcuts if list is completely empty
    val displayShortcuts = if (shortcuts.isEmpty()) {
        listOf(
            HomepageShortcut(id = -1, title = "Google", url = "https://www.google.com"),
            HomepageShortcut(id = -2, title = "YouTube", url = "https://www.youtube.com"),
            HomepageShortcut(id = -3, title = "Wikipedia", url = "https://www.wikipedia.org"),
            HomepageShortcut(id = -4, title = "Reddit", url = "https://www.reddit.com"),
            HomepageShortcut(id = -5, title = "GitHub", url = "https://www.github.com"),
            HomepageShortcut(id = -6, title = "DuckDuckGo", url = "https://duckduckgo.com"),
            HomepageShortcut(id = -7, title = "Amazon", url = "https://www.amazon.com")
        )
    } else {
        shortcuts
    }

    val totalCount = displayShortcuts.size + 1
    val rowCount = (totalCount + 3) / 4

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (rowIndex in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (colIndex in 0..3) {
                    val globalIndex = rowIndex * 4 + colIndex
                    if (globalIndex < displayShortcuts.size) {
                        val shortcut = displayShortcuts[globalIndex]
                        Box(modifier = Modifier.weight(1f)) {
                            CleanFavoriteTile(
                                shortcut = shortcut,
                                wallpaperActive = wallpaperActive,
                                isDark = isDark,
                                onClick = { onShortcutClicked(shortcut.url) },
                                onLongClick = {
                                    if (shortcut.id > 0) onEditShortcut(shortcut)
                                }
                            )
                        }
                    } else if (globalIndex == displayShortcuts.size) {
                        // Add Button
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onAddClick() },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDark) Color(0xFF121212) else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Shortcut",
                                        tint = if (isDark) Color.White else Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanFavoriteTile(
    shortcut: HomepageShortcut,
    wallpaperActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tileBg = if (isDark) Color(0xFF121212) else Color.White
    val tileBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
    val textPrimary = if (isDark) Color.White else Color.Black
    val iconFallbackBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF4F4F5)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                ),
            shape = RoundedCornerShape(18.dp),
            color = tileBg,
            border = BorderStroke(1.dp, tileBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (!shortcut.iconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = shortcut.iconUrl,
                        contentDescription = shortcut.title,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = iconFallbackBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = shortcut.title.take(1).uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = shortcut.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RealSavedVideoItem(
    video: CapturedMedia,
    isDark: Boolean,
    effectiveTextColor: Color,
    effectiveSubTextColor: Color,
    onPlayVideo: (CapturedMedia) -> Unit,
    onDeleteVideo: (Long) -> Unit
) {
    val cardBg = if (isDark) Color(0xFF121212) else Color.White
    val cardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
    val playBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF4F4F5)
    val accent = if (isDark) Color.White else Color.Black

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayVideo(video) },
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = playBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.pageTitle.ifBlank { "Saved Web Video" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = effectiveTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.pageUrl.ifBlank { "Video Stream" },
                    fontSize = 11.sp,
                    color = effectiveSubTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { onDeleteVideo(video.id) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = effectiveSubTextColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Brand color palette helper (Monochrome only: strictly Light and AMOLED)
private fun getSiteBrandColor(title: String, url: String): Color {
    return Color.Unspecified
}

@Composable
fun CleanAddShortcutDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Shortcut",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Site Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Web Address (URL)") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (url.isNotBlank()) {
                                val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                                val cleanTitle = if (title.isBlank()) cleanUrl.removePrefix("https://").removePrefix("http://").takeWhile { it != '/' } else title
                                onAdd(cleanTitle, cleanUrl)
                            }
                        },
                        enabled = url.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun CleanEditShortcutDialog(
    shortcut: HomepageShortcut,
    onDismiss: () -> Unit,
    onSave: (Long, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var title by remember { mutableStateOf(shortcut.title) }
    var url by remember { mutableStateOf(shortcut.url) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Shortcut",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Site Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Web Address (URL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onDelete(shortcut.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                if (url.isNotBlank()) {
                                    onSave(shortcut.id, title.ifBlank { shortcut.title }, url)
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

@Composable
fun CleanWallpaperDialog(
    currentWallpaper: String?,
    onDismiss: () -> Unit,
    onSelectWallpaper: (String?) -> Unit
) {
    val presets = listOf(
        Pair("Default (Minimal)", null),
        Pair("Deep Cosmos", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?q=80&w=1200"),
        Pair("Dark Mountains", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=1200"),
        Pair("Ocean Sunset", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1200"),
        Pair("Abstract Flow", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1200")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Start Page Background",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (name, url) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentWallpaper == url) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectWallpaper(url) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (url == null) Icons.Default.Palette else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (currentWallpaper == url) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (currentWallpaper == url) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentWallpaper == url) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
