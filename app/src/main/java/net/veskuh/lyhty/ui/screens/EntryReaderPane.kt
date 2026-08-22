package net.veskuh.lyhty.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
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
    onMarkAllRead: (() -> Unit)? = null,
    onToggleBookmark: ((Long) -> Unit)? = null,
    onNextEntry: (() -> Boolean)? = null,
    onPreviousEntry: (() -> Boolean)? = null,
    onAdvanceToNextFeed: (() -> String?)? = null,
    onSetTheme: ((ReaderTheme) -> Unit)? = null,
    onSetFontSizeScale: ((Float) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

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

        LaunchedEffect(entry.id) {
            if (entry.status == "unread") {
                onMarkRead(entry.id)
            }
        }

        val isFlexMode = postureInfo.posture == DevicePosture.FLEX_TABLETOP
        val windowAdaptiveInfo = currentWindowAdaptiveInfo()
        val isCompact = windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

        var totalDragX by remember { mutableFloatStateOf(0f) }
        var isAtEndSignaled by remember(entry.id) { mutableStateOf(false) }
        var slideDirection by remember { mutableIntStateOf(1) }
        val entryCache = remember { mutableStateMapOf<Long, EntryEntity>() }
        entryCache[entry.id] = entry

        val scrollState = remember(entry.id) { ScrollState(initial = 0) }
        val rawProgress = if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue.toFloat() else 0f
        val animatedProgress by animateFloatAsState(
            targetValue = rawProgress.coerceIn(0f, 1f),
            label = "ReaderReadingProgress"
        )

        val gestureModifier = Modifier.pointerInput(entry.id) {
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onDragEnd = {
                    val thresholdPx = 60.dp.toPx()
                    if (totalDragX < -thresholdPx) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Swiped Left -> Next Article (slide in from Right)
                        slideDirection = 1
                        val moved = onNextEntry?.invoke() ?: false
                        if (!moved) {
                            if (!isAtEndSignaled) {
                                isAtEndSignaled = true
                                Toast.makeText(context, "Last article in current view", Toast.LENGTH_SHORT).show()
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
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Swiped Right -> Previous Article (slide in from Left)
                        slideDirection = -1
                        val moved = onPreviousEntry?.invoke() ?: false
                        if (!moved) {
                            Toast.makeText(context, "First article in current view", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onHorizontalDrag = { _, dragAmount ->
                    totalDragX += dragAmount
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Reading Progress Indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .semantics { contentDescription = "Reading Progress" },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            // Top Reader Quick-Bar (Font size & Theme toggles)
            if (onSetTheme != null || onSetFontSizeScale != null) {
                var isThemeMenuExpanded by remember { mutableStateOf(false) }
                var isFontMenuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSetFontSizeScale != null) {
                        Box {
                            IconButton(onClick = { isFontMenuExpanded = true }) {
                                Icon(Icons.Default.FormatSize, contentDescription = "Font Size")
                            }
                            DropdownMenu(
                                expanded = isFontMenuExpanded,
                                onDismissRequest = { isFontMenuExpanded = false }
                            ) {
                                listOf(0.85f to "Small (85%)", 1.0f to "Default (100%)", 1.15f to "Large (115%)", 1.3f to "Extra Large (130%)").forEach { (scale, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (kotlin.math.abs(fontSizeScale - scale) < 0.05f) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            isFontMenuExpanded = false
                                            onSetFontSizeScale(scale)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (onSetTheme != null) {
                        Box {
                            IconButton(onClick = { isThemeMenuExpanded = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "Reader Theme")
                            }
                            DropdownMenu(
                                expanded = isThemeMenuExpanded,
                                onDismissRequest = { isThemeMenuExpanded = false }
                            ) {
                                listOf(ReaderTheme.OLED_DARK to "OLED Black", ReaderTheme.SEPIA to "Warm Sepia", ReaderTheme.LIGHT to "Light").forEach { (theme, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (readerTheme == theme) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            isThemeMenuExpanded = false
                                            onSetTheme(theme)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isFlexMode) {
                // Tabletop / Flex-Mode Split
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(gestureModifier)
                    ) {
                        AnimatedContent(
                            targetState = entry.id,
                            transitionSpec = {
                                if (slideDirection >= 0) {
                                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                                } else {
                                    slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                                }
                            },
                            label = "ReaderContentFlexTransition"
                        ) { targetId ->
                            val cachedEntry = entryCache[targetId] ?: entry
                            ReaderContent(
                                entry = cachedEntry,
                                fontSizeScale = fontSizeScale,
                                scrollState = scrollState,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        ReaderQuickJumpPill(
                            scrollState = scrollState,
                            hasNextEntry = onNextEntry != null,
                            hasNextFeed = onAdvanceToNextFeed != null,
                            onJumpNext = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                slideDirection = 1
                                val moved = onNextEntry?.invoke() ?: false
                                if (!moved) {
                                    val nextFeedTitle = onAdvanceToNextFeed?.invoke()
                                    if (nextFeedTitle != null) {
                                        Toast.makeText(context, "Switched to $nextFeedTitle", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No more unread articles", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp, end = 16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(postureInfo.hingeBoundsDp.coerceAtLeast(1f).dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .navigationBarsPadding(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
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
                                isCompact = isCompact,
                                onFetchFullText = onFetchFullText,
                                onMarkRead = onMarkRead,
                                onMarkUnread = onMarkUnread,
                                onMarkAllRead = onMarkAllRead,
                                onToggleBookmark = onToggleBookmark,
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
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(gestureModifier)
                    ) {
                        AnimatedContent(
                            targetState = entry.id,
                            transitionSpec = {
                                if (slideDirection >= 0) {
                                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                                } else {
                                    slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                                }
                            },
                            label = "ReaderContentFlatTransition"
                        ) { targetId ->
                            val cachedEntry = entryCache[targetId] ?: entry
                            ReaderContent(entry = cachedEntry, fontSizeScale = fontSizeScale, scrollState = scrollState)
                        }

                        ReaderQuickJumpPill(
                            scrollState = scrollState,
                            hasNextEntry = onNextEntry != null,
                            hasNextFeed = onAdvanceToNextFeed != null,
                            onJumpNext = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                slideDirection = 1
                                val moved = onNextEntry?.invoke() ?: false
                                if (!moved) {
                                    val nextFeedTitle = onAdvanceToNextFeed?.invoke()
                                    if (nextFeedTitle != null) {
                                        Toast.makeText(context, "Switched to $nextFeedTitle", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No more unread articles", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp, end = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Miniflux Action Toolbar
                    MinifluxActionRow(
                        entry = entry,
                        isCompact = isCompact,
                        onFetchFullText = onFetchFullText,
                        onMarkRead = onMarkRead,
                        onMarkUnread = onMarkUnread,
                        onMarkAllRead = onMarkAllRead,
                        onToggleBookmark = onToggleBookmark,
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
    }
}
