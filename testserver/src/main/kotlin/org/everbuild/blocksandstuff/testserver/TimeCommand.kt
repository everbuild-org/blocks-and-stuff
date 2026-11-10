package org.everbuild.blocksandstuff.testserver

import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance

private const val DAY_LENGTH = 24_000L

private val NAMED_TIMES =
    mapOf(
        "day" to 1_000L,
        "noon" to 6_000L,
        "sunset" to 12_000L,
        "night" to 13_000L,
        "midnight" to 18_000L,
        "sunrise" to 23_000L,
    )

class TimeCommand : Command("time") {
    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("Usage: /time <set <day|noon|sunset|night|midnight|sunrise|ticks>|add <ticks>|query [daytime|gametime|day]>")
        }

        val name = ArgumentType.Word("value")
        val ticks = ArgumentType.Integer("ticks").min(0)

        addSyntax({ sender, context ->
            val instance = sender.resolveInstance() ?: return@addSyntax sender.sendInstanceError()
            val value = context.get(name)
            val time = NAMED_TIMES[value.lowercase()] ?: value.toLongOrNull()
            if (time == null) {
                sender.sendMessage("Unknown time: $value")
                return@addSyntax
            }
            instance.setTime(time)
            sender.sendMessage("Time set to $time")
        }, ArgumentType.Literal("set"), name)

        addSyntax({ sender, context ->
            val instance = sender.resolveInstance() ?: return@addSyntax sender.sendInstanceError()
            val value = context.get(ticks)
            instance.setTime(instance.time + value)
            sender.sendMessage("Time added: $value (now ${instance.time})")
        }, ArgumentType.Literal("add"), ticks)

        addSyntax({ sender, _ -> sender.queryTime("daytime") }, ArgumentType.Literal("query"))
        addSyntax({ sender, _ -> sender.queryTime("daytime") }, ArgumentType.Literal("query"), ArgumentType.Literal("daytime"))
        addSyntax({ sender, _ -> sender.queryTime("gametime") }, ArgumentType.Literal("query"), ArgumentType.Literal("gametime"))
        addSyntax({ sender, _ -> sender.queryTime("day") }, ArgumentType.Literal("query"), ArgumentType.Literal("day"))
    }

    private fun CommandSender.queryTime(kind: String) {
        val instance = resolveInstance() ?: return sendInstanceError()
        val value =
            when (kind) {
                "gametime" -> instance.worldAge
                "day" -> Math.floorDiv(instance.time, DAY_LENGTH)
                else -> Math.floorMod(instance.time, DAY_LENGTH)
            }
        sendMessage("$kind = $value")
    }
}

private fun CommandSender.resolveInstance(): Instance? =
    (this as? Player)?.instance ?: MinecraftServer.getInstanceManager().instances.firstOrNull()

private fun CommandSender.sendInstanceError() {
    sendMessage("No instance available for this command.")
}
