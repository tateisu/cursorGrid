package jp.juggler.cursorGrid.encoder

import jp.juggler.cursorGrid.data.XCursorImageMeta
import java.awt.AlphaComposite
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

private fun List<XCursorImage>.writeFile(outputFile: File) {
    RandomAccessFile(outputFile, "rw").use { raf ->
        raf.setLength(0)
        val nToc = size
        val headerSize = XCursorConstants.FILE_HEADER_SIZE
        val tocSize = XCursorConstants.TOC_HEADER_SIZE * nToc
        val dataOffset = headerSize + tocSize

        val headerBuffer = ByteBuffer.allocate(headerSize)
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
        headerBuffer.putInt(XCursorConstants.MAGIC)
        headerBuffer.putInt(headerSize)
        headerBuffer.putInt(1)
        headerBuffer.putInt(nToc)
        raf.channel.write(headerBuffer.flip())

        var currentPosition = dataOffset.toLong()
        val dataBuffers = mutableListOf<ByteBuffer>()

        for (img in this) {
            val chunkSize = XCursorConstants.CHUNK_HEADER_SIZE +
                img.meta.width * img.meta.height * 4
            val chunkBuffer = ByteBuffer.allocate(chunkSize)
            chunkBuffer.order(ByteOrder.LITTLE_ENDIAN)
            chunkBuffer.putInt(chunkSize)
            chunkBuffer.putInt(XCursorConstants.TYPE_IMAGE)
            chunkBuffer.putInt(img.meta.size)
            chunkBuffer.putInt(1)
            chunkBuffer.putInt(img.meta.width)
            chunkBuffer.putInt(img.meta.height)
            chunkBuffer.putInt(img.meta.xHot)
            chunkBuffer.putInt(img.meta.yHot)
            chunkBuffer.putInt(img.meta.delay)
            chunkBuffer.asIntBuffer().put(img.pixels)
            chunkBuffer.position(chunkSize)

            val tocBuffer = ByteBuffer.allocate(XCursorConstants.TOC_HEADER_SIZE)
            tocBuffer.order(ByteOrder.LITTLE_ENDIAN)
            tocBuffer.putInt(XCursorConstants.TYPE_IMAGE)
            tocBuffer.putInt(img.meta.size)
            tocBuffer.putInt(currentPosition.toInt())
            raf.channel.write(tocBuffer.flip())

            dataBuffers.add(chunkBuffer)
            currentPosition += chunkSize
        }

        for (buffer in dataBuffers) {
            raf.channel.write(buffer.flip())
        }
    }
}

fun File.readPngAndResize(
    targetWidth: Int,
    targetHeight: Int,
): BufferedImage {
    val image = ImageIO.read(this)
        ?: error("Failed to read PNG: $canonicalPath")
    return when {
        image.width == targetWidth &&
            image.height == targetHeight -> image

        else -> BufferedImage(
            targetWidth,
            targetHeight,
            BufferedImage.TYPE_INT_ARGB
        ).also { resized ->
            val g2d = resized.createGraphics()
            try {
                g2d.composite = AlphaComposite.Src
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null)
            } finally {
                g2d.dispose()
            }
        }
    }
}

fun List<XCursorImageMeta>.encodeXCursor(baseDir: File, outputFile: File) {
    map {
        XCursorImage(
            meta = it,
            pixels = IntArray(it.width * it.height).also { pixels ->
                File(baseDir, it.pngFile!!).readPngAndResize(
                    targetWidth = it.width,
                    it.height,
                ).getRGB(
                    0,
                    0,
                    it.width,
                    it.height,
                    pixels,
                    0,
                    it.width,
                )
            },
        )
    }.writeFile(outputFile)
}
