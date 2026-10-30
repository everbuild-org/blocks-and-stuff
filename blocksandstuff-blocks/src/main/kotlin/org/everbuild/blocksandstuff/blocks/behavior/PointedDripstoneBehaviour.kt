package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.PacketSendingUtils
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

private const val MAX_STALACTITE_LENGTH = 11
private const val MAX_GROWTH_LENGTH = 7
private const val MAX_STALAGMITE_DISTANCE = 10
private const val WATER_DRIP_CHANCE = 0.17578125
private const val LAVA_DRIP_CHANCE = 0.05859375
private const val GROWTH_CHANCE = 0.011377778
private const val PARTICLE_CHANCE = 0.12

class PointedDripstoneBehaviour(
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            val block = ctx.block
            if (block.getProperty("vertical_direction") != "down") return@Rule RuleResult.Next

            val above = ctx.instance.getBlock(ctx.blockPosition.add(0.0, 1.0, 0.0))
            if (above.compare(Block.POINTED_DRIPSTONE)) return@Rule RuleResult.Next

            drip(ctx)
            if (random.nextDouble() < GROWTH_CHANCE) tryGrow(ctx)
            RuleResult.Next
        }

    private fun drip(ctx: RandomTickActivityContext) {
        val tip = findTip(ctx.instance, ctx.blockPosition, MAX_STALACTITE_LENGTH) ?: return
        val tipBlock = ctx.instance.getBlock(tip)
        if (tipBlock.getProperty("waterlogged") == "true") return

        val source = ctx.blockPosition.add(0.0, 2.0, 0.0)
        val sourceBlock = ctx.instance.getBlock(source)
        val primaryFluid = dripFluid(sourceBlock)
        val (fluidSource, fluid) =
            if (primaryFluid != null) {
                source to primaryFluid
            } else {
                val support = ctx.blockPosition.add(0.0, 1.0, 0.0)
                val supportBlock = ctx.instance.getBlock(support)
                val fallback = dripFluid(supportBlock) ?: return
                support to fallback
            }
        val chance = if (fluid == DripFluid.WATER) WATER_DRIP_CHANCE else LAVA_DRIP_CHANCE

        if (random.nextDouble() < PARTICLE_CHANCE) {
            sendDripParticle(ctx.instance, tip, fluid)
        }
        if (random.nextDouble() >= chance) return

        if (fluid == DripFluid.WATER && ctx.instance.getBlock(fluidSource).compare(Block.MUD)) {
            ctx.instance.setBlock(fluidSource, Block.CLAY)
            playDripSound(ctx.instance, fluidSource, SoundEvent.BLOCK_POINTED_DRIPSTONE_DRIP_WATER)
            return
        }

        val cauldron = findCauldron(ctx.instance, tip, fluid) ?: return
        val filled =
            if (fluid == DripFluid.WATER) {
                Cauldrons.addWaterLevel(ctx.instance, cauldron)
            } else {
                Cauldrons.addLava(ctx.instance, cauldron)
            }
        if (!filled) return

        val sound =
            if (fluid == DripFluid.WATER) {
                SoundEvent.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON
            } else {
                SoundEvent.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON
            }
        playDripSound(ctx.instance, cauldron, sound)
    }

    private fun tryGrow(ctx: RandomTickActivityContext) {
        val support = ctx.instance.getBlock(ctx.blockPosition.add(0.0, 1.0, 0.0))
        val water = ctx.instance.getBlock(ctx.blockPosition.add(0.0, 2.0, 0.0))
        if (!support.compare(Block.DRIPSTONE_BLOCK) || !isWaterSource(water)) return

        val tip = findTip(ctx.instance, ctx.blockPosition, MAX_GROWTH_LENGTH) ?: return
        val tipBlock = ctx.instance.getBlock(tip)
        if (tipBlock.getProperty("waterlogged") == "true") return

        if (random.nextBoolean()) {
            growStalactite(ctx.instance, tip)
        } else {
            growStalagmite(ctx.instance, tip)
        }
    }

    private fun growStalactite(
        instance: Instance,
        tip: Point,
    ) {
        val target = tip.add(0.0, -1.0, 0.0)
        val targetBlock = instance.getBlock(target)
        if (targetBlock.compare(Block.POINTED_DRIPSTONE) &&
            targetBlock.getProperty("vertical_direction") == "up" &&
            targetBlock.getProperty("thickness") == "tip"
        ) {
            instance.setBlock(target, targetBlock.withProperty("thickness", "tip_merge"), false)
            instance.setBlock(tip, instance.getBlock(tip).withProperty("thickness", "tip_merge"), false)
            return
        }
        if (!targetBlock.air() && !targetBlock.compare(Block.WATER)) return

        val replacement =
            Block.POINTED_DRIPSTONE
                .withProperty("vertical_direction", "down")
                .withProperty("thickness", "tip")
                .withProperty("waterlogged", targetBlock.compare(Block.WATER).toString())
        instance.setBlock(target, replacement)
    }

    private fun growStalagmite(
        instance: Instance,
        tip: Point,
    ) {
        var position = tip.add(0.0, -1.0, 0.0)
        repeat(MAX_STALAGMITE_DISTANCE) {
            val block = instance.getBlock(position)
            if (block.compare(Block.POINTED_DRIPSTONE) &&
                block.getProperty("vertical_direction") == "up" &&
                block.getProperty("thickness") == "tip"
            ) {
                growStalagmiteAt(instance, position)
                return
            }

            val below = instance.getBlock(position.add(0.0, -1.0, 0.0))
            if ((block.air() || block.compare(Block.WATER)) && isSupport(below)) {
                if (placeStalagmite(instance, position, block)) return
            }

            if (!canDripThrough(block)) return
            position = position.add(0.0, -1.0, 0.0)
        }
    }

    private fun growStalagmiteAt(
        instance: Instance,
        tip: Point,
    ) {
        val target = tip.add(0.0, 1.0, 0.0)
        val targetBlock = instance.getBlock(target)
        if (!targetBlock.air() && !targetBlock.compare(Block.WATER)) return
        placeStalagmite(instance, target, targetBlock)
    }

    private fun placeStalagmite(
        instance: Instance,
        position: Point,
        current: Block,
    ): Boolean {
        val replacement =
            Block.POINTED_DRIPSTONE
                .withProperty("vertical_direction", "up")
                .withProperty("thickness", "tip")
                .withProperty("waterlogged", current.compare(Block.WATER).toString())
        instance.setBlock(position, replacement)
        return true
    }

    private fun findCauldron(
        instance: Instance,
        tip: Point,
        fluid: DripFluid,
    ): Point? {
        for (distance in 1..MAX_STALACTITE_LENGTH) {
            val position = tip.add(0.0, -distance.toDouble(), 0.0)
            val block = instance.getBlock(position)
            val canFill =
                if (fluid == DripFluid.WATER) Cauldrons.canReceiveWater(block) else Cauldrons.canReceiveLava(block)
            if (canFill) return position
            if (!canDripThrough(block)) return null
        }
        return null
    }
}

