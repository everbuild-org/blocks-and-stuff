package org.everbuild.blocksandstuff.common.item

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule.UpdateState

interface DroppedItemFactory {
    fun spawn(instance: Instance, position: Point, block: Block)

    companion object {
        @JvmStatic
        var current: DroppedItemFactory = DefaultDroppedItemFactory()
        @JvmStatic
        var doDropItems: Boolean = true

        fun maybeDrop(instance: Instance, position: Point, block: Block) {
            if (doDropItems) {
                current.spawn(instance, position, block)
            }
        }

        fun maybeDrop(state: UpdateState) {
            if (doDropItems) {
                current.spawn(state.instance as Instance, state.blockPosition, state.currentBlock)
            }
        }
    }
}