package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.util.PrimedTnt
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import org.everbuild.blocksandstuff.redstone.VibrationBus

/**
 * TNT: ignites on a rising power edge, spawning a primed TNT entity and clearing the block. Overrides
 * the vanilla "redstone block ignites directly adjacent TNT" rule only when powered through the
 * engine.
 */
class TntComponent : RedstoneComponent {
    private var ignited = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        if (ignited || !isPowered(ctx)) return ComponentEvaluation.None
        ignited = true
        return ComponentEvaluation.Trigger { context ->
            VibrationBus.emit(context.instance, context.position, 10)
            PrimedTnt.spawn(context.instance, context.position)
            context.setState(context.position, Block.AIR)
        }
    }
}
