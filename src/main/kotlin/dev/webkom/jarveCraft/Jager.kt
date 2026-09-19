package dev.webkom.jarveCraft

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.EntityEffect
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random
import net.kyori.adventure.sound.Sound as AdventureSound

class Jager(private val plugin: JavaPlugin, private val shotManager: ShotManager) : Listener {

    private val positiveEffects = listOf(
        PotionEffectType.SPEED,
        PotionEffectType.JUMP_BOOST,
        PotionEffectType.HEALTH_BOOST
    )

    fun applyJagerShotEffect(entity: LivingEntity) {
        entity.playEffect(EntityEffect.PROTECTED_FROM_DEATH)
        entity.addPotionEffect(PotionEffect(PotionEffectType.NAUSEA, 100, 9))
        entity.removePotionEffect(PotionEffectType.POISON)
        entity.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 100, 1))

        val randomBuff = positiveEffects[Random.nextInt(positiveEffects.size)]
        entity.addPotionEffect(PotionEffect(randomBuff, 2000, 3))

        entity.showTitle(
            Title.title(
                Component.text("Ta en Shot!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                Component.empty()
            )
        )

        entity.playSound(
            AdventureSound.sound(
                Key.key("jarvecraft", "shot_sound"),
                AdventureSound.Source.PLAYER,
                2.0f,
                1.0f
            )
        )

        entity.playSound(
            AdventureSound.sound(
                Sound.BLOCK_ANVIL_LAND,
                AdventureSound.Source.BLOCK,
                1.0f,
                1.0f
            )
        )

        val player = entity as? Player
        if (player != null) {
            shotManager.shotsScoreboardIncrement(player)
        }
    }

    @EventHandler
    fun nausea(e: PlayerItemConsumeEvent) {
        if (e.item.type == Material.GOLDEN_APPLE) {
            e.player.addPotionEffect(PotionEffect(PotionEffectType.NAUSEA, 200, 9))
        }
    }

    @EventHandler
    fun onRespawn(e: PlayerRespawnEvent) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            applyJagerShotEffect(e.player)
        })
    }

    @EventHandler
    fun hitByBottle(e: ProjectileHitEvent) {
        val thrownPotion = e.entity as? ThrownPotion ?: return
        val item = thrownPotion.item
        val meta = item.itemMeta as? PotionMeta ?: return

        val isCustomShot = meta.persistentDataContainer.has(
            NamespacedKey("jarvecraft", "is_jagermeister"),
            PersistentDataType.BYTE
        )

        if (item.type == Material.SPLASH_POTION && isCustomShot) {
            val splashRadius = 4.0
            val affected = thrownPotion.location.getNearbyLivingEntities(splashRadius)

            affected.forEach { entity ->
                applyJagerShotEffect(entity)
            }
        }
    }
}