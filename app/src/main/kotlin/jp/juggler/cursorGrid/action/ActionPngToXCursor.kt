package jp.juggler.cursorGrid.action

import jp.juggler.cursorGrid.CliArgs
import jp.juggler.cursorGrid.IAction
import jp.juggler.cursorGrid.data.XCursorImageMeta
import jp.juggler.cursorGrid.encoder.encodeXCursor
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.serialization.json.Json
import java.io.File

private val jsonFormat = Json { ignoreUnknownKeys = true }

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalCli::class)
class ActionPngToXCursor : Subcommand(
    "png2xcur",
    "Encode PNG files to xcursor"
), IAction {
    val inJson by argument(ArgType.String, description = "Input JSON metadata file")
    val outFile by argument(ArgType.String, description = "Output xcursor file")

    override fun execute() = Unit

    override fun CliArgs.runWithCliArgs() {
        if (verbose) println("Png2xcur: inJson=$inJson, outFile=$outFile")

        val jsonFile = File(inJson)
        if (!jsonFile.isFile) {
            println("File not found: ${jsonFile.absolutePath}")
            return
        }

        val meta = jsonFormat.decodeFromString<List<XCursorImageMeta>>(jsonFile.readText())
        val outputFile = File(outFile)
        outputFile.parentFile?.mkdirs()

        meta.encodeXCursor(jsonFile.parentFile, outputFile)

        println("Encoded ${meta.size} images to ${outputFile.absolutePath}")
    }
}
