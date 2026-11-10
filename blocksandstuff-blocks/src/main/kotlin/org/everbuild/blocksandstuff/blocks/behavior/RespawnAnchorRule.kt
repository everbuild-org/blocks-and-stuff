package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.util.explodeBlocks
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private const val MAX_CHARGES = 4
private const val EXPLOSION_POWER = 5
private const val NETHER_DIMENSION = "minecraft:the_nether"

val RESPAWN_ANCHOR_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:respawn_anchor") { ctx ->
        val charges = ctx.block.getProperty("charges")?.toIntOrNull() ?: 0
        val held = ctx.player.itemInMainHand

        if (held.material() == Material.GLOWSTONE && charges < MAX_CHARGES) {
            ctx.replaceBlock(ctx.block.withProperty("charges", (charges + 1).toString()))
            if (ctx.player.gameMode != GameMode.CREATIVE) {
                ctx.player.itemInMainHand = held.withAmount(held.amount() - 1)
            }
            playSoundFor(ctx, SoundEvent.BLOCK_RESPAWN_ANCHOR_CHARGE)
            return@Rule RuleResult.Consume
        }

        if (charges == 0) return@Rule RuleResult.Next

        if (ctx.instance.dimensionName != NETHER_DIMENSION) {
            explode(ctx.instance, ctx.blockPosition)
            return@Rule RuleResult.Consume
        }

        ctx.player.setRespawnPoint(
            Pos(ctx.blockPosition.x() + 0.5, ctx.blockPosition.y() + 1.0, ctx.blockPosition.z() + 0.5),
        )
        playSoundFor(ctx, SoundEvent.BLOCK_RESPAWN_ANCHOR_SET_SPAWN)
        RuleResult.Consume
    }

private fun explode(
    instance: Instance,
    center: Point,
) {
    instance.playSoundExcept(
        null,
        Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 4.0f, 1.0f),
        center,
    )
    instance.sendGroupedPacket(
        ParticlePacket(Particle.EXPLOSION_EMITTER, center, Vec.ZERO, 0.0f, 1),
    )

    explodeBlocks(instance, center, EXPLOSION_POWER)
}

fun BlockRulesetEditor.addRespawnAnchor() {
    blocks(Block.RESPAWN_ANCHOR) { interact.append { install(RESPAWN_ANCHOR_RULE) } }
}
