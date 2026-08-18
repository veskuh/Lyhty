package net.veskuh.lyhty.ui.screens

import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.ui.components.DevicePosture
import net.veskuh.lyhty.ui.components.PostureInfo
import net.veskuh.lyhty.ui.state.ReaderTheme

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
                            ReaderContent(entry = cachedEntry, fontSizeScale = fontSizeScale)
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
                            ReaderContent(entry = cachedEntry, fontSizeScale = fontSizeScale)
                        }
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

private sealed interface ReaderBlock {
    data class Text(val content: AnnotatedString) : ReaderBlock
    data class Image(val url: String, val alt: String = "") : ReaderBlock
}

private fun Spanned.toAnnotatedString(primaryColor: Color): AnnotatedString {
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
                is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val blocks = remember(entry.id, entry.content, primaryColor) {
        if (entry.content.isBlank()) return@remember emptyList<ReaderBlock>()
        val imgRegex = Regex("(?i)<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>")
        val result = mutableListOf<ReaderBlock>()

        val htmlWithBreaks = entry.content
            .replace(Regex("(?i)<p[^>]*>"), "")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("(?i)<div[^>]*>"), "")
            .replace(Regex("(?i)</div>"), "\n\n")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)<h[1-6][^>]*>"), "\n\n")
            .replace(Regex("(?i)</h[1-6]>"), "\n\n")

        var lastIndex = 0
        imgRegex.findAll(htmlWithBreaks).forEach { matchResult ->
            val textPart = htmlWithBreaks.substring(lastIndex, matchResult.range.first)
            val imgUrl = matchResult.groupValues[1]
            val altMatch = Regex("(?i)alt=[\"']([^\"']+)[\"']").find(matchResult.value)
            val altText = altMatch?.groupValues?.get(1) ?: ""

            if (textPart.isNotBlank()) {
                textPart.split(Regex("\n{2,}")).forEach { paragraphStr ->
                    val spanned = try {
                        Html.fromHtml(paragraphStr.trim(), Html.FROM_HTML_MODE_LEGACY)
                    } catch (_: Throwable) {
                        android.text.SpannableString(paragraphStr.trim())
                    }
                    val annotated = spanned.toAnnotatedString(primaryColor)
                    if (annotated.text.isNotBlank()) {
                        result.add(ReaderBlock.Text(annotated))
                    }
                }
            }

            if (imgUrl.isNotBlank()) {
                result.add(ReaderBlock.Image(url = imgUrl, alt = altText))
            }

            lastIndex = matchResult.range.last + 1
        }

        if (lastIndex < htmlWithBreaks.length) {
            val remainingText = htmlWithBreaks.substring(lastIndex)
            remainingText.split(Regex("\n{2,}")).forEach { paragraphStr ->
                val spanned = try {
                    Html.fromHtml(paragraphStr.trim(), Html.FROM_HTML_MODE_LEGACY)
                } catch (_: Throwable) {
                    android.text.SpannableString(paragraphStr.trim())
                }
                val annotated = spanned.toAnnotatedString(primaryColor)
                if (annotated.text.isNotBlank()) {
                    result.add(ReaderBlock.Text(annotated))
                }
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
            Text(
                text = "Published ${entry.publishedAt} ${if (entry.author.isNotBlank()) "by ${entry.author}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
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
                                val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
                                Text(
                                    text = block.content,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = (16 * fontSizeScale).sp,
                                        lineHeight = (26 * fontSizeScale).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    onTextLayout = { layoutResult.value = it },
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures { pos ->
                                            layoutResult.value?.let { layout ->
                                                val offset = layout.getOffsetForPosition(pos)
                                                block.content.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                                    .firstOrNull()?.let { annotation ->
                                                        try {
                                                            uriHandler.openUri(annotation.item)
                                                        } catch (_: Throwable) {}
                                                    }
                                            }
                                        }
                                    }
                                )
                            }
                            is ReaderBlock.Image -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(block.url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = block.alt.ifBlank { "Article image" },
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
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
