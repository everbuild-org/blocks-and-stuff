package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerBlockPlaceEvent

object BlockBehaviorRuleRegistrations {
    @JvmStatic
    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        handler.addListener(PlayerBlockPlaceEvent::class.java) {
            val handler = MinecraftServer.getBlockManager().getHandler(it.block.key().asString())
            if (it.block.handler() != handler) it.block = it.block.withHandler(handler)
        }
        val copperBlocks = CopperOxidationRule.getOxidizableBlocks()
        for (copperBlock in copperBlocks) {
            val copperOxidationRule = CopperOxidationRule(copperBlock)
            MinecraftServer.getBlockManager().registerHandler(copperBlock.key()) { copperOxidationRule }
        }
    }
}
