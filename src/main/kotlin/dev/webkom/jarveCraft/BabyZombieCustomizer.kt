package dev.webkom.jarveCraft

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack

class BabyZombieCustomizer : Listener {

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val zombie = event.entity as? Zombie ?: return
        if (zombie.isAdult) return

        val equipment = zombie.equipment ?: return
        val helmet = ItemStack(Material.CARVED_PUMPKIN).apply {
            editMeta { meta ->
                meta.setItemModel(NamespacedKey("jarvecraft", "carved_pumpkin"))
            }
        }
        equipment.helmet = helmet
        equipment.helmetDropChance = 0.0f
    }

}