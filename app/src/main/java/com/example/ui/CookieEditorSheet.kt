package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.viewmodel.BrowserViewModel
import org.json.JSONArray
import org.json.JSONObject

data class CookieItem(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean = true,
    val httpOnly: Boolean = false
)

object CookiePersistence {
    private const val PREFS_NAME = "browser_cookies_persistence"
    private const val KEY_COOKIES_PREFIX = "cookies_"
    private const val KEY_PROFILES_PREFIX = "profiles_"

    // Save active custom cookies for a cleanUrl
    fun saveCustomCookies(context: Context, cleanUrl: String, cookies: List<CookieItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        cookies.forEach { cookie ->
            val obj = JSONObject()
            obj.put("name", cookie.name)
            obj.put("value", cookie.value)
            obj.put("domain", cookie.domain)
            obj.put("path", cookie.path)
            obj.put("secure", cookie.secure)
            obj.put("httpOnly", cookie.httpOnly)
            array.put(obj)
        }
        prefs.edit().putString(KEY_COOKIES_PREFIX + cleanUrl, array.toString()).apply()
    }

    // Load active custom cookies for a cleanUrl
    fun loadCustomCookies(context: Context, cleanUrl: String, defaultDomain: String): List<CookieItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_COOKIES_PREFIX + cleanUrl, null) ?: return emptyList()
        val list = mutableListOf<CookieItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CookieItem(
                        name = obj.optString("name", ""),
                        value = obj.optString("value", ""),
                        domain = obj.optString("domain", defaultDomain),
                        path = obj.optString("path", "/"),
                        secure = obj.optBoolean("secure", true),
                        httpOnly = obj.optBoolean("httpOnly", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Save a custom named profile
    fun saveProfile(context: Context, cleanUrl: String, profileName: String, cookies: List<CookieItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profilesKey = KEY_PROFILES_PREFIX + cleanUrl
        val existingProfilesJson = prefs.getString(profilesKey, "{}") ?: "{}"
        try {
            val rootObj = JSONObject(existingProfilesJson)
            val array = JSONArray()
            cookies.forEach { cookie ->
                val obj = JSONObject()
                obj.put("name", cookie.name)
                obj.put("value", cookie.value)
                obj.put("domain", cookie.domain)
                obj.put("path", cookie.path)
                obj.put("secure", cookie.secure)
                obj.put("httpOnly", cookie.httpOnly)
                array.put(obj)
            }
            rootObj.put(profileName, array)
            prefs.edit().putString(profilesKey, rootObj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load custom named profiles
    fun loadProfiles(context: Context, cleanUrl: String): Map<String, List<CookieItem>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profilesKey = KEY_PROFILES_PREFIX + cleanUrl
        val existingProfilesJson = prefs.getString(profilesKey, "{}") ?: "{}"
        val map = mutableMapOf<String, List<CookieItem>>()
        try {
            val rootObj = JSONObject(existingProfilesJson)
            val keys = rootObj.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                val array = rootObj.getJSONArray(name)
                val list = mutableListOf<CookieItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CookieItem(
                            name = obj.optString("name", ""),
                            value = obj.optString("value", ""),
                            domain = obj.optString("domain", ""),
                            path = obj.optString("path", "/"),
                            secure = obj.optBoolean("secure", true),
                            httpOnly = obj.optBoolean("httpOnly", false)
                        )
                    )
                }
                map[name] = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    // Delete a profile
    fun deleteProfile(context: Context, cleanUrl: String, profileName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profilesKey = KEY_PROFILES_PREFIX + cleanUrl
        val existingProfilesJson = prefs.getString(profilesKey, "{}") ?: "{}"
        try {
            val rootObj = JSONObject(existingProfilesJson)
            rootObj.remove(profileName)
            prefs.edit().putString(profilesKey, rootObj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Inject all custom cookies stored for this host/url into the native CookieManager
    fun restoreCookiesForUrl(context: Context, url: String?) {
        if (url == null || url.isBlank()) return
        try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return
            
            val cookieManager = CookieManager.getInstance()
            val defaultDomain = host
            
            // Try to load cookies saved for this exact url
            val savedCookies = loadCustomCookies(context, url, defaultDomain).toMutableList()
            
            // Also try to load cookies saved for just the host (base URL with empty path)
            val baseUri = Uri.Builder().scheme(uri.scheme).authority(uri.authority).path("").toString()
            if (baseUri != url) {
                val baseSaved = loadCustomCookies(context, baseUri, defaultDomain)
                baseSaved.forEach { baseCookie ->
                    if (savedCookies.none { it.name == baseCookie.name }) {
                        savedCookies.add(baseCookie)
                    }
                }
            }
            
            if (savedCookies.isNotEmpty()) {
                if (!cookieManager.acceptCookie()) {
                    cookieManager.setAcceptCookie(true)
                }
                savedCookies.forEach { cookie ->
                    val cookieStr = StringBuilder().apply {
                        append("${cookie.name}=${cookie.value}")
                        if (cookie.domain.isNotEmpty()) {
                            append("; Domain=${cookie.domain}")
                        }
                        if (cookie.path.isNotEmpty()) {
                            append("; Path=${cookie.path}")
                        }
                        if (cookie.secure) {
                            append("; Secure")
                        }
                        if (cookie.httpOnly) {
                            append("; HttpOnly")
                        }
                    }.toString()
                    cookieManager.setCookie(url, cookieStr)
                }
                cookieManager.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieEditorSheet(
    url: String,
    onClose: () -> Unit,
    onReloadPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanUrl = if (url.isBlank() || url == "dineinstyle.com") "https://dineinstyle.com" else url
    val defaultDomain = remember(cleanUrl) {
        try {
            Uri.parse(cleanUrl).host ?: "dineinstyle.com"
        } catch (e: Exception) {
            "dineinstyle.com"
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var cookiesList by remember { mutableStateOf<List<CookieItem>>(emptyList()) }
    var profilesMap by remember { mutableStateOf<Map<String, List<CookieItem>>>(emptyMap()) }

    // Load active custom cookies with system CookieManager sync
    fun loadCookies() {
        val cookieManager = CookieManager.getInstance()
        val cookieString = cookieManager.getCookie(cleanUrl) ?: ""
        
        // Load custom persisted metadata
        val customPersisted = CookiePersistence.loadCustomCookies(context, cleanUrl, defaultDomain).toMutableList()
        
        // Parse system cookies
        val systemCookies = if (cookieString.isBlank()) emptyList() else {
            cookieString.split(";").mapNotNull { pair ->
                val eqIndex = pair.indexOf('=')
                if (eqIndex != -1) {
                    val name = pair.substring(0, eqIndex).trim()
                    val value = pair.substring(eqIndex + 1).trim()
                    if (name.isNotEmpty()) {
                        CookieItem(name = name, value = value, domain = defaultDomain, path = "/", secure = true, httpOnly = false)
                    } else null
                } else {
                    val name = pair.trim()
                    if (name.isNotEmpty()) {
                        CookieItem(name = name, value = "", domain = defaultDomain, path = "/", secure = true, httpOnly = false)
                    } else null
                }
            }
        }
        
        // Merge system cookies into persisted list (preserves custom metadata like domain, path, secure)
        val mergedList = mutableListOf<CookieItem>()
        systemCookies.forEach { sysCookie ->
            val matchIndex = customPersisted.indexOfFirst { it.name == sysCookie.name }
            if (matchIndex != -1) {
                val existing = customPersisted[matchIndex]
                val updated = existing.copy(value = sysCookie.value)
                mergedList.add(updated)
                customPersisted.removeAt(matchIndex)
            } else {
                mergedList.add(sysCookie)
            }
        }
        
        // Add any remaining persisted cookies not returned currently
        mergedList.addAll(customPersisted)
        
        // Save back merged list
        CookiePersistence.saveCustomCookies(context, cleanUrl, mergedList)
        cookiesList = mergedList
    }

    // Load custom backup session profiles
    fun loadProfiles() {
        profilesMap = CookiePersistence.loadProfiles(context, cleanUrl)
    }

    LaunchedEffect(cleanUrl) {
        loadCookies()
        loadProfiles()
    }

    var expandedCookieName by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSaveProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    val filteredCookies = remember(cookiesList, searchQuery) {
        if (searchQuery.isBlank()) cookiesList else {
            cookiesList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.value.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            // --- HEADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "Cookie-Editor",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Domain: $defaultDomain",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- SESSION PROFILES ROW (Frictionless Backups) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session Backups / Profiles",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { showSaveProfileDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Save Profile",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Current", fontSize = 11.sp)
                    }
                }
                
                if (profilesMap.isEmpty()) {
                    Text(
                        text = "No saved profiles. Click 'Save Current' to back up this session.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        profilesMap.forEach { (profileName, cookies) ->
                            ProfileChip(
                                name = profileName,
                                onClick = {
                                    // RESTORE PROFILE: 1 CLICK
                                    val cookieManager = CookieManager.getInstance()
                                    // Delete active cookies
                                    cookiesList.forEach { cookie ->
                                        cookieManager.setCookie(cleanUrl, "${cookie.name}=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
                                        cookieManager.setCookie(cleanUrl, "${cookie.name}=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=$defaultDomain; Path=/")
                                    }
                                    // Set profile cookies
                                    cookies.forEach { cookie ->
                                        val cookieStr = "${cookie.name}=${cookie.value}; Domain=${cookie.domain}; Path=${cookie.path}" +
                                                (if (cookie.secure) "; Secure" else "") + (if (cookie.httpOnly) "; HttpOnly" else "")
                                        cookieManager.setCookie(cleanUrl, cookieStr)
                                    }
                                    cookieManager.flush()
                                    
                                    // Update active persisted cookies
                                    CookiePersistence.saveCustomCookies(context, cleanUrl, cookies)
                                    loadCookies()
                                    Toast.makeText(context, "Restored session profile: '$profileName'", Toast.LENGTH_SHORT).show()
                                    onReloadPage()
                                },
                                onDelete = {
                                    CookiePersistence.deleteProfile(context, cleanUrl, profileName)
                                    loadProfiles()
                                    Toast.makeText(context, "Deleted profile '$profileName'", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cookies...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", modifier = Modifier.size(20.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("cookie_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // --- COOKIE LIST ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredCookies.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cookie,
                            contentDescription = "No Cookies",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No cookies found on this page" else "No matching cookies found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        items(filteredCookies.size) { index ->
                            val cookie = filteredCookies[index]
                            CookieCard(
                                cookie = cookie,
                                isExpanded = expandedCookieName == cookie.name,
                                onExpandToggle = {
                                    expandedCookieName = if (expandedCookieName == cookie.name) null else cookie.name
                                },
                                onSave = { oldName, name, value, domain, path, secure, httpOnly ->
                                    val cookieManager = CookieManager.getInstance()
                                    if (oldName != null && oldName != name) {
                                        cookieManager.setCookie(cleanUrl, "$oldName=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
                                        cookieManager.setCookie(cleanUrl, "$oldName=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=$defaultDomain; Path=/")
                                    }
                                    val cookieStringValue = "$name=$value; Domain=$domain; Path=$path" +
                                            (if (secure) "; Secure" else "") + (if (httpOnly) "; HttpOnly" else "")
                                    cookieManager.setCookie(cleanUrl, cookieStringValue)
                                    cookieManager.flush()

                                    // Save update to persisted list
                                    val updatedList = cookiesList.map {
                                        if (it.name == (oldName ?: name)) {
                                            CookieItem(name, value, domain, path, secure, httpOnly)
                                        } else it
                                    }
                                    CookiePersistence.saveCustomCookies(context, cleanUrl, updatedList)

                                    loadCookies()
                                    Toast.makeText(context, "Cookie updated!", Toast.LENGTH_SHORT).show()
                                    onReloadPage()
                                },
                                onDelete = { name ->
                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setCookie(cleanUrl, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
                                    cookieManager.setCookie(cleanUrl, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=$defaultDomain; Path=/")
                                    cookieManager.flush()

                                    val updatedList = cookiesList.filter { it.name != name }
                                    CookiePersistence.saveCustomCookies(context, cleanUrl, updatedList)

                                    loadCookies()
                                    Toast.makeText(context, "Cookie deleted!", Toast.LENGTH_SHORT).show()
                                    onReloadPage()
                                },
                                defaultDomain = defaultDomain
                            )
                        }
                    }
                }
            }

            // --- BOTTOM TOOLBAR ACTION BAR ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. ADD COOKIE (+)
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(48.dp)
                            .testTag("cookie_add_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Cookie",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 2. DELETE ALL COOKIES
                    IconButton(
                        onClick = {
                            val cookieManager = CookieManager.getInstance()
                            cookiesList.forEach { cookie ->
                                cookieManager.setCookie(cleanUrl, "${cookie.name}=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/")
                                cookieManager.setCookie(cleanUrl, "${cookie.name}=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=$defaultDomain; Path=/")
                            }
                            cookieManager.flush()
                            
                            CookiePersistence.saveCustomCookies(context, cleanUrl, emptyList())
                            loadCookies()
                            Toast.makeText(context, "All cookies deleted!", Toast.LENGTH_SHORT).show()
                            onReloadPage()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                            .size(48.dp)
                            .testTag("cookie_delete_all_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete All",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 3. IMPORT COOKIES
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .size(48.dp)
                            .testTag("cookie_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Input,
                            contentDescription = "Import Cookies",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 4. EXPORT COOKIES TO CLIPBOARD (JSON format)
                    IconButton(
                        onClick = {
                            val array = JSONArray()
                            cookiesList.forEach { cookie ->
                                val obj = JSONObject()
                                obj.put("name", cookie.name)
                                obj.put("value", cookie.value)
                                obj.put("domain", cookie.domain)
                                obj.put("path", cookie.path)
                                obj.put("secure", cookie.secure)
                                obj.put("httpOnly", cookie.httpOnly)
                                array.put(obj)
                            }
                            val jsonString = array.toString(2)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Browser Cookies", jsonString)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Cookies copied to clipboard in JSON format!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                            .size(48.dp)
                            .testTag("cookie_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Export Cookies",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // --- SAVE PROFILE DIALOG ---
    if (showSaveProfileDialog) {
        Dialog(onDismissRequest = { showSaveProfileDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Save Session Backup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "This will capture all current cookies for '$defaultDomain' as a named backup session.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Backup Session Name") },
                        singleLine = true,
                        placeholder = { Text("e.g. Session-A") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSaveProfileDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newProfileName.isBlank()) {
                                    Toast.makeText(context, "Please enter a profile name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                CookiePersistence.saveProfile(context, cleanUrl, newProfileName.trim(), cookiesList)
                                loadProfiles()
                                showSaveProfileDialog = false
                                Toast.makeText(context, "Session saved as '${newProfileName.trim()}'!", Toast.LENGTH_SHORT).show()
                                newProfileName = ""
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // --- ADD DIALOG ---
    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }
        var newDomain by remember { mutableStateOf(defaultDomain) }
        var newPath by remember { mutableStateOf("/") }
        var newSecure by remember { mutableStateOf(true) }
        var newHttpOnly by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showAddDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create Cookie",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Cookie Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_cookie_name_input")
                    )

                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Cookie Value") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_cookie_val_input")
                    )

                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = { newDomain = it },
                        label = { Text("Domain") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPath,
                        onValueChange = { newPath = it },
                        label = { Text("Path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { newSecure = !newSecure }
                        ) {
                            Checkbox(
                                checked = newSecure,
                                onCheckedChange = { newSecure = it }
                            )
                            Text("Secure", fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { newHttpOnly = !newHttpOnly }
                        ) {
                            Checkbox(
                                checked = newHttpOnly,
                                onCheckedChange = { newHttpOnly = it }
                            )
                            Text("HttpOnly", fontSize = 13.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isBlank()) {
                                    Toast.makeText(context, "Cookie name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val cookieManager = CookieManager.getInstance()
                                val cookieStr = "$newName=$newValue; Domain=$newDomain; Path=$newPath" +
                                        (if (newSecure) "; Secure" else "") + (if (newHttpOnly) "; HttpOnly" else "")
                                cookieManager.setCookie(cleanUrl, cookieStr)
                                cookieManager.flush()

                                val newCookie = CookieItem(newName, newValue, newDomain, newPath, newSecure, newHttpOnly)
                                val updatedList = cookiesList + newCookie
                                CookiePersistence.saveCustomCookies(context, cleanUrl, updatedList)

                                loadCookies()
                                showAddDialog = false
                                Toast.makeText(context, "Cookie added!", Toast.LENGTH_SHORT).show()
                                onReloadPage()
                            },
                            modifier = Modifier.testTag("add_cookie_confirm_btn")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // --- IMPORT DIALOG ---
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showImportDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Import Cookies",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Paste JSON cookies array or a raw Cookie Header string.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        placeholder = { Text("[{\"name\": \"myCookie\", \"value\": \"someValue\"}]") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_cookie_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showImportDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = importInput.trim()
                                if (trimmed.isEmpty()) {
                                    Toast.makeText(context, "Paste input cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val cookieManager = CookieManager.getInstance()
                                var success = false
                                try {
                                    if (trimmed.startsWith("[")) {
                                        val array = JSONArray(trimmed)
                                        val importedCookies = mutableListOf<CookieItem>()
                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)
                                            val name = obj.optString("name")
                                            val value = obj.optString("value")
                                            if (!name.isNullOrEmpty()) {
                                                val domain = obj.optString("domain", defaultDomain)
                                                val path = obj.optString("path", "/")
                                                val secure = obj.optBoolean("secure", true)
                                                val httpOnly = obj.optBoolean("httpOnly", false)
                                                
                                                val cookieStr = "$name=$value; Domain=$domain; Path=$path" +
                                                        (if (secure) "; Secure" else "") + (if (httpOnly) "; HttpOnly" else "")
                                                cookieManager.setCookie(cleanUrl, cookieStr)
                                                importedCookies.add(CookieItem(name, value, domain, path, secure, httpOnly))
                                            }
                                        }
                                        
                                        val updatedList = cookiesList.filter { oldCookie ->
                                            importedCookies.none { it.name == oldCookie.name }
                                        } + importedCookies
                                        CookiePersistence.saveCustomCookies(context, cleanUrl, updatedList)
                                        success = true
                                    } else {
                                        val parts = trimmed.split(";")
                                        val importedCookies = mutableListOf<CookieItem>()
                                        parts.forEach { part ->
                                            val eq = part.indexOf('=')
                                            if (eq != -1) {
                                                val name = part.substring(0, eq).trim()
                                                val value = part.substring(eq + 1).trim()
                                                if (name.isNotEmpty()) {
                                                    cookieManager.setCookie(cleanUrl, "$name=$value; Domain=$defaultDomain; Path=/; Secure")
                                                    importedCookies.add(CookieItem(name, value, defaultDomain, "/", secure = true, httpOnly = false))
                                                }
                                            }
                                        }
                                        val updatedList = cookiesList.filter { oldCookie ->
                                            importedCookies.none { it.name == oldCookie.name }
                                        } + importedCookies
                                        CookiePersistence.saveCustomCookies(context, cleanUrl, updatedList)
                                        success = true
                                    }
                                    cookieManager.flush()
                                    loadCookies()
                                    showImportDialog = false
                                    Toast.makeText(context, "Import successful!", Toast.LENGTH_SHORT).show()
                                    onReloadPage()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid format. Check your input.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.testTag("import_cookie_confirm_btn")
                        ) {
                            Text("Import")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileChip(
    name: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete Profile",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieCard(
    cookie: CookieItem,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onSave: (oldName: String?, name: String, value: String, domain: String, path: String, secure: Boolean, httpOnly: Boolean) -> Unit,
    onDelete: (name: String) -> Unit,
    defaultDomain: String
) {
    var editName by remember(cookie.name) { mutableStateOf(cookie.name) }
    var editValue by remember(cookie.value) { mutableStateOf(cookie.value) }
    var editDomain by remember(cookie.domain) { mutableStateOf(cookie.domain) }
    var editPath by remember(cookie.path) { mutableStateOf(cookie.path) }
    var editSecure by remember(cookie.secure) { mutableStateOf(cookie.secure) }
    var editHttpOnly by remember(cookie.httpOnly) { mutableStateOf(cookie.httpOnly) }
    
    var showAdvanced by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isExpanded) 1.5.dp else 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // --- COLLAPSED VIEW HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cookie,
                        contentDescription = "Cookie icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = cookie.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = cookie.value,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- EXPANDED EDIT VIEW ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left-side icon buttons: Delete & Save
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(
                                onClick = { onSave(cookie.name, editName, editValue, editDomain, editPath, editSecure, editHttpOnly) },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .size(38.dp)
                                    .testTag("cookie_save_${cookie.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDelete(cookie.name) },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                                    .size(38.dp)
                                    .testTag("cookie_delete_${cookie.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Right-side inputs: Name & Value
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Name", fontSize = 11.sp) },
                                textStyle = TextStyle(fontSize = 13.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            OutlinedTextField(
                                value = editValue,
                                onValueChange = { editValue = it },
                                label = { Text("Value", fontSize = 11.sp) },
                                textStyle = TextStyle(fontSize = 13.sp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    // Show Advanced Button / Advanced fields
                    AnimatedVisibility(visible = showAdvanced) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editDomain,
                                onValueChange = { editDomain = it },
                                label = { Text("Domain", fontSize = 11.sp) },
                                textStyle = TextStyle(fontSize = 13.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            OutlinedTextField(
                                value = editPath,
                                onValueChange = { editPath = it },
                                label = { Text("Path", fontSize = 11.sp) },
                                textStyle = TextStyle(fontSize = 13.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { editSecure = !editSecure }
                                ) {
                                    Checkbox(
                                        checked = editSecure,
                                        onCheckedChange = { editSecure = it }
                                    )
                                    Text("Secure", fontSize = 13.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { editHttpOnly = !editHttpOnly }
                                ) {
                                    Checkbox(
                                        checked = editHttpOnly,
                                        onCheckedChange = { editHttpOnly = it }
                                    )
                                    Text("HttpOnly", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { showAdvanced = !showAdvanced },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("cookie_toggle_advanced_${cookie.name}")
                        ) {
                            Text(
                                text = if (showAdvanced) "Hide Advanced" else "Show Advanced",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
