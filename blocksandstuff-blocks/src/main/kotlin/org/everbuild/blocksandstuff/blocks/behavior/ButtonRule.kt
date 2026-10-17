package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val buttons: Collection<Block> = BlockTags.getTaggedWith("minecraft:buttons")

val BUTTON_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:button") { ctx ->
        if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

        val block = ctx.block
        if (block.getProperty("powered") == "true") return@Rule RuleResult.Consume

        ctx.replaceBlock(block.withProperty("powered", "true"))
        ctx.instance.playSoundExcept(
            ctx.player,
            Sound.sound(buttonSound(block, true), Sound.Source.BLOCK, 0.3f, 0.6f),
            ctx.blockPosition,
        )

        val position = ctx.blockPosition
        ctx.instance.scheduler().scheduleTask(
            {
                val current = ctx.instance.getBlock(position)
                if (current.key() == block.key() && current.getProperty("powered") == "true") {
                    ctx.instance.setBlock(position, current.withProperty("powered", "false"))
                    ctx.instance.playSoundExcept(
                        null,
                        Sound.sound(buttonSound(block, false), Sound.Source.BLOCK, 0.3f, 0.5f),
                        position,
                    )
                }
            },
            TaskSchedule.tick(ticksToStayPressed(block)),
            TaskSchedule.stop(),
        )

        RuleResult.Consume
    }

private fun ticksToStayPressed(block: Block): Int = if (block.key().value().contains("stone")) 20 else 30

private fun buttonSound(
    block: Block,
    on: Boolean,
): SoundEvent {
    val name = block.key().value()
    return when {
        name.startsWith("bamboo") -> {
            if (on) SoundEvent.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON else SoundEvent.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF
        }

        name.startsWith("cherry") -> {
            if (on) SoundEvent.BLOCK_CHERRY_WOOD_BUTTON_CLICK_ON else SoundEvent.BLOCK_CHERRY_WOOD_BUTTON_CLICK_OFF
        }

        name.startsWith("crimson") || name.startsWith("warped") -> {
            if (on) SoundEvent.BLOCK_NETHER_WOOD_BUTTON_CLICK_ON else SoundEvent.BLOCK_NETHER_WOOD_BUTTON_CLICK_OFF
        }

        name.contains("stone") -> {
            if (on) SoundEvent.BLOCK_STONE_BUTTON_CLICK_ON else SoundEvent.BLOCK_STONE_BUTTON_CLICK_OFF
        }

        else -> {
            if (on) SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON else SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_OFF
        }
    }
}

fun BlockRulesetEditor.addButtons() {
    buttons.forEach { block ->
        blocks(block) { interact.append { install(BUTTON_RULE) } }
    }
}
