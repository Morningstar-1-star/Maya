package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class OfflinePageArchive(
    val id: String,
    val title: String,
    val originalUrl: String,
    val filePath: String,
    val dateAdded: Long,
    val fileSize: Long
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
            return sdf.format(Date(dateAdded))
        }

    val formattedSize: String
        get() {
            return when {
                fileSize >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSize / (1024f * 1024f))
                fileSize >= 1024 -> String.format(Locale.US, "%d KB", fileSize / 1024)
                else -> "$fileSize B"
            }
        }
}

object OfflineArchiveManager {
    private const val PREFS_NAME = "browser_offline_archives"
    private const val KEY_ARCHIVES_JSON = "key_archives_json"

    private val _archives = MutableStateFlow<List<OfflinePageArchive>>(emptyList())
    val archives: StateFlow<List<OfflinePageArchive>> = _archives.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun init(context: Context) {
        loadArchives(context)
    }

    private fun loadArchives(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ARCHIVES_JSON, "[]") ?: "[]"
        val list = mutableListOf<OfflinePageArchive>()

        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val file = File(obj.getString("filePath"))
                if (file.exists()) {
                    list.add(
                        OfflinePageArchive(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            originalUrl = obj.getString("originalUrl"),
                            filePath = obj.getString("filePath"),
                            dateAdded = obj.getLong("dateAdded"),
                            fileSize = file.length()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _archives.value = list.sortedByDescending { it.dateAdded }
    }

    private fun saveArchivesList(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (item in _archives.value) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("originalUrl", item.originalUrl)
                put("filePath", item.filePath)
                put("dateAdded", item.dateAdded)
                put("fileSize", item.fileSize)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ARCHIVES_JSON, arr.toString()).apply()
    }

    /**
     * Saves the currently rendered WebView page as a standalone .mhtml archive.
     */
    fun savePageArchive(
        context: Context,
        webView: WebView,
        title: String,
        url: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (_isSaving.value) {
            onComplete(false, "Another archive is already being saved.")
            return
        }

        _isSaving.value = true
        val archiveDir = File(context.filesDir, "web_archives")
        if (!archiveDir.exists()) {
            archiveDir.mkdirs()
        }

        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
        val id = UUID.randomUUID().toString().take(8)
        val file = File(archiveDir, "${cleanTitle}_$id.mhtml")

        try {
            webView.saveWebArchive(file.absolutePath, false) { savedPath ->
                _isSaving.value = false
                if (savedPath != null && File(savedPath).exists() && File(savedPath).length() > 0) {
                    val savedFile = File(savedPath)
                    val archiveItem = OfflinePageArchive(
                        id = id,
                        title = if (title.isNotBlank()) title else "Archived Webpage",
                        originalUrl = url,
                        filePath = savedFile.absolutePath,
                        dateAdded = System.currentTimeMillis(),
                        fileSize = savedFile.length()
                    )
                    val updated = _archives.value.toMutableList().apply { add(0, archiveItem) }
                    _archives.value = updated
                    saveArchivesList(context)
                    Toast.makeText(context, "Saved offline page (${archiveItem.formattedSize})", Toast.LENGTH_SHORT).show()
                    onComplete(true, savedPath)
                } else {
                    onComplete(false, "Failed to capture web archive.")
                }
            }
        } catch (e: Exception) {
            _isSaving.value = false
            onComplete(false, e.localizedMessage)
        }
    }

    fun deleteArchive(context: Context, archive: OfflinePageArchive) {
        try {
            val file = File(archive.filePath)
            if (file.exists()) {
                file.delete()
            }
            val updated = _archives.value.filter { it.id != archive.id }
            _archives.value = updated
            saveArchivesList(context)
            Toast.makeText(context, "Removed from offline archives", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareArchive(context: Context, archive: OfflinePageArchive) {
        try {
            val file = File(archive.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Archive file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "multipart/related"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, archive.title)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Offline Web Archive").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
