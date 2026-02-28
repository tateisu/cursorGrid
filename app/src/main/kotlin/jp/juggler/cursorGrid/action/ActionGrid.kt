package jp.juggler.cursorGrid.action

import jp.juggler.cursorGrid.CliArgs
import jp.juggler.cursorGrid.IAction
import jp.juggler.cursorGrid.encoder.XCursorImage
import jp.juggler.cursorGrid.encoder.decodeXCursor
import jp.juggler.cursorGrid.encoder.toBufferedImage
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private fun parseColor(value: String): Color? {
    val hex = if (value.startsWith("#")) value.substring(1) else value
    return when {
        value.equals("transparent", ignoreCase = true) -> Color(0, 0, 0, 0)
        hex.length == 6 -> {
            val r = hex.substring(0, 2).toIntOrNull(16) ?: return null
            val g = hex.substring(2, 4).toIntOrNull(16) ?: return null
            val b = hex.substring(4, 6).toIntOrNull(16) ?: return null
            Color(r, g, b)
        }
        hex.length == 8 -> {
            val a = hex.substring(0, 2).toIntOrNull(16) ?: return null
            val r = hex.substring(2, 4).toIntOrNull(16) ?: return null
            val g = hex.substring(4, 6).toIntOrNull(16) ?: return null
            val b = hex.substring(6, 8).toIntOrNull(16) ?: return null
            Color(r, g, b, a)
        }
        else -> null
    }
}

private fun String.ellipsize(maxLen: Int): String =
    when {
        length <= maxLen -> this
        else -> "${this.take(maxLen)}…"
    }

@OptIn(ExperimentalCli::class)
class ActionGrid : Subcommand("grid", "Output cursor theme as grid image"), IAction {
    val inFolder by argument(ArgType.String, description = "Input cursor theme folder or zip file")
    val outFile by argument(ArgType.String, description = "Output PNG file")
    val bgColor by option(
        ArgType.String,
        shortName = "b",
        description = "Background color (#RRGGBB, #AARRGGBB, or transparent). Default: #757575"
    ).default("#757575")
    val fontFile by option(
        ArgType.String,
        shortName = "F",
        description = "Font file (TTF/OTF). Default: sans-serif"
    ).default("")

    override fun execute() = Unit

    override fun CliArgs.runWithCliArgs() {
        if (verbose) println("Grid: inFolder=$inFolder, outFile=$outFile, bgColor=$bgColor, fontFile=$fontFile")

        val backgroundColor = parseColor(bgColor)
            ?: error("Invalid background color: $bgColor. Use #RRGGBB, #AARRGGBB, or transparent")

        val inputFile = File(inFolder)
        if (!inputFile.exists()) {
            println("File not found: ${inputFile.absolutePath}")
            return
        }

        val workDir = when {
            inputFile.isDirectory -> inputFile

            inputFile.extension.lowercase() == "zip" -> File(
                System.getProperty("java.io.tmpdir"),
                "cursorGrid_${System.currentTimeMillis()}"
            ).also { tempDir ->
                tempDir.mkdirs()
                ZipFile(inputFile).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        val entryFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                entryFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }

            else -> error("$inputFile is not nor directory, zip file")
        }

        val cursorFiles = workDir.walk()
            .filter { it.isFile }
            .filter { !it.name.contains(".") || it.name == it.nameWithoutExtension }
            .sortedBy { it.name.lowercase() }
            .toList()

        if (cursorFiles.isEmpty()) {
            println("No cursor files found")
            return
        }

        if (verbose) println("Found ${cursorFiles.size} cursor files")

        val allImages = mutableListOf<Pair<String, List<XCursorImage>>>()

        for (cursorFile in cursorFiles) {
            try {
                val images = cursorFile.decodeXCursor()
                if (images.isNotEmpty()) {
                    allImages.add(cursorFile.name to images)
                    if (verbose) println("  ${cursorFile.name}: ${images.size} sizes")
                }
            } catch (_: Exception) {
                if (verbose) println("  ${cursorFile.name}: skipped (not XCursor)")
            }
        }

        if (allImages.isEmpty()) {
            println("No valid cursor files found")
            return
        }

        println("Loaded ${allImages.size} cursor files")

        val maxSize = allImages.flatMap { it.second }.maxOfOrNull { it.meta.size } ?: 24
        println("Target size: ${maxSize}px")

        val targetImages = allImages.mapNotNull { (name, images) ->
            val img = images.minByOrNull { abs(it.meta.size - maxSize) }
            if (img != null) name to img else null
        }

        if (targetImages.isEmpty()) {
            println("No images to process")
            return
        }

        val cols = max(1, sqrt(targetImages.size.toDouble()).toInt())
        val rows = (targetImages.size + cols - 1) / cols

        val maxWidth = targetImages.maxOf { it.second.meta.width }
        val maxHeight = targetImages.maxOf { it.second.meta.height }

        // セルとセルの間の間隔(Horizontal,Vertical)
        val cellSpacingH = maxWidth / 8
        val cellSpacingV = maxHeight / 8
        // 出力画像周辺の余白(縦横共通)
        val outerMargin = min(maxWidth, maxHeight) / 2

        val fontSize = max(12, maxWidth / 8)
        val baseFont = if (fontFile.isNotEmpty()) {
            val fontPath = File(fontFile)
            if (!fontPath.exists()) error("Font file not found: ${fontPath.absolutePath}")
            Font.createFont(Font.TRUETYPE_FONT, fontPath)
        } else {
            Font(Font.SANS_SERIF, Font.PLAIN, 1)
        }
        val font = baseFont.deriveFont(fontSize.toFloat())
        val textHeight = fontSize + 4
        val cellWidth = maxWidth + cellSpacingH
        val cellHeight = maxHeight + textHeight + cellSpacingV

        val gridImage = BufferedImage(
            outerMargin * 2 + cols * cellWidth - cellSpacingH,
            outerMargin * 2 + rows * cellHeight - cellSpacingV,
            BufferedImage.TYPE_INT_ARGB
        )
        val g2d = gridImage.createGraphics()
        g2d.color = backgroundColor
        g2d.fillRect(0, 0, gridImage.width, gridImage.height)

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.font = font
        g2d.color = Color.BLACK

        val sizeStats = mutableMapOf<String, Int>()

        targetImages.forEachIndexed { index, (name, img) ->
            val col = index % cols
            val row = index / cols
            val cellX = outerMargin + col * cellWidth
            val cellY = outerMargin + row * cellHeight

            val imgX = cellX + (maxWidth - img.meta.width) / 2

            val bufferedImage = img.toBufferedImage()
            g2d.drawImage(bufferedImage, imgX, cellY, null)

            val label = name.ellipsize( 32)
            val textWidth = g2d.fontMetrics.stringWidth(label)
            val textX = cellX + (maxWidth - textWidth) / 2
            val textY = cellY + maxHeight + fontSize
            g2d.drawString(label, textX, textY)

            val sizeKey = "${img.meta.width}x${img.meta.height}px"
            sizeStats[sizeKey] = (sizeStats[sizeKey] ?: 0) + 1
        }

        g2d.dispose()

        sizeStats.entries
            .sortedByDescending { it.value }
            .forEach { (size, count) ->
                println("- $count : $size")
            }

        val outputFile = File(outFile)
        outputFile.parentFile?.mkdirs()
        ImageIO.write(gridImage, "PNG", outputFile)
        println("Saved: ${outputFile.absolutePath}")
    }
}