fun BlockRulesetEditor.addPointedDripstone() {
    val rule = PointedDripstoneBehaviour().rule("blocksandstuff:pointed_dripstone")
    blocks(Block.POINTED_DRIPSTONE) { randomTick.append { install(rule) } }
}

private enum class DripFluid {
    WATER,
    LAVA,
}

private fun findTip(
    instance: Instance,
    start: Point,
    range: Int,
): Point? {
    var position = start
    repeat(range) {
        val block = instance.getBlock(position)
        if (!block.compare(Block.POINTED_DRIPSTONE) || block.getProperty("vertical_direction") != "down") {
            return null
        }
        when (block.getProperty("thickness")) {
            "tip" -> return position
            "tip_merge" -> return null
        }
        position = position.add(0.0, -1.0, 0.0)
    }
    return null
}

private fun canDripThrough(block: Block): Boolean {
    if (block.air()) return true
    if (block.liquid()) return false
    return !block.collisionShape().isFaceFull(BlockFace.TOP)
}

private fun dripFluid(block: Block): DripFluid? =
    when {
        block.compare(Block.MUD) -> DripFluid.WATER
        isWaterSource(block) -> DripFluid.WATER
        isLavaSource(block) -> DripFluid.LAVA
        block.getProperty("waterlogged") == "true" -> DripFluid.WATER
        else -> null
    }

private fun isWaterSource(block: Block): Boolean = block.compare(Block.WATER) && (block.getProperty("level") ?: "0") == "0"

private fun isLavaSource(block: Block): Boolean = block.compare(Block.LAVA)

private fun isSupport(block: Block): Boolean = block.collisionShape().isFaceFull(BlockFace.TOP) || block.compare(Block.POINTED_DRIPSTONE)

private fun sendDripParticle(
    instance: Instance,
    tip: Point,
    fluid: DripFluid,
) {
    val particle = if (fluid == DripFluid.WATER) Particle.DRIPPING_DRIPSTONE_WATER else Particle.DRIPPING_DRIPSTONE_LAVA
    PacketSendingUtils.sendPacket(
        instance,
        ParticlePacket(
            particle,
            tip.x() + 0.5,
            tip.y() + 0.5,
            tip.z() + 0.5,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            1,
        ),
    )
}

private fun playDripSound(
    instance: Instance,
    position: Point,
    event: SoundEvent,
) {
    instance.playSoundExcept(null, Sound.sound(event, Sound.Source.BLOCK, 1.0f, 1.0f), position)
}
