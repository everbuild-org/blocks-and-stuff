package org.everbuild.blocksandstuff.testserver

import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import org.everbuild.blocksandstuff.common.InstanceOptions
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider

private enum class RuleType {
    BOOLEAN,
    INTEGER,
}

private enum class GameRule(
    val mcName: String,
    val type: RuleType,
) {
    RANDOM_TICK_SPEED("randomTickSpeed", RuleType.INTEGER) {
        override fun read(options: InstanceOptions): String = options.randomTickSpeed.toString()

        override fun write(
            instance: Instance,
            options: InstanceOptions,
            raw: String,
        ): Boolean {
            val value = raw.toIntOrNull()?.takeIf { it >= 0 } ?: return false
            options.randomTickSpeed = value
            return true
        }
    },
    DO_FIRE_TICK("doFireTick", RuleType.BOOLEAN) {
        override fun read(options: InstanceOptions): String = options.fireTick.toString()

        override fun write(
            instance: Instance,
            options: InstanceOptions,
            raw: String,
        ): Boolean {
            options.fireTick = raw.toBooleanStrictOrNull() ?: return false
            return true
        }
    },
    DO_TILE_DROPS("doTileDrops", RuleType.BOOLEAN) {
        override fun read(options: InstanceOptions): String = options.tileDrops.toString()

        override fun write(
            instance: Instance,
            options: InstanceOptions,
            raw: String,
        ): Boolean {
            options.tileDrops = raw.toBooleanStrictOrNull() ?: return false
            return true
        }
    },
    DO_DAYLIGHT_CYCLE("doDaylightCycle", RuleType.BOOLEAN) {
        override fun read(options: InstanceOptions): String = options.daylightCycle.toString()

        override fun write(
            instance: Instance,
            options: InstanceOptions,
            raw: String,
        ): Boolean {
            val value = raw.toBooleanStrictOrNull() ?: return false
            options.daylightCycle = value
            instance.defaultClock()?.let { if (value) it.resume() else it.pause() }
            return true
        }
    },
    DO_VINES_SPREAD("doVinesSpread", RuleType.BOOLEAN) {
        override fun read(options: InstanceOptions): String = options.vinesSpread.toString()

        override fun write(
            instance: Instance,
            options: InstanceOptions,
            raw: String,
        ): Boolean {
            options.vinesSpread = raw.toBooleanStrictOrNull() ?: return false
            return true
        }
    },
    PLAYERS_SLEEPING_PERCENTAGE("playersSleepingPercentage", RuleType.INTEGER) {
        override fun read(options: InstanceOptions): String = options.playersSleepingPercentage.toString()

        override fun write(
            instance: Instance,
            options: InstanceOptions,
            raw: String,
        ): Boolean {
            val value = raw.toIntOrNull()?.takeIf { it in 0..100 } ?: return false
            options.playersSleepingPercentage = value
            return true
        }
    },
    ;

    abstract fun read(options: InstanceOptions): String

    abstract fun write(
        instance: Instance,
        options: InstanceOptions,
        raw: String,
    ): Boolean

    companion object {
        fun byName(name: String): GameRule? = entries.firstOrNull { it.mcName.equals(name, ignoreCase = true) }
    }
}

class GameruleCommand : Command("gamerule") {
    init {
        setDefaultExecutor { sender, _ ->
            val instance = sender.resolveInstance() ?: return@setDefaultExecutor sender.sendInstanceError()
            val options = InstanceOptionsProvider.getForInstance(instance)
            GameRule.entries.forEach { sender.sendMessage("${it.mcName} = ${it.read(options)}") }
        }

        val rule = ArgumentType.Word("rule")
        val value = ArgumentType.Word("value")

        addSyntax({ sender, context ->
            val gameRule = GameRule.byName(context.get(rule))
            val instance = sender.resolveInstance()
            if (gameRule == null) {
                sender.sendMessage("Unknown gamerule: ${context.get(rule)}")
                return@addSyntax
            }
            if (instance == null) {
                sender.sendInstanceError()
                return@addSyntax
            }
            val options = InstanceOptionsProvider.getForInstance(instance)
            sender.sendMessage("${gameRule.mcName} = ${gameRule.read(options)}")
        }, rule)

        addSyntax({ sender, context ->
            val gameRule = GameRule.byName(context.get(rule))
            val instance = sender.resolveInstance()
            if (gameRule == null) {
                sender.sendMessage("Unknown gamerule: ${context.get(rule)}")
                return@addSyntax
            }
            if (instance == null) {
                sender.sendInstanceError()
                return@addSyntax
            }

            val options = InstanceOptionsProvider.getForInstance(instance)
            val raw = context.get(value)
            if (!gameRule.write(instance, options, raw)) {
                val expected = if (gameRule.type == RuleType.BOOLEAN) "true|false" else "a non-negative integer"
                sender.sendMessage("Invalid value for ${gameRule.mcName}: $raw (expected $expected)")
                return@addSyntax
            }
            sender.sendMessage("${gameRule.mcName} set to ${gameRule.read(options)}")
        }, rule, value)
    }
}

private fun CommandSender.resolveInstance(): Instance? =
    (this as? Player)?.instance ?: MinecraftServer.getInstanceManager().instances.firstOrNull()

private fun CommandSender.sendInstanceError() {
    sendMessage("No instance available for this command.")
}
