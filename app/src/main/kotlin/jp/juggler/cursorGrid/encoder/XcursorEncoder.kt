package jp.juggler.cursorGrid.encoder

import jp.juggler.cursorGrid.data.XCursorImageMeta
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
            val chunkSize = XCursorConstants.CHUNK_HEADER_SIZE + img.meta.width * img.meta.height * 4

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

fun List<XCursorImageMeta>.encodeXCursor(baseDir: File, outputFile: File) {
    map { imgMeta ->
        val pngFile = File(baseDir, imgMeta.pngFile!!)
        val bufferedImage = ImageIO.read(pngFile)
            ?: error("Failed to read PNG: ${pngFile.absolutePath}")

        val width = bufferedImage.width
        val height = bufferedImage.height
        val pixels = IntArray(width * height)
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width)

        XCursorImage(
            pixels = pixels,
            meta = XCursorImageMeta(
                size = imgMeta.size,
                width = width,
                height = height,
                xHot = imgMeta.xHot,
                yHot = imgMeta.yHot,
                delay = imgMeta.delay,
            )
        )
    }.writeFile(outputFile)
}
