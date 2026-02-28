@file:OptIn(ExperimentalCli::class)

package jp.juggler.cursorGrid

import jp.juggler.cursorGrid.action.ActionGrid
import jp.juggler.cursorGrid.action.ActionPngToXCursor
import jp.juggler.cursorGrid.action.ActionXCursorToPng
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default

class CliArgs {
    val parser = ArgParser("cursorGrid")

    val verbose by parser.option(
        ArgType.Boolean,
        shortName = "v",
        description = "Verbose output"
    ).default(false)

    val actions = arrayOf<Subcommand>(
        ActionGrid(),
        ActionXCursorToPng(),
        ActionPngToXCursor(),
    ).also { parser.subcommands(*it) }

    fun parse(args: Array<out String>): Subcommand {
        val result = parser.parse(args)
        return actions.find { it.name == result.commandName }
            ?: error("missing action '${result.commandName}'")
    }
}
