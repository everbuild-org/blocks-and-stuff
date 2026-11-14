package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.tick
import kotlin.math.ceil
import kotlin.math.min

private val pressurePlates: Collection<Block> = BlockTags.getTaggedWith("minecraft:pressure_plates")

val PRESSURE_PLATE_TICK_RULE: Rule<TickActivityContext> =
    Rule("blocksandstuff:pressure_plate_tick") { ctx ->
        val block = ctx.block
        val count = entitiesOnBlock(ctx.instance, ctx.blockPosition)

        val powered = block.getProperty("powered")
        if (powered != null) {
            val shouldPower = count > 0
            if ((powered == "true") != shouldPower) {
                ctx.replaceBlock(block.withProperty("powered", shouldPower.toString()))
                playPlateSound(ctx, block, shouldPower)
            }
            return@Rule RuleResult.Next
        }

        val power = block.getProperty("power") ?: return@Rule RuleResult.Next
        val maxWeight = maxWeight(block)
        val desired = if (count == 0) 0 else min(15, ceil(count * 15.0 / maxWeight).toInt())
        val current = power.toIntOrNull() ?: 0
        if (current != desired) {
            ctx.replaceBlock(block.withProperty("power", desired.toString()))
            if (current == 0 || desired == 0) playPlateSound(ctx, block, desired > 0)
        }

        RuleResult.Next
    }

fun BlockRulesetEditor.addPressurePlates() {
    pressurePlates.forEach { block ->
        blocks(block) { tick.append { install(PRESSURE_PLATE_TICK_RULE) } }
    }
}

private fun entitiesOnBlock(
    instance: Instance,
    position: Point,
): Int {
    val minX = position.x()
    val minY = position.y()
    val minZ = position.z()
    val maxX = minX + 1.0
    val maxY = minY + 1.0
    val maxZ = minZ + 1.0

    return instance.entities.count { entity ->
        val box = entity.boundingBox
        val origin = entity.position
        val entityMinX = origin.x() + box.minX()
        val entityMaxX = origin.x() + box.maxX()
        val entityMinY = origin.y() + box.minY()
        val entityMaxY = origin.y() + box.maxY()
        val entityMinZ = origin.z() + box.minZ()
        val entityMaxZ = origin.z() + box.maxZ()

        entityMaxX > minX && entityMinX < maxX &&
            entityMaxY > minY && entityMinY < maxY &&
            entityMaxZ > minZ && entityMinZ < maxZ
    }
}

private fun maxWeight(block: Block): Int = if (block.key().value().startsWith("heavy_weighted")) 150 else 15

private fun playPlateSound(
    ctx: TickActivityContext,
    block: Block,
    on: Boolean,
) {
    ctx.instance.playSoundExcept(
        null,
        Sound.sound(plateSound(block, on), Sound.Source.BLOCK, 0.3f, if (on) 0.6f else 0.5f),
        ctx.blockPosition,
    )
}

private fun plateSound(
    block: Block,
    on: Boolean,
): SoundEvent {
    val name = block.key().value()
    return when {
        name.startsWith("light_weighted") || name.startsWith("heavy_weighted") -> {
            if (on) SoundEvent.BLOCK_METAL_PRESSURE_PLATE_CLICK_ON else SoundEvent.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF
        }

        name.startsWith("bamboo") -> {
            if (on) SoundEvent.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_ON else SoundEvent.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_OFF
        }

        name.startsWith("cherry") -> {
            if (on) SoundEvent.BLOCK_CHERRY_WOOD_PRESSURE_PLATE_CLICK_ON else SoundEvent.BLOCK_CHERRY_WOOD_PRESSURE_PLATE_CLICK_OFF
        }

        name.startsWith("crimson") || name.startsWith("warped") -> {
            if (on) SoundEvent.BLOCK_NETHER_WOOD_PRESSURE_PLATE_CLICK_ON else SoundEvent.BLOCK_NETHER_WOOD_PRESSURE_PLATE_CLICK_OFF
        }

        name.contains("stone") -> {
            if (on) SoundEvent.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON else SoundEvent.BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF
        }

        else -> {
            if (on) SoundEvent.BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON else SoundEvent.BLOCK_WOODEN_PRESSURE_PLATE_CLICK_OFF
        }
    }
}
