package dev.webkom.jarveCraft

import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

enum class GameStage(val priority: Int, val payload: String) {
    OVERWORLD(
        priority = 1,
        payload = """
        {
          "on": true,
          "bri": 125,
          "tt": 10,
          "seg": [{
            "fx": 115,
            "pal": 10,
            "sx": 35,
            "ix": 50,
            "grp": 1,
            "spc": 0,
            "mi": false,
            "rev": false
          }]
        }
        """.trimIndent()
    ),

    NETHER(
        priority = 2,
        payload = """
        {
          "on": true,
          "bri": 255,
          "tt": 5,
          "seg": [{
            "col": [
              [255, 50, 0],
              [255, 0, 0],
              [255, 180, 0]
            ],
            "fx": 45,
            "pal": 35,
            "sx": 145,
            "ix": 140,
            "grp": 5,
            "spc": 0,
            "mi": true,
            "rev": true
          }]
        }
        """.trimIndent()
    ),

    THE_END(
        priority = 3,
        payload = """
        {
          "on": true,
          "bri": 125,
          "tt": 10,
          "seg": [{
            "col": [
              [170, 0, 255],
              [0, 0, 0],
              [255, 225, 60]
            ],
            "fx": 74,
            "pal": 4,
            "sx": 5,
            "ix": 5,
            "grp": 1,
            "spc": 0,
            "mi": false,
            "rev": false
          }]
        }
        """.trimIndent()
    )
}



class WledController(
    private val plugin: JavaPlugin,
    private var wledIp: String,
    private var enabled: Boolean = true,
    private var timeoutSeconds: Long = 2L
) : Listener {

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build()

    @Volatile
    private var endpoint = URI.create("http://$wledIp/json/state")

    @Volatile
    private var currentStage: GameStage = GameStage.OVERWORLD

    private val isAlertActive = AtomicBoolean(false)
    private val hasLoggedUnreachable = AtomicBoolean(false)

    private val deathFlashPayload = """
    {
      "on": true,
      "bri": 125,
      "tt": 0,
      "seg": [{
        "col": [ 
          [255, 0, 0],
          [0, 0, 0],
          [0, 0, 0]
        ],
        "fx": 25,
        "sx": 220,
        "ix": 200,
        "pal": 0,
        "grp": 1,
        "spc": 0,
        "mi": false,
        "rev": false
      }]
    }
    """.trimIndent()

    init {
        if (enabled) {
            applyStateAsync(currentStage.payload)
        } else {
            plugin.logger.info("[WLED] Light synchronization is disabled in config.yml.")
        }
    }

    fun updateConfig(enabled: Boolean, ip: String, timeout: Long) {
        this.enabled = enabled
        this.wledIp = ip
        this.timeoutSeconds = timeout
        this.endpoint = URI.create("http://$wledIp/json/state")
        this.hasLoggedUnreachable.set(false)
        if (enabled) {
            applyStateAsync(currentStage.payload)
        }
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        val targetEnvironment = event.player.world.environment
        val candidateStage = when (targetEnvironment) {
            World.Environment.NORMAL -> GameStage.OVERWORLD
            World.Environment.NETHER -> GameStage.NETHER
            World.Environment.THE_END -> GameStage.THE_END
            else -> return
        }

        if (candidateStage.priority > currentStage.priority) {
            currentStage = candidateStage
            plugin.logger.info("Progression updated to ${candidateStage.name}. Updating WLED baseline.")
            if (!isAlertActive.get()) {
                applyStateAsync(candidateStage.payload)
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        triggerDeathFlash()
    }

    fun triggerDeathFlash() {
        if (!enabled) return

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            isAlertActive.set(true)
            try {
                sendPost(deathFlashPayload)

                Thread.sleep(800)

                sendPost(currentStage.payload)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                handleNetworkError(e)
            } finally {
                isAlertActive.set(false)
            }
        })
    }

    private fun applyStateAsync(payload: String) {
        if (!enabled) return

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                sendPost(payload)
            } catch (e: Exception) {
                handleNetworkError(e)
            }
        })
    }

    private fun sendPost(json: String) {
        val request = HttpRequest.newBuilder()
            .uri(endpoint)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()

        client.send(request, HttpResponse.BodyHandlers.discarding())

        if (hasLoggedUnreachable.compareAndSet(true, false)) {
            plugin.logger.info("[WLED] Connection restored to $endpoint!")
        }
    }

    private fun handleNetworkError(e: Exception) {
        if (hasLoggedUnreachable.compareAndSet(false, true)) {
            plugin.logger.warning("[WLED] Unreachable at $endpoint (${e.message}). Silencing future connection errors until reachable.")
        }
    }

    fun forceStage(stage: GameStage) {
        currentStage = stage
        if (!isAlertActive.get()) {
            applyStateAsync(stage.payload)
        }
    }
}