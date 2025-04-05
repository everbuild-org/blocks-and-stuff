package org.everbuild.blocksandstuff.fluids.pickup

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.utils.eyePosition
import org.everbuild.blocksandstuff.fluids.findBlockFace
import org.everbuild.blocksandstuff.fluids.raycastForFluid

fun getFluidPickupEventNode() = EventNode.all("fluid-pickup")
    .addListener(PlayerUseItemEvent::class.java) { event: PlayerUseItemEvent ->
        if (event.player.itemInMainHand != ItemStack.of(Material.BUCKET)) return@addListener

        val instance = event.player.instance

        val liquidBlock =
            raycastForFluid(
                event.player,
                event.player.eyePosition(),
                event.player.position.direction(),
                event.player.getAttributeValue(Attribute.BLOCK_INTERACTION_RANGE)
            ) ?: return@addListener

        val blockFace = findBlockFace(player = event.player, liquidBlock) ?: return@addListener

        val pickupEvent = FluidPickupEvent(
            event.instance,
            event.player,
            event.instance.getBlock(liquidBlock),
            BlockVec(liquidBlock.x(), liquidBlock.y(), liquidBlock.z()),
            Block.AIR
        )
        MinecraftServer.getGlobalEventHandler().callCancellable(pickupEvent) {
            instance.placeBlock(
                PlayerPlacement(
                    pickupEvent.blockToPlace,
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
