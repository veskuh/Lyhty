package net.veskuh.lyhty.util

import android.graphics.Typeface
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
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
import androidx.compose.ui.unit.sp

sealed interface ReaderBlock {
    data class Text(val content: AnnotatedString) : ReaderBlock
    data class Image(val url: String, val alt: String = "") : ReaderBlock
    data class Quote(val content: AnnotatedString) : ReaderBlock
    data class ListItem(val content: AnnotatedString) : ReaderBlock
}

object HtmlParserUtil {

    private val REGEX_P_OPEN = Regex("(?i)<p[^>]*>")
    private val REGEX_P_CLOSE = Regex("(?i)</p>")
    private val REGEX_DIV_OPEN = Regex("(?i)<div[^>]*>")
    private val REGEX_DIV_CLOSE = Regex("(?i)</div>")
    private val REGEX_CITE_OPEN = Regex("(?i)<cite[^>]*>")
    private val REGEX_CITE_CLOSE = Regex("(?i)</cite>")
    private val REGEX_BR = Regex("(?i)<br\\s*/?>")
    private val REGEX_H_CLOSE = Regex("(?i)</h[1-6]>")
    private val REGEX_UL_CLOSE = Regex("(?i)</ul>")
    private val REGEX_OL_CLOSE = Regex("(?i)</ol>")
    private val REGEX_MULTIPLE_NEWLINES = Regex("\n{2,}")

    private val REGEX_BLOCK_PATTERN = Regex("(?is)(<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>|<blockquote[^>]*>.*?</blockquote>|<li[^>]*>.*?</li>)")
    private val REGEX_IMG_TAG = Regex("(?i)<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>")
    private val REGEX_SRC_ATTR = Regex("(?i)src=[\"']([^\"']+)[\"']")
    private val REGEX_ALT_ATTR = Regex("(?i)alt=[\"']([^\"']+)[\"']")
    private val REGEX_BLOCKQUOTE_TAGS = Regex("(?is)^<blockquote[^>]*>|StrictBlock$|</blockquote>$")
    private val REGEX_LI_TAGS = Regex("(?is)^<li[^>]*>|</li>$")

    fun parseHtmlToBlocks(
        htmlContent: String,
        primaryColor: Color,
        fontSizeScale: Float,
        density: Float
    ): List<ReaderBlock> {
        if (htmlContent.isBlank()) return emptyList()
        val result = mutableListOf<ReaderBlock>()

        var lastIndex = 0
        REGEX_BLOCK_PATTERN.findAll(htmlContent).forEach { match ->
            val textPart = htmlContent.substring(lastIndex, match.range.first)
            parseHtmlParagraphs(textPart, primaryColor, fontSizeScale, density) { annotated ->
                result.add(ReaderBlock.Text(annotated))
            }

            val matchedValue = match.value
            if (matchedValue.startsWith("<img", ignoreCase = true)) {
                extractImageBlock(matchedValue)?.let { result.add(it) }
            } else if (matchedValue.startsWith("<blockquote", ignoreCase = true)) {
                val innerHtml = matchedValue.replace(REGEX_BLOCKQUOTE_TAGS, "")
                parseContainerInnerHtml(innerHtml, primaryColor, fontSizeScale, density, wrapBlock = { ReaderBlock.Quote(it) }) { block ->
                    result.add(block)
                }
            } else if (matchedValue.startsWith("<li", ignoreCase = true)) {
                val innerHtml = matchedValue.replace(REGEX_LI_TAGS, "")
                parseContainerInnerHtml(innerHtml, primaryColor, fontSizeScale, density, wrapBlock = { ReaderBlock.ListItem(it) }) { block ->
                    result.add(block)
                }
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < htmlContent.length) {
            val remainingText = htmlContent.substring(lastIndex)
            parseHtmlParagraphs(remainingText, primaryColor, fontSizeScale, density) { annotated ->
                result.add(ReaderBlock.Text(annotated))
            }
        }

        return result
    }

    fun extractImageBlock(imgTagStr: String): ReaderBlock.Image? {
        val srcMatch = REGEX_SRC_ATTR.find(imgTagStr)
        val altMatch = REGEX_ALT_ATTR.find(imgTagStr)
        val imgUrl = srcMatch?.groupValues?.get(1) ?: ""
        val altText = altMatch?.groupValues?.get(1) ?: ""
        return if (imgUrl.isNotBlank()) ReaderBlock.Image(url = imgUrl, alt = altText) else null
    }

    private fun parseContainerInnerHtml(
        innerHtml: String,
        primaryColor: Color,
        fontSizeScale: Float,
        density: Float,
        wrapBlock: (AnnotatedString) -> ReaderBlock,
        onBlock: (ReaderBlock) -> Unit
    ) {
        var lastIndex = 0
        REGEX_IMG_TAG.findAll(innerHtml).forEach { match ->
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
        val htmlWithBreaks = cleanHtmlBreaks(rawText)
        htmlWithBreaks.split(REGEX_MULTIPLE_NEWLINES).forEach { paragraphStr ->
            val trimmed = paragraphStr.trim()
            if (trimmed.isNotBlank()) {
                val annotated = parseHtmlToAnnotatedString(trimmed, primaryColor, fontSizeScale, density)
                if (annotated.text.isNotBlank()) {
                    onText(annotated)
                }
            }
        }
    }

    private fun cleanHtmlBreaks(rawText: String): String {
        return rawText
            .replace(REGEX_P_OPEN, "")
            .replace(REGEX_P_CLOSE, "\n\n")
            .replace(REGEX_DIV_OPEN, "")
            .replace(REGEX_DIV_CLOSE, "\n\n")
            .replace(REGEX_CITE_OPEN, "\n\n")
            .replace(REGEX_CITE_CLOSE, "")
            .replace(REGEX_BR, "\n")
            .replace(REGEX_H_CLOSE, "\n\n")
            .replace(REGEX_UL_CLOSE, "\n\n")
            .replace(REGEX_OL_CLOSE, "\n\n")
    }

    fun parseHtmlToAnnotatedString(
        rawText: String,
        primaryColor: Color,
        fontSizeScale: Float,
        density: Float
    ): AnnotatedString {
        val htmlWithBreaks = cleanHtmlBreaks(rawText)
        val spanned = try {
            Html.fromHtml(htmlWithBreaks.trim(), Html.FROM_HTML_MODE_LEGACY)
        } catch (_: Throwable) {
            SpannableString(htmlWithBreaks.trim())
        }
        return spanned.toAnnotatedString(primaryColor, fontSizeScale, density)
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
}
