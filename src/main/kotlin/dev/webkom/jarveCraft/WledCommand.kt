package dev.webkom.jarveCraft

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class WledCommand(
    private val plugin: JarveCraft,
    private val controller: WledController
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /$label [overworld|nether|end|death|reload]", NamedTextColor.RED))
            return true
        }
        when (args[0].lowercase()) {
            "overworld" -> {
                controller.forceStage(GameStage.OVERWORLD)
                sender.sendMessage(Component.text("WLED: Switched to Overworld theme", NamedTextColor.GREEN))
            }
            "nether" -> {
                controller.forceStage(GameStage.NETHER)
                sender.sendMessage(Component.text("WLED: Switched to Nether theme", NamedTextColor.GOLD))
            }
            "end" -> {
                controller.forceStage(GameStage.THE_END)
                sender.sendMessage(Component.text("WLED: Switched to The End theme", NamedTextColor.LIGHT_PURPLE))
            }
            "death" -> {
                controller.triggerDeathFlash()
                sender.sendMessage(Component.text("WLED: Triggered death flash", NamedTextColor.RED))
            }
            "reload" -> {
                plugin.reloadPluginConfig()
                sender.sendMessage(Component.text("JarveCraft: Config reloaded successfully!", NamedTextColor.GREEN))
            }
            else -> {
                sender.sendMessage(Component.text("Unknown option. Options: overworld, nether, end, death, reload", NamedTextColor.RED))
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val options = listOf("overworld", "nether", "end", "death", "reload")
        if (args.size == 1) {
            return options.filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}