package jp.juggler.cursorGrid.data

import kotlinx.serialization.Serializable

@Serializable
data class XCursorImageMeta(
    val size: Int,
    val width: Int,
    val height: Int,
    val xHot: Int,
    val yHot: Int,
    val delay: Int = 0,
    val pngFile: String? = null,
)