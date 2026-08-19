package net.veskuh.lyhty.ui.screens

import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.ui.components.DevicePosture
import net.veskuh.lyhty.ui.components.PostureInfo
import net.veskuh.lyhty.ui.state.ReaderTheme
import net.veskuh.lyhty.util.DateFormatter

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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
        var slideDirection by remember { androidx.compose.runtime.mutableIntStateOf(1) }
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
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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

        Column(modifier = Modifier.fillMaxSize()) {
            // Slender Reading Progress Indicator (2.dp)
            if (rawProgress > 0f) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            if (isFlexMode) {
                // Tabletop Flex Mode split (Top: Content, Bottom: Miniflux Desk)
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Top Half Display (Reader Content)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = postureInfo.hingeBoundsDp.dp)
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
                            }
                        )
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
                                isCompact = isCompact,
                                onFetchFullText = onFetchFullText,
                                onMarkRead = onMarkRead,
                                onMarkUnread = onMarkUnread,
                                onMarkAllRead = onMarkAllRead,
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
                            }
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

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ReaderQuickJumpPill(
    scrollState: ScrollState,
    hasNextEntry: Boolean,
    hasNextFeed: Boolean,
    onJumpNext: () -> Unit
) {
    val isNearEnd = scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 200
    androidx.compose.animation.AnimatedVisibility(
        visible = scrollState.value > 120 && (hasNextEntry || hasNextFeed),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 16.dp, end = 16.dp)
    ) {
        Surface(
            onClick = onJumpNext,
            shape = RoundedCornerShape(24.dp),
            color = if (isNearEnd) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isNearEnd) Icons.Default.DoneAll else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isNearEnd) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isNearEnd) "Next Feed ➔" else "Next Article",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isNearEnd) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private sealed interface ReaderBlock {
    data class Text(val content: AnnotatedString) : ReaderBlock
    data class Image(val url: String, val alt: String = "") : ReaderBlock
    data class Quote(val content: AnnotatedString) : ReaderBlock
    data class ListItem(val content: AnnotatedString) : ReaderBlock
}

private fun Spanned.toAnnotatedString(primaryColor: Color, fontSizeScale: Float, density: Float): AnnotatedString {
    return buildAnnotatedString {
        append(this@toAnnotatedString.toString())
        val spans = getSpans(0, length, Any::class.java)
        for (span in spans) {
            val start = getSpanStart(span)
            val end = getSpanEnd(span)
            when (span) {
                is StyleSpan -> {
                    when (span.style) {
                        Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                    }
                }
                is RelativeSizeSpan -> {
                    val targetSize = (16 * span.sizeChange * fontSizeScale).sp
                    addStyle(SpanStyle(fontSize = targetSize), start, end)
                }
                is AbsoluteSizeSpan -> {
                    val sizeSp = if (span.dip) (span.size * fontSizeScale).sp else ((span.size / density) * fontSizeScale).sp
                    addStyle(SpanStyle(fontSize = sizeSp), start, end)
                }
                is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                is URLSpan -> {
                    val urlStr = span.url ?: ""
                    if (urlStr.isNotBlank()) {
                        addStringAnnotation(tag = "URL", annotation = urlStr, start = start, end = end)
                        addStyle(
                            SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            ),
                            start,
                            end
                        )
                    }
                }
                is TypefaceSpan -> {
                    if (span.family == "monospace") {
                        addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
                    }
                }
                is ForegroundColorSpan -> {
                    // Ignore raw HTML colors so theme contrast stays crisp on OLED black
                }
            }
        }
    }
}

private fun extractImageBlock(imgTagStr: String): ReaderBlock.Image? {
    val srcMatch = Regex("(?i)src=[\"']([^\"']+)[\"']").find(imgTagStr)
    val altMatch = Regex("(?i)alt=[\"']([^\"']+)[\"']").find(imgTagStr)
    val imgUrl = srcMatch?.groupValues?.get(1) ?: ""
    val altText = altMatch?.groupValues?.get(1) ?: ""
    return if (imgUrl.isNotBlank()) ReaderBlock.Image(url = imgUrl, alt = altText) else null
}

