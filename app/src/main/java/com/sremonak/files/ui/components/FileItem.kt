package com.sremonak.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sremonak.files.data.model.FileCategory
import com.sremonak.files.data.model.FileInfo
import com.sremonak.files.ui.theme.ApkColor
import com.sremonak.files.ui.theme.ArchiveColor
import com.sremonak.files.ui.theme.AudioColor
import com.sremonak.files.ui.theme.CodeColor
import com.sremonak.files.ui.theme.DocumentColor
import com.sremonak.files.ui.theme.FolderColor
import com.sremonak.files.ui.theme.FontColor
import com.sremonak.files.ui.theme.ImageColor
import com.sremonak.files.ui.theme.OtherColor
import com.sremonak.files.ui.theme.PdfColor
import com.sremonak.files.ui.theme.VideoColor
import com.sremonak.files.util.FileUtils

fun iconAndColorFor(info: FileInfo): Pair<ImageVector, Color> {
    if (info.isDirectory) return Icons.Filled.Folder to FolderColor
    return when (info.category) {
        FileCategory.IMAGE -> Icons.Filled.Image to ImageColor
        FileCategory.VIDEO -> Icons.Filled.Videocam to VideoColor
        FileCategory.AUDIO -> Icons.Filled.Audiotrack to AudioColor
        FileCategory.DOCUMENT -> Icons.Filled.Description to DocumentColor
        FileCategory.PDF -> Icons.Filled.PictureAsPdf to PdfColor
        FileCategory.APK -> Icons.Filled.Android to ApkColor
        FileCategory.ARCHIVE -> Icons.Filled.Archive to ArchiveColor
        FileCategory.CODE -> Icons.Filled.Code to CodeColor
        FileCategory.FONT -> Icons.Filled.FontDownload to FontColor
        FileCategory.OTHER -> Icons.Filled.InsertDriveFile to OtherColor
    }
}

@Composable
fun FileTypeIcon(info: FileInfo, size: Dp = 44.dp) {
    val (icon, color) = iconAndColorFor(info)
    if (!info.isDirectory && info.category == FileCategory.IMAGE) {
        AsyncImage(
            model = info.file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    info: FileInfo,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        leadingContent = { FileTypeIcon(info) },
        headlineContent = {
            Text(info.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            val subtitle = if (info.isDirectory) {
                "${info.childCount} items"
            } else {
                "${FileUtils.formatBytes(info.size)} • ${FileUtils.formatDate(info.lastModified)}"
            }
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            when {
                selectionMode -> Checkbox(checked = selected, onCheckedChange = null)
                info.isFavorite -> Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B))
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    info: FileInfo,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            FileTypeIcon(info, size = 56.dp)
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = null)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = info.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = if (info.isDirectory) "${info.childCount} items" else FileUtils.formatBytes(info.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
