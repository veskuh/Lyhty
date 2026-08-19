package net.veskuh.lyhty.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import net.veskuh.lyhty.data.local.entity.EntryEntity
import net.veskuh.lyhty.ui.components.DevicePosture
import net.veskuh.lyhty.ui.components.PostureInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HtmlReaderParsingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun readerRendersEmptyContentFallbackWhenBlank() {
        val entry = EntryEntity(
            id = 1,
            feedId = 10,
            feedTitle = "Test Feed",
            title = "Empty Content Entry",
            content = "",
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No content preview available.").assertIsDisplayed()
    }

    @Test
    fun readerRendersHeadingsAndBodyParagraphs() {
        val htmlContent = """
            <h1>Main Article Heading</h1>
            <p>This is the first paragraph with <b>bold text</b> and <i>italic text</i>.</p>
            <h2>Secondary Subheading</h2>
            <p>This is a second paragraph with <code>code snippet</code>.</p>
        """.trimIndent()

        val entry = EntryEntity(
            id = 2,
            feedId = 10,
            feedTitle = "Tech Feed",
            title = "Rich HTML Article",
            content = htmlContent,
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Main Article Heading", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Secondary Subheading", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("first paragraph", substring = true).assertIsDisplayed()
    }

    @Test
    fun readerRendersBlockquotesAndLists() {
        val htmlContent = """
            <blockquote>This is an important quoted passage from the original author.</blockquote>
            <ul>
                <li>First bullet item</li>
                <li>Second bullet item</li>
            </ul>
        """.trimIndent()

        val entry = EntryEntity(
            id = 3,
            feedId = 10,
            feedTitle = "News Feed",
            title = "Blockquotes and Lists",
            content = htmlContent,
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("important quoted passage", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("First bullet item", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Second bullet item", substring = true).assertIsDisplayed()
    }

    @Test
    fun readerRendersMultiParagraphBlockquotesWithoutSentinelLeak() {
        val htmlContent = """
            <blockquote><p>First paragraph inside quote</p><p>Second paragraph inside quote</p></blockquote>
        """.trimIndent()

        val entry = EntryEntity(
            id = 99,
            feedId = 10,
            feedTitle = "Multi-paragraph Feed",
            title = "Multi-paragraph Blockquote Article",
            content = htmlContent,
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("First paragraph inside quote", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Second paragraph inside quote", substring = true).assertIsDisplayed()
    }

    @Test
    fun readerRendersInlineImagesAndLinks() {
        val htmlContent = """
            <p>Check out this <a href="https://example.com/article">external reference link</a>.</p>
            <img src="https://example.com/diagram.png" alt="Architecture Diagram" />
            <p>Image description below diagram.</p>
        """.trimIndent()

        val entry = EntryEntity(
            id = 4,
            feedId = 10,
            feedTitle = "Diagram Feed",
            title = "Article with Images and Links",
            content = htmlContent,
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("external reference link", substring = true).assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Architecture Diagram"), useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Image description below diagram.", substring = true).assertExists()
    }

    @Test
    fun readerRendersFormattingSpansUnderlineAndStrikethrough() {
        val htmlContent = """
            <p>Text with <u>underlined text</u> and <s>strikethrough text</s>.</p>
        """.trimIndent()

        val entry = EntryEntity(
            id = 5,
            feedId = 10,
            feedTitle = "Style Feed",
            title = "Underline and Strikethrough Article",
            content = htmlContent,
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("underlined text", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("strikethrough text", substring = true).assertIsDisplayed()
    }

    @Test
    fun readerRendersFullBlockquoteWithCiteForOSNewsArticle() {
        val htmlContent = """
            <p class="wp-block-paragraph">A great and concise history of the run-up to MS-DOS 2.0.</p>

            <blockquote class="wp-block-quote is-layout-flow wp-block-quote-is-layout-flow">
            <p class="wp-block-paragraph">Yet, there was a nagging feeling that a single-tasking CP/M clone was inadequate for the new generation of personal computers. Digital Research released multi-user, multitasking MP/M-86 in September 1981 and announced the single‑user multitasking Concurrent CP/M in early 1982. In response, Microsoft came up with a plan for tiered approach to its operating systems: single-user/single-tasking MS-DOS at the bottom; multi-user/multitasking XENIX at the top; and in the middle, something called XEDOS: a single-user version of XENIX. This “pyramid of upward-compatible operating systems” was announced in a <em>Byte Magazine </em>editorial in January 1982.</p>
            <cite><a href="https://substack.com/@nemanjatrifunovic/p-188254464">↫ Nemanja Trifunovic</a></cite></blockquote>

            <p class="wp-block-paragraph">I&#8217;ve always found this tiered approach fascinating, and the world surely would&#8217;ve looked quite different had Microsoft been able to make it work.</p>
        """.trimIndent()

        val entry = EntryEntity(
            id = 145852,
            feedId = 1,
            feedTitle = "OSNews",
            title = "The road to MS-DOS 2",
            content = htmlContent,
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("A great and concise history", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("nagging feeling that a single-tasking", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Nemanja Trifunovic", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("tiered approach fascinating", substring = true).assertIsDisplayed()
    }

    @Test
    fun readerRendersBulletedListForCoalitionsPost() {
        val htmlContent = """
            <p><a href="https://www.manton.org/2026/08/18/when-i-blogged-a-couple.html">My post earlier about X</a> didn&rsquo;t resonate exactly as I had intended. Let me zoom out.</p>
            <p>Let&rsquo;s think of proponents of the social web as a coalition. People actively choose to be on the fediverse, atmosphere, and (though it&rsquo;s a dated term) blogosphere. There are people who:</p>
            <ul>
            <li>Dislike Elon Musk for political reasons.</li>
            <li>See federation as the answer to developer-hostile platforms.</li>
            <li>Want a more distributed web with personal blogs.</li>
            <li>Hope smaller communities will encourage civility.</li>
            <li>Care deeply about web standards.</li>
            </ul>
            <p>These are all different things. Which of these matters most to you will shape your perspective.</p>
        """.trimIndent()

        val entry = EntryEntity(
            id = 188254,
            feedId = 2,
            feedTitle = "Manton Reece",
            title = "Coalitions",
            content = htmlContent,
            status = "unread"
        )

        composeTestRule.setContent {
            EntryReaderPane(
                entry = entry,
                postureInfo = PostureInfo(DevicePosture.NORMAL),
                fontSizeScale = 1.0f,
                onFetchFullText = {},
                onMarkRead = {},
                onMarkUnread = {},
                onNextEntry = null,
                onPreviousEntry = null
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Dislike Elon Musk for political reasons.", substring = true).assertExists()
        composeTestRule.onNodeWithText("See federation as the answer to developer-hostile platforms.", substring = true).assertExists()
        composeTestRule.onNodeWithText("Want a more distributed web with personal blogs.", substring = true).assertExists()
        composeTestRule.onNodeWithText("Hope smaller communities will encourage civility.", substring = true).assertExists()
        composeTestRule.onNodeWithText("Care deeply about web standards.", substring = true).assertExists()
    }
}
