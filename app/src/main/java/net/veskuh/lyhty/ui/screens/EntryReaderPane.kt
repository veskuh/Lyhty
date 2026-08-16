package net.veskuh.lyhty.ui.screens

import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.ui.components.DevicePosture
import net.veskuh.lyhty.ui.components.PostureInfo
import net.veskuh.lyhty.ui.state.ReaderTheme

@Composable
fun EntryReaderPane(
    entry: EntryEntity?,
    postureInfo: PostureInfo,
    fontSizeScale: Float,
    readerTheme: ReaderTheme = ReaderTheme.OLED_DARK,
    onFetchFullText: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkUnread: (Long) -> Unit,
    onNextEntry: (() -> Unit)? = null,
    onPreviousEntry: (() -> Unit)? = null,
    onSetTheme: ((ReaderTheme) -> Unit)? = null,
    onSetFontSizeScale: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (entry == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select an article to read",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            val isFlexMode = postureInfo.posture == DevicePosture.FLEX_TABLETOP

            if (isFlexMode) {
                // Tabletop Flex Mode split (Top: Content, Bottom: Miniflux Desk)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Half Display (Reader Content)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = postureInfo.hingeBoundsDp.dp)
                    ) {
                        ReaderContent(entry = entry, fontSizeScale = fontSizeScale)
                    }

                    // Bottom Half Display (Miniflux Action Desk)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Miniflux Flex Control Desk",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MinifluxActionRow(
                                entry = entry,
                                fontSizeScale = fontSizeScale,
                                readerTheme = readerTheme,
                                onFetchFullText = onFetchFullText,
                                onMarkRead = onMarkRead,
                                onMarkUnread = onMarkUnread,
                                onNextEntry = onNextEntry,
                                onPreviousEntry = onPreviousEntry,
                                onSetTheme = onSetTheme,
                                onSetFontSizeScale = onSetFontSizeScale,
                                onOpenBrowser = {
                                    if (entry.url.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entry.url))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Flat / Standard Reader Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Sticky Miniflux Action Row with Icon + Label Buttons
                    MinifluxActionRow(
                        entry = entry,
                        fontSizeScale = fontSizeScale,
                        readerTheme = readerTheme,
                        onFetchFullText = onFetchFullText,
                        onMarkRead = onMarkRead,
                        onMarkUnread = onMarkUnread,
                        onNextEntry = onNextEntry,
                        onPreviousEntry = onPreviousEntry,
                        onSetTheme = onSetTheme,
                        onSetFontSizeScale = onSetFontSizeScale,
                        onOpenBrowser = {
                            if (entry.url.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entry.url))
                                context.startActivity(intent)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ReaderContent(entry = entry, fontSizeScale = fontSizeScale)
                }
            }
        }
    }
}

@Composable
private fun ReaderContent(
    entry: EntryEntity,
    fontSizeScale: Float
) {
    val scrollState = rememberScrollState()
    val plainText = try {
        Html.fromHtml(entry.content, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    } catch (_: Throwable) {
        entry.content
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = entry.feedTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (24 * fontSizeScale).sp
            ),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Published ${entry.publishedAt} ${if (entry.author.isNotBlank()) "by ${entry.author}" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = plainText.ifBlank { "No content preview available." },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (16 * fontSizeScale).sp,
                lineHeight = (24 * fontSizeScale).sp
            )
        )
    }
}

@Composable
private fun MinifluxActionRow(
    entry: EntryEntity,
    fontSizeScale: Float,
    readerTheme: ReaderTheme,
    onFetchFullText: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkUnread: (Long) -> Unit,
    onNextEntry: (() -> Unit)?,
    onPreviousEntry: (() -> Unit)?,
    onSetTheme: ((ReaderTheme) -> Unit)?,
    onSetFontSizeScale: ((Float) -> Unit)?,
    onOpenBrowser: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Enforcing Strict UX Rule: Icon + Label for all action buttons

        // [ 🌐 Fetch Full Text ]
        Button(
            onClick = { onFetchFullText(entry.id) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Language, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Fetch Full Text")
        }

        // [ 👁️ Mark Read ] / [ 👁️ Mark Unread ]
        if (entry.status == "unread") {
            OutlinedButton(onClick = { onMarkRead(entry.id) }) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark Read")
            }
        } else {
            OutlinedButton(onClick = { onMarkUnread(entry.id) }) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark Unread")
            }
        }

        // [ 🔗 Open Browser ]
        OutlinedButton(onClick = onOpenBrowser) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Open Browser")
        }

        if (onPreviousEntry != null) {
            OutlinedButton(onClick = onPreviousEntry) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prev")
            }
        }

        if (onNextEntry != null) {
            OutlinedButton(onClick = onNextEntry) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Next")
            }
        }

        // Reader Theme controls: OLED / Sepia / Light
        if (onSetTheme != null) {
            OutlinedButton(onClick = {
                val nextTheme = when (readerTheme) {
                    ReaderTheme.OLED_DARK -> ReaderTheme.SEPIA
                    ReaderTheme.SEPIA -> ReaderTheme.LIGHT
                    ReaderTheme.LIGHT -> ReaderTheme.OLED_DARK
                }
                onSetTheme(nextTheme)
            }) {
                Icon(Icons.Default.Palette, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    when (readerTheme) {
                        ReaderTheme.OLED_DARK -> "Theme: OLED"
                        ReaderTheme.SEPIA -> "Theme: Sepia"
                        ReaderTheme.LIGHT -> "Theme: Light"
                    }
                )
            }
        }

        // Font scaling controls
        if (onSetFontSizeScale != null) {
            OutlinedButton(onClick = {
                val nextScale = if (fontSizeScale >= 1.5f) 1.0f else fontSizeScale + 0.25f
                onSetFontSizeScale(nextScale)
            }) {
                Icon(Icons.Default.FormatSize, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Font: ${(fontSizeScale * 100).toInt()}%")
            }
        }
    }
}
