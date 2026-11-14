package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Mob heads mirror the redstone input in their `powered` state (jaw/ear animation). */
class DragonHeadComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation = poweredBlockState(ctx)
}
