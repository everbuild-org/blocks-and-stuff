package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import java.util.Random

val DRAGON_EGG_TELEPORT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:dragon_egg_teleport") { ctx ->
        if (!ctx.block.compare(Block.DRAGON_EGG)) return@Rule RuleResult.Next

        val random = Random()
        repeat(16) {
            val target =
                ctx.blockPosition.add(
                    random.nextInt(15) - 7.0,
                    random.nextInt(7) - 3.0,
                    random.nextInt(15) - 7.0,
                )
            if (!isValidTarget(ctx, target)) return@repeat

            ctx.instance.setBlock(ctx.blockPosition, Block.AIR, false)
            ctx.instance.setBlock(target, Block.DRAGON_EGG)
            playSoundFor(ctx, SoundEvent.ENTITY_ENDERMAN_TELEPORT)
            return@Rule RuleResult.Consume
        }
        RuleResult.Next
    }

private fun isValidTarget(
    ctx: InteractionActivityContext,
    target: net.minestom.server.coordinate.Point,
): Boolean {
    val block = ctx.instance.getBlock(target)
    return block.air() || block.replaceable()
}

fun BlockRulesetEditor.addDragonEgg() {
    blocks(Block.DRAGON_EGG) { interact.append { install(DRAGON_EGG_TELEPORT_RULE) } }
}
