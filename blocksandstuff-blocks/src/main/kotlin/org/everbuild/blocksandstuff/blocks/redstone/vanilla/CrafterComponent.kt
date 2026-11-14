package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.blocks.behavior.CrafterHandler
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Crafter: crafts once on a rising redstone edge, four ticks later. */
class CrafterComponent : RedstoneComponent {
    private var lastPowered = false
    private var triggerAt = -1L
    private var craftingUntil = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val now = ctx.tick()
        val powered = isPowered(ctx)
        var crafting = block.getProperty("crafting") == "true"

        if (powered && !lastPowered && triggerAt < 0) {
            triggerAt = now + CRAFT_DELAY_TICKS
            ctx.scheduleTick(CRAFT_DELAY_TICKS)
        }
        lastPowered = powered

        if (triggerAt >= 0 && now >= triggerAt) {
            triggerAt = -1
            val crafted =
                (ctx.instance.getBlock(ctx.position).handlerOrRegistered() as? CrafterHandler)
                    ?.craft(ctx.instance, ctx.position) == true
            if (crafted) {
                craftingUntil = now + CRAFTING_TICKS
                ctx.scheduleTick(CRAFTING_TICKS)
                crafting = true
            }
        }

        if (craftingUntil >= 0 && now >= craftingUntil) {
            craftingUntil = -1
            crafting = false
        }

        var updated = ctx.instance.getBlock(ctx.position)
        if (updated.getProperty("triggered") != powered.toString()) {
            updated = updated.withProperty("triggered", powered.toString())
        }
        if (updated.getProperty("crafting") != crafting.toString()) {
            updated = updated.withProperty("crafting", crafting.toString())
        }

        return if (updated.stateId() == block.stateId()) {
            ComponentEvaluation.None
        } else {
            ComponentEvaluation.SetState(updated)
        }
    }
    private companion object {
        private const val CRAFT_DELAY_TICKS = 4
        private const val CRAFTING_TICKS = 6
    }
}
