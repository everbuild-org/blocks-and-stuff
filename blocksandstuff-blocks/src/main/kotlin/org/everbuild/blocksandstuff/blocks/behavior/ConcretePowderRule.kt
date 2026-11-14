package org.everbuild.blocksandstuff.blocks.behavior

import java.util.concurrent.ThreadLocalRandom
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.place
import org.everbuild.twaddle.stacked_behaviour_block.type.tick

private val concretePowders: Collection<Block> = BlockTags.getTaggedWith("minecraft:concrete_powders")

val CONCRETE_POWDER_PLACE_RULE: Rule<PlacementActivityContext> =
    Rule("blocksandstuff:concrete_powder_place") { ctx ->
        if (touchesWater(ctx.instance, ctx.blockPosition)) {
            val solid = solidify(ctx.block)
            ctx.instance.scheduler().scheduleNextTick {
                ctx.instance.setBlock(ctx.blockPosition, solid)
            }
        }
        RuleResult.Next
    }

val CONCRETE_POWDER_TICK_RULE: Rule<TickActivityContext> =
    Rule("blocksandstuff:concrete_powder_tick") { ctx ->
        if (ThreadLocalRandom.current().nextInt(5) == 0 && touchesWater(ctx.instance, ctx.blockPosition)) {
            ctx.instance.setBlock(ctx.blockPosition, solidify(ctx.block))
        }
        RuleResult.Next
    }

val CONCRETE_POWDER_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:concrete_powder_destroy") { ctx ->
        checkNeighboringConcretePowder(ctx.instance, ctx.blockPosition)
        RuleResult.Next
    }

fun BlockRulesetEditor.addConcretePowders() {
    concretePowders.forEach { block ->
        blocks(block) {
            place.append { install(CONCRETE_POWDER_PLACE_RULE) }
            tick.append { install(CONCRETE_POWDER_TICK_RULE) }
            destroy.append { install(CONCRETE_POWDER_DESTROY_RULE) }
        }
    }
}

private fun neighbors(position: Point): List<Point> =
    BlockFace.entries.map { position.add(it.toDirection().vec()) }

private fun touchesWater(instance: Instance, position: Point): Boolean =
    neighbors(position).any { instance.getBlock(it).isWater() }

private fun checkNeighboringConcretePowder(instance: Instance, position: Point) {
    for (neighborPos in neighbors(position)) {
        val neighbor = instance.getBlock(neighborPos)
        if (neighbor.name().endsWith("_concrete_powder") && touchesWater(instance, neighborPos)) {
            instance.setBlock(neighborPos, solidify(neighbor))
        }
    }
}

private fun solidify(powderBlock: Block): Block {
    val concreteName = powderBlock.name().replace("_concrete_powder", "_concrete")
    return Block.fromKey(concreteName) ?: powderBlock
}
