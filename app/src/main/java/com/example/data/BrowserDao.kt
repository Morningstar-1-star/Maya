package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // --- Tabs Management ---
    @Query("SELECT * FROM browser_tabs ORDER BY timestamp ASC")
    fun getAllTabs(): Flow<List<BrowserTab>>

    @Query("SELECT * FROM browser_tabs WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedTab(): BrowserTab?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: BrowserTab): Long

    @Update
    suspend fun updateTab(tab: BrowserTab)

    @Delete
    suspend fun deleteTab(tab: BrowserTab)

    @Query("DELETE FROM browser_tabs WHERE id = :id")
    suspend fun deleteTabById(id: Long)

    @Query("UPDATE browser_tabs SET isSelected = 0")
    suspend fun deselectAllTabs()

    @Transaction
    suspend fun selectTab(tabId: Long) {
        deselectAllTabs()
        updateTabSelection(tabId, true)
    }

    @Query("UPDATE browser_tabs SET isSelected = :isSelected WHERE id = :id")
    suspend fun updateTabSelection(id: Long, isSelected: Boolean)

    @Query("DELETE FROM browser_tabs")
    suspend fun clearAllTabs()


    // --- History Management ---
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history_entries")
    suspend fun clearAllHistory()


    // --- Bookmarks Management ---
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean

    // --- Homepage Shortcuts Management ---
    @Query("SELECT * FROM homepage_shortcuts ORDER BY timestamp ASC")
    fun getAllShortcuts(): Flow<List<HomepageShortcut>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: HomepageShortcut)

    @Query("DELETE FROM homepage_shortcuts WHERE id = :id")
    suspend fun deleteShortcutById(id: Long)

    // --- Captured Media Management ---
    @Query("SELECT * FROM captured_media ORDER BY timestamp DESC")
    fun getAllCapturedMedia(): Flow<List<CapturedMedia>>

    @Query("SELECT * FROM captured_media WHERE isLiked = 1 OR isSaved = 1 ORDER BY timestamp DESC")
    fun getLikedSavedMedia(): Flow<List<CapturedMedia>>

    @Query("SELECT * FROM captured_media WHERE url = :url LIMIT 1")
    suspend fun getMediaByUrl(url: String): CapturedMedia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: CapturedMedia): Long

    @Update
    suspend fun updateMedia(media: CapturedMedia)

    @Query("DELETE FROM captured_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("DELETE FROM captured_media")
    suspend fun clearAllCapturedMedia()
}
