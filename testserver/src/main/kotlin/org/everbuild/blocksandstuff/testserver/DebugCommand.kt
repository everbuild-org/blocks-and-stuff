package org.everbuild.blocksandstuff.testserver

import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

class DebugCommand : Command("debug") {
    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("/debug [blockinfo]")
        }

        addSyntax({ sender, _ ->
            val target = (sender as Player).getTargetBlockPosition(10) ?: return@addSyntax
            sender.sendMessage("Target block: ${target.x()}, ${target.y()}, ${target.z()} ${sender.instance.getBlock(target)}")
        }, ArgumentType.Literal("blockinfo"))
    }
}