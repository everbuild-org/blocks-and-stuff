package org.everbuild.blocksandstuff.testserver

import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations

class DebugCommand : Command("debug") {
    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("/debug [blockinfo|redstone]")
        }

        addSyntax({ sender, _ ->
            val target = (sender as Player).getTargetBlockPosition(10) ?: return@addSyntax
            sender.sendMessage("Target block: ${target.x()}, ${target.y()}, ${target.z()} ${sender.instance.getBlock(target)}")
        }, ArgumentType.Literal("blockinfo"))

        addSyntax({ sender, _ ->
            val sender = sender as Player
            val target = sender.getTargetBlockPosition(10) ?: return@addSyntax
            val instance = sender.instance
            val simulator = RedstoneRegistrations.simulator(instance)

            val block = instance.getBlock(target)
            sender.sendMessage("target ${block.key()} ${block.properties()}")
            sender.sendMessage("simulator=${simulator != null}")

            for (face in BlockFace.entries) {
                val neighbour = target.relative(face)
                val neighbourBlock = instance.getBlock(neighbour)
                val into = face.oppositeFace
                val weak = simulator?.weakSignal(neighbour, into) ?: 0
                val direct = simulator?.directSignal(neighbour, into) ?: 0
                sender.sendMessage("  $face -> ${neighbourBlock.key()} weak=$weak direct=$direct")
            }
        }, ArgumentType.Literal("redstone"))
    }
}
