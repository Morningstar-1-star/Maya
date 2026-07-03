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
    val lastTextHash: String? = null
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


