package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.blocks.event.CopperOxidationEvent
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.concurrent.ThreadLocalRandom

val COPPER_OXIDATION_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:copper_oxidation") { ctx ->
        val exposedToAir = countExposedSides(ctx.instance, ctx.blockPosition) > 0
        if (!exposedToAir) return@Rule RuleResult.Next
        if (ThreadLocalRandom.current().nextInt(100) >= 5) return@Rule RuleResult.Next

        val block = ctx.block
        var nextStage = oxidationStages[block.defaultState()] ?: return@Rule RuleResult.Next
        nextStage = nextStage.copyPropertiesFrom(block)
        val handler = MinecraftServer.getBlockManager().getHandler(nextStage.key().asString())
        if (handler != null) {
            nextStage = nextStage.withHandler(handler)
        }

        val event = CopperOxidationEvent(block, nextStage, ctx.blockPosition, ctx.instance)
        EventDispatcher.call(event)
        if (event.isCancelled) return@Rule RuleResult.Next

        ctx.replaceBlock(nextStage)
        RuleResult.Next
    }

private fun countExposedSides(
    instance: Instance,
    blockPosition: Point,
): Int {
    var exposedSides = 0
    for (face in BlockFace.entries) {
        val neighborPos = blockPosition.add(face.toDirection().vec())
        if (instance.getBlock(neighborPos) == Block.AIR) exposedSides++
    }
    return exposedSides
}

private fun Block.copyPropertiesFrom(source: Block): Block {
    val carried = source.properties().filterKeys { getProperty(it) != null }
    return if (carried.isEmpty()) this else withProperties(carried)
}

private val oxidationStages =
    mapOf(
        Block.COPPER_BLOCK to Block.EXPOSED_COPPER,
        Block.EXPOSED_COPPER to Block.WEATHERED_COPPER,
        Block.WEATHERED_COPPER to Block.OXIDIZED_COPPER,
        Block.CUT_COPPER to Block.EXPOSED_CUT_COPPER,
        Block.EXPOSED_CUT_COPPER to Block.WEATHERED_CUT_COPPER,
        Block.WEATHERED_CUT_COPPER to Block.OXIDIZED_CUT_COPPER,
        Block.CUT_COPPER_STAIRS to Block.EXPOSED_CUT_COPPER_STAIRS,
        Block.EXPOSED_CUT_COPPER_STAIRS to Block.WEATHERED_CUT_COPPER_STAIRS,
        Block.WEATHERED_CUT_COPPER_STAIRS to Block.OXIDIZED_CUT_COPPER_STAIRS,
        Block.CUT_COPPER_SLAB to Block.EXPOSED_CUT_COPPER_SLAB,
        Block.EXPOSED_CUT_COPPER_SLAB to Block.WEATHERED_CUT_COPPER_SLAB,
        Block.WEATHERED_CUT_COPPER_SLAB to Block.OXIDIZED_CUT_COPPER_SLAB,
        Block.CHISELED_COPPER to Block.EXPOSED_CHISELED_COPPER,
        Block.EXPOSED_CHISELED_COPPER to Block.WEATHERED_CHISELED_COPPER,
        Block.WEATHERED_CHISELED_COPPER to Block.OXIDIZED_CHISELED_COPPER,
        Block.COPPER_GRATE to Block.EXPOSED_COPPER_GRATE,
        Block.EXPOSED_COPPER_GRATE to Block.WEATHERED_COPPER_GRATE,
        Block.WEATHERED_COPPER_GRATE to Block.OXIDIZED_COPPER_GRATE,
        Block.COPPER_BULB to Block.EXPOSED_COPPER_BULB,
        Block.EXPOSED_COPPER_BULB to Block.WEATHERED_COPPER_BULB,
        Block.WEATHERED_COPPER_BULB to Block.OXIDIZED_COPPER_BULB,
        Block.COPPER_BARS to Block.EXPOSED_COPPER_BARS,
        Block.EXPOSED_COPPER_BARS to Block.WEATHERED_COPPER_BARS,
        Block.WEATHERED_COPPER_BARS to Block.OXIDIZED_COPPER_BARS,
        Block.COPPER_CHAIN to Block.EXPOSED_COPPER_CHAIN,
        Block.EXPOSED_COPPER_CHAIN to Block.WEATHERED_COPPER_CHAIN,
        Block.WEATHERED_COPPER_CHAIN to Block.OXIDIZED_COPPER_CHAIN,
        Block.COPPER_LANTERN to Block.EXPOSED_COPPER_LANTERN,
        Block.EXPOSED_COPPER_LANTERN to Block.WEATHERED_COPPER_LANTERN,
        Block.WEATHERED_COPPER_LANTERN to Block.OXIDIZED_COPPER_LANTERN,
        Block.COPPER_DOOR to Block.EXPOSED_COPPER_DOOR,
        Block.EXPOSED_COPPER_DOOR to Block.WEATHERED_COPPER_DOOR,
        Block.WEATHERED_COPPER_DOOR to Block.OXIDIZED_COPPER_DOOR,
        Block.COPPER_TRAPDOOR to Block.EXPOSED_COPPER_TRAPDOOR,
        Block.EXPOSED_COPPER_TRAPDOOR to Block.WEATHERED_COPPER_TRAPDOOR,
        Block.WEATHERED_COPPER_TRAPDOOR to Block.OXIDIZED_COPPER_TRAPDOOR,
        Block.COPPER_GOLEM_STATUE to Block.EXPOSED_COPPER_GOLEM_STATUE,
        Block.EXPOSED_COPPER_GOLEM_STATUE to Block.WEATHERED_COPPER_GOLEM_STATUE,
        Block.WEATHERED_COPPER_GOLEM_STATUE to Block.OXIDIZED_COPPER_GOLEM_STATUE,
        Block.LIGHTNING_ROD to Block.EXPOSED_LIGHTNING_ROD,
        Block.EXPOSED_LIGHTNING_ROD to Block.WEATHERED_LIGHTNING_ROD,
        Block.WEATHERED_LIGHTNING_ROD to Block.OXIDIZED_LIGHTNING_ROD,
    )

fun BlockRulesetEditor.addCopperOxidation() {
    oxidationStages.keys.forEach { block ->
        blocks(block) { randomTick.append { install(COPPER_OXIDATION_RULE) } }
    }
}
