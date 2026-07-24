package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

object TabThumbnailManager {
    private const val TAG = "TabThumbnailManager"
    private const val THUMB_WIDTH = 300
    private const val THUMB_HEIGHT = 500

    // Keep track of frame index for rolling animated frames (0, 1, 2)
    private val tabFrameIndices = mutableMapOf<Long, Int>()

    // Live update trigger to notify UI to reload thumbnails
    private val _thumbnailUpdateTrigger = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val thumbnailUpdateTrigger: StateFlow<Map<Long, Long>> = _thumbnailUpdateTrigger.asStateFlow()

    private fun getThumbnailsDir(context: Context): File {
        val dir = File(context.cacheDir, "tab_thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Captures a screenshot of the specified WebView and saves it as a lightweight rolling thumbnail frame.
     */
    fun captureThumbnail(context: Context, tabId: Long, webView: WebView) {
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) {
            Log.d(TAG, "Cannot capture thumbnail: WebView dimensions are 0 for tab $tabId")
            return
        }

        try {
            // Create a bitmap of the visible viewport of the WebView
            val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fullBitmap)
            
            // Draw WebView contents into the bitmap
            webView.draw(canvas)

            // Validate that the captured bitmap is not entirely blank, solid black, or solid white
            if (isBitmapSolidColor(fullBitmap)) {
                Log.d(TAG, "Captured bitmap is blank/white/black for tab $tabId, skipping.")
                fullBitmap.recycle()
                return
            }

            // Downscale to lightweight size (around 300x500 px) to save RAM and disk
            val scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, THUMB_WIDTH, THUMB_HEIGHT, true)
            fullBitmap.recycle()

            // Save in a rolling buffer of 3 frames (0, 1, 2) for animated live previews
            val nextFrameIdx = ((tabFrameIndices[tabId] ?: -1) + 1) % 3
            tabFrameIndices[tabId] = nextFrameIdx

            val dir = getThumbnailsDir(context)
            val file = File(dir, "tab_${tabId}_$nextFrameIdx.jpg")

            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            scaledBitmap.recycle()

            // Notify listeners about the update
            val currentMap = _thumbnailUpdateTrigger.value.toMutableMap()
            currentMap[tabId] = System.currentTimeMillis()
            _thumbnailUpdateTrigger.value = currentMap

            Log.d(TAG, "Successfully captured frame $nextFrameIdx for tab $tabId")
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing thumbnail for tab $tabId", e)
        }
    }

    /**
     * Checks if the captured bitmap consists of a single solid color (e.g. all white or all black).
     * We sample a grid of pixels to be highly performant.
     */
    private fun isBitmapSolidColor(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return true

        val firstPixel = bitmap.getPixel(0, 0)
        
        // Sample a 5x5 grid of pixels
        for (i in 1..4) {
            for (j in 1..4) {
                val px = (w * i) / 5
                val py = (h * j) / 5
                if (bitmap.getPixel(px, py) != firstPixel) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Returns all available saved frame files for a given tab, in order (0, 1, 2).
     */
    fun getThumbnailFrames(context: Context, tabId: Long): List<File> {
        val dir = getThumbnailsDir(context)
        val frames = mutableListOf<File>()
        for (i in 0..2) {
            val file = File(dir, "tab_${tabId}_$i.jpg")
            if (file.exists() && file.length() > 0) {
                frames.add(file)
            }
        }
        return frames
    }

    /**
     * Returns the timestamp when the tab's thumbnail was last updated.
     */
    fun getLastUpdatedTime(context: Context, tabId: Long): Long {
        val frames = getThumbnailFrames(context, tabId)
        if (frames.isEmpty()) return 0L
        return frames.maxOf { it.lastModified() }
    }

    /**
     * Returns a string describing how long ago the thumbnail was updated (e.g. "Updated 2s ago")
     */
    fun getUpdatedAgoString(context: Context, tabId: Long): String {
        val lastUpdate = getLastUpdatedTime(context, tabId)
        if (lastUpdate <= 0L) return ""
        val diffMs = System.currentTimeMillis() - lastUpdate
        val diffSec = diffMs / 1000
        return when {
            diffSec < 5 -> "Live Preview"
            diffSec < 60 -> "Updated ${diffSec}s ago"
            diffSec < 3600 -> "Updated ${diffSec / 60}m ago"
            else -> "Updated ${diffSec / 3600}h ago"
        }
    }

    /**
     * Clears all thumbnails for a specific tab when it's closed.
     */
    fun clearTabThumbnails(context: Context, tabId: Long) {
        val dir = getThumbnailsDir(context)
        for (i in 0..2) {
            val file = File(dir, "tab_${tabId}_$i.jpg")
            if (file.exists()) {
                file.delete()
            }
        }
        tabFrameIndices.remove(tabId)
        
        val currentMap = _thumbnailUpdateTrigger.value.toMutableMap()
        currentMap.remove(tabId)
        _thumbnailUpdateTrigger.value = currentMap
    }

    /**
     * Clears all cached thumbnails in the entire app.
     */
    fun clearAllThumbnails(context: Context) {
        val dir = getThumbnailsDir(context)
        dir.listFiles()?.forEach { it.delete() }
        tabFrameIndices.clear()
        _thumbnailUpdateTrigger.value = emptyMap()
    }
}
