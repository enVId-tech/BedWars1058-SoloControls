package dev.etran

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class SoloControlsPlugin : JavaPlugin(), Listener {

    /** /bw-solocontrols config ...: edit options in game; changes apply immediately and are saved to config.yml. */
    private val configCmd = ConfigCommand(this) { /* options are read on use, nothing to rebuild */ }

    override fun onEnable() {
        if (!BedWarsHook.init()) {
            logger.severe("BedWars1058 (or a compatible fork) was not found - disabling.")
            server.pluginManager.disablePlugin(this)
            return
        }
        saveDefaultConfig()
        // Leftover from the removed testing-player feature.
        runCatching { File(dataFolder, "bots.journal").delete() }
        server.pluginManager.registerEvents(this, this)
        logger.info("BedWars1058-SoloControls enabled (BedWars API: ${BedWarsHook.prefix}).")
    }

    /** "/bw start [seconds]" -> force start even with a single player. Everything else goes to BedWars. */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPreprocess(e: PlayerCommandPreprocessEvent) {
        if (!config.getBoolean("intercept-bw-start", true)) return
        val parts = e.message.trim().split(Regex("\\s+"))
        if (parts.size < 2 || parts.size > 3) return
        val root = parts[0].lowercase().removePrefix("/")
        if (root != "bw" && root != "bedwars") return
        if (!parts[1].equals("start", ignoreCase = true)) return
        if (!e.player.hasPermission("solocontrols.use")) return
        // Only take over when the player is actually in a lobby; otherwise let BedWars answer.
        val arena = BedWarsHook.arenaOf(e.player) ?: return
        if (!BedWarsHook.isPlayer(arena, e.player)) return
        e.isCancelled = true
        start(e.player, parts.getOrNull(2)?.toIntOrNull())
    }

    private fun start(p: Player, seconds: Int?) {
        val arena = BedWarsHook.arenaOf(p)
        if (arena == null) {
            p.sendMessage("§cYou are not in a BedWars arena.")
            return
        }
        val error = BedWarsHook.forceStart(arena, seconds ?: config.getInt("start-countdown", 3))
        if (error != null) p.sendMessage("§c$error") else p.sendMessage("§aStarting the game...")
    }

    /** Ends the game the player is playing in, or (with solocontrols.end.spectating) the one they are spectating. */
    private fun end(p: Player) {
        val arena = BedWarsHook.arenaOf(p)
        if (arena == null) {
            p.sendMessage("§cYou are not in a BedWars arena.")
            return
        }
        val spectating = BedWarsHook.isSpectator(arena, p)
        val perm = if (spectating) "solocontrols.end.spectating" else "solocontrols.end"
        if (!p.hasPermission(perm)) {
            p.sendMessage("§cYou need the $perm permission.")
            return
        }
        val error = BedWarsHook.endGame(arena)
        if (error != null) p.sendMessage("§c$error")
        else p.sendMessage("§aEnded the game on ${BedWarsHook.arenaName(arena)}.")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.firstOrNull()?.lowercase()) {
            "start" -> {
                if (sender is Player) start(sender, args.getOrNull(1)?.toIntOrNull()) else sender.sendMessage("Players only.")
            }
            "end" -> {
                if (sender is Player) end(sender) else sender.sendMessage("Players only.")
            }
            "reload" -> configCmd.reload(sender)
            "config" -> configCmd.handle(sender, args.drop(1), label)
            else -> sender.sendMessage("/$label <start [seconds] | end | reload | config ...>")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when {
            args.size == 1 -> listOf("start", "end", "reload", "config").filter { it.startsWith(args[0], ignoreCase = true) }
            args[0].equals("config", ignoreCase = true) -> configCmd.complete(args.drop(1))
            else -> emptyList()
        }
    }
}
