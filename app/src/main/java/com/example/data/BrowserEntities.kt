package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browser_tabs")
data class BrowserTab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val isSelected: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val groupName: String? = null,
    val isLocked: Boolean = false
)

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isWatchMode: Boolean = false,
    val lastTextHash: String? = null,
    val hasUpdateAlert: Boolean = false,
    val lastUpdateDetails: String? = null
)

@Entity(tableName = "homepage_shortcuts")
data class HomepageShortcut(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "captured_media")
data class CapturedMedia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val type: String, // "image" or "video"
    val pageTitle: String,
    val pageUrl: String,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_scripts")
data class UserScript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val matchUrl: String, // e.g. "*" or specific domains
    val code: String, // Javascript code
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class AdFilterSubscription(
    val id: String,
    val name: String,
    val url: String,
    val lastUpdated: String,
    val size: String,
    val enabled: Boolean
)

@Entity(tableName = "download_entries")
data class DownloadEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filename: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Downloading", // "Downloading", "Completed", "Failed"
    val size: String = "Unknown size"
)

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val pageUrl: String,
    val pageTitle: String,
    val type: String, // "image", "video", "link", "note"
    val collectionName: String, // e.g. "Sports", "Food", "Goals", "Events", "Shopping", "Personal"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val extraData: String? = null // e.g. "$29.99"
)

@Entity(tableName = "category_lists")
data class CategoryListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String = "Movie",
    val colorHex: String = "#3B82F6",
    val type: String = "General", // "Movies", "Books", "Music", "Video Games", "Places", "Web Links"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "list_items")
data class ListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val rating: Float = 0f, // 1 to 5 stars
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val releaseDate: String? = null,
    val webUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "telegram_media")
data class TelegramMedia(
    @PrimaryKey val fileId: String,
    val messageId: Long = 0,
    val mediaType: String = "photo", // "photo", "video", "text", "file"
    val fileUrl: String? = null,
    val caption: String? = null,
    val postedAt: Long = System.currentTimeMillis()
)




