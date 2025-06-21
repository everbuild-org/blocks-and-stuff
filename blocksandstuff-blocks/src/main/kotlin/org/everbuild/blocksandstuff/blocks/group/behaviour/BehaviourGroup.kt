package org.everbuild.blocksandstuff.blocks.group.behaviour

import java.util.function.Function
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.IntoBlockGroup

class BehaviourGroup(
    override val blockGroup: BlockGroup,
    private val valueFunction: Function<Block, BlockHandler>
) : IntoBlockGroup {
    fun createHandler(block: Block): BlockHandler {
        return valueFunction.apply(block)
    }

    constructor(blockGroup: BlockGroup, blockHandler: (Block) -> BlockHandler) : this(
        blockGroup,
        Function { it -> blockHandler(it) }
    )

}