private fun parseHtmlToAnnotatedString(
    rawText: String,
    primaryColor: Color,
    fontSizeScale: Float,
    density: Float
): AnnotatedString {
    val htmlWithBreaks = rawText
        .replace(Regex("(?i)<p[^>]*>"), "")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)<div[^>]*>"), "")
        .replace(Regex("(?i)</div>"), "\n\n")
        .replace(Regex("(?i)<cite[^>]*>"), "\n\n")
        .replace(Regex("(?i)</cite>"), "")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</h[1-6]>"), "\n\n")
        .replace(Regex("(?i)</ul>"), "\n\n")
        .replace(Regex("(?i)</ol>"), "\n\n")

    val spanned = try {
        Html.fromHtml(htmlWithBreaks.trim(), Html.FROM_HTML_MODE_LEGACY)
    } catch (_: Throwable) {
        android.text.SpannableString(htmlWithBreaks.trim())
    }
    return spanned.toAnnotatedString(primaryColor, fontSizeScale, density)
}

private fun parseContainerInnerHtml(
    innerHtml: String,
    primaryColor: Color,
    fontSizeScale: Float,
    density: Float,
    wrapBlock: (AnnotatedString) -> ReaderBlock,
    onBlock: (ReaderBlock) -> Unit
) {
    val imgPattern = Regex("(?i)<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>")
    var lastIndex = 0

    imgPattern.findAll(innerHtml).forEach { match ->
        val textPart = innerHtml.substring(lastIndex, match.range.first).trim()
        if (textPart.isNotBlank()) {
            val annotated = parseHtmlToAnnotatedString(textPart, primaryColor, fontSizeScale, density)
            if (annotated.text.isNotBlank()) {
                onBlock(wrapBlock(annotated))
            }
        }

        extractImageBlock(match.value)?.let { onBlock(it) }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < innerHtml.length) {
        val remainingText = innerHtml.substring(lastIndex).trim()
        if (remainingText.isNotBlank()) {
            val annotated = parseHtmlToAnnotatedString(remainingText, primaryColor, fontSizeScale, density)
            if (annotated.text.isNotBlank()) {
                onBlock(wrapBlock(annotated))
            }
        }
    }
}

private fun parseHtmlParagraphs(
    rawText: String,
    primaryColor: Color,
    fontSizeScale: Float,
    density: Float,
    onText: (AnnotatedString) -> Unit
) {
    val htmlWithBreaks = rawText
        .replace(Regex("(?i)<p[^>]*>"), "")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)<div[^>]*>"), "")
        .replace(Regex("(?i)</div>"), "\n\n")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</h[1-6]>"), "\n\n")
        .replace(Regex("(?i)</ul>"), "\n\n")
        .replace(Regex("(?i)</ol>"), "\n\n")

    htmlWithBreaks.split(Regex("\n{2,}")).forEach { paragraphStr ->
        val trimmed = paragraphStr.trim()
        if (trimmed.isNotBlank()) {
            val annotated = parseHtmlToAnnotatedString(trimmed, primaryColor, fontSizeScale, density)
            if (annotated.text.isNotBlank()) {
                onText(annotated)
            }
        }
    }
}

