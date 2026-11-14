package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.placement.WireConnections
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

/**
 * Right-clicking an isolated redstone wire toggles it between the cross and dot shapes. A dot does
 * not power horizontally, but still powers the block below it.
 */
val REDSTONE_WIRE_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:redstone_wire") { ctx ->
        if (!WireConnections.isIsolated(ctx.instance, ctx.blockPosition)) return@Rule RuleResult.Next

        val properties = listOf("north", "east", "south", "west")
        val current = properties.map { ctx.block.getProperty(it) ?: "none" }.distinct()
        if (current.size != 1) return@Rule RuleResult.Next

        val next =
            when (current.first()) {
                "side" -> "none"
                "none" -> "side"
                else -> return@Rule RuleResult.Next
            }
        ctx.replaceBlock(ctx.block.withProperties(properties.associateWith { next }))
        RuleResult.Consume
    }

fun BlockRulesetEditor.addRedstoneWire() {
    blocks(Block.REDSTONE_WIRE) { interact.append { install(REDSTONE_WIRE_RULE) } }
}
