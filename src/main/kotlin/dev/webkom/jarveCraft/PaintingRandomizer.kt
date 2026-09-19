package dev.webkom.jarveCraft

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Art
import org.bukkit.entity.Painting
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.hanging.HangingPlaceEvent
import java.io.File

@Suppress("DEPRECATION")
class PaintingRandomizer(private val plugin: JarveCraft) : Listener {

    private val customPaintings = mutableListOf<Art>()

    init {
        loadCustomPaintings()
    }

    fun loadCustomPaintings() {
        customPaintings.clear()

        // 1. Try reading from custom_paintings.txt in plugin resources
        val stream = plugin.getResource("custom_paintings.txt")
        if (stream != null) {
            stream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val clean = line.trim()
                    if (clean.isNotEmpty() && !clean.startsWith("#")) {
                        runCatching { Art.valueOf(clean.uppercase()) }
                            .getOrNull()
                            ?.let { if (!customPaintings.contains(it)) customPaintings.add(it) }
                    }
                }
            }
        }

        // 2. Also check development folders on disk if available
        val candidateDirs = listOf(
            File("src/main/resources/resourcepack/assets/minecraft/textures/painting"),
            File("../src/main/resources/resourcepack/assets/minecraft/textures/painting"),
            File("resourcepack/assets/minecraft/textures/painting")
        )
        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { f -> f.extension.equals("png", ignoreCase = true) }?.forEach { f ->
                    val artName = f.nameWithoutExtension.uppercase()
                    runCatching { Art.valueOf(artName) }.getOrNull()?.let {
                        if (!customPaintings.contains(it)) {
                            customPaintings.add(it)
                        }
                    }
                }
            }
        }

        plugin.logger.info("[PaintingRandomizer] Loaded ${customPaintings.size} custom painting(s): ${customPaintings.map { it.key.key }}")
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPaintingPlace(event: HangingPlaceEvent) {
        val painting = event.entity as? Painting ?: return
        if (customPaintings.isEmpty()) return

        for (art in customPaintings.shuffled()) {
            if (painting.setArt(art, false)) {
                return // Successfully placed a random custom painting!
            }
        }

        // If none of the custom paintings fit the wall space, cancel so vanilla paintings never appear
        event.isCancelled = true
        event.player?.sendMessage(
            Component.text(
                "This wall space is too small for any of the custom paintings!",
                NamedTextColor.RED
            )
        )
    }
}
