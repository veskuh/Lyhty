package net.veskuh.lyhty.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.window.layout.FoldingFeature

import androidx.compose.ui.platform.LocalDensity

enum class DevicePosture {
    NORMAL, // Standard flat canvas or single display
    FLEX_TABLETOP, // Half-opened 90° posture (horizontal hinge)
    BOOK_POSTURE // Half-opened vertical hinge
}

data class PostureInfo(
    val posture: DevicePosture,
    val hingeBoundsDp: Float = 0f,
    val isSeparating: Boolean = false
)

@Composable
fun rememberPostureInfo(foldingFeature: FoldingFeature?): PostureInfo {
    val density = LocalDensity.current
    return remember(foldingFeature, density) {
        if (foldingFeature == null) {
            PostureInfo(DevicePosture.NORMAL)
        } else {
            val isTabletop = foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                    foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
            val isBook = foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                    foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL

            val posture = when {
                isTabletop -> DevicePosture.FLEX_TABLETOP
                isBook -> DevicePosture.BOOK_POSTURE
                else -> DevicePosture.NORMAL
            }

            val hingeHeightDp = with(density) { foldingFeature.bounds.height().toDp().value }

            PostureInfo(
                posture = posture,
                hingeBoundsDp = hingeHeightDp,
                isSeparating = foldingFeature.isSeparating
            )
        }
    }
}
