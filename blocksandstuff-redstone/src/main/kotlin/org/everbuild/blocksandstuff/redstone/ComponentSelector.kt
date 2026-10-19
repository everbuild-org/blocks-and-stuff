package org.everbuild.blocksandstuff.redstone

import net.minestom.server.instance.block.Block

/** Selects which blocks a component applies to. */
sealed interface ComponentSelector {
    data class ByBlock(val block: Block) : ComponentSelector

    data class ByTag(val tag: String) : ComponentSelector
}

/** Creates a fresh component instance for a concrete block. */
typealias ComponentFactory = (Block) -> RedstoneComponent
