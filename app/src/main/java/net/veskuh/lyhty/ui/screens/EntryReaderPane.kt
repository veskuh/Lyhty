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

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.remember

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun EntryReaderPane(
    entry: EntryEntity?,
    postureInfo: PostureInfo,
    fontSizeScale: Float,
    readerTheme: ReaderTheme = ReaderTheme.OLED_DARK,
    onFetchFullText: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkUnread: (Long) -> Unit,
    onNextEntry: (() -> Boolean)? = null,
    onPreviousEntry: (() -> Boolean)? = null,
    onAdvanceToNextFeed: (() -> String?)? = null,
    onSetTheme: ((ReaderTheme) -> Unit)? = null,
    onSetFontSizeScale: ((Float) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
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
            return@Surface
        }

        val isFlexMode = postureInfo.posture == DevicePosture.FLEX_TABLETOP

            var totalDragX by remember { mutableFloatStateOf(0f) }
            var isAtEndSignaled by remember(entry.id) { mutableStateOf(false) }
            var slideDirection by remember { androidx.compose.runtime.mutableIntStateOf(1) }

            val gestureModifier = Modifier.pointerInput(entry.id) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val thresholdPx = 60.dp.toPx()
                        if (totalDragX < -thresholdPx) {
                            // Swiped Left -> Next Article (slide in from Right)
                            slideDirection = 1
                            val moved = onNextEntry?.invoke() ?: false
                            if (!moved) {
                                if (!isAtEndSignaled) {
                                    isAtEndSignaled = true
                                    Toast.makeText(
                                        context,
                                        "End of current list. Swipe again to jump to next unread feed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val nextFeedTitle = onAdvanceToNextFeed?.invoke()
                                    if (nextFeedTitle != null) {
                                        Toast.makeText(context, "Switched to $nextFeedTitle", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No more unread articles", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else if (totalDragX > thresholdPx) {
                            // Swiped Right -> Previous Article (slide in from Left)
                            slideDirection = -1
                            val moved = onPreviousEntry?.invoke() ?: false
                            if (!moved) {
                                Toast.makeText(context, "First article in current list", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            }

            if (isFlexMode) {
                // Tabletop Flex Mode split (Top: Content, Bottom: Miniflux Desk)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Half Display (Reader Content)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = postureInfo.hingeBoundsDp.dp)
                            .then(gestureModifier)
                    ) {
                        AnimatedContent(
                            targetState = entry,
                            transitionSpec = {
                                if (slideDirection >= 0) {
                                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                                } else {
                                    slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                                }
                            },
                            label = "ReaderContentFlexTransition"
                        ) { currentEntry ->
                            ReaderContent(entry = currentEntry, fontSizeScale = fontSizeScale)
                        }
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
                                onFetchFullText = onFetchFullText,
                                onMarkRead = onMarkRead,
                                onMarkUnread = onMarkUnread,
                                onNextEntry = { slideDirection = 1; onNextEntry?.invoke() },
                                onPreviousEntry = { slideDirection = -1; onPreviousEntry?.invoke() },
                                onBack = onBack,
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
                        onFetchFullText = onFetchFullText,
                        onMarkRead = onMarkRead,
                        onMarkUnread = onMarkUnread,
                        onNextEntry = { slideDirection = 1; onNextEntry?.invoke() },
                        onPreviousEntry = { slideDirection = -1; onPreviousEntry?.invoke() },
                        onBack = onBack,
                        onOpenBrowser = {
                            if (entry.url.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entry.url))
                                context.startActivity(intent)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(gestureModifier)
                    ) {
                        AnimatedContent(
                            targetState = entry,
                            transitionSpec = {
                                if (slideDirection >= 0) {
                                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                                } else {
                                    slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                                }
                            },
                            label = "ReaderContentFlatTransition"
                        ) { currentEntry ->
                            ReaderContent(entry = currentEntry, fontSizeScale = fontSizeScale)
                        }
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
    val plainText = remember(entry.id, entry.content) {
        try {
            Html.fromHtml(entry.content, Html.FROM_HTML_MODE_COMPACT).toString().trim()
        } catch (_: Throwable) {
            entry.content
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
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
}

@Composable
private fun MinifluxActionRow(
    entry: EntryEntity,
    onFetchFullText: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkUnread: (Long) -> Unit,
    onNextEntry: (() -> Unit)?,
    onPreviousEntry: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
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

        // [ ← Back to Articles ]
        if (onBack != null) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back to Articles")
            }
        }

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
    }
}
