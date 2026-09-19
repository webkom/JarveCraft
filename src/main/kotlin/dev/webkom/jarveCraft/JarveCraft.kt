package dev.webkom.jarveCraft

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEggThrowEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionType

class JarveCraft : JavaPlugin(), Listener {

    private lateinit var wledController: WledController

    override fun onEnable() {
        saveDefaultConfig()

        val shotManager = ShotManager()
        server.pluginManager.registerEvents(shotManager, this)

        val jager = Jager(this, shotManager)
        server.pluginManager.registerEvents(jager, this)

        val wledIp = config.getString("wled.ip", "192.168.1.67") ?: "192.168.1.67"
        val wledEnabled = config.getBoolean("wled.enabled", true)
        val wledTimeout = config.getLong("wled.timeout-seconds", 2L)

        wledController = WledController(this, wledIp, wledEnabled, wledTimeout)
        server.pluginManager.registerEvents(wledController, this)

        val wledCommand = WledCommand(this, wledController)
        getCommand("wled")?.let { cmd ->
            cmd.setExecutor(wledCommand)
            cmd.tabCompleter = wledCommand
        }

        if (config.getBoolean("resource-pack.enabled", false)) {
            val packUrl = config.getString("resource-pack.url", "") ?: ""
            val packHash = config.getString("resource-pack.hash", null)
            val prompt = config.getString("resource-pack.prompt", null)
            val force = config.getBoolean("resource-pack.force", false)
            if (packUrl.isNotBlank()) {
                server.pluginManager.registerEvents(ResourcePackListener(packUrl, packHash, prompt, force), this)
                logger.info("[ResourcePack] Auto-send enabled for URL: $packUrl")
            }
        }

        server.pluginManager.registerEvents(DimensionTracking(this), this)
        server.pluginManager.registerEvents(AchievementGiveaway(this, jager), this)
        server.pluginManager.registerEvents(BabyZombieCustomizer(), this)
        server.pluginManager.registerEvents(PaintingRandomizer(this), this)

        registerJagerRecipe()
    }

    fun reloadPluginConfig() {
        reloadConfig()
        val wledIp = config.getString("wled.ip", "192.168.1.67") ?: "192.168.1.67"
        val wledEnabled = config.getBoolean("wled.enabled", true)
        val wledTimeout = config.getLong("wled.timeout-seconds", 2L)
        wledController.updateConfig(wledEnabled, wledIp, wledTimeout)
        logger.info("[JarveCraft] Configuration reloaded.")
    }

    private fun registerJagerRecipe() {
        val resultItem = ItemStack(Material.SPLASH_POTION).apply {
            editMeta(PotionMeta::class.java) { meta ->
                meta.displayName(Component.text("Jägermeister"))
                meta.setMaxStackSize(1)
                meta.itemModel = NamespacedKey("jarvecraft", "jagermeister")
                meta.persistentDataContainer.set(
                    NamespacedKey("jarvecraft", "is_jagermeister"),
                    PersistentDataType.BYTE,
                    1
                )
                meta.basePotionType = PotionType.THICK
            }
        }

        val recipe = ShapedRecipe(NamespacedKey(this, "jagermeister"), resultItem).apply {
            shape(
                " D ",
                "DBD",
                "   "
            )
            setIngredient('D', Material.DIAMOND)
            setIngredient('B', Material.GLASS_BOTTLE)
        }

        Bukkit.addRecipe(recipe)
    }
}
