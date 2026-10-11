package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val doors: Collection<Block> = BlockTags.getTaggedWith("minecraft:doors")

val DOOR_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:door") { ctx ->
        val block = ctx.block
        val half = block.getProperty("half") ?: return@Rule RuleResult.Next

        if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

        val position = ctx.blockPosition
        val otherHalfPos = if (half == "lower") position.add(0.0, 1.0, 0.0) else position.sub(0.0, 1.0, 0.0)
        val otherHalf = ctx.instance.getBlock(otherHalfPos)

        if (!otherHalf.compare(block) || otherHalf.getProperty("half") == half) return@Rule RuleResult.Next

        val newOpen = (block.getProperty("open") != "true").toString()
        val updated = block.withProperty("open", newOpen)

        ctx.instance.setBlock(position, updated.withProperty("half", half))
        ctx.instance.setBlock(
            otherHalfPos,
            updated.withProperty("half", if (half == "lower") "upper" else "lower"),
        )

        ctx.instance.playSoundExcept(
            ctx.player,
            Sound.sound(doorSound(block, newOpen == "true"), Sound.Source.BLOCK, 1.0f, 1.0f),
            position,
        )

        RuleResult.Consume
    }

private fun doorSound(
    block: Block,
    open: Boolean,
): SoundEvent {
    val name = block.key().value()
    return when {
        name.startsWith("bamboo") -> if (open) SoundEvent.BLOCK_BAMBOO_WOOD_DOOR_OPEN else SoundEvent.BLOCK_BAMBOO_WOOD_DOOR_CLOSE
        name.startsWith("cherry") -> if (open) SoundEvent.BLOCK_CHERRY_WOOD_DOOR_OPEN else SoundEvent.BLOCK_CHERRY_WOOD_DOOR_CLOSE
        name.startsWith("crimson") || name.startsWith("warped") ->
            if (open) SoundEvent.BLOCK_NETHER_WOOD_DOOR_OPEN else SoundEvent.BLOCK_NETHER_WOOD_DOOR_CLOSE
        name.contains("copper") -> if (open) SoundEvent.BLOCK_COPPER_DOOR_OPEN else SoundEvent.BLOCK_COPPER_DOOR_CLOSE
        else -> if (open) SoundEvent.BLOCK_WOODEN_DOOR_OPEN else SoundEvent.BLOCK_WOODEN_DOOR_CLOSE
    }
}

fun BlockRulesetEditor.addDoors() {
    doors
        .filter { it.key().asString() != "minecraft:iron_door" }
        .forEach { block ->
            if (isWaxableCopper(block)) {
                blocks(block) { interact.append { install(DOOR_RULE) } }
            } else {
                blocks(block) { interact.append { install(DOOR_RULE) } }
            }
        }
}
