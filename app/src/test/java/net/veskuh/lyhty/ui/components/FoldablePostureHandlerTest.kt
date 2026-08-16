package net.veskuh.lyhty.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    fun `PostureInfo correctly evaluates device posture states`() {
        val normalPosture = PostureInfo(DevicePosture.NORMAL)
        assertFalse(normalPosture.isSeparating)
        assertEquals(0f, normalPosture.hingeBoundsDp)

        val flexPosture = PostureInfo(
            posture = DevicePosture.FLEX_TABLETOP,
            hingeBoundsDp = 48f,
            isSeparating = true
        )

        assertEquals(DevicePosture.FLEX_TABLETOP, flexPosture.posture)
        assertTrue(flexPosture.isSeparating)
        assertEquals(48f, flexPosture.hingeBoundsDp)

        val bookPosture = PostureInfo(
            posture = DevicePosture.BOOK_POSTURE,
            hingeBoundsDp = 32f,
            isSeparating = true
        )

        assertEquals(DevicePosture.BOOK_POSTURE, bookPosture.posture)
        assertTrue(bookPosture.isSeparating)
        assertEquals(32f, bookPosture.hingeBoundsDp)
    }

    @Test
    fun `rememberPostureInfo returns NORMAL posture when foldingFeature is null`() {
        composeTestRule.setContent {
            val postureInfo = rememberPostureInfo(foldingFeature = null)
            assertEquals(DevicePosture.NORMAL, postureInfo.posture)
            assertFalse(postureInfo.isSeparating)
        }
    }
}