@Composable
private fun ReaderContent(
    entry: EntryEntity,
    fontSizeScale: Float,
    scrollState: ScrollState = rememberScrollState()
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current.density
    val blocks = remember(entry.id, entry.content, primaryColor, fontSizeScale, density) {
        if (entry.content.isBlank()) return@remember emptyList<ReaderBlock>()
        val result = mutableListOf<ReaderBlock>()

        val blockPattern = Regex("(?is)(<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>|<blockquote[^>]*>.*?</blockquote>|<li[^>]*>.*?</li>)")

        var lastIndex = 0
        blockPattern.findAll(entry.content).forEach { match ->
            val textPart = entry.content.substring(lastIndex, match.range.first)
            parseHtmlParagraphs(textPart, primaryColor, fontSizeScale, density) { annotated ->
                result.add(ReaderBlock.Text(annotated))
            }

            val matchedValue = match.value
            if (matchedValue.startsWith("<img", ignoreCase = true)) {
                extractImageBlock(matchedValue)?.let { result.add(it) }
            } else if (matchedValue.startsWith("<blockquote", ignoreCase = true)) {
                val innerHtml = matchedValue.replace(Regex("(?is)^<blockquote[^>]*>|</blockquote>$"), "")
                parseContainerInnerHtml(innerHtml, primaryColor, fontSizeScale, density, wrapBlock = { ReaderBlock.Quote(it) }) { block ->
                    result.add(block)
                }
            } else if (matchedValue.startsWith("<li", ignoreCase = true)) {
                val innerHtml = matchedValue.replace(Regex("(?is)^<li[^>]*>|</li>$"), "")
                parseContainerInnerHtml(innerHtml, primaryColor, fontSizeScale, density, wrapBlock = { ReaderBlock.ListItem(it) }) { block ->
                    result.add(block)
                }
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < entry.content.length) {
            val remainingText = entry.content.substring(lastIndex)
            parseHtmlParagraphs(remainingText, primaryColor, fontSizeScale, density) { annotated ->
                result.add(ReaderBlock.Text(annotated))
            }
        }

        result
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
            val formattedDate = remember(entry.publishedAt) {
                DateFormatter.formatRelativeTime(entry.publishedAt)
            }
            val subtitleText = remember(formattedDate, entry.author) {
                buildString {
                    if (formattedDate.isNotBlank()) {
                        append(formattedDate)
                    }
                    if (entry.author.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("by ${entry.author}")
                    }
                }
            }
            if (subtitleText.isNotBlank()) {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (blocks.isEmpty()) {
                Text(
                    text = "No content preview available.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = (16 * fontSizeScale).sp,
                        lineHeight = (26 * fontSizeScale).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    blocks.forEach { block ->
                        when (block) {
                            is ReaderBlock.Text -> {
                                ReaderTextBlock(
                                    content = block.content,
                                    fontSizeScale = fontSizeScale,
                                    onOpenUrl = { url ->
                                        try {
                                            uriHandler.openUri(url)
                                        } catch (_: Throwable) {}
                                    }
                                )
                            }
                            is ReaderBlock.Quote -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    ReaderTextBlock(
                                        content = block.content,
                                        fontSizeScale = fontSizeScale,
                                        onOpenUrl = { url ->
                                            try {
                                                uriHandler.openUri(url)
                                            } catch (_: Throwable) {}
                                        },
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            is ReaderBlock.ListItem -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = (16 * fontSizeScale).sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ReaderTextBlock(
                                        content = block.content,
                                        fontSizeScale = fontSizeScale,
                                        onOpenUrl = { url ->
                                            try {
                                                uriHandler.openUri(url)
                                            } catch (_: Throwable) {}
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            is ReaderBlock.Image -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(block.url)
                                        .decoderFactory(SvgDecoder.Factory())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = block.alt.ifBlank { "Article image" },
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 480.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderTextBlock(
    content: AnnotatedString,
    fontSizeScale: Float,
    onOpenUrl: (String) -> Unit,
    fontStyle: FontStyle = FontStyle.Normal,
    modifier: Modifier = Modifier
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = (16 * fontSizeScale).sp,
            lineHeight = (26 * fontSizeScale).sp,
            fontStyle = fontStyle
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        onTextLayout = { layoutResult.value = it },
        modifier = modifier.pointerInput(content) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layout ->
                    val offset = layout.getOffsetForPosition(pos)
                    content.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            onOpenUrl(annotation.item)
                        }
                }
            }
        }
    )
}

@Composable
private fun MinifluxActionRow(
    entry: EntryEntity,
    isCompact: Boolean = false,
    onFetchFullText: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkUnread: (Long) -> Unit,
    onMarkAllRead: (() -> Unit)? = null,
    onNextEntry: (() -> Unit)?,
    onPreviousEntry: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
    onOpenBrowser: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
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
