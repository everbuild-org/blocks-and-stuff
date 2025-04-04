package org.everbuild.averium.worlds.fluid.listener

import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.utils.eyePosition
import org.everbuild.blocksandstuff.fluids.findBlockFace
import org.everbuild.blocksandstuff.fluids.raycastForFluid

fun setupFluidPickupEvent() {

    MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent::class.java) {
        event: PlayerUseItemEvent ->
        run {
            val instance = event.player.instance
            val itemInMainHand = event.player.itemInMainHand
            println("PlayerUseItemEvent")
            val liquidBlock =
                raycastForFluid(
                    event.player,
                    event.player.eyePosition(),
                    event.player.position.direction(),
                    5.0
                ) ?: return@run
            if (itemInMainHand == ItemStack.of(Material.BUCKET)) {
                println("PlayerUseItemEvent: BUCKET")
                val blockFace = findBlockFace(player = event.player, liquidBlock) ?: return@run
                println("PlayerUseItemEvent: BUCKET: blockFace")
                instance.placeBlock(
                    PlayerPlacement(
                        Block.AIR,
                        instance,
                        liquidBlock,
                        event.player,
                        event.hand,
                        blockFace,
                        liquidBlock.x().toFloat(),
                        liquidBlock.y().toFloat(),
                        liquidBlock.z().toFloat(),
                    )
                )
            }
        }
    }

//    MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent::class.java) {
//        event: PlayerBlockInteractEvent -> if (event.isCancelled) return@addListener
//    }

//    listen<PlayerBlockInteractEvent> {
//        val instance = it.player.instance
//        val itemInMainHand = it.player.itemInMainHand
//        val ecs = it.player.averium.getEcsEntity()
//        it.isCancelled = !ecs.has<BuildComponent>()
//        if (it.isCancelled) return@listen
//
//    }
}