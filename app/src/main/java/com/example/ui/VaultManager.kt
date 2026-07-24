package com.example.ui

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.example.data.CategoryListEntity
import com.example.data.ListItemEntity
import com.example.data.TelegramMedia
import com.example.data.VaultItem
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

// Vault Collections Categories definition
data class VaultCollection(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
)

val vaultCollectionsList = listOf(
    VaultCollection("Shopping", Icons.Default.ShoppingBag, Color(0xFF3B82F6), "Saved products, deals & items"),
    VaultCollection("Food", Icons.Default.Fastfood, Color(0xFF10B981), "Recipes, restaurant reviews & menus"),
    VaultCollection("Sports", Icons.Default.SportsFootball, Color(0xFFEF4444), "Athletes, gear, stats & moments"),
    VaultCollection("Goals", Icons.Default.OutlinedFlag, Color(0xFF8B5CF6), "Inspirations, achievements & targets"),
    VaultCollection("Events", Icons.Default.Event, Color(0xFFF59E0B), "Concerts, conferences & meetups"),
    VaultCollection("Personal", Icons.Default.Lock, Color(0xFF6B7280), "Private bookmarks, links & thoughts")
)

@Composable
fun VaultSaveTray(
    imageUrl: String,
    title: String,
    pageUrl: String,
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedCollection by remember { mutableStateOf("Shopping") }
    var itemTitle by remember { mutableStateOf(if (title.isBlank()) "Saved Web Image" else title) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}, // prevent click-through
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Save to Vault Collection",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image Preview + Editable Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUrl),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = itemTitle,
                            onValueChange = { itemTitle = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pageUrl,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Select Collection",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable folder row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    vaultCollectionsList.forEach { col ->
                        val isSelected = selectedCollection == col.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) col.color.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) col.color else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCollection = col.name }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = col.icon,
                                    contentDescription = col.name,
                                    tint = if (isSelected) col.color else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = col.name,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Text("Cancel", fontSize = 14.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            viewModel.saveImageToVault(
                                url = imageUrl,
                                title = itemTitle,
                                pageUrl = pageUrl,
                                pageTitle = title,
                                collectionName = selectedCollection
                            )
                            Toast.makeText(context, "Saved to $selectedCollection collection!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Save Item", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VaultManagerScreen(
    allVaultItems: List<VaultItem>,
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 4 Hub Navigation Tabs
    var currentHubTab by remember { mutableStateOf("Lists") } // "Vault", "Lists", "Telegram", "Settings"

    // Collections states
    var selectedCategoryTab by remember { mutableStateOf("All") }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Manual Entry fields
    var manualTitle by remember { mutableStateOf("") }
    var manualUrl by remember { mutableStateOf("") }
    var manualNotes by remember { mutableStateOf("") }
    var manualColSelection by remember { mutableStateOf("Shopping") }

    // Category Lists states
    val categoryLists by viewModel.allCategoryLists.collectAsState()
    val allListItems by viewModel.allListItems.collectAsState()
    var activeCategoryList by remember { mutableStateOf<CategoryListEntity?>(null) }
    var listSearchQuery by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddListItemDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatType by remember { mutableStateOf("Movies") }

    // Auto-fill Search parser dialog state
    var showMetadataSearchDialog by remember { mutableStateOf(false) }
    var searchMetadataQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ListItemEntity>>(emptyList()) }
    var isSearchingMetadata by remember { mutableStateOf(false) }

    // Telegram Media Hub states
    val telegramMediaList by viewModel.allTelegramMedia.collectAsState()
    val isTelegramLoading by viewModel.isTelegramLoading.collectAsState()
    val tgBotToken by viewModel.tgBotToken.collectAsState()
    val tgChannelName by viewModel.tgChannelName.collectAsState()
    var tgFilterChip by remember { mutableStateOf("All") }
    var showTgUploadDialog by remember { mutableStateOf(false) }
    var selectedMediaForViewer by remember { mutableStateOf<TelegramMedia?>(null) }

    // Telegram Upload Dialog fields
    var uploadCaption by remember { mutableStateOf("") }
    var uploadType by remember { mutableStateOf("photo") }
    var uploadFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uploadFileUri = uri
    }

    // Backup & Restore states
    var backupJsonInput by remember { mutableStateOf("") }

    val filteredVaultItems = remember(allVaultItems, selectedCategoryTab) {
        if (selectedCategoryTab == "All") allVaultItems
        else allVaultItems.filter { it.collectionName.equals(selectedCategoryTab, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Premium Midnight Dark
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = when (currentHubTab) {
                        "Vault" -> "My Vault Collection"
                        "Lists" -> "Category Lists (Listy)"
                        "Telegram" -> "Telegram Channel Hub"
                        else -> "Vault Settings & Backup"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    if (currentHubTab == "Vault") {
                        IconButton(onClick = { showAddItemDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item", tint = Color.White)
                        }
                    } else if (currentHubTab == "Lists") {
                        IconButton(onClick = { viewModel.saveActiveTabToCategoryList(activeCategoryList?.id ?: 1L); Toast.makeText(context, "Active tab saved to list!", Toast.LENGTH_SHORT).show() }) {
                            Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "Save Tab", tint = Color(0xFF38BDF8))
                        }
                    } else if (currentHubTab == "Telegram") {
                        IconButton(onClick = { viewModel.refreshTelegramFeed() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    }
                }
            }

            // Hub Navigation Segment Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf(
                    "Lists" to Icons.Default.Category,
                    "Vault" to Icons.Default.Folder,
                    "Telegram" to Icons.Default.Send,
                    "Settings" to Icons.Default.Settings
                )

                tabs.forEach { (tabName, icon) ->
                    val isSelected = currentHubTab == tabName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                            .clickable { currentHubTab = tabName }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabName,
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tabName,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Body based on selected Tab
            Box(modifier = Modifier.weight(1f)) {
                when (currentHubTab) {
                    "Lists" -> MyListsHubScreen(
                        categoryLists = categoryLists,
                        allListItems = allListItems,
                        activeCategoryList = activeCategoryList,
                        onSelectCategory = { activeCategoryList = it },
                        searchQuery = listSearchQuery,
                        onSearchChange = { listSearchQuery = it },
                        onToggleCompleted = { viewModel.toggleListItemCompleted(it) },
                        onDeleteItem = { viewModel.deleteListItem(it) },
                        onDeleteList = { viewModel.deleteCategoryList(it); activeCategoryList = null },
                        onAddNewCategoryClick = { showAddCategoryDialog = true },
                        onAddListItemClick = { showAddListItemDialog = true },
                        onOpenMetadataSearch = { showMetadataSearchDialog = true },
                        viewModel = viewModel,
                        onClose = onClose
                    )

                    "Vault" -> VaultCollectionsTabScreen(
                        filteredItems = filteredVaultItems,
                        selectedCategoryTab = selectedCategoryTab,
                        onCategorySelect = { selectedCategoryTab = it },
                        viewModel = viewModel,
                        onClose = onClose
                    )

                    "Telegram" -> TelegramHubScreen(
                        telegramMedia = telegramMediaList,
                        isLoading = isTelegramLoading,
                        filterChip = tgFilterChip,
                        onFilterChange = { tgFilterChip = it },
                        onMediaClick = { selectedMediaForViewer = it },
                        onOpenUpload = { showTgUploadDialog = true },
                        channelName = tgChannelName
                    )

                    "Settings" -> VaultSettingsScreen(
                        botToken = tgBotToken,
                        channelName = tgChannelName,
                        onSaveTgConfig = { token, ch -> viewModel.setTelegramConfig(token, ch) },
                        onExportJson = {
                            val json = viewModel.exportBackupJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Vault Backup", json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                        },
                        onImportJson = { jsonStr ->
                            val success = viewModel.importBackupJson(jsonStr)
                            if (success) {
                                Toast.makeText(context, "Successfully restored backup data!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Add Vault Item Dialog
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Add Vault Item", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualTitle,
                        onValueChange = { manualTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualUrl,
                        onValueChange = { manualUrl = it },
                        label = { Text("URL / Image Link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualNotes,
                        onValueChange = { manualNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (manualTitle.isNotBlank()) {
                        viewModel.addManualVaultItem(manualTitle, manualUrl, "image", manualColSelection, manualNotes)
                        showAddItemDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Create New Category List", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("List Name (e.g. Anime, Recipes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Type", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Movies", "Books", "Music", "Video Games", "Places", "Web Links").forEach { type ->
                            val isSel = newCatType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.1f))
                                    .clickable { newCatType = type }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(type, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCatName.isNotBlank()) {
                        viewModel.addCategoryList(
                            name = newCatName,
                            iconName = newCatType,
                            colorHex = "#3B82F6",
                            type = newCatType
                        )
                        showAddCategoryDialog = false
                        newCatName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal Add List Item Dialog
    if (showAddListItemDialog && activeCategoryList != null) {
        var itemTitle by remember { mutableStateOf("") }
        var itemSub by remember { mutableStateOf("") }
        var itemDesc by remember { mutableStateOf("") }
        var itemImg by remember { mutableStateOf("") }
        var itemRating by remember { mutableStateOf(4f) }
        var itemNotes by remember { mutableStateOf("") }
        var itemWebUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddListItemDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Add to ${activeCategoryList?.name}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = itemTitle,
                        onValueChange = { itemTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = itemSub,
                        onValueChange = { itemSub = it },
                        label = { Text("Subtitle / Author / Genre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = itemImg,
                        onValueChange = { itemImg = it },
                        label = { Text("Cover Poster Image URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = itemWebUrl,
                        onValueChange = { itemWebUrl = it },
                        label = { Text("Stream or Web URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = itemDesc,
                        onValueChange = { itemDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = itemNotes,
                        onValueChange = { itemNotes = it },
                        label = { Text("User Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Rating (${itemRating.toInt()} Stars)", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = itemRating,
                        onValueChange = { itemRating = it },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (itemTitle.isNotBlank()) {
                        viewModel.addListItem(
                            listId = activeCategoryList!!.id,
                            title = itemTitle,
                            subtitle = itemSub.ifBlank { null },
                            description = itemDesc.ifBlank { null },
                            imageUrl = itemImg.ifBlank { null },
                            rating = itemRating,
                            notes = itemNotes.ifBlank { null },
                            webUrl = itemWebUrl.ifBlank { null }
                        )
                        showAddListItemDialog = false
                    }
                }) { Text("Save Item") }
            },
            dismissButton = {
                TextButton(onClick = { showAddListItemDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal Online Metadata Search Dialog
    if (showMetadataSearchDialog && activeCategoryList != null) {
        AlertDialog(
            onDismissRequest = { showMetadataSearchDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Online Search & Auto-Fill", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchMetadataQuery,
                            onValueChange = { searchMetadataQuery = it },
                            placeholder = { Text("Search title...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isSearchingMetadata = true
                                coroutineScope.launch {
                                    val res = com.example.network.TelegramEngine.searchMetadata(
                                        searchMetadataQuery,
                                        activeCategoryList!!.type
                                    )
                                    searchResults = res
                                    isSearchingMetadata = false
                                }
                            }
                        ) {
                            Text("Search")
                        }
                    }

                    if (isSearchingMetadata) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (searchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            viewModel.addListItem(
                                                listId = activeCategoryList!!.id,
                                                title = item.title,
                                                subtitle = item.subtitle,
                                                description = item.description,
                                                imageUrl = item.imageUrl,
                                                rating = item.rating,
                                                webUrl = item.webUrl,
                                                releaseDate = item.releaseDate
                                            )
                                            Toast.makeText(context, "Added ${item.title}!", Toast.LENGTH_SHORT).show()
                                            showMetadataSearchDialog = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = item.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(item.subtitle ?: "", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMetadataSearchDialog = false }) { Text("Close") }
            }
        )
    }

    // Modal Telegram Upload Dialog
    if (showTgUploadDialog) {
        AlertDialog(
            onDismissRequest = { showTgUploadDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Upload to Telegram Channel", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Channel: @${tgChannelName.ifBlank { "Not set" }}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("photo" to "Photo", "video" to "Video", "file" to "Document/APK", "text" to "Text Message").forEach { (type, label) ->
                            val isSel = uploadType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.1f))
                                    .clickable { uploadType = type }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    if (uploadType != "text") {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uploadFileUri != null) "File Selected" else "Select File from Storage")
                        }
                        if (uploadFileUri != null) {
                            Text(uploadFileUri?.path ?: "", color = Color(0xFF38BDF8), fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    OutlinedTextField(
                        value = uploadCaption,
                        onValueChange = { uploadCaption = it },
                        label = { Text("Caption / Message Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.uploadTelegramMedia(
                        mediaType = uploadType,
                        caption = uploadCaption,
                        fileUri = uploadFileUri
                    ) { result ->
                        result.fold(
                            onSuccess = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
                            onFailure = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                        )
                    }
                    showTgUploadDialog = false
                    uploadCaption = ""
                    uploadFileUri = null
                }) {
                    Text("Post to Channel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTgUploadDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Fullscreen Telegram Media Viewer
    if (selectedMediaForViewer != null) {
        val media = selectedMediaForViewer!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { selectedMediaForViewer = null },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Media Viewer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { selectedMediaForViewer = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (media.mediaType == "video" && !media.fileUrl.isNullOrBlank()) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(media.fileUrl)
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!media.fileUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = media.fileUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(media.caption ?: "Text Post", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(16.dp))
                    }
                }

                if (!media.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(media.caption!!, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar (Wallpaper, Download, Copy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (media.mediaType == "photo" && !media.fileUrl.isNullOrBlank()) {
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val url = URL(media.fileUrl)
                                        val bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream())
                                        val wpManager = WallpaperManager.getInstance(context)
                                        wpManager.setBitmap(bitmap)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Applied as Wallpaper!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wallpaper", fontSize = 11.sp)
                        }
                    }

                    if (!media.fileUrl.isNullOrBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val request = DownloadManager.Request(Uri.parse(media.fileUrl))
                                        .setTitle("Telegram Download")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "tg_media_${System.currentTimeMillis()}")
                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    dm.enqueue(request)
                                    Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 11.sp)
                        }
                    }

                    if (!media.caption.isNullOrBlank()) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Telegram Caption", media.caption)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Caption copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Text", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. MY LISTS HUB SCREEN (Listy Engine)
// -------------------------------------------------------------------------------------
@Composable
fun MyListsHubScreen(
    categoryLists: List<CategoryListEntity>,
    allListItems: List<ListItemEntity>,
    activeCategoryList: CategoryListEntity?,
    onSelectCategory: (CategoryListEntity?) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggleCompleted: (ListItemEntity) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onDeleteList: (Long) -> Unit,
    onAddNewCategoryClick: () -> Unit,
    onAddListItemClick: () -> Unit,
    onOpenMetadataSearch: () -> Unit,
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    if (activeCategoryList == null) {
        // Categories Overview Grid
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Category Lists", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(
                    onClick = onAddNewCategoryClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New List", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categoryLists) { cat ->
                    val itemCount = allListItems.count { it.listId == cat.id }
                    val completedCount = allListItems.count { it.listId == cat.id && it.isCompleted }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                            .clickable { onSelectCategory(cat) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(cat.colorHex)).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (cat.type) {
                                            "Movies" -> Icons.Default.Movie
                                            "Books" -> Icons.Default.Book
                                            "Music" -> Icons.Default.MusicNote
                                            "Video Games" -> Icons.Default.SportsEsports
                                            "Places" -> Icons.Default.Place
                                            else -> Icons.Default.Link
                                        },
                                        contentDescription = null,
                                        tint = Color(android.graphics.Color.parseColor(cat.colorHex)),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Text(
                                    "$completedCount/$itemCount",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(cat.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(cat.type, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    } else {
        // Active List Detail Screen
        val currentItems = remember(allListItems, activeCategoryList, searchQuery) {
            allListItems.filter { it.listId == activeCategoryList.id && (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSelectCategory(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(activeCategoryList.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onOpenMetadataSearch) {
                        Icon(Icons.Default.TravelExplore, contentDescription = "Online Search", tint = Color(0xFF38BDF8))
                    }
                    IconButton(onClick = onAddListItemClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.White)
                    }
                    IconButton(onClick = { onDeleteList(activeCategoryList.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete List", tint = Color.Red)
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filter ${activeCategoryList.name}...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) }
            )

            if (currentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No items saved in ${activeCategoryList.name}", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onOpenMetadataSearch) {
                            Text("Search Online & Add")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentItems) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { onToggleCompleted(item) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                                )

                                if (!item.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Image(
                                        painter = rememberAsyncImagePainter(model = item.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = if (item.isCompleted) Color.White.copy(alpha = 0.4f) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (!item.subtitle.isNullOrBlank()) {
                                        Text(item.subtitle!!, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                    if (item.rating > 0f) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            repeat(item.rating.toInt()) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    if (!item.notes.isNullOrBlank()) {
                                        Text("Note: ${item.notes}", color = Color(0xFF38BDF8), fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                if (!item.webUrl.isNullOrBlank()) {
                                    IconButton(onClick = {
                                        viewModel.loadUrl(item.webUrl!!)
                                        onClose()
                                    }) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = Color(0xFF38BDF8))
                                    }
                                }

                                IconButton(onClick = { onDeleteItem(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 2. VAULT COLLECTIONS TAB SCREEN
// -------------------------------------------------------------------------------------
@Composable
fun VaultCollectionsTabScreen(
    filteredItems: List<VaultItem>,
    selectedCategoryTab: String,
    onCategorySelect: (String) -> Unit,
    viewModel: BrowserViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Collection selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isAll = selectedCategoryTab == "All"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAll) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f))
                    .clickable { onCategorySelect("All") }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("All Content", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            vaultCollectionsList.forEach { col ->
                val isSel = selectedCategoryTab == col.name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) col.color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (isSel) col.color else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { onCategorySelect(col.name) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(col.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No items in this collection yet", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    Text("Long-press any web image to save to Vault", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = item.url),
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                IconButton(
                                    onClick = {
                                        viewModel.deleteVaultItem(item.id)
                                        Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                }
                            }

                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                if (!item.extraData.isNullOrBlank()) {
                                    Text(item.extraData!!, color = Color(0xFF38BDF8), fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadUrl(item.pageUrl)
                                            onClose()
                                        }
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("Visit Site", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3. TELEGRAM HUB SCREEN
// -------------------------------------------------------------------------------------
@Composable
fun TelegramHubScreen(
    telegramMedia: List<TelegramMedia>,
    isLoading: Boolean,
    filterChip: String,
    onFilterChange: (String) -> Unit,
    onMediaClick: (TelegramMedia) -> Unit,
    onOpenUpload: () -> Unit,
    channelName: String
) {
    val filteredList = remember(telegramMedia, filterChip) {
        when (filterChip) {
            "Wallpapers" -> telegramMedia.filter { it.mediaType == "photo" }
            "Videos" -> telegramMedia.filter { it.mediaType == "video" }
            "Posts" -> telegramMedia.filter { it.mediaType == "text" }
            "Files" -> telegramMedia.filter { it.mediaType == "file" }
            else -> telegramMedia
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Wallpapers", "Videos", "Posts", "Files").forEach { chip ->
                    val isSel = filterChip == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.05f))
                            .clickable { onFilterChange(chip) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(chip, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF3B82F6))
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Telegram media items found", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList) { media ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .clickable { onMediaClick(media) }
                        ) {
                            Column {
                                if (!media.fileUrl.isNullOrBlank() && media.mediaType != "file") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (media.mediaType == "video") 160.dp else 180.dp)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = media.fileUrl),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (media.mediaType == "video") {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(40.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (media.mediaType == "file") Icons.Default.InsertDriveFile else Icons.Default.Message,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                if (!media.caption.isNullOrBlank()) {
                                    Text(
                                        text = media.caption,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Upload FAB
        FloatingActionButton(
            onClick = onOpenUpload,
            containerColor = Color(0xFF3B82F6),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Upload, contentDescription = "Upload")
        }
    }
}

// -------------------------------------------------------------------------------------
// 4. VAULT SETTINGS & BACKUP SCREEN
// -------------------------------------------------------------------------------------
@Composable
fun VaultSettingsScreen(
    botToken: String,
    channelName: String,
    onSaveTgConfig: (String, String) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: (String) -> Unit
) {
    var tokenInput by remember { mutableStateOf(botToken) }
    var channelInput by remember { mutableStateOf(channelName) }
    var jsonInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Telegram Configuration Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Telegram Channel Bot API Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Bot API Token") },
                    placeholder = { Text("123456789:ABCdef...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = channelInput,
                    onValueChange = { channelInput = it },
                    label = { Text("Public Channel Username (without @)") },
                    placeholder = { Text("my_channel") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onSaveTgConfig(tokenInput, channelInput) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Save & Sync Channel Feed")
                }
            }
        }

        // Backup & Restore Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JSON Database Backup & Restore", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = onExportJson,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export All Records to JSON")
                }

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste JSON Backup to Restore") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        if (jsonInput.isNotBlank()) {
                            onImportJson(jsonInput)
                            jsonInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Restore from JSON")
                }
            }
        }
    }
}
