package net.veskuh.lyhty.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.util.DateFormatter
import net.veskuh.lyhty.util.HtmlParserUtil
import net.veskuh.lyhty.util.ReaderBlock

@Composable
fun ReaderQuickJumpPill(
    scrollState: ScrollState,
    hasNextEntry: Boolean,
    hasNextFeed: Boolean,
    onJumpNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNearEnd = scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 200
    AnimatedVisibility(
        visible = scrollState.value > 120 && (hasNextEntry || hasNextFeed),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
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

@Composable
fun ReaderContent(
    entry: EntryEntity,
    fontSizeScale: Float,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current.density
    val blocks = remember(entry.id, entry.content, primaryColor, fontSizeScale, density) {
        HtmlParserUtil.parseHtmlToBlocks(entry.content, primaryColor, fontSizeScale, density)
    }

    Column(
        modifier = modifier
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
fun ReaderTextBlock(
    content: AnnotatedString,
    fontSizeScale: Float,
    onOpenUrl: (String) -> Unit,
    fontStyle: FontStyle = FontStyle.Normal,
    modifier: Modifier = Modifier
) {
    val layoutResult = remember { androidx.compose.runtime.mutableStateOf<TextLayoutResult?>(null) }
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
