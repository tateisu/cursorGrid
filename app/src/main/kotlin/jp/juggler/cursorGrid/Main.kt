package jp.juggler.cursorGrid

import kotlinx.cli.ExperimentalCli


@OptIn(ExperimentalCli::class)
fun main(args: Array<out String>) {
    val cliArgs = CliArgs()
    val subcommand = cliArgs.parse(args)
    if (subcommand !is IAction) {
        error("subcommand is not action. ${subcommand.name}")
    } else {
        with(subcommand) {
            cliArgs.runWithCliArgs()
        }
    }
}
