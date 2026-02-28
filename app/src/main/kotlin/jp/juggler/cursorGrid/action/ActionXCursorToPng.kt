package jp.juggler.cursorGrid.action

import jp.juggler.cursorGrid.CliArgs
import jp.juggler.cursorGrid.IAction
import jp.juggler.cursorGrid.encoder.decodeXCursor
import jp.juggler.cursorGrid.encoder.toBufferedImage
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
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
    val outDir by argument(ArgType.String, description = "Output directory")
    val force by option(ArgType.Boolean, shortName = "f", description = "Overwrite existing files").default(false)

    override fun execute() = Unit

    override fun CliArgs.runWithCliArgs() {
        if (verbose) println("Xcur2png: inFile=$inFile, outDir=$outDir, force=$force")
        val inputFile = File(inFile)

        if (!inputFile.exists()) error("File not found: ${inputFile.absolutePath}")

        val outputDir = File(outDir)
        if (outputDir.exists()) {
            if (!outputDir.isDirectory) error("Output path exists but is not a directory: ${outputDir.absolutePath}")
        } else {
            if (!outputDir.mkdirs()) error("Failed to create directory: ${outputDir.absolutePath}")
        }

        val images = inputFile.decodeXCursor()
        println("Decoded ${images.size} images from ${inputFile.name}")

        val baseName = inputFile.nameWithoutExtension

        val sizeFrameCount = mutableMapOf<Int, Int>()
        val sizeFrameIndex = mutableMapOf<Int, Int>()

        for (img in images) {
            val size = img.meta.size
            sizeFrameCount[size] = (sizeFrameCount[size] ?: 0) + 1
        }

        val outputFiles = mutableListOf<File>()

        for ((index, img) in images.withIndex()) {
            if (verbose) {
                println("Image $index: size=${img.meta.size}, ${img.meta.width}x${img.meta.height}, hot=(${img.meta.xHot},${img.meta.yHot})")
            }

            val size = img.meta.size
            val frameIndex = sizeFrameIndex.getOrDefault(size, 0)
            sizeFrameIndex[size] = frameIndex + 1

            val outputName = if ((sizeFrameCount[size] ?: 1) > 1) {
                "${baseName}_${size}_%03d.png".format(frameIndex)
            } else {
                "${baseName}_${size}.png"
            }
            val outputFile = File(outputDir, outputName)
            if (!force && outputFile.exists()) error("File already exists: ${outputFile.absolutePath}")
            val bufferedImage = img.toBufferedImage()
            ImageIO.write(bufferedImage, "PNG", outputFile)
            outputFiles.add(outputFile)
            println("  Saved: ${outputFile.absolutePath}")
        }

        val meta = images.mapIndexed { index, img ->
            val size = img.meta.size
            val frameIndex = images.take(index).count { it.meta.size == size }
            val pngFile = if ((sizeFrameCount[size] ?: 1) > 1) {
                "${baseName}_${size}_%03d.png".format(frameIndex)
            } else {
                "${baseName}_${size}.png"
            }
            img.meta.copy(pngFile = pngFile)
        }
        val jsonFile = File(outputDir, "${baseName}.json")
        if (!force && jsonFile.exists()) error("File already exists: ${jsonFile.absolutePath}")
        jsonFile.writeText(jsonFormat.encodeToString(meta))
        println("  Saved: ${jsonFile.absolutePath}")
    }
}
