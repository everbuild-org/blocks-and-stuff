package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.PacketSendingUtils
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import java.util.Random

private fun boneMealAvailable(ctx: InteractionActivityContext): ItemStack? =
    ctx.player.itemInMainHand.takeIf { it.material() == Material.BONE_MEAL }

private fun consumeBoneMeal(
    player: Player,
    boneMeal: ItemStack,
) {
    if (player.gameMode == GameMode.CREATIVE) return
    player.itemInMainHand = if (boneMeal.amount() <= 1) ItemStack.AIR else boneMeal.withAmount(boneMeal.amount() - 1)
}

private fun useBoneMeal(ctx: InteractionActivityContext) {
    playSoundFor(ctx, SoundEvent.ITEM_BONE_MEAL_USE)
    PacketSendingUtils.sendPacket(
        ctx.instance,
        ParticlePacket(
            Particle.HAPPY_VILLAGER,
            ctx.blockPosition.x() + 0.5,
            ctx.blockPosition.y() + 0.5,
            ctx.blockPosition.z() + 0.5,
            0.5f,
            0.5f,
            0.5f,
            0.0f,
            6,
        ),
    )
}

val AGE_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:bone_meal_age") { ctx ->
        val boneMeal = boneMealAvailable(ctx) ?: return@Rule RuleResult.Next
        val age = ctx.block.getProperty("age")?.toIntOrNull() ?: return@Rule RuleResult.Next
        val maxAge = maxAgeOf(ctx.block)
        if (age >= maxAge) return@Rule RuleResult.Next

        val newAge = minOf(maxAge, age + 2 + Random().nextInt(4))
        ctx.replaceBlock(ctx.block.withProperty("age", newAge.toString()))
        consumeBoneMeal(ctx.player, boneMeal)
        useBoneMeal(ctx)
        RuleResult.Consume
    }

val SAPLING_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:bone_meal_sapling") { ctx ->
        val boneMeal = boneMealAvailable(ctx) ?: return@Rule RuleResult.Next
        if (!ctx.instance.getBlock(ctx.blockPosition.add(0.0, 1.0, 0.0)).air()) return@Rule RuleResult.Next
        if (!growSapling(ctx.instance, ctx.blockPosition, ctx.block, Random())) return@Rule RuleResult.Next

        consumeBoneMeal(ctx.player, boneMeal)
        useBoneMeal(ctx)
        RuleResult.Consume
    }

val GRASS_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:bone_meal_grass") { ctx ->
        val boneMeal = boneMealAvailable(ctx) ?: return@Rule RuleResult.Next
        val target = ctx.blockPosition.add(0.0, 1.0, 0.0)
        if (!ctx.instance.getBlock(target).air()) return@Rule RuleResult.Next

        ctx.instance.setBlock(target, randomVegetation())
        consumeBoneMeal(ctx.player, boneMeal)
        useBoneMeal(ctx)
        RuleResult.Consume
    }

private fun randomVegetation(): Block =
    when (Random().nextInt(8)) {
        0 -> Block.DANDELION
        1 -> Block.POPPY
        2 -> Block.CORNFLOWER
        else -> Block.SHORT_GRASS
    }

fun BlockRulesetEditor.addBoneMeal() {
    val agePlants =
        (CROP_BLOCKS +
            listOf(
                Block.NETHER_WART,
                Block.TORCHFLOWER_CROP,
                Block.PITCHER_CROP,
                Block.PUMPKIN_STEM,
                Block.MELON_STEM,
            )).distinctBy { it.key() }
    agePlants.forEach { block -> blocks(block) { interact.append { install(AGE_BONE_MEAL_RULE) } } }
    SAPLING_BLOCKS.forEach { block -> blocks(block) { interact.append { install(SAPLING_BONE_MEAL_RULE) } } }
    blocks(Block.AZALEA) { interact.append { install(SAPLING_BONE_MEAL_RULE) } }
    blocks(Block.FLOWERING_AZALEA) { interact.append { install(SAPLING_BONE_MEAL_RULE) } }
    blocks(Block.GRASS_BLOCK) { interact.append { install(GRASS_BONE_MEAL_RULE) } }
}
