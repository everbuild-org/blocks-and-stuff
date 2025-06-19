package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.instance.block.Block

object BlockBehaviorRuleRegistrations {
    @JvmStatic
    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        val manager = MinecraftServer.getBlockManager()
        val blockRegistry = Block.staticRegistry();
        handler.addListener(PlayerBlockPlaceEvent::class.java) {
            val handler = MinecraftServer.getBlockManager().getHandler(it.block.key().asString())
            if (it.block.handler() != handler) it.block = it.block.withHandler(handler)
        }

        val woodenTrapDoors = blockRegistry.getTag(Key.key("minecraft:wooden_trapdoors"))
        if (woodenTrapDoors != null) {
            for (trapDoor in woodenTrapDoors) {
                manager.registerHandler(trapDoor.key()) {
                    WoodenTrapDoorOpenRule(blockRegistry.get(trapDoor))
                }
            }
        }

        val copperBlocks = CopperOxidationRule.getOxidizableBlocks()
        for (copperBlock in copperBlocks) {
            val copperOxidationRule = CopperOxidationRule(copperBlock)
            MinecraftServer.getBlockManager().registerHandler(copperBlock.key()) { copperOxidationRule }
        }
    }
}
