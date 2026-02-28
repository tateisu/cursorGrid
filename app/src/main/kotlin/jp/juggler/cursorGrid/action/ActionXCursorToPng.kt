package jp.juggler.cursorGrid.action

import jp.juggler.cursorGrid.CliArgs
import jp.juggler.cursorGrid.IAction
import jp.juggler.cursorGrid.encoder.decodeXCursor
import jp.juggler.cursorGrid.encoder.toBufferedImage
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.serialization.json.Json
import java.io.File
import javax.imageio.ImageIO

private val jsonFormat = Json { prettyPrint = true }

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalCli::class)
class ActionXCursorToPng : Subcommand(
    "xcur2png",
    "Decode xcursor file to PNG",
), IAction {
    val inFile by argument(ArgType.String, description = "Input xcursor file")
    val outFile by argument(ArgType.String, description = "Output file prefix (PNG and JSON)")

    override fun execute() = Unit

    override fun CliArgs.runWithCliArgs() {
        if (verbose) println("Xcur2png: inFile=$inFile, outFile=$outFile")
        val inputFile = File(inFile)

        if (!inputFile.exists()) {
            println("File not found: ${inputFile.absolutePath}")
            return
        }

        val images = inputFile.decodeXCursor()
        println("Decoded ${images.size} images from ${inputFile.name}")

        val outputBase = File(outFile)
        outputBase.parentFile?.mkdirs()

        val sizeFrameCount = mutableMapOf<Int, Int>()
        val sizeFrameIndex = mutableMapOf<Int, Int>()

        for (img in images) {
            val size = img.meta.size
            sizeFrameCount[size] = (sizeFrameCount[size] ?: 0) + 1
        }

        for ((index, img) in images.withIndex()) {
            if (verbose) {
                println("Image $index: size=${img.meta.size}, ${img.meta.width}x${img.meta.height}, hot=(${img.meta.xHot},${img.meta.yHot})")
            }

            val size = img.meta.size
            val frameIndex = sizeFrameIndex.getOrDefault(size, 0)
            sizeFrameIndex[size] = frameIndex + 1

            val outputName = if ((sizeFrameCount[size] ?: 1) > 1) {
                "${outputBase.nameWithoutExtension}_${size}_%03d.png".format(frameIndex)
            } else {
                "${outputBase.nameWithoutExtension}_${size}.png"
            }
            val outputFile = File(outputBase.parentFile, outputName)
            val bufferedImage = img.toBufferedImage()
            ImageIO.write(bufferedImage, "PNG", outputFile)
            println("  Saved: ${outputFile.absolutePath}")
        }

        val meta = images.mapIndexed { index, img ->
            val size = img.meta.size
            val frameIndex = images.take(index).count { it.meta.size == size }
            val pngFile = if ((sizeFrameCount[size] ?: 1) > 1) {
                "${outputBase.nameWithoutExtension}_${size}_%03d.png".format(frameIndex)
            } else {
                "${outputBase.nameWithoutExtension}_${size}.png"
            }
            img.meta.copy(pngFile = pngFile)
        }
        val jsonFile = File(outputBase.parentFile, "${outputBase.nameWithoutExtension}.json")
        jsonFile.writeText(jsonFormat.encodeToString(meta))
        println("  Saved: ${jsonFile.absolutePath}")
    }
}
