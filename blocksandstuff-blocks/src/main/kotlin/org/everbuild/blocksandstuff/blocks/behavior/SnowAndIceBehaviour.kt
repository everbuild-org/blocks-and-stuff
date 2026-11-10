package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.component.DataComponents
import net.minestom.server.instance.block.Block
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick

private const val MELT_LIGHT_LEVEL = 12
private val SILK_TOUCH: RegistryKey<Enchantment> = RegistryKey.of("silk_touch")

val ICE_MELT_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:ice_melt") { ctx ->
        if (!ctx.block.compare(Block.ICE)) return@Rule RuleResult.Next
        if (!isBright(ctx)) return@Rule RuleResult.Next

        ctx.replaceBlock(Block.WATER)
        RuleResult.Next
    }

val FROSTED_ICE_MELT_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:frosted_ice_melt") { ctx ->
        if (!ctx.block.compare(Block.FROSTED_ICE)) return@Rule RuleResult.Next

        val age = ctx.block.getProperty("age")?.toIntOrNull() ?: 0
        ctx.replaceBlock(
            if (age >= 3) Block.WATER else ctx.block.withProperty("age", (age + 1).toString()),
        )
        RuleResult.Next
    }

val SNOW_MELT_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:snow_melt") { ctx ->
        if (!ctx.block.compare(Block.SNOW)) return@Rule RuleResult.Next
        if (!isBright(ctx)) return@Rule RuleResult.Next

        val layers = ctx.block.getProperty("layers")?.toIntOrNull() ?: 1
        ctx.replaceBlock(
            if (layers <= 1) Block.AIR else ctx.block.withProperty("layers", (layers - 1).toString()),
        )
        RuleResult.Next
    }

private fun isBright(ctx: RandomTickActivityContext): Boolean =
    ctx.instance.getBlockLight(ctx.blockPosition.blockX(), ctx.blockPosition.blockY(), ctx.blockPosition.blockZ()) >=
        MELT_LIGHT_LEVEL

val ICE_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:ice_destroy") { ctx ->
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next
        if (ctx.newBlock.compare(Block.WATER)) return@Rule RuleResult.Next

        when {
            ctx.block.compare(Block.ICE) && hasSilkTouch(ctx) -> {
                DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ctx.block)
            }

            ctx.block.compare(Block.ICE) -> {
                ctx.instance.scheduler().scheduleNextTick {
                    if (ctx.instance.getBlock(ctx.blockPosition).air()) {
                        ctx.instance.setBlock(ctx.blockPosition, Block.WATER)
                    }
                }
            }

            ctx.block.compare(Block.PACKED_ICE) || ctx.block.compare(Block.BLUE_ICE) -> {
                DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ctx.block)
            }
        }
        RuleResult.Next
    }

private fun hasSilkTouch(ctx: DestroyActivityContext): Boolean =
    hasSilkTouch(ctx.player?.itemInMainHand)

private fun hasSilkTouch(item: net.minestom.server.item.ItemStack?): Boolean =
    item?.get(DataComponents.ENCHANTMENTS)?.has(SILK_TOUCH) == true

object IceBreakListener {
    private var enabled = false

    fun enable() {
        if (enabled) return
        enabled = true

        net.minestom.server.MinecraftServer.getGlobalEventHandler().addListener(
            net.minestom.server.event.player.PlayerBlockBreakEvent::class.java,
        ) { event ->
            if (event.isCancelled) return@addListener
            if (!event.block.compare(Block.ICE)) return@addListener
            if (hasSilkTouch(event.player.itemInMainHand)) return@addListener

            val instance = event.instance
            val position = event.blockPosition
            instance.scheduler().scheduleNextTick {
                if (instance.getBlock(position).air()) {
                    instance.setBlock(position, Block.WATER)
                }
            }
        }
    }
}

fun BlockRulesetEditor.addSnowAndIce() {
    blocks(Block.ICE) {
        randomTick.append { install(ICE_MELT_RULE) }
        destroy.append { install(ICE_DESTROY_RULE) }
    }
    blocks(Block.FROSTED_ICE) { randomTick.append { install(FROSTED_ICE_MELT_RULE) } }
    blocks(Block.SNOW) { randomTick.append { install(SNOW_MELT_RULE) } }
    listOf(Block.PACKED_ICE, Block.BLUE_ICE).forEach { block ->
        blocks(block) { destroy.append { install(ICE_DESTROY_RULE) } }
    }
}
