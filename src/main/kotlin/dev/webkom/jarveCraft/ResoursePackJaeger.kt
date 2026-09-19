package dev.webkom.jarveCraft

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class ResourcePackListener(
    private val packUrl: String,
    private val packHash: String? = null,
    private val promptText: String? = null,
    private val required: Boolean = false
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (packUrl.isBlank()) return

        val player = event.player
        val prompt = if (!promptText.isNullOrBlank()) Component.text(promptText) else null

        if (!packHash.isNullOrBlank()) {
            player.setResourcePack(packUrl, packHash, required, prompt)
        } else {
            player.setResourcePack(packUrl, "", required, prompt)
        }
    }
}
