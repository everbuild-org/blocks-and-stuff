package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.component.DataComponents
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.item.component.LodestoneTracker
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

val LODESTONE_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:lodestone") { ctx ->
        val held = ctx.player.itemInMainHand
        if (held.material() != Material.COMPASS) return@Rule RuleResult.Next

        val tracker =
            LodestoneTracker(
                ctx.instance.dimensionName,
                ctx.blockPosition,
                true,
            )
        ctx.player.itemInMainHand = held.with(DataComponents.LODESTONE_TRACKER, tracker)
        playSoundFor(ctx, SoundEvent.ITEM_LODESTONE_COMPASS_LOCK)
        RuleResult.Consume
    }

fun BlockRulesetEditor.addLodestone() {
    blocks(Block.LODESTONE) { interact.append { install(LODESTONE_RULE) } }
}
