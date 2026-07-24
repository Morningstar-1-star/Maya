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

    @Query("SELECT * FROM bookmarks WHERE isWatchMode = 1")
    suspend fun getWatchedBookmarks(): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): Bookmark?

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()

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

    // --- User Scripts Management ---
    @Query("SELECT * FROM user_scripts ORDER BY timestamp DESC")
    fun getAllUserScripts(): Flow<List<UserScript>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserScript(script: UserScript): Long

    @Update
    suspend fun updateUserScript(script: UserScript)

    @Delete
    suspend fun deleteUserScript(script: UserScript)

    @Query("DELETE FROM user_scripts WHERE id = :id")
    suspend fun deleteUserScriptById(id: Long)

    // --- Downloads Management ---
    @Query("SELECT * FROM download_entries ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntry): Long

    @Update
    suspend fun updateDownload(download: DownloadEntry)

    @Query("DELETE FROM download_entries WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM download_entries")
    suspend fun clearAllDownloads()

    // --- Vault Management ---
    @Query("SELECT * FROM vault_items ORDER BY timestamp DESC")
    fun getAllVaultItems(): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItem): Long

    @Update
    suspend fun updateVaultItem(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: Long)

    @Query("DELETE FROM vault_items")
    suspend fun clearAllVaultItems()

    // --- Category Lists Management (Listy Engine) ---
    @Query("SELECT * FROM category_lists ORDER BY timestamp ASC")
    fun getAllCategoryLists(): Flow<List<CategoryListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryList(list: CategoryListEntity): Long

    @Query("DELETE FROM category_lists WHERE id = :id")
    suspend fun deleteCategoryListById(id: Long)

    // --- List Items Management ---
    @Query("SELECT * FROM list_items ORDER BY timestamp DESC")
    fun getAllListItems(): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY timestamp DESC")
    fun getListItemsByListId(listId: Long): Flow<List<ListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItem(item: ListItemEntity): Long

    @Update
    suspend fun updateListItem(item: ListItemEntity)

    @Query("DELETE FROM list_items WHERE id = :id")
    suspend fun deleteListItemById(id: Long)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun deleteListItemsByListId(listId: Long)

    // --- Telegram Media Hub Management ---
    @Query("SELECT * FROM telegram_media ORDER BY postedAt DESC")
    fun getAllTelegramMedia(): Flow<List<TelegramMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelegramMediaList(items: List<TelegramMedia>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelegramMedia(item: TelegramMedia)

    @Query("DELETE FROM telegram_media WHERE fileId = :fileId")
    suspend fun deleteTelegramMediaByFileId(fileId: String)

    @Query("DELETE FROM telegram_media")
    suspend fun clearAllTelegramMedia()
}
