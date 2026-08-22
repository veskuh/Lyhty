package net.veskuh.lyhty.ui.components

import android.graphics.Rect
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.window.layout.FoldingFeature
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoldablePostureHandlerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `rememberPostureInfo returns NORMAL when foldingFeature is null`() {
        var postureInfo: PostureInfo? = null

        composeTestRule.setContent {
            postureInfo = rememberPostureInfo(null)
        }

        assertEquals(DevicePosture.NORMAL, postureInfo?.posture)
        assertEquals(0f, postureInfo?.hingeBoundsDp)
        assertFalse(postureInfo?.isSeparating ?: true)
    }

    @Test
    fun `rememberPostureInfo detects FLEX_TABLETOP posture`() {
        val mockFeature: FoldingFeature = mockk {
            every { state } returns FoldingFeature.State.HALF_OPENED
            every { orientation } returns FoldingFeature.Orientation.HORIZONTAL
            every { bounds } returns Rect(0, 500, 1080, 550)
            every { isSeparating } returns true
        }

        var postureInfo: PostureInfo? = null

        composeTestRule.setContent {
            postureInfo = rememberPostureInfo(mockFeature)
        }

        assertEquals(DevicePosture.FLEX_TABLETOP, postureInfo?.posture)
        assertTrue((postureInfo?.hingeBoundsDp ?: 0f) > 0f)
        assertTrue(postureInfo?.isSeparating ?: false)
    }

    @Test
    fun `rememberPostureInfo detects BOOK_POSTURE`() {
        val mockFeature: FoldingFeature = mockk {
            every { state } returns FoldingFeature.State.HALF_OPENED
            every { orientation } returns FoldingFeature.Orientation.VERTICAL
            every { bounds } returns Rect(500, 0, 550, 2000)
            every { isSeparating } returns true
        }

        var postureInfo: PostureInfo? = null

        composeTestRule.setContent {
            postureInfo = rememberPostureInfo(mockFeature)
        }

        assertEquals(DevicePosture.BOOK_POSTURE, postureInfo?.posture)
        assertTrue(postureInfo?.isSeparating ?: false)
    }

    @Test
    fun `rememberPostureInfo returns NORMAL when foldingFeature is FLAT`() {
        val mockFeature: FoldingFeature = mockk {
            every { state } returns FoldingFeature.State.FLAT
            every { orientation } returns FoldingFeature.Orientation.HORIZONTAL
            every { bounds } returns Rect(0, 500, 1080, 550)
            every { isSeparating } returns false
        }

        var postureInfo: PostureInfo? = null

        composeTestRule.setContent {
            postureInfo = rememberPostureInfo(mockFeature)
        }

        assertEquals(DevicePosture.NORMAL, postureInfo?.posture)
        assertFalse(postureInfo?.isSeparating ?: true)
    }
}
