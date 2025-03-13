package org.everbuild.blocksandstuff.blocks.group.placement

import java.util.function.Function
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.IntoBlockGroup

class PlacementGroup(
    override val blockGroup: BlockGroup,
    private val valueFunction: Function<Block, BlockPlacementRule>
) : IntoBlockGroup, IntoPlacementRule {
    override fun createRule(block: Block): BlockPlacementRule {
        return valueFunction.apply(block)
    }
}
