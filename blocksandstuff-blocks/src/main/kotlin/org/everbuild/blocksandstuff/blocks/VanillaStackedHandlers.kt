package org.everbuild.blocksandstuff.blocks

import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.item.MaterialTags
import org.everbuild.twaddle.stacked_block_behaviour.Rule
import org.everbuild.twaddle.stacked_block_behaviour.RuleResult
import org.everbuild.twaddle.stacked_block_behaviour.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext

private fun Block.asVariant(variant: Block): Block =
    variant
        .withProperties(properties())
        .withNbt(nbt())
        .withHandler(handler())

private val scrapeTargets = mapOf(
    Block.EXPOSED_COPPER_TRAPDOOR.key() to Block.COPPER_TRAPDOOR,
    Block.WEATHERED_COPPER_TRAPDOOR.key() to Block.EXPOSED_COPPER_TRAPDOOR,
    Block.OXIDIZED_COPPER_TRAPDOOR.key() to Block.WEATHERED_COPPER_TRAPDOOR,

    Block.WAXED_COPPER_TRAPDOOR.key() to Block.COPPER_TRAPDOOR,
    Block.WAXED_EXPOSED_COPPER_TRAPDOOR.key() to Block.EXPOSED_COPPER_TRAPDOOR,
    Block.WAXED_WEATHERED_COPPER_TRAPDOOR.key() to Block.WEATHERED_COPPER_TRAPDOOR,
    Block.WAXED_OXIDIZED_COPPER_TRAPDOOR.key() to Block.OXIDIZED_COPPER_TRAPDOOR,
)

private val waxTargets = mapOf(
    Block.COPPER_TRAPDOOR.key() to Block.WAXED_COPPER_TRAPDOOR,
    Block.EXPOSED_COPPER_TRAPDOOR.key() to Block.WAXED_EXPOSED_COPPER_TRAPDOOR,
    Block.WEATHERED_COPPER_TRAPDOOR.key() to Block.WAXED_WEATHERED_COPPER_TRAPDOOR,
    Block.OXIDIZED_COPPER_TRAPDOOR.key() to Block.WAXED_OXIDIZED_COPPER_TRAPDOOR,
)

private val axes = Material.staticRegistry().getTag(MaterialTags.AXES)!!

val ScrapeCopper: Rule<InteractionActivityContext> =
    Rule(Rule.Key("twaddle:scrape_copper")) { context ->
        val held = context.player.getItemInHand(context.hand)
        val target = scrapeTargets[context.block.key()]

        if (held.material() !in axes || target == null) {
            RuleResult.next()
        } else {
            context.replaceBlock(context.block.asVariant(target))
            RuleResult.consume()
        }
    }

val WaxCopper: Rule<InteractionActivityContext> =
    Rule(Rule.Key("twaddle:wax_copper")) { context ->
        val held = context.player.getItemInHand(context.hand)
        val target = waxTargets[context.block.key()]

        if (held.material() != Material.HONEYCOMB || target == null) {
            RuleResult.next()
        } else {
            context.replaceBlock(context.block.asVariant(target))
            RuleResult.consume()
        }
    }

val OpenTrapdoor: Rule<InteractionActivityContext> =
    Rule(Rule.Key("twaddle:open_trapdoor")) { context ->
        if (context.player.isSneaking) {
            RuleResult.next()
        } else {
            val currentlyOpen = context.block.getProperty("open") == "true"
            context.replaceBlock(
                context.block.withProperty("open", (!currentlyOpen).toString())
            )

            RuleResult.consume()
        }
    }

val copperTrapdoorVariants = listOf(
    Block.COPPER_TRAPDOOR,
    Block.EXPOSED_COPPER_TRAPDOOR,
    Block.WEATHERED_COPPER_TRAPDOOR,
    Block.OXIDIZED_COPPER_TRAPDOOR,
    Block.WAXED_COPPER_TRAPDOOR,
    Block.WAXED_EXPOSED_COPPER_TRAPDOOR,
    Block.WAXED_WEATHERED_COPPER_TRAPDOOR,
    Block.WAXED_OXIDIZED_COPPER_TRAPDOOR,
)

object VanillaStackedHandlers {
    val ruleset = BlockRuleset.build {
        for (block in copperTrapdoorVariants) {
            addBlock(block) {
                interact.replace {
                    install(OpenTrapdoor)
                    install(ScrapeCopper)
                    install(WaxCopper)
                }
            }
        }
    }
}