package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.UserScript
import com.example.viewmodel.BrowserViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.zip.ZipInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScriptsSubScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val scripts by viewModel.allUserScripts.collectAsState()
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var scriptToEdit by remember { mutableStateOf<UserScript?>(null) }
    
    // ZIP Extension Importer Activity Result Contract
    val zipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(fileUri) ?: return@launch
                    val zipInputStream = ZipInputStream(inputStream)
                    var entry = zipInputStream.nextEntry
                    
                    var manifestContent = ""
                    val fileContents = mutableMapOf<String, String>()
                    
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            // Read contents
                            val reader = BufferedReader(InputStreamReader(zipInputStream))
                            val sb = java.lang.StringBuilder()
                            var line = reader.readLine()
                            while (line != null) {
                                sb.append(line).append("\n")
                                line = reader.readLine()
                            }
                            val content = sb.toString()
                            
                            if (name.endsWith("manifest.json", ignoreCase = true)) {
                                manifestContent = content
                            } else if (name.endsWith(".js", ignoreCase = true)) {
                                val cleanName = name.substringAfterLast("/")
                                fileContents[cleanName] = content
                            }
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                    zipInputStream.close()
                    
                    withContext(Dispatchers.Main) {
                        if (manifestContent.isNotBlank()) {
                            try {
                                val json = JSONObject(manifestContent)
                                val extName = json.optString("name", "Imported Extension")
                                val extDesc = json.optString("description", "Imported custom Chrome/WebExtension ZIP")
                                
                                val contentScripts = json.optJSONArray("content_scripts")
                                if (contentScripts != null && contentScripts.length() > 0) {
                                    var importedAny = false
                                    for (i in 0 until contentScripts.length()) {
                                        val scriptObj = contentScripts.optJSONObject(i) ?: continue
                                        val matchesArray = scriptObj.optJSONArray("matches")
                                        val matchUrl = if (matchesArray != null && matchesArray.length() > 0) {
                                            matchesArray.optString(0, "*")
                                        } else {
                                            "*"
                                        }
                                        
                                        val jsArray = scriptObj.optJSONArray("js")
                                        if (jsArray != null && jsArray.length() > 0) {
                                            for (j in 0 until jsArray.length()) {
                                                val jsFileName = jsArray.optString(j, "").substringAfterLast("/")
                                                val jsCode = fileContents[jsFileName]
                                                if (!jsCode.isNullOrBlank()) {
                                                    val scriptName = if (jsArray.length() > 1) "$extName - Part ${j + 1}" else extName
                                                    viewModel.insertUserScript(
                                                        UserScript(
                                                            name = scriptName,
                                                            description = extDesc,
                                                            matchUrl = matchUrl,
                                                            code = jsCode,
                                                            isEnabled = true
                                                        )
                                                    )
                                                    importedAny = true
                                                }
                                            }
                                        }
                                    }
                                    if (importedAny) {
                                        android.widget.Toast.makeText(context, "Successfully imported content scripts from $extName!", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Parsed manifest but couldn't find/match declared .js files in ZIP.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    // Fallback if no content scripts in manifest: import any JS files found
                                    if (fileContents.isNotEmpty()) {
                                        fileContents.forEach { (fileName, code) ->
                                            viewModel.insertUserScript(
                                                UserScript(
                                                    name = "$extName - $fileName",
                                                    description = extDesc,
                                                    matchUrl = "*",
                                                    code = code,
                                                    isEnabled = true
                                                )
                                            )
                                        }
                                        android.widget.Toast.makeText(context, "Imported ${fileContents.size} script files from ZIP!", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "No .js files found in the ZIP to import.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                if (fileContents.isNotEmpty()) {
                                    fileContents.forEach { (fileName, code) ->
                                        viewModel.insertUserScript(
                                            UserScript(
                                                name = fileName.replace(".js", ""),
                                                description = "Imported raw JS script",
                                                matchUrl = "*",
                                                code = code,
                                                isEnabled = true
                                            )
                                        )
                                    }
                                    android.widget.Toast.makeText(context, "Imported ${fileContents.size} scripts (Manifest parsing failed).", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Error reading manifest: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            if (fileContents.isNotEmpty()) {
                                fileContents.forEach { (fileName, code) ->
                                    val formattedName = fileName.replace(".js", "").replace("_", " ")
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                    viewModel.insertUserScript(
                                        UserScript(
                                            name = formattedName,
                                            description = "Imported raw JS script (No manifest.json)",
                                            matchUrl = "*",
                                            code = code,
                                            isEnabled = true
                                        )
                                    )
                                }
                                android.widget.Toast.makeText(context, "No manifest.json. Imported ${fileContents.size} JS files as raw User Scripts!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "Invalid extension ZIP: No manifest.json or .js files found.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error importing extension ZIP: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Form states
    var formName by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formMatchUrl by remember { mutableStateOf("") }
    var formCode by remember { mutableStateOf("") }

    // Preset templates
    val templates = listOf(
        UserScript(
            name = "Force Dark Mode CSS",
            description = "Applies high-quality custom CSS inversion to force dark mode on all elements of any webpage.",
            matchUrl = "*",
            code = """(function() {
    if (!document.getElementById('custom-user-dark')) {
        const style = document.createElement('style');
        style.id = 'custom-user-dark';
        style.innerHTML = `
            html { filter: invert(1) hue-rotate(180deg) !important; background: #000 !important; }
            img, video, iframe, canvas { filter: invert(1) hue-rotate(180deg) !important; }
        `;
        document.head.appendChild(style);
    }
})();"""
        ),
        UserScript(
            name = "Blur Out Images",
            description = "Blurs all images on current webpage until you hover over them. Great for focus and reading.",
            matchUrl = "*",
            code = """(function() {
    const style = document.createElement('style');
    style.innerHTML = `
        img { filter: blur(15px) !important; transition: filter 0.3s ease !important; }
        img:hover { filter: none !important; }
    `;
    document.head.appendChild(style);
})();"""
        ),
        UserScript(
            name = "Auto-Refresh Page",
            description = "Periodically refreshes the page every 30 seconds. Perfect for monitoring updates.",
            matchUrl = "example.com",
            code = """(function() {
    setTimeout(function() {
        location.reload();
    }, 30000);
    console.log("Script Manager: Will reload page in 30 seconds");
})();"""
        ),
        UserScript(
            name = "Show Image Alt Text",
            description = "Appends the 'alt' description text of every image underneath it inside a beautiful bubble.",
            matchUrl = "*",
            code = """(function() {
    const imgs = document.querySelectorAll('img');
    imgs.forEach(img => {
        if (img.alt && !img.dataset.altShown) {
            img.dataset.altShown = "true";
            const bubble = document.createElement('div');
            bubble.innerText = "Alt: " + img.alt;
            bubble.style.cssText = "background: rgba(0,0,0,0.85); color: #22c55e; font-size: 11px; padding: 4px 8px; border-radius: 4px; position: absolute; z-index: 999999;";
            img.parentNode.insertBefore(bubble, img.nextSibling);
        }
    });
})();"""
        ),
        UserScript(
            name = "GreasyFork Quick Finder",
            description = "Adds a subtle, floating search button in the bottom right corner of the page that queries GreasyFork for scripts for the current website domain.",
            matchUrl = "*",
            code = """(function() {
    if (window.gfButtonInjected) return;
    window.gfButtonInjected = true;
    
    const btn = document.createElement('div');
    btn.innerText = "🔍 GreasyFork";
    btn.style.cssText = "position: fixed; bottom: 80px; right: 20px; z-index: 999999; background: #c62828; color: white; padding: 8px 12px; border-radius: 20.dp; font-size: 12px; font-weight: bold; cursor: pointer; box-shadow: 0 4px 12px rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.2);";
    
    btn.onclick = function() {
        const domain = window.location.hostname;
        window.open('https://greasyfork.org/scripts?q=' + encodeURIComponent(domain));
    };
    
    document.body.appendChild(btn);
})();"""
        )
    )

    fun openForm(script: UserScript? = null) {
        scriptToEdit = script
        if (script != null) {
            formName = script.name
            formDescription = script.description
            formMatchUrl = script.matchUrl
            formCode = script.code
        } else {
            formName = ""
            formDescription = ""
            formMatchUrl = "*"
            formCode = "// Write your Javascript here...\n(function() {\n    'use strict';\n    console.log('Script loaded successfully!');\n})();"
        }
        showEditDialog = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner/Intro Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "User scripts run sandboxed Javascript matching the URL wildcard. You can use '*' for all pages, or domain parts (e.g., 'wikipedia').",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { openForm(null) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Script", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    zipLauncher.launch("application/zip")
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import ZIP (.zip)", fontSize = 14.sp)
            }
        }

        // Templates Section Heading
        Text(
            text = "ONE-CLICK PRESET LIBRARY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Horizontal scrolling preset library cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            templates.forEach { tmpl ->
                val alreadyAdded = scripts.any { it.name == tmpl.name }
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .clickable(!alreadyAdded) {
                            viewModel.insertUserScript(tmpl.copy(isEnabled = true))
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (alreadyAdded) Color(0xFF0F172A) else Color(0xFF0B1224)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (alreadyAdded) Color(0xFF22C55E).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tmpl.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alreadyAdded) Color(0xFF22C55E) else Color.White,
                                maxLines = 1
                            )
                            if (alreadyAdded) {
                                Icon(Icons.Default.Check, contentDescription = "Added", tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = tmpl.description,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 3,
                            lineHeight = 15.sp,
                            modifier = Modifier.height(45.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "URL: ${tmpl.matchUrl}",
                                fontSize = 10.sp,
                                color = Color(0xFFA78BFA),
                                fontWeight = FontWeight.Bold
                            )
                            if (!alreadyAdded) {
                                Text(
                                    text = "Add +",
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scripts List Heading
        Text(
            text = "YOUR USER SCRIPTS (${scripts.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF34D399),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                    Text("No scripts found. Add one above!", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                }
            }
        } else {
            scripts.forEach { script ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1224)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = script.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = script.description,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = script.isEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateUserScript(script.copy(isEnabled = isChecked))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF34D399),
                                    checkedTrackColor = Color(0xFF34D399).copy(alpha = 0.3f)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = Color(0xFF1E1B4B),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Matches: ${script.matchUrl}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF818CF8),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { openForm(script) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Script",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteUserScript(script) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Script",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Interactive Monospace Code Editor Dialog
    if (showEditDialog) {
        Dialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF030712),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (scriptToEdit == null) "Create User Script" else "Edit User Script",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showEditDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                    // Form Fields Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Script Name
                        OutlinedTextField(
                            value = formName,
                            onValueChange = { formName = it },
                            label = { Text("Script Name", color = Color.White.copy(alpha = 0.6f)) },
                            placeholder = { Text("e.g. My Awesome Ad-Blocker") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Script Description
                        OutlinedTextField(
                            value = formDescription,
                            onValueChange = { formDescription = it },
                            label = { Text("Description", color = Color.White.copy(alpha = 0.6f)) },
                            placeholder = { Text("Describe what this script does...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // URL Match Pattern
                        OutlinedTextField(
                            value = formMatchUrl,
                            onValueChange = { formMatchUrl = it },
                            label = { Text("Match URL Pattern", color = Color.White.copy(alpha = 0.6f)) },
                            placeholder = { Text("Use '*' for all pages, or specific keywords") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Javascript Code Editor with Monospace font!
                        Text(
                            text = "JAVASCRIPT SOURCE CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        OutlinedTextField(
                            value = formCode,
                            onValueChange = { formCode = it },
                            placeholder = { Text("// Javascript code here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFF34D399)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFA78BFA),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color(0xFF050B18),
                                unfocusedContainerColor = Color(0xFF050B18)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                    // Dialog Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (formName.isNotBlank() && formCode.isNotBlank()) {
                                    val newScript = UserScript(
                                        id = scriptToEdit?.id ?: 0,
                                        name = formName,
                                        description = formDescription,
                                        matchUrl = formMatchUrl,
                                        code = formCode,
                                        isEnabled = scriptToEdit?.isEnabled ?: true
                                    )
                                    if (scriptToEdit == null) {
                                        viewModel.insertUserScript(newScript)
                                    } else {
                                        viewModel.updateUserScript(newScript)
                                    }
                                    showEditDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399)),
                            enabled = formName.isNotBlank() && formCode.isNotBlank()
                        ) {
                            Text("Save Script", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
