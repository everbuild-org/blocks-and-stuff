package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.PacketSendingUtils
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.util.FallingBlock
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import org.everbuild.twaddle.stacked_behaviour_block.type.tick
import java.util.Random

val DRAGON_EGG_TELEPORT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:dragon_egg_teleport") { ctx ->
        if (!ctx.block.compare(Block.DRAGON_EGG)) return@Rule RuleResult.Next
        if (!teleportEgg(ctx.instance, ctx.blockPosition, Random())) return@Rule RuleResult.Next
        playSoundFor(ctx, SoundEvent.ENTITY_ENDERMAN_TELEPORT)
        RuleResult.Consume
    }

val DRAGON_EGG_BREAK_TELEPORT_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:dragon_egg_break_teleport") { ctx ->
        if (ctx.player == null) return@Rule RuleResult.Next
        if (ctx.player!!.gameMode == net.minestom.server.entity.GameMode.CREATIVE) return@Rule RuleResult.Next
        if (!ctx.block.compare(Block.DRAGON_EGG)) return@Rule RuleResult.Next
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next
        teleportEgg(ctx.instance, ctx.blockPosition, Random())
        RuleResult.Next
    }

val DRAGON_EGG_PARTICLE_RULE: Rule<TickActivityContext> =
    Rule("blocksandstuff:dragon_egg_particles") { ctx ->
        if (!ctx.block.compare(Block.DRAGON_EGG)) return@Rule RuleResult.Next
        if (Random().nextInt(5) != 0) return@Rule RuleResult.Next

        PacketSendingUtils.sendPacket(
            ctx.instance,
            ParticlePacket(
                Particle.PORTAL,
                ctx.blockPosition.x() + 0.5,
                ctx.blockPosition.y() + 0.5,
                ctx.blockPosition.z() + 0.5,
                0.5f,
                0.5f,
                0.5f,
                0.0f,
                4,
            ),
        )
        RuleResult.Next
    }

internal fun teleportEgg(
    instance: Instance,
    position: Point,
    random: Random,
): Boolean {
    repeat(16) {
        val target =
            position.add(
                random.nextInt(15) - 7.0,
                random.nextInt(7) - 3.0,
                random.nextInt(15) - 7.0,
            )
        if (!instance.isChunkLoaded(target)) return@repeat
        val block = instance.getBlock(target)
        if (!block.air() && !block.replaceable()) return@repeat

        instance.setBlock(position, Block.AIR, false)
        spawnTeleportTrail(instance, position, target)
        if (canFall(instance, target)) {
            FallingBlock.spawn(Block.DRAGON_EGG, instance, net.minestom.server.coordinate.BlockVec(target.x(), target.y(), target.z()))
        } else {
            instance.setBlock(target, Block.DRAGON_EGG)
        }
        return true
    }
    return false
}

private fun spawnTeleportTrail(
    instance: Instance,
    from: Point,
    to: Point,
) {
    val steps = 16
    for (i in 1 until steps) {
        val t = i.toDouble() / steps
        PacketSendingUtils.sendPacket(
            instance,
            ParticlePacket(
                Particle.PORTAL,
                from.x() + (to.x() - from.x()) * t,
                from.y() + (to.y() - from.y()) * t,
                from.z() + (to.z() - from.z()) * t,
                0.05f,
                0.05f,
                0.05f,
                0.0f,
                1,
            ),
        )
    }
}

private fun canFall(
    instance: Instance,
    position: Point,
): Boolean {
    val below = instance.getBlock(position.add(0.0, -1.0, 0.0))
    return below.air() || below.liquid() || below.replaceable()
}

fun BlockRulesetEditor.addDragonEgg() {
    blocks(Block.DRAGON_EGG) {
        interact.append { install(DRAGON_EGG_TELEPORT_RULE) }
        destroy.append { install(DRAGON_EGG_BREAK_TELEPORT_RULE) }
        tick.append { install(DRAGON_EGG_PARTICLE_RULE) }
    }
}
