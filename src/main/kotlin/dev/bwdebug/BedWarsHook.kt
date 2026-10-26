package dev.etran

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** Reflection helper: works with com.andrei1058.bedwars and forks that use com.tomkeuper.bedwars. */
object Reflect {
    private fun assignable(param: Class<*>, arg: Any?): Boolean {
        if (arg == null) return !param.isPrimitive
        if (param.isInstance(arg)) return true
        return when (param) {
            java.lang.Integer.TYPE -> arg is Int
            java.lang.Boolean.TYPE -> arg is Boolean
            else -> false
        }
    }

    fun call(target: Any, method: String, vararg args: Any?): Any? = try {
        val m = target.javaClass.methods.firstOrNull { m ->
            m.name == method && m.parameterTypes.size == args.size &&
                m.parameterTypes.indices.all { assignable(m.parameterTypes[it], args[it]) }
        }
        if (m == null) null else {
            m.isAccessible = true
            m.invoke(target, *args)
        }
    } catch (_: Throwable) {
        null
    }
}

object BedWarsHook {
    private val PREFIXES = listOf("com.andrei1058.bedwars", "com.tomkeuper.bedwars")
    lateinit var prefix: String
        private set
    private lateinit var api: Any

    fun init(): Boolean {
        for (p in PREFIXES) {
            val cls = try {
                Class.forName("$p.api.BedWars")
            } catch (_: Throwable) {
                continue
            }
            val reg = Bukkit.getServicesManager().getRegistration(cls) ?: continue
            prefix = p
            api = reg.provider as Any
            return true
        }
        return false
    }

    fun arenaOf(p: Player): Any? {
        val util = Reflect.call(api, "getArenaUtil") ?: return null
        return Reflect.call(util, "getArenaByPlayer", p)
    }

    fun statusOf(arena: Any): String = (Reflect.call(arena, "getStatus") as? Enum<*>)?.name ?: ""

    fun isPlayer(arena: Any, p: Player): Boolean = Reflect.call(arena, "isPlayer", p) as? Boolean ?: false

    private fun gameState(name: String): Any? =
        try {
            Class.forName("$prefix.api.arena.GameState").enumConstants.firstOrNull { (it as Enum<*>).name == name }
        } catch (_: Throwable) {
            null
        }

    /**
     * Starts [arena] after [seconds] regardless of how many players it has.
     * Returns null on success or a message explaining why it could not start.
     */
    fun forceStart(arena: Any, seconds: Int): String? {
        when (statusOf(arena)) {
            "playing" -> return "The game is already running."
            "restarting" -> return "The arena is restarting."
            "waiting" -> {
                val starting = gameState("starting") ?: return "Could not find BedWars' GameState.starting."
                Reflect.call(arena, "changeStatus", starting)
            }
        }
        val task = Reflect.call(arena, "getStartingTask") ?: return "BedWars did not create a starting task."
        Reflect.call(task, "setCountdown", seconds.coerceAtLeast(1))
        return null
    }

    fun isSpectator(arena: Any, p: Player): Boolean = Reflect.call(arena, "isSpectator", p) as? Boolean ?: false

    fun arenaName(arena: Any): String =
        (Reflect.call(arena, "getArenaName") as? String) ?: (Reflect.call(arena, "getWorldName") as? String) ?: "arena"

    /**
     * Ends the game immediately: BedWars' restarting state sends everyone back to the lobby and resets the map.
     * Returns null on success or a message explaining why it could not.
     */
    fun endGame(arena: Any): String? {
        when (statusOf(arena)) {
            "restarting" -> return "The arena is already restarting."
            "waiting", "starting" -> return "The game has not started yet."
        }
        val restarting = gameState("restarting") ?: return "Could not find BedWars' GameState.restarting."
        Reflect.call(arena, "changeStatus", restarting)
        return null
    }
}
