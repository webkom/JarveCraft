package dev.webkom.jarveCraft

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.plugin.java.JavaPlugin

class AchievementGiveaway(private val plugin: JavaPlugin, private val jager: Jager) : Listener {

    @EventHandler
    fun giveAchievement(e: PlayerAdvancementDoneEvent) {
        if (e.advancement.key.toString() == "minecraft:story/obtain_armor") {
            jager.applyJagerShotEffect(e.player)
        }
    }
}