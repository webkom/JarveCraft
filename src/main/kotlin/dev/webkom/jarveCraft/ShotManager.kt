package dev.webkom.jarveCraft

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard

class ShotManager : Listener {
    private val scoreboardManager = Bukkit.getScoreboardManager()
    val shotScoreboard: Scoreboard = scoreboardManager.newScoreboard

    private val objective: Objective = shotScoreboard.registerNewObjective(
        "shots",
        Criteria.DUMMY,
        Component.text("Shots Counter", NamedTextColor.GOLD, TextDecoration.BOLD)
    ).apply {
        displaySlot = DisplaySlot.SIDEBAR
    }

    @EventHandler
    fun playerJoin(e: PlayerJoinEvent) {
        val player = e.player
        player.scoreboard = shotScoreboard

        val score = objective.getScore(player)
        if (!score.isScoreSet) {
            score.score = 0
        }
    }

    fun shotsScoreboardIncrement(player: Player) {
        val score = objective.getScore(player)
        score.score += 1
    }

    @EventHandler
    fun playerDeath(e: PlayerDeathEvent) {
        shotsScoreboardIncrement(e.player)
    }
}