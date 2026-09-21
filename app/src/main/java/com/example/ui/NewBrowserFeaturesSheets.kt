package com.example.ui

import android.content.Context
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.network.ProxyMode
import com.example.network.ProxyTorManager
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// 1. PROXY & TOR MANAGER SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyTorSheet(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val currentMode by ProxyTorManager.currentMode.collectAsState()
    val isApplied by ProxyTorManager.isProxyApplied.collectAsState()
    val statusMsg by ProxyTorManager.statusMessage.collectAsState()
    val isTorRunning by ProxyTorManager.isTorDaemonRunning.collectAsState()
    val autoRouteOnion by ProxyTorManager.autoRouteOnion.collectAsState()
    val customHost by ProxyTorManager.customHost.collectAsState()
    val customPort by ProxyTorManager.customPort.collectAsState()
    val selectedGateway by ProxyTorManager.selectedGateway.collectAsState()

    var editHost by remember(customHost) { mutableStateOf(customHost) }
    var editPort by remember(customPort) { mutableStateOf(customPort.toString()) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentMode != ProxyMode.DIRECT) Color(0xFF10B981).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentMode == ProxyMode.TOR_ORBOT || currentMode == ProxyMode.TOR_LOCAL) Icons.Default.Shield
                            else Icons.Default.Dns,
                            contentDescription = null,
                            tint = if (currentMode != ProxyMode.DIRECT) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Proxy & Tor Network",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            statusMsg,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Orbot Tor Status Card
            if (currentMode == ProxyMode.TOR_ORBOT || currentMode == ProxyMode.TOR_LOCAL) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTorRunning == true) Color(0xFF10B981).copy(alpha = 0.12f)
                        else if (isTorRunning == false) Color(0xFFEF4444).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isTorRunning == true) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isTorRunning == true) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isTorRunning == true) "Tor SOCKS5 Daemon Reachable"
                                    else if (isTorRunning == false) "Tor Daemon Not Detected"
                                    else "Checking Tor daemon...",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isTorRunning == true) "Traffic is actively encrypted & anonymized"
                                    else "Ensure Orbot app is started in VPN or SOCKS mode",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { ProxyTorManager.checkTorDaemonStatus() }) {
                            Text("Recheck", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "ROUTING MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            // Modes
            ProxyMode.values().forEach { mode ->
                val isSelected = currentMode == mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { ProxyTorManager.setProxyMode(mode) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { ProxyTorManager.setProxyMode(mode) }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                mode.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Custom Proxy Config Fields if Custom mode selected
            if (currentMode == ProxyMode.CUSTOM_SOCKS5 || currentMode == ProxyMode.CUSTOM_HTTP) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Custom Proxy Server Config", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editHost,
                                onValueChange = { editHost = it },
                                label = { Text("Host / IP") },
                                modifier = Modifier.weight(2f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = editPort,
                                onValueChange = { editPort = it },
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val p = editPort.toIntOrNull() ?: 8080
                                ProxyTorManager.setCustomProxy(editHost, p)
                                Toast.makeText(context, "Proxy updated: $editHost:$p", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Apply Custom Proxy")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // .onion Automated Routing Settings
            Text(
                "TOR (.ONION) AUTOMATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Detect .onion Domains", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        "Automatically offer Tor2Web gateway or Tor routing when visiting .onion hidden services",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoRouteOnion,
                    onCheckedChange = { ProxyTorManager.setAutoRouteOnion(it) }
                )
            }

            Spacer(Modifier.height(12.dp))

            Text("Tor2Web Fallback Gateway:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("onion.pet", "onion.ws", "onion.ly").forEach { gw ->
                    val isChosen = selectedGateway == gw
                    FilterChip(
                        selected = isChosen,
                        onClick = { ProxyTorManager.setSelectedGateway(gw) },
                        label = { Text(gw, fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. ONION GATEWAY CONFIRMATION DIALOG
// -------------------------------------------------------------

@Composable
fun OnionGatewayDialog(
    onionUrl: String,
    onRouteGateway: (String) -> Unit,
    onEnableTor: () -> Unit,
    onDismiss: () -> Unit
) {
    val gateway = ProxyTorManager.selectedGateway.collectAsState().value
    val gatewayUrl = remember(onionUrl, gateway) {
        ProxyTorManager.convertToTor2WebGateway(onionUrl, gateway)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF8B5CF6))
        },
        title = {
            Text("Tor Hidden Service (.onion)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column {
                Text(
                    "You navigated to a Tor .onion domain:\n$onionUrl",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Recommended Action:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(
                            "• Open via Tor2Web Gateway ($gateway) without installing extra apps, OR\n• Enable Tor SOCKS5 proxy via Orbot for end-to-end anonymity.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRouteGateway(gatewayUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text("Open with $gateway")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onEnableTor) {
                    Text("Turn On Tor")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// -------------------------------------------------------------
// 3. MAGNET & TORRENT ACTION SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnetTorrentSheet(
    parsedMagnet: ParsedMagnet,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF3B82F6))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Magnet / Torrent Link", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("BitTorrent Stream & Download", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Torrent Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Torrent Name", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        parsedMagnet.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (parsedMagnet.infoHash.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("InfoHash (BTIH)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            parsedMagnet.infoHash,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (parsedMagnet.trackers.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Trackers (${parsedMagnet.trackers.size})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        parsedMagnet.trackers.take(3).forEach { tr ->
                            Text(
                                tr,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (parsedMagnet.trackers.size > 3) {
                            Text("+ ${parsedMagnet.trackers.size - 3} more trackers", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Primary: Open in external client
            Button(
                onClick = {
                    val launched = MagnetTorrentManager.openInExternalBitTorrentClient(context, parsedMagnet.rawUri)
                    if (!launched) {
                        Toast.makeText(context, "No BitTorrent client found. Opening store...", Toast.LENGTH_LONG).show()
                        MagnetTorrentManager.openTorrentClientStoreSearch(context)
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open in BitTorrent Client (Flud / LibreTorrent)", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        MagnetTorrentManager.copyToClipboard(context, parsedMagnet.rawUri, "Magnet Link")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Magnet", fontSize = 12.sp)
                }

                if (parsedMagnet.infoHash.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            MagnetTorrentManager.copyToClipboard(context, parsedMagnet.infoHash, "InfoHash")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Hash", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        MagnetTorrentManager.shareMagnet(context, parsedMagnet.displayName, parsedMagnet.rawUri)
                    },
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. ANTI-TRACKING & PRIVACY SHIELD SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiTrackingPrivacySheet(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val trackerStripperOn by PrivacyShieldManager.trackerStripperEnabled.collectAsState()
    val canvasOn by PrivacyShieldManager.canvasProtectionEnabled.collectAsState()
    val audioOn by PrivacyShieldManager.audioProtectionEnabled.collectAsState()
    val hwOn by PrivacyShieldManager.hardwareSpoofEnabled.collectAsState()
    val totalBlocked by PrivacyShieldManager.totalTrackersStripped.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Privacy & Anti-Tracking Shield", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Tracker Stripping & Fingerprint Resistance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stat Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Blocked URL Trackers", fontSize = 12.sp, color = Color(0xFF047857))
                        Text(
                            "$totalBlocked trackers stripped",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Controls
            ShieldToggleRow(
                title = "URL Tracker Parameter Stripper",
                desc = "Removes utm_*, fbclid, gclid, msclkid, yclid, and link trackers on navigation",
                icon = Icons.Default.Link,
                checked = trackerStripperOn,
                onCheckedChange = { PrivacyShieldManager.setTrackerStripperEnabled(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            ShieldToggleRow(
                title = "Canvas Anti-Fingerprinting",
                desc = "Injects microscopic imperceptible noise into canvas readouts to defeat browser fingerprinting",
                icon = Icons.Default.Palette,
                checked = canvasOn,
                onCheckedChange = { PrivacyShieldManager.setCanvasProtectionEnabled(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            ShieldToggleRow(
                title = "AudioContext Fingerprint Shield",
                desc = "Adds subtle audio frequency jitter preventing unique acoustic hardware identification",
                icon = Icons.Default.VolumeUp,
                checked = audioOn,
                onCheckedChange = { PrivacyShieldManager.setAudioProtectionEnabled(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            ShieldToggleRow(
                title = "Hardware & Battery Identity Masking",
                desc = "Spoofs CPU concurrency, RAM memory values, and battery level APIs to generic constants",
                icon = Icons.Default.Smartphone,
                checked = hwOn,
                onCheckedChange = { PrivacyShieldManager.setHardwareSpoofEnabled(it) }
            )
        }
    }
}

@Composable
private fun ShieldToggleRow(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (checked) Color(0xFF10B981) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// -------------------------------------------------------------
// 5. OFFLINE ARCHIVES (.MHTML) SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineArchivesSheet(
    viewModel: BrowserViewModel,
    activeWebView: WebView?,
    currentTitle: String,
    currentUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val archives by OfflineArchiveManager.archives.collectAsState()
    val isSaving by OfflineArchiveManager.isSaving.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFFF59E0B))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Offline Web Archives", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Standalone .mhtml Snapshots", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(14.dp))

            // Save Active Page Button
            Button(
                onClick = {
                    if (activeWebView != null) {
                        OfflineArchiveManager.savePageArchive(context, activeWebView, currentTitle, currentUrl) { success, msg ->
                            if (!success) {
                                Toast.makeText(context, msg ?: "Could not save archive", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "No active page to save", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Saving Page Archive (.mhtml)...")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Current Page Offline (.mhtml)")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("SAVED PAGES (${archives.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            if (archives.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("No saved web archives yet", color = Color.Gray, fontSize = 14.sp)
                        Text("Tap 'Save Current Page Offline' to save complete pages", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(archives) { archive ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.loadUrl("file://${archive.filePath}")
                                            onClose()
                                        }
                                ) {
                                    Text(
                                        archive.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${archive.formattedSize} · ${archive.formattedDate}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        archive.originalUrl,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { OfflineArchiveManager.shareArchive(context, archive) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }

                                IconButton(onClick = { OfflineArchiveManager.deleteArchive(context, archive) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. AI PAGE INTELLIGENCE SHEET (SUMMARIZE & CHAT WITH PAGE)
// -------------------------------------------------------------

data class PageChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPageAssistantSheet(
    viewModel: BrowserViewModel,
    activeWebView: WebView?,
    currentTitle: String,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Summary, 1 = Chat

    // Extracted page content state
    var pageText by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(true) }

    // Summary state
    var summaryText by remember { mutableStateOf<String?>(null) }
    var isSummarizing by remember { mutableStateOf(false) }

    // Chat state
    val chatMessages = remember { mutableStateListOf<PageChatMessage>() }
    var chatInput by remember { mutableStateOf("") }
    var isChatting by remember { mutableStateOf(false) }

    // Extract text from active webview on open
    LaunchedEffect(activeWebView) {
        if (activeWebView != null) {
            val jsExtract = """
                (function() {
                    try {
                        const article = document.querySelector('article') || document.querySelector('main') || document.body;
                        return article.innerText || "";
                    } catch(e) {
                        return "";
                    }
                })();
            """.trimIndent()

            activeWebView.evaluateJavascript(jsExtract) { rawResult ->
                isExtracting = false
                val unquoted = rawResult?.removeSurrounding("\"")
                    ?.replace("\\n", "\n")
                    ?.replace("\\t", " ")
                    ?.replace("\\\"", "\"") ?: ""
                pageText = unquoted.take(15000)
            }
        } else {
            isExtracting = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF6366F1))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AI Web Intelligence", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(currentTitle.ifEmpty { "Active Webpage" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Page Summary") },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Chat with Page") },
                    icon = { Icon(Icons.Default.Functions, contentDescription = null) }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (selectedTab == 0) {
                // PAGE SUMMARY TAB
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (summaryText == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Executive Summary & Key Takeaways", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "Generate an instant AI breakdown of this webpage's core arguments and insights.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        isSummarizing = true
                                        coroutineScope.launch {
                                            val summary = viewModel.generatePageSummary(pageText)
                                            summaryText = summary
                                            isSummarizing = false
                                        }
                                    },
                                    enabled = !isSummarizing && pageText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                ) {
                                    if (isSummarizing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Reading & Analyzing Webpage...")
                                    } else {
                                        Text("Generate Instant Summary")
                                    }
                                }
                            }
                        }
                    } else {
                        // Display generated summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AI EXECUTIVE SUMMARY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF6366F1), letterSpacing = 1.sp)
                                    val context = LocalContext.current
                                    IconButton(
                                        onClick = {
                                            MagnetTorrentManager.copyToClipboard(context, summaryText ?: "", "Page Summary")
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    summaryText ?: "",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // CHAT WITH PAGE TAB
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        reverseLayout = true
                    ) {
                        items(chatMessages.reversed()) { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (msg.isUser) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Text(
                                        msg.text,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 13.sp,
                                        color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (chatMessages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Functions, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(6.dp))
                                        Text("Ask anything about this webpage", fontSize = 13.sp, color = Color.Gray)
                                        Text("e.g., 'What are the main findings?' or 'Summarize the pros & cons'", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    if (isChatting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Ask about this page...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (chatInput.isNotBlank() && !isChatting) {
                                        val q = chatInput.trim()
                                        chatMessages.add(PageChatMessage(isUser = true, text = q))
                                        chatInput = ""
                                        isChatting = true
                                        coroutineScope.launch {
                                            val prompt = "Context from webpage: ${pageText.take(6000)}\n\nUser question: $q\n\nAnswer concisely based on the context above:"
                                            val answer = viewModel.askGemini(prompt)
                                            chatMessages.add(PageChatMessage(isUser = false, text = answer))
                                            isChatting = false
                                        }
                                    }
                                }
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank() && !isChatting) {
                                    val q = chatInput.trim()
                                    chatMessages.add(PageChatMessage(isUser = true, text = q))
                                    chatInput = ""
                                    isChatting = true
                                    coroutineScope.launch {
                                        val prompt = "Context from webpage: ${pageText.take(6000)}\n\nUser question: $q\n\nAnswer concisely based on the context above:"
                                        val answer = viewModel.askGemini(prompt)
                                        chatMessages.add(PageChatMessage(isUser = false, text = answer))
                                        isChatting = false
                                    }
                                }
                            },
                            enabled = chatInput.isNotBlank() && !isChatting
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
