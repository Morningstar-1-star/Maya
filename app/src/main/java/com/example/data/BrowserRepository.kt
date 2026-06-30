package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val browserDao: BrowserDao) {

    val allTabs: Flow<List<BrowserTab>> = browserDao.getAllTabs()
    val allHistory: Flow<List<HistoryEntry>> = browserDao.getAllHistory()
    val allBookmarks: Flow<List<Bookmark>> = browserDao.getAllBookmarks()

    suspend fun getSelectedTab(): BrowserTab? = browserDao.getSelectedTab()

    suspend fun insertTab(tab: BrowserTab): Long = browserDao.insertTab(tab)

    suspend fun updateTab(tab: BrowserTab) = browserDao.updateTab(tab)

    suspend fun deleteTab(tab: BrowserTab) = browserDao.deleteTab(tab)

    suspend fun deleteTabById(id: Long) = browserDao.deleteTabById(id)

    suspend fun selectTab(tabId: Long) = browserDao.selectTab(tabId)

    suspend fun deselectAllTabs() = browserDao.deselectAllTabs()

    suspend fun clearAllTabs() = browserDao.clearAllTabs()


    suspend fun insertHistory(entry: HistoryEntry) = browserDao.insertHistory(entry)

    suspend fun deleteHistoryById(id: Long) = browserDao.deleteHistoryById(id)

    suspend fun clearAllHistory() = browserDao.clearAllHistory()


    suspend fun insertBookmark(bookmark: Bookmark) = browserDao.insertBookmark(bookmark)

    suspend fun deleteBookmarkById(id: Long) = browserDao.deleteBookmarkById(id)

    suspend fun deleteBookmarkByUrl(url: String) = browserDao.deleteBookmarkByUrl(url)

    suspend fun isBookmarked(url: String): Boolean = browserDao.isBookmarked(url)

    // --- Shortcuts ---
    val allShortcuts: Flow<List<HomepageShortcut>> = browserDao.getAllShortcuts()

    suspend fun insertShortcut(shortcut: HomepageShortcut) = browserDao.insertShortcut(shortcut)

    suspend fun deleteShortcutById(id: Long) = browserDao.deleteShortcutById(id)

    // --- Captured Media ---
    val allCapturedMedia: Flow<List<CapturedMedia>> = browserDao.getAllCapturedMedia()
    val likedSavedMedia: Flow<List<CapturedMedia>> = browserDao.getLikedSavedMedia()

    suspend fun getMediaByUrl(url: String): CapturedMedia? = browserDao.getMediaByUrl(url)

    suspend fun insertMedia(media: CapturedMedia): Long = browserDao.insertMedia(media)

    suspend fun updateMedia(media: CapturedMedia) = browserDao.updateMedia(media)

    suspend fun deleteMediaById(id: Long) = browserDao.deleteMediaById(id)

    suspend fun clearAllCapturedMedia() = browserDao.clearAllCapturedMedia()
}
