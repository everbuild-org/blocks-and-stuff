package org.everbuild.blocksandstuff.fluids.listener

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.fluids.MinestomFluids

fun setupFluidPlacementEvent() {
    MinecraftServer
        .getGlobalEventHandler()
        .addListener(PlayerBlockInteractEvent::class.java) { event: PlayerBlockInteractEvent ->
            if (event.isCancelled) return@addListener
            if (event.player.getItemInHand(event.hand) != event.player.itemInMainHand) return@addListener

            val instance = event.player.instance
            val held = event.player.itemInMainHand
            val heldMaterial = held.material()

            if (heldMaterial != Material.WATER_BUCKET && heldMaterial != Material.LAVA_BUCKET) return@addListener

            val blockFace = event.blockFace
            val updated = instance.getBlock(event.blockPosition)
            var placePosition: Pos =
                event.blockPosition
                    .relative(blockFace)
                    .asVec()
                    .asPosition()
            val blockToPlace: Block =
                when (heldMaterial) {
                    Material.LAVA_BUCKET -> {
                        Block.LAVA
                    }

                    Material.WATER_BUCKET -> {
                        if (isWaterloggable(updated)) {
                            placePosition = event.blockPosition.asVec().asPosition()
                            updated.withProperty("waterlogged", "true")
                        } else {
                            Block.WATER
                        }
                    }

                    else -> {
                        return@addListener
                    }
                }

            event.isCancelled = true
            instance.placeBlock(
                PlayerPlacement(
                    blockToPlace,
                    updated,
                    instance,
                    placePosition,
                    event.player,
                    PlayerHand.MAIN,
                    blockFace,
                    placePosition.x.toFloat(),
                    placePosition.y.toFloat(),
                    placePosition.z.toFloat(),
                ),
            )

            if (blockToPlace == Block.WATER || blockToPlace == Block.LAVA) {
                MinestomFluids.scheduleTick(instance, placePosition, blockToPlace)

                for (face in BlockFace.entries) {
                    val neighbor = placePosition.relative(face)
                    val neighborBlock = instance.getBlock(neighbor)
                    if (MinestomFluids.getFluidOnBlock(neighborBlock) !== MinestomFluids.EMPTY) {
                        MinestomFluids.scheduleTick(instance, neighbor, neighborBlock)
                    }
                }
            }

            if (event.player.gameMode != GameMode.CREATIVE) {
                event.player.inventory.setEquipment(
                    EquipmentSlot.MAIN_HAND,
                    event.player.heldSlot,
                    ItemStack.of(Material.BUCKET),
                )
            }
        }
}

fun isWaterloggable(block: Block): Boolean {
    val tags = Block.staticRegistry()
    return (
        tags.getTag(Key.key("minecraft:stairs"))!!.contains(block) ||
            tags.getTag(Key.key("minecraft:slabs"))!!.contains(block) ||
            tags.getTag(Key.key("minecraft:fences"))!!.contains(block) ||
            tags.getTag(Key.key("minecraft:trapdoors"))!!.contains(block)
    )
}
