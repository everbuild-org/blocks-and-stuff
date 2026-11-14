package org.everbuild.blocksandstuff.testserver

import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider

class RandomTickSpeedCommand : Command("randomtickspeed", "rts") {
    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("Usage: /randomtickspeed <value>")
        }

        val value = ArgumentType.Integer("value").min(0)
        addSyntax({ sender, context ->
            val player = sender as? Player
            if (player == null) {
                sender.sendMessage("This command can only be used by a player.")
                return@addSyntax
            }

            val speed = context.get(value)
            InstanceOptionsProvider.getForInstance(player.instance).randomTickSpeed = speed
            sender.sendMessage("Random tick speed set to $speed")
        }, value)
    }
}
