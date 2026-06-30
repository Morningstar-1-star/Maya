package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.HomepageShortcut
import kotlinx.coroutines.delay

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
    onProductClicked: (String) -> Unit = {}
) {
    var isEditPageOpen by remember { mutableStateOf(false) }
    var isAddShortcutDialogOpen by remember { mutableStateOf(false) }
    var isEditShortcutDialogOpen by remember { mutableStateOf(false) }
    var shortcutToEdit by remember { mutableStateOf<HomepageShortcut?>(null) }
    var showFavoritesSection by remember { mutableStateOf(true) }
    var showICloudTabsSection by remember { mutableStateOf(false) }
    
    // Manage local dynamic list of iCloud tab cards
    var iCloudTabsList by remember {
        mutableStateOf(
            listOf(
                ICloudTabItem(
                    id = 1,
                    title = "27 features that are on iPad but not on iPhone",
                    url = "idownloadblog.com",
                    targetUrl = "https://www.idownloadblog.com",
                    device = "Ankur's Mac",
                    imageUrl = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?q=80&w=300"
                ),
                ICloudTabItem(
                    id = 2,
                    title = "Apple Trade In - Apple",
                    url = "apple.com",
                    targetUrl = "https://www.apple.com/shop/trade-in",
                    device = "Ankur's iPad",
                    imageUrl = "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?q=80&w=300"
                ),
                ICloudTabItem(
                    id = 3,
                    title = "iCloud Calendar",
                    url = "icloud.com",
                    targetUrl = "https://www.icloud.com",
                    device = "Ankur Thakur's iPad",
                    imageUrl = "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?q=80&w=300"
                )
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (wallpaperUrl == null) Color(0xFFF2F2F7) else Color.Black)
    ) {
        // Smoothly fade in/out the background image if custom wallpaper is active
        AnimatedVisibility(
            visible = wallpaperUrl != null,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (wallpaperUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = wallpaperUrl),
                    contentDescription = "Custom Wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Subtle darkening layer on top of wallpaper to guarantee text legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }
        }

        // Main scrollable container
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            
            // --- FAVORITES SECTION ---
            if (showFavoritesSection) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Favorites",
                                color = if (wallpaperUrl == null) Color.Black else Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            
                            Text(
                                text = "Show All",
                                color = Color(0xFF007AFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isAddShortcutDialogOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Favorites responsive 4-column Grid
                        FavoritesGrid(
                            shortcuts = shortcuts,
                            wallpaperActive = wallpaperUrl != null,
                            onShortcutClicked = onShortcutClicked,
                            onDeleteShortcut = onDeleteShortcut,
                            onEditShortcut = { shortcut ->
                                shortcutToEdit = shortcut
                                isEditShortcutDialogOpen = true
                            },
                            onAddClick = { isAddShortcutDialogOpen = true }
                        )
                    }
                }
            }

            // --- ICLOUD TABS SECTION ---
            if (showICloudTabsSection) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "iCloud",
                                tint = if (wallpaperUrl == null) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "iCloud Tabs",
                                color = if (wallpaperUrl == null) Color.Black else Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stack of elegant custom tab preview cards
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            iCloudTabsList.forEachIndexed { index, tab ->
                                ICloudTabCard(
                                    tab = tab,
                                    index = index,
                                    wallpaperActive = wallpaperUrl != null,
                                    onClick = { onShortcutClicked(tab.targetUrl) },
                                    onDelete = {
                                        iCloudTabsList = iCloudTabsList.filter { it.id != tab.id }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- NEWS FEED SECTION ---
            if (showNewsSection) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Article,
                                    contentDescription = "News",
                                    tint = if (wallpaperUrl == null) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "World News",
                                    color = if (wallpaperUrl == null) Color.Black else Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            // Elegant option to hide/close the news section instantly!
                            IconButton(
                                onClick = { onShowNewsSectionChange(false) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide News Feed",
                                    tint = if (wallpaperUrl == null) Color.DarkGray else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Curated feed of real looking, beautifully designed tech and global news!
                        val newsFeed = remember {
                            listOf(
                                NewsArticle(
                                    id = "news_1",
                                    title = "Google AI Studio powers elite browser customizations with Jetpack Compose",
                                    summary = "A new paradigm in mobile browser design leverages ultra-responsive local state engines, advanced Material Design 3 guidelines, and high-performance layout rendering.",
                                    publisher = "TechCrunch",
                                    timeAgo = "10m ago",
                                    imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=400"
                                ),
                                NewsArticle(
                                    id = "news_2",
                                    title = "Mediterranean coastlines see record summer travel as remote workers relocate",
                                    summary = "Charming seaside towns across Greece and Italy adapt to a wave of digital nomads seeking warm climates, fast networks, and serene working backdrops.",
                                    publisher = "National Geographic",
                                    timeAgo = "1h ago",
                                    imageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=400"
                                ),
                                NewsArticle(
                                    id = "news_3",
                                    title = "Breakthrough in sustainable energy: Solar-slate materials drop 40% in cost",
                                    summary = "New manufacturing processes for structural solar slates make standard rooftops capable of capturing premium solar energy at competitive building prices.",
                                    publisher = "Reuters",
                                    timeAgo = "3h ago",
                                    imageUrl = "https://images.unsplash.com/photo-1509391366360-2e959784a276?q=80&w=400"
                                ),
                                NewsArticle(
                                    id = "news_4",
                                    title = "Minimalist slate designs trending across modern digital interfaces",
                                    summary = "Top interface designers are shifting back to spacious typography, high-contrast dark tones, and subtle spring-based movement to enhance user interaction.",
                                    publisher = "The Verge",
                                    timeAgo = "5h ago",
                                    imageUrl = "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?q=80&w=400"
                                )
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            newsFeed.forEach { article ->
                                NewsCard(
                                    article = article,
                                    wallpaperActive = wallpaperUrl != null,
                                    onClick = { onShortcutClicked(article.articleUrl) }
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTTOM EDIT / CUSTOMIZATION BUTTON ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { isEditPageOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wallpaperUrl == null) Color.White else Color.White.copy(alpha = 0.25f),
                            contentColor = if (wallpaperUrl == null) Color(0xFF007AFF) else Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = if (wallpaperUrl == null) BorderStroke(0.5.dp, Color.LightGray) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Edit Background",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Start Page",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- CUSTOM WALLPAPER EDIT DRAWER / DIALOG ---
        if (isEditPageOpen) {
            EditStartPageDialog(
                currentWallpaperUrl = wallpaperUrl,
                showFavorites = showFavoritesSection,
                showICloudTabs = showICloudTabsSection,
                showNews = showNewsSection,
                onShowFavoritesChange = { showFavoritesSection = it },
                onShowICloudTabsChange = { showICloudTabsSection = it },
                onShowNewsChange = onShowNewsSectionChange,
                onDismiss = { isEditPageOpen = false },
                onSelectWallpaper = { url ->
                    onWallpaperChanged(url)
                }
            )
        }

        // --- ADD CUSTOM WEBSITE SHORTCUT DIALOG ---
        if (isAddShortcutDialogOpen) {
            AddShortcutDialog(
                onDismiss = { isAddShortcutDialogOpen = false },
                onAddShortcut = { title, url, iconUrl ->
                    onAddShortcut(title, url, iconUrl)
                }
            )
        }

        // --- EDIT CUSTOM WEBSITE SHORTCUT DIALOG ---
        if (isEditShortcutDialogOpen) {
            shortcutToEdit?.let { shortcut ->
                EditShortcutDialog(
                    shortcut = shortcut,
                    onDismiss = {
                        isEditShortcutDialogOpen = false
                        shortcutToEdit = null
                    },
                    onUpdateShortcut = onUpdateShortcut
                )
            }
        }
    }
}

// Data model for custom iCloud tabs simulation
data class ICloudTabItem(
    val id: Int,
    val title: String,
    val url: String,
    val targetUrl: String,
    val device: String,
    val imageUrl: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesGrid(
    shortcuts: List<HomepageShortcut>,
    wallpaperActive: Boolean,
    onShortcutClicked: (String) -> Unit,
    onDeleteShortcut: (Long) -> Unit,
    onEditShortcut: (HomepageShortcut) -> Unit,
    onAddClick: () -> Unit
) {
    val totalCount = shortcuts.size + 1
    val rowCount = (totalCount + 3) / 4

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        for (rowIndex in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (colIndex in 0..3) {
                    val globalIndex = rowIndex * 4 + colIndex
                    if (globalIndex < shortcuts.size) {
                        val shortcut = shortcuts[globalIndex]
                        
                        // Staggered animated scale on first launch
                        var targetScale by remember { mutableStateOf(0.7f) }
                        var targetAlpha by remember { mutableStateOf(0f) }
                        
                        LaunchedEffect(shortcut.id) {
                            delay(globalIndex * 40L)
                            targetScale = 1.0f
                            targetAlpha = 1.0f
                        }

                        val animatedScale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "scale"
                        )
                        
                        val animatedAlpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = tween(durationMillis = 300),
                            label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    alpha = animatedAlpha
                                }
                        ) {
                            FavoriteTileItem(
                                shortcut = shortcut,
                                wallpaperActive = wallpaperActive,
                                onClicked = { onShortcutClicked(shortcut.url) },
                                onDelete = { onDeleteShortcut(shortcut.id) },
                                onEdit = { onEditShortcut(shortcut) }
                            )
                        }
                    } else if (globalIndex == shortcuts.size) {
                        // Always show beautiful "+" button to add website in the very next slot
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAddClick() }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth(0.85f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (wallpaperActive) Color.White.copy(alpha = 0.2f) else Color.White)
                                        .border(
                                            width = 0.5.dp,
                                            color = if (wallpaperActive) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Shortcut",
                                        tint = if (wallpaperActive) Color.White else Color.DarkGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add",
                                    color = if (wallpaperActive) Color.White.copy(alpha = 0.8f) else Color.DarkGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Fill remaining spaces with blank Spacer to keep alignment straight
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteTileItem(
    shortcut: HomepageShortcut,
    wallpaperActive: Boolean,
    onClicked: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }

    val scaleState by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tile_press"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleState)
            .pointerInput(shortcut.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClicked() },
                    onLongPress = {
                        isLongPressed = true
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (wallpaperActive) Color.White.copy(alpha = 0.15f) else Color.White)
                .border(
                    width = 0.5.dp,
                    color = if (wallpaperActive) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!shortcut.iconUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = shortcut.iconUrl),
                        contentDescription = shortcut.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // Generate visual styles identical to Apple's Safari favorites
                when (shortcut.title.lowercase()) {
                    "amazon" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFF9900)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "a",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }
                    "idb", "ankur idb" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "i",
                                    color = Color(0xFF007AFF),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "DB",
                                    color = Color.Black,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                    "increase platelet count..." -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MNT",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    "thrombocytopenia (lo..." -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MAYO",
                                color = Color(0xFF1A365D),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "CLINIC",
                                color = Color(0xFF1A365D),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                "apple" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF2F2F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Clean minimalist Apple symbol simulation
                        Text(
                            text = "",
                            color = Color.DarkGray,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                "wikipedia" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "W",
                            color = Color.Black,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
                "google" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        // Draw Google multicolored clean stylized "G" logo
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                else -> {
                    // Custom user added shortcuts: show beautiful single letter avatar
                    val letter = shortcut.title.firstOrNull()?.uppercaseChar() ?: 'W'
                    val bgGradient = remember(shortcut.id) {
                        val hues = listOf(
                            listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2)),
                            listOf(Color(0xFF11998e), Color(0xFF38ef7d)),
                            listOf(Color(0xFF00c6ff), Color(0xFF0072ff)),
                            listOf(Color(0xFFfc4a1a), Color(0xFFf7b733)),
                            listOf(Color(0xFFed1c24), Color(0xFFfdb813))
                        )
                        hues[(shortcut.id % hues.size).toInt()]
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(bgGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Small red delete badge if long-pressed or customizable
            if (isLongPressed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                        .clickable {
                            onDelete()
                            isLongPressed = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete shortcut",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                        .clickable {
                            onEdit()
                            isLongPressed = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit shortcut",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                // Clear long press state on clicking outside/background tap
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { isLongPressed = false })
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label
        Text(
            text = when (shortcut.title) {
                "Increase platelet count..." -> "Increase..."
                "Thrombocytopenia (lo..." -> "Thrombocy..."
                else -> shortcut.title
            },
            color = if (wallpaperActive) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@Composable
fun ICloudTabCard(
    tab: ICloudTabItem,
    index: Int,
    wallpaperActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Smooth initial reveal slide anim
    var offsetTarget by remember { mutableStateOf(50.dp) }
    var alphaTarget by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        delay(150L + index * 60L) // Staggered reveal after Favorites
        offsetTarget = 0.dp
        alphaTarget = 1.0f
    }
    
    val animatedOffset by animateDpAsState(
        targetValue = offsetTarget,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "offset"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = alphaTarget,
        animationSpec = tween(durationMillis = 350),
        label = "alpha"
    )
    
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = animatedOffset)
            .scale(pressScale)
            .graphicsLayer { alpha = animatedAlpha }
            .pointerInput(tab.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (wallpaperActive) Color.White.copy(alpha = 0.15f) else Color.White
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = if (wallpaperActive) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (wallpaperActive) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left webpreview image thumbnail
            Card(
                modifier = Modifier
                    .size(54.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = tab.imageUrl),
                    contentDescription = tab.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text contents
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tab.title,
                    color = if (wallpaperActive) Color.White else Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.url,
                    color = if (wallpaperActive) Color.White.copy(alpha = 0.6f) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Cloud syncd",
                        tint = if (wallpaperActive) Color.White.copy(alpha = 0.4f) else Color.LightGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "on ${tab.device}",
                        color = if (wallpaperActive) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            
            // Delete close tab button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove card",
                    tint = if (wallpaperActive) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val publisher: String,
    val timeAgo: String,
    val imageUrl: String,
    val articleUrl: String = "https://www.google.com/search?q=" + title.replace(" ", "+")
)

@Composable
fun NewsCard(
    article: NewsArticle,
    wallpaperActive: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scaleState by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "news_press"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleState)
            .pointerInput(article.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (wallpaperActive) Color.Black.copy(alpha = 0.55f) else Color.White
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = if (wallpaperActive) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = article.publisher,
                        color = if (wallpaperActive) Color(0xFFD4E157) else Color(0xFF007AFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = if (wallpaperActive) Color.White.copy(alpha = 0.5f) else Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = article.timeAgo,
                        color = if (wallpaperActive) Color.White.copy(alpha = 0.6f) else Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.title,
                    color = if (wallpaperActive) Color.White else Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = article.summary,
                    color = if (wallpaperActive) Color.White.copy(alpha = 0.7f) else Color.DarkGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Image(
                painter = rememberAsyncImagePainter(model = article.imageUrl),
                contentDescription = article.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStartPageDialog(
    currentWallpaperUrl: String?,
    showFavorites: Boolean,
    showICloudTabs: Boolean,
    showNews: Boolean,
    onShowFavoritesChange: (Boolean) -> Unit,
    onShowICloudTabsChange: (Boolean) -> Unit,
    onShowNewsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSelectWallpaper: (String?) -> Unit
) {
    val wallpapers = listOf(
        Pair("None", null),
        Pair("Mediterranean", "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1200"),
        Pair("Sunset Peak", "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?q=80&w=1200"),
        Pair("Warm Stars", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=1200"),
        Pair("Forest Mist", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=1200"),
        Pair("Dark Slate", "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=1200")
    )
    
    var customUrlInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Customize Start Page",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Configure layouts, toggle sections, and choose an ambient iOS wallpaper backdrop.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Layout Toggles
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Favorites Section",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = showFavorites,
                            onCheckedChange = onShowFavoritesChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "World News Section",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = showNews,
                            onCheckedChange = onShowNewsChange
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                // Wallpaper selector header
                Text(
                    text = "Select Background Wallpaper",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Presets Horizontal Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(wallpapers) { (name, url) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onSelectWallpaper(url) }
                                .width(70.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (url == null) Color(0xFFF2F2F7) else Color.DarkGray)
                                    .border(
                                        width = if (currentWallpaperUrl == url) 2.5.dp else 0.5.dp,
                                        color = if (currentWallpaperUrl == url) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (url != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = url),
                                        contentDescription = name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "None",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                // Custom URL Wallpaper Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Or enter custom Image URL:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            placeholder = { Text("https://example.com/art.jpg") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                        Button(
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    onSelectWallpaper(customUrlInput.trim())
                                    customUrlInput = ""
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Apply", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun AddShortcutDialog(
    onDismiss: () -> Unit,
    onAddShortcut: (String, String, String?) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var newIconUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Website Favorite",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Add a customizable quick-access shortcut tile to your Favorites start page.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title (e.g. YouTube)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text("URL (e.g. youtube.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newIconUrl,
                    onValueChange = { newIconUrl = it },
                    label = { Text("Custom Icon URL (Optional)") },
                    placeholder = { Text("https://example.com/logo.png") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "If custom icon URL is blank, an elegant dynamic letter branding icon will be generated automatically.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                        onAddShortcut(newTitle.trim(), newUrl.trim(), newIconUrl.trim().takeIf { it.isNotEmpty() })
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShortcutDialog(
    shortcut: HomepageShortcut,
    onDismiss: () -> Unit,
    onUpdateShortcut: (Long, String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf(shortcut.title) }
    var url by remember { mutableStateOf(shortcut.url) }
    var iconUrl by remember { mutableStateOf(shortcut.iconUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Website Shortcut",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Website Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Website URL Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Custom Icon URL (Optional)") },
                    placeholder = { Text("https://example.com/logo.png") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "If custom icon URL is blank, an elegant dynamic letter branding icon will be generated automatically.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        onUpdateShortcut(shortcut.id, title.trim(), url.trim(), iconUrl.trim().takeIf { it.isNotEmpty() })
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("Save Changes", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
