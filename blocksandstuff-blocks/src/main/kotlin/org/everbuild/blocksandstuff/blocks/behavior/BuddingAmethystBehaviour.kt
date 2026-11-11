package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

private const val AMETHYST_GROWTH_CHANCE = 5

/**
 * Vanilla-like budding amethyst growth: on each random tick a random face grows or advances one
 * amethyst bud/cluster stage.
 */
class BuddingAmethystBehaviour(
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            if (random.nextInt(AMETHYST_GROWTH_CHANCE) != 0) return@Rule RuleResult.Next

            val face = BlockFace.entries[random.nextInt(BlockFace.entries.size)]
            val targetPosition = ctx.blockPosition.relative(face)
            val target = ctx.instance.getBlock(targetPosition)
            val next =
                when {
                    canGrowInto(target) -> Block.SMALL_AMETHYST_BUD
                    target.compare(Block.SMALL_AMETHYST_BUD) -> Block.MEDIUM_AMETHYST_BUD
                    target.compare(Block.MEDIUM_AMETHYST_BUD) -> Block.LARGE_AMETHYST_BUD
                    target.compare(Block.LARGE_AMETHYST_BUD) -> Block.AMETHYST_CLUSTER
                    else -> return@Rule RuleResult.Next
                }

            val waterlogged = target.getProperty("waterlogged") ?: isWaterSource(target).toString()
            ctx.instance.setBlock(
                targetPosition,
                next
                    .withProperty("facing", facingValue(face))
                    .withProperty("waterlogged", waterlogged),
            )

            val pitch = 0.5f + random.nextFloat()
            ctx.instance.playSoundExcept(
                null,
                Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_CHIME, Sound.Source.BLOCK, pitch, pitch),
                ctx.blockPosition,
            )
            RuleResult.Next
        }
}

internal val AMETHYST_DROP_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:amethyst_drop") { ctx ->
        if (ctx.player == null) return@Rule RuleResult.Next

        if (ctx.block.compare(Block.AMETHYST_CLUSTER)) {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ItemStack.of(Material.AMETHYST_SHARD, 4))
        } else {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ctx.block)
        }
        RuleResult.Next
    }

private fun facingValue(face: BlockFace): String =
    when (face) {
        BlockFace.TOP -> "up"
        BlockFace.BOTTOM -> "down"
        else -> face.name.lowercase()
    }

private fun canGrowInto(block: Block): Boolean = block.air() || isWaterSource(block)

private fun isWaterSource(block: Block): Boolean =
    block.compare(Block.WATER) && (block.getProperty("level") ?: "0") == "0"

fun BlockRulesetEditor.addBuddingAmethyst() {
    val rule = BuddingAmethystBehaviour().rule("blocksandstuff:budding_amethyst")
    blocks(Block.BUDDING_AMETHYST) { randomTick.append { install(rule) } }

    amethystBlocks().forEach { block ->
        blocks(block) { destroy.append { install(AMETHYST_DROP_RULE) } }
    }
}

private fun amethystBlocks(): List<Block> =
    listOf(
        Block.AMETHYST_CLUSTER,
        Block.SMALL_AMETHYST_BUD,
        Block.MEDIUM_AMETHYST_BUD,
        Block.LARGE_AMETHYST_BUD,
    )
