package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.GameMode
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.Compostable
import net.minestom.server.loot.number.ResolvableInt
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.PacketSendingUtils
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import org.everbuild.twaddle.stacked_behaviour_block.type.tick
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.math.min

private const val READY_LEVEL = 8
private const val FULL_LEVEL = 7
private const val READY_DELAY_TICKS = 20
private val readyTimers = ConcurrentHashMap<ComposterKey, Int>()

private data class ComposterKey(
    val instance: Instance,
    val position: Point,
)

private val layerChances =
    mapOf(
        Key.key("minecraft:compostable/low") to 0.3,
        Key.key("minecraft:compostable/low_medium") to 0.5,
        Key.key("minecraft:compostable/medium") to 0.65,
        Key.key("minecraft:compostable/medium_high") to 0.85,
        Key.key("minecraft:compostable/always_add_one") to 1.0,
    )

private data class CompostLayers(
    val amount: Int,
    val chance: Double,
)

val COMPOSTER_INTERACT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:composter_interact") { ctx ->
        val level = ctx.block.getProperty("level")?.toIntOrNull() ?: return@Rule RuleResult.Next

        when {
            level == READY_LEVEL -> {
                DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ItemStack.of(Material.BONE_MEAL))
                readyTimers.remove(ComposterKey(ctx.instance, ctx.blockPosition))
                ctx.instance.setBlock(ctx.blockPosition, ctx.block.withProperty("level", "0"))
                playSound(ctx, SoundEvent.BLOCK_COMPOSTER_EMPTY)
                RuleResult.Consume
            }

            level == FULL_LEVEL -> {
                RuleResult.Next
            }

            else -> {
                val item = ctx.player.getItemInHand(ctx.hand)
                val compostable = item.get(DataComponents.COMPOSTABLE) ?: return@Rule RuleResult.Next
                val layers = layersFor(compostable)
                val success = level == 0 || ThreadLocalRandom.current().nextDouble() < layers.chance

                consumeItem(ctx)
                if (!success) {
                    playSound(ctx, SoundEvent.BLOCK_COMPOSTER_FILL)
                    return@Rule RuleResult.Consume
                }

                val added = if (level == 0) max(1, layers.amount) else layers.amount
                val newLevel = min(FULL_LEVEL, level + added)
                if (newLevel == FULL_LEVEL) readyTimers[ComposterKey(ctx.instance, ctx.blockPosition)] = 0
                ctx.replaceBlock(ctx.block.withProperty("level", newLevel.toString()))
                playSound(ctx, SoundEvent.BLOCK_COMPOSTER_FILL_SUCCESS)
                playParticles(ctx)
                RuleResult.Consume
            }
        }
    }

val COMPOSTER_TICK_RULE: Rule<TickActivityContext> =
    Rule("blocksandstuff:composter_tick") { ctx ->
        val key = ComposterKey(ctx.instance, ctx.blockPosition)
        val level = ctx.block.getProperty("level")?.toIntOrNull() ?: return@Rule RuleResult.Next
        if (level != FULL_LEVEL) {
            readyTimers.remove(key)
            return@Rule RuleResult.Next
        }

        val elapsed = readyTimers.merge(key, 1, Int::plus) ?: 1
        if (elapsed < READY_DELAY_TICKS) return@Rule RuleResult.Next

        readyTimers.remove(key)
        ctx.instance.setBlock(ctx.blockPosition, ctx.block.withProperty("level", READY_LEVEL.toString()))
        playSound(ctx, SoundEvent.BLOCK_COMPOSTER_READY)
        RuleResult.Next
    }

val COMPOSTER_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:composter_destroy") { ctx ->
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next
        readyTimers.remove(ComposterKey(ctx.instance, ctx.blockPosition))
        if (ctx.block.getProperty("level") == READY_LEVEL.toString()) {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ItemStack.of(Material.BONE_MEAL))
        }
        RuleResult.Next
    }

fun BlockRulesetEditor.addComposters() {
    blocks(Block.COMPOSTER) {
        interact.append { install(COMPOSTER_INTERACT_RULE) }
        tick.append { install(COMPOSTER_TICK_RULE) }
        destroy.append { install(COMPOSTER_DESTROY_RULE) }
    }
}

private fun layersFor(compostable: Compostable): CompostLayers =
    when (val layers = compostable.layers()) {
        is ResolvableInt.Constant -> CompostLayers(layers.value().coerceAtLeast(0), 1.0)
        is ResolvableInt.Reference -> CompostLayers(1, layerChances[layers.key()] ?: 1.0)
    }

private fun consumeItem(ctx: InteractionActivityContext) {
    if (ctx.player.gameMode == GameMode.CREATIVE) return
    val item = ctx.player.getItemInHand(ctx.hand)
    ctx.player.setItemInHand(
        ctx.hand,
        if (item.amount() <= 1) ItemStack.AIR else item.withAmount(item.amount() - 1),
    )
}

private fun playSound(
    ctx: InteractionActivityContext,
    sound: SoundEvent,
) {
    ctx.instance.playSoundExcept(
        null,
        Sound.sound(sound, Sound.Source.BLOCK, 1.0f, 1.0f),
        ctx.blockPosition,
    )
}

private fun playSound(
    ctx: TickActivityContext,
    sound: SoundEvent,
) {
    ctx.instance.playSoundExcept(
        null,
        Sound.sound(sound, Sound.Source.BLOCK, 1.0f, 1.0f),
        ctx.blockPosition,
    )
}

private fun playParticles(ctx: InteractionActivityContext) {
    PacketSendingUtils.sendPacket(
        ctx.instance,
        ParticlePacket(
            Particle.COMPOSTER,
            ctx.blockPosition.x() + 0.5,
            ctx.blockPosition.y() + 1.0,
            ctx.blockPosition.z() + 0.5,
            0.35f,
            0.35f,
            0.35f,
            0.0f,
            8,
        ),
    )
}
