package jp.juggler.cursorGrid.encoder

import jp.juggler.cursorGrid.data.XCursorImageMeta
import java.awt.image.BufferedImage
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object XCursorConstants {
    const val MAGIC = 0x72756358
    const val FILE_HEADER_SIZE = 16
    const val TOC_HEADER_SIZE = 12
    const val CHUNK_HEADER_SIZE = 36

    const val TYPE_IMAGE = 0xFFFD0002.toInt()
    @Suppress("unused")
    const val TYPE_ANIM = 0xFFFD0003.toInt()
}

class XCursorImage(
    val meta: XCursorImageMeta,
    val pixels: IntArray,
)

fun XCursorImage.toBufferedImage(): BufferedImage {
    val image = BufferedImage(meta.width, meta.height, BufferedImage.TYPE_INT_ARGB)
    image.setRGB(0, 0, meta.width, meta.height, pixels, 0, meta.width)
    return image
}

data class XCursorTocEntry(
    val type: Int,
    val subtype: Int,
    val position: Long
)

private fun readImage(raf: RandomAccessFile, position: Long): XCursorImage {
    raf.seek(position)

    val chunkBuffer = ByteBuffer.allocate(XCursorConstants.CHUNK_HEADER_SIZE)
    chunkBuffer.order(ByteOrder.LITTLE_ENDIAN)
    raf.channel.read(chunkBuffer)
    chunkBuffer.flip()

    /* val chunkSize = */ chunkBuffer.int
    /* val type = */ chunkBuffer.int
    val subtype = chunkBuffer.int
    /* val version = */ chunkBuffer.int
    val width = chunkBuffer.int
    val height = chunkBuffer.int
    val xHot = chunkBuffer.int
    val yHot = chunkBuffer.int
    val delay = chunkBuffer.int

    val pixelCount = width * height
    val pixelBuffer = ByteBuffer.allocate(pixelCount * 4)
    pixelBuffer.order(ByteOrder.LITTLE_ENDIAN)
    raf.channel.read(pixelBuffer)
    pixelBuffer.flip()

    val pixels = IntArray(pixelCount)
    pixelBuffer.asIntBuffer().get(pixels)

    return XCursorImage(
        pixels = pixels,
        meta = XCursorImageMeta(
            size = subtype,
            width = width,
            height = height,
            xHot = xHot,
            yHot = yHot,
            delay = delay,
        )
    )
}

fun File.decodeXCursor(): List<XCursorImage> {
    RandomAccessFile(this, "r").use { raf ->
        val headerBuffer = ByteBuffer.allocate(XCursorConstants.FILE_HEADER_SIZE)
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
        raf.channel.read(headerBuffer)
        headerBuffer.flip()

        val magic = headerBuffer.int
        require(magic == XCursorConstants.MAGIC) {
            "Invalid XCursor magic: 0x${magic.toString(16)}"
        }

        /* val headerSize = */ headerBuffer.int
        /* val version = */ headerBuffer.int
        val nToc = headerBuffer.int

        val tocEntries = mutableListOf<XCursorTocEntry>()
        val tocBuffer = ByteBuffer.allocate(XCursorConstants.TOC_HEADER_SIZE * nToc)
        tocBuffer.order(ByteOrder.LITTLE_ENDIAN)
        raf.channel.read(tocBuffer)
        tocBuffer.flip()

        repeat(nToc) {
            tocEntries.add(
                XCursorTocEntry(
                    type = tocBuffer.int,
                    subtype = tocBuffer.int,
                    position = tocBuffer.int.toLong() and 0xFFFFFFFFL
                )
            )
        }

        val images = mutableListOf<XCursorImage>()
        for (entry in tocEntries) {
            if (entry.type == XCursorConstants.TYPE_IMAGE) {
                val image = readImage(raf, entry.position)
                images.add(image)
            }
        }

        return images
    }
}
