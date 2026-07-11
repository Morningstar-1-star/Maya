package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CapturedMedia

@Composable
fun DetectedVideoActionSheet(
    media: CapturedMedia,
    onClose: () -> Unit,
    onPlayFullscreen: (CapturedMedia) -> Unit,
    onPlayInBackground: (CapturedMedia) -> Unit,
    onAddToQueue: (CapturedMedia) -> Unit,
    onDownload: (CapturedMedia) -> Unit,
    onTrimDownload: (CapturedMedia) -> Unit = {},
    onSaveToPlaylist: (CapturedMedia) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detected_video_action_sheet"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Drag Handle Indicator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sheet Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Video Detected!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (media.pageTitle.isBlank()) "Media Stream Found" else media.pageTitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Panel",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(8.dp))

            // Action rows
            VideoActionItem(
                title = "Play Fullscreen / Expand Video",
                subtitle = "Watch in the premium browser video player",
                icon = Icons.Default.Fullscreen,
                iconColor = Color(0xFF38BDF8),
                onClick = {
                    onPlayFullscreen(media)
                    onClose()
                }
            )

            VideoActionItem(
                title = "Play in Background / Listen Audio",
                subtitle = "Keep playing the video sound even when screen is off",
                icon = Icons.Default.Headphones,
                iconColor = Color(0xFF10B981),
                onClick = {
                    onPlayInBackground(media)
                    onClose()
                }
            )

            VideoActionItem(
                title = "Queue Video / Add to Playlist",
                subtitle = "Add to background queue to autoplay next",
                icon = Icons.Default.PlaylistAdd,
                iconColor = Color(0xFFF59E0B),
                onClick = {
                    onAddToQueue(media)
                    onClose()
                }
            )

            VideoActionItem(
                title = "Save to Online Playlist",
                subtitle = "Watch anytime in browser video player from the home page",
                icon = Icons.Default.Bookmark,
                iconColor = Color(0xFF3B82F6),
                onClick = {
                    onSaveToPlaylist(media)
                    Toast.makeText(context, "Saved to online playlist!", Toast.LENGTH_SHORT).show()
                    onClose()
                }
            )

            VideoActionItem(
                title = "Download Video",
                subtitle = "Save video file offline to device downloads",
                icon = Icons.Default.Download,
                iconColor = Color(0xFFEC4899),
                onClick = {
                    onDownload(media)
                    onClose()
                }
            )

            VideoActionItem(
                title = "Trim & Snippet Video",
                subtitle = "Crop & download only a custom length / part of this video",
                icon = Icons.Default.ContentCut,
                iconColor = Color(0xFFF43F5E),
                onClick = {
                    onTrimDownload(media)
                    onClose()
                }
            )

            VideoActionItem(
                title = "Copy Video Stream Link",
                subtitle = "Copy the direct media playback URL to clipboard",
                icon = Icons.Default.ContentCopy,
                iconColor = Color(0xFFA78BFA),
                onClick = {
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Video Stream URL", media.url)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied video stream link!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to copy link", Toast.LENGTH_SHORT).show()
                    }
                    onClose()
                }
            )

            VideoActionItem(
                title = "Share Video Stream",
                subtitle = "Share direct playback link with friends",
                icon = Icons.Default.Share,
                iconColor = Color(0xFF6366F1),
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Shared Video Stream")
                            putExtra(Intent.EXTRA_TEXT, media.url)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Video Stream"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to share link", Toast.LENGTH_SHORT).show()
                    }
                    onClose()
                }
            )
        }
    }
}

@Composable
private fun VideoActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}
