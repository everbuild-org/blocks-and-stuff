package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.redstone.vanilla.NoteBlockInstruments
import org.everbuild.blocksandstuff.blocks.redstone.vanilla.NoteBlockSound
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

/** Right-clicking a note block plays it and advances its pitch, wrapping from 24 back to 0. */
val NOTE_BLOCK_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:note_block") { ctx ->
        val note =
            ctx.block
                .getProperty("note")
                ?.toIntOrNull()
                ?.coerceIn(0, 24) ?: 0
        val next = if (note >= 24) 0 else note + 1
        val instrument =
            NoteBlockInstruments.forBlockBelow(ctx.instance.getBlock(ctx.blockPosition.add(0.0, -1.0, 0.0)))
        val updated = ctx.block.withProperty("note", next.toString()).withProperty("instrument", instrument)

        ctx.replaceBlock(updated)
        NoteBlockSound.play(ctx.instance, ctx.blockPosition, updated)

        RuleResult.Consume
    }

fun BlockRulesetEditor.addNoteBlocks() {
    blocks(Block.NOTE_BLOCK) { interact.append { install(NOTE_BLOCK_RULE) } }
}
