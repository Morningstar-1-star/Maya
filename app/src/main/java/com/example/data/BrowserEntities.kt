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
    val groupName: String? = null
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
    val timestamp: Long = System.currentTimeMillis()
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

