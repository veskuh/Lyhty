package net.veskuh.lyhty.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import net.veskuh.lyhty.data.local.entity.EntryEntity

@Composable
fun MinifluxActionRow(
    entry: EntryEntity,
    isCompact: Boolean = false,
    onFetchFullText: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkUnread: (Long) -> Unit,
    onMarkAllRead: (() -> Unit)? = null,
    onToggleBookmark: ((Long) -> Unit)? = null,
    onNextEntry: (() -> Unit)?,
    onPreviousEntry: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [ ← Articles ]
            if (onBack != null) {
                if (isCompact) {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Articles")
                    }
                } else {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Articles")
                    }
                }
            }

            // [ 🌐 Full Text ]
            if (isCompact) {
                IconButton(
                    onClick = { onFetchFullText(entry.id) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Language, contentDescription = "Full Text")
                }
            } else {
                Button(
                    onClick = { onFetchFullText(entry.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Full Text")
                }
            }

            // [ 🔗 Browser ]
            if (isCompact) {
                IconButton(
                    onClick = onOpenBrowser,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Browser")
                }
            } else {
                OutlinedButton(onClick = onOpenBrowser) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Browser")
                }
            }
        }

        // Overflow / Others Menu
        Box {
            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Others menu")
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                val isUnread = entry.status == "unread"

                DropdownMenuItem(
                    text = { Text(if (entry.starred) "Unstar Article" else "Star Article") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (entry.starred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (entry.starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleBookmark?.invoke(entry.id)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Mark as Read") },
                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                    enabled = isUnread,
                    onClick = {
                        isMenuExpanded = false
                        onMarkRead(entry.id)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Mark as Unread") },
                    leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                    enabled = !isUnread,
                    onClick = {
                        isMenuExpanded = false
                        onMarkUnread(entry.id)
                    }
                )

                if (onMarkAllRead != null) {
                    DropdownMenuItem(
                        text = { Text("Mark All as Read") },
                        leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onMarkAllRead()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Previous Article") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                    enabled = onPreviousEntry != null,
                    onClick = {
                        isMenuExpanded = false
                        onPreviousEntry?.invoke()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Next Article") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    enabled = onNextEntry != null,
                    onClick = {
                        isMenuExpanded = false
                        onNextEntry?.invoke()
                    }
                )
            }
        }
    }
}
