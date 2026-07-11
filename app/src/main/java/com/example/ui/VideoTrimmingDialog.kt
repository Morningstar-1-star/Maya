package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CapturedMedia
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmingDialog(
    media: CapturedMedia,
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val trimProgress by viewModel.trimProgressState.collectAsStateWithLifecycle()

    var videoDurationMs by remember { mutableStateOf(0L) }
    var isFetchingDuration by remember { mutableStateOf(false) }

    // Fetch video duration in background on start
    LaunchedEffect(media.url) {
        isFetchingDuration = true
        withContext(Dispatchers.IO) {
            var retriever: android.media.MediaMetadataRetriever? = null
            try {
                retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(media.url, HashMap<String, String>())
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                videoDurationMs = durationStr?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    retriever?.release()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                isFetchingDuration = false
            }
        }
    }

    // Slider State
    var startValMs by remember { mutableStateOf(0f) }
    var endValMs by remember { mutableStateOf(10000f) }

    // Update default range once duration is loaded
    LaunchedEffect(videoDurationMs) {
        if (videoDurationMs > 0) {
            startValMs = 0f
            endValMs = videoDurationMs.toFloat()
        }
    }

    // Manual input states
    var startInputSeconds by remember { mutableStateOf("0") }
    var endInputSeconds by remember { mutableStateOf("10") }

    // Synchronize inputs when slider moves
    LaunchedEffect(startValMs, endValMs) {
        startInputSeconds = (startValMs / 1000).toInt().toString()
        endInputSeconds = (endValMs / 1000).toInt().toString()
    }

    Dialog(onDismissRequest = {
        if (trimProgress == null) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A) // Dark deep slate blue
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Trim",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Video Snipper",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (trimProgress == null) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // If background job is in progress, show beautiful live progress tracker
                trimProgress?.let { progress ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (progress.phase) {
                                "Downloading" -> {
                                    CircularProgressIndicator(
                                        color = Color(0xFFF43F5E),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Downloading Source video...",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                "Trimming" -> {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Clipping requested piece...",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                "Success" -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Trim Successful!",
                                        color = Color(0xFF10B981),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                "Failed" -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Trim Failed",
                                        color = Color(0xFFEF4444),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = progress.message,
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Show linear bar
                            LinearProgressIndicator(
                                progress = progress.progress,
                                color = if (progress.phase == "Success") Color(0xFF10B981) else Color(0xFFF43F5E),
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (progress.phase == "Success" || progress.phase == "Failed") {
                                Button(
                                    onClick = {
                                        viewModel.clearTrimProgress()
                                        if (progress.phase == "Success") {
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.1f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(100.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Done", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } ?: run {
                    // Control view
                    // Selected video URL / Name
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (media.pageTitle.isBlank()) "Media Stream File" else media.pageTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = media.url,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isFetchingDuration) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = Color(0xFFF43F5E),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Analyzing remote video duration...",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        if (videoDurationMs > 0) {
                            // Beautiful Range Slider
                            Text(
                                text = "Select Time Range:",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            RangeSlider(
                                value = startValMs..endValMs,
                                onValueChange = { range ->
                                    startValMs = range.start
                                    endValMs = range.endInclusive
                                },
                                valueRange = 0f..videoDurationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF43F5E),
                                    activeTrackColor = Color(0xFFF43F5E),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )

                            // Timestamp indicator row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Start: ${formatMsToTime(startValMs.toLong())}",
                                    color = Color(0xFFF43F5E),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "End: ${formatMsToTime(endValMs.toLong())}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Duration",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Total Clip Length: ${formatMsToTime((endValMs - startValMs).toLong())}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Duration could not be fetched automatically. Manual timing entry.
                            Text(
                                text = "Could not detect duration automatically. Please type the segment start/end seconds manually below:",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Manual Entry inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start (seconds):",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = startInputSeconds,
                                    onValueChange = { input ->
                                        startInputSeconds = input.filter { it.isDigit() }
                                        val sec = startInputSeconds.toFloatOrNull() ?: 0f
                                        val ms = sec * 1000
                                        if (videoDurationMs > 0) {
                                            if (ms <= endValMs) {
                                                startValMs = ms.coerceIn(0f, videoDurationMs.toFloat())
                                            }
                                        } else {
                                            startValMs = ms
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End (seconds):",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = endInputSeconds,
                                    onValueChange = { input ->
                                        endInputSeconds = input.filter { it.isDigit() }
                                        val sec = endInputSeconds.toFloatOrNull() ?: 0f
                                        val ms = sec * 1000
                                        if (videoDurationMs > 0) {
                                            if (ms >= startValMs) {
                                                endValMs = ms.coerceIn(0f, videoDurationMs.toFloat())
                                            }
                                        } else {
                                            endValMs = ms
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val start = startValMs.toLong()
                                    val end = endValMs.toLong()
                                    if (end <= start) {
                                        android.widget.Toast.makeText(context, "End time must be greater than start time!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        val originalFilename = if (media.pageTitle.isNotBlank()) {
                                            "${media.pageTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}.mp4"
                                        } else {
                                            "${System.currentTimeMillis()}.mp4"
                                        }
                                        viewModel.downloadAndTrimVideo(
                                            context = context,
                                            videoUrl = media.url,
                                            originalFilename = originalFilename,
                                            startMs = start,
                                            endMs = end
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF43F5E),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Trim & Download", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMsToTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
