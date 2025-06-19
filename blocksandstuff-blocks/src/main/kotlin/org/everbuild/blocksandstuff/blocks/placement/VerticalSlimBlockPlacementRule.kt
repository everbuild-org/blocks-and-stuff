package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.blocks.placement.common.AbstractConnectingBlockPlacementRule
import org.everbuild.blocksandstuff.common.tag.BlockTags

class VerticalSlimBlockPlacementRule(block: Block) : AbstractConnectingBlockPlacementRule(block) {
    private val walls = tagManager.getTag(Key.key("minecraft:walls"))!!
    private val glassPanes = BlockTags.getTaggedWith("blocksandstuff:glass_panes")
    private val canConnect = RegistryTag.direct(
        glassPanes.toList()
                + walls.toList()
                + listOf(Block.IRON_BARS)
    )

    override fun canConnect(instance: Block.Getter, pos: Point, blockFace: BlockFace): Boolean {
        val instanceBlock = instance.getBlock(pos)
        val isFaceFull = instanceBlock.registry().collisionShape().isFaceFull(blockFace)
        return !cannotConnect.contains(instanceBlock) && isFaceFull || canConnect.contains(instanceBlock) || instanceBlock.key() == this.block.key()
    }
}