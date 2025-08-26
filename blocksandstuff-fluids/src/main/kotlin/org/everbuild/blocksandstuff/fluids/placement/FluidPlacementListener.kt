package org.everbuild.blocksandstuff.fluids.placement

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.fluids.MinestomFluids

@Suppress("UnstableApiUsage")
fun getFluidPlacementEventNode() = EventNode.all("fluid-placement")
    .addListener(PlayerBlockInteractEvent::class.java) { event: PlayerBlockInteractEvent ->
        if (event.isCancelled) return@addListener
        val instance = event.player.instance
        val itemInHand = event.player.getItemInHand(event.hand)

        val blockFace = event.blockFace
        var blockToPlace: Block?
        var placePosition: Pos = event.blockPosition.relative(blockFace).asVec().asPosition()

        if (itemInHand == ItemStack.of(Material.LAVA_BUCKET)) {
            blockToPlace = Block.LAVA

            val placeEvent = FluidPlaceEvent(
                instance,
                event.player,
                Block.AIR,
                BlockVec(placePosition.x, placePosition.y, placePosition.z),
                blockToPlace
            )

            MinecraftServer.getGlobalEventHandler().callCancellable(placeEvent) {
                instance.placeBlock(
                    PlayerPlacement(
                        placeEvent.blockToPlace,
                        instance,
                        placePosition,
                        event.player,
                        event.hand,
                        blockFace,
                        placePosition.x.toFloat(),
                        placePosition.y.toFloat(),
                        placePosition.z.toFloat()
                    )
                )
                event.player.refreshPosition(placePosition)
                if (event.player.gameMode != GameMode.CREATIVE) {
                    event.player.setItemInHand(event.hand, ItemStack.of(Material.BUCKET))
                }
            }

        } else if (itemInHand == ItemStack.of(Material.WATER_BUCKET)) {
            val block = instance.getBlock(event.blockPosition)

            if (isWaterloggable(block)) {
                blockToPlace = block.withProperty("waterlogged", "true")
                placePosition = event.blockPosition.asVec().asPosition()
            } else {
                blockToPlace = Block.WATER
            }

            val placeEvent = FluidPlaceEvent(
                instance,
                event.player,
                block,
                BlockVec(placePosition.x, placePosition.y, placePosition.z),
                blockToPlace
            )

            MinecraftServer.getGlobalEventHandler().callCancellable(placeEvent) {
                instance.placeBlock(
                    PlayerPlacement(
                        placeEvent.blockToPlace,
                        instance,
                        placePosition,
                        event.player,
                        event.hand,
                        blockFace,
                        placePosition.x.toFloat(),
                        placePosition.y.toFloat(),
                        placePosition.z.toFloat()
                    )
                )
                if (placeEvent.blockToPlace != Block.WATER) {
                    // Schedule update for the current block
                    MinestomFluids.scheduleTick(instance, placePosition, Block.WATER)

                    // Schedule updates for adjacent blocks (ensures fluid spreads properly)
                    for (face in BlockFace.entries) {
                        val neighbor = placePosition.relative(face)
                        val neighborBlock = instance.getBlock(neighbor)
                        if (MinestomFluids.getFluidOnBlock(neighborBlock) !== MinestomFluids.EMPTY) {
                            MinestomFluids.scheduleTick(instance, neighbor, neighborBlock)
                        }
                    }
                }
                event.player.refreshPosition(placePosition, false, true)
                if (event.player.gameMode != GameMode.CREATIVE) {
                    event.player.setItemInHand(event.hand, ItemStack.of(Material.BUCKET))
                }
            }

        }
        event.player.inventory.update()

    }

fun isWaterloggable(block: Block): Boolean {
    val tags = Block.staticRegistry()
    return (tags.getTag(Key.key("minecraft:stairs"))!!.contains(block)
            || tags.getTag(Key.key("minecraft:slabs"))!!.contains(block)
            || tags.getTag(Key.key("minecraft:fences"))!!.contains(block)
            || tags.getTag(Key.key("minecraft:trapdoors"))!!.contains(block))
//    if (block.compare(Block.LADDER)
//        || block.compare(Block.SUGAR_CANE)
//        || block.compare(Block.BUBBLE_COLUMN)
//        || block.compare(Block.NETHER_PORTAL)
//        || block.compare(Block.END_PORTAL)
//        || block.compare(Block.END_GATEWAY)
//        || block.compare(Block.KELP)
//        || block.compare(Block.KELP_PLANT)
//        || block.compare(Block.SEAGRASS)
//        || block.compare(Block.TALL_SEAGRASS)
//        || block.compare(Block.SEA_PICKLE)
//        || tags.getTag(Tag.BasicType.BLOCKS, "minecraft:signs")!!.contains(block.namespace())
//        || block.name().contains("door")
//        || block.name().contains("coral")
//    if (tags.getTag(Key.key("minecraft:stairs"))!!.contains(block)) {
//        return true
//    }
//    return !block.isSolid || !block.isAir
}
