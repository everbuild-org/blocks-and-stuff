package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.registry.RegistryTag
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.blocks.placement.common.AbstractConnectingBlockPlacementRule
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.isWaterSource

class WallBlockPlacementRule(block: Block) : AbstractConnectingBlockPlacementRule(block) {
    private val walls = tagManager.getTag(Key.key("minecraft:walls"))!!
    private val glassPanes = BlockTags.getTaggedWith("blocksandstuff:glass_panes")
    private val fenceGates = BlockTags.getTaggedWith("minecraft:fence_gates")
    private val canConnect = RegistryTag.direct(
        glassPanes.toList()
                + walls.toList()
                + fenceGates.toList()
                + listOf(Block.IRON_BARS)
    )

    override fun canConnect(instance: Block.Getter, pos: Point, blockFace: BlockFace): Boolean {
        val instanceBlock = instance.getBlock(pos)
        val isFaceFull = instanceBlock.registry()!!.collisionShape().isFaceFull(blockFace)
        return !cannotConnect.contains(instanceBlock) && isFaceFull || canConnect.contains(instanceBlock) || instanceBlock.key() == this.block.key()
    }

    override fun stringify(connect: Boolean, instance: Block.Getter, pos: Point, direction: BlockFace): String {
        if (!connect) return "none"
        val above = instance.getBlock(pos.add(0.0, 1.0, 0.0))
        if (!above.isAir) return "tall"
        return "low"
    }

    override fun transmute(instance: Block.Getter, pos: Point, block: Block): Block {
        val instanceBlock = instance.getBlock(pos)
        val blocksAndDirections = BlockFace.entries
            .filter { it.toDirection().horizontal() }
            .map {
                canConnect(instance, pos.add(it.toDirection().vec()), it)
            }

        val axis1 = blocksAndDirections[0] && blocksAndDirections[1]
        val axis2 = blocksAndDirections[2] && blocksAndDirections[3]

        val hasPillar = !((!axis1 && axis2) || (!axis2 && axis1))
                || !blocksAndDirections.any { it }
                || blocksAndDirections.count { it } % 2 == 1

        val blockAbove = instance.getBlock(pos.add(0.0, 1.0, 0.0))
        val blockAboveConnect = !blockAbove.isAir || (blockAbove.key() == block.key() && blockAbove.getProperty("up") == "false")

        return block
            .withProperty("waterlogged", instanceBlock.isWater().toString())
            .withProperty("up", (hasPillar || blockAboveConnect).toString())
    }
}