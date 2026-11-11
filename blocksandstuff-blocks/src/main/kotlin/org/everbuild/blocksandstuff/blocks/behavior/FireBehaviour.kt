package org.everbuild.blocksandstuff.blocks.behavior

import java.util.Random
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.tick

private const val FIRE_MAX_AGE = 15
private const val FIRE_TICK_CHANCE = 20
private const val FIRE_SPREAD_CHANCE = 3

val FIRE_TICK_RULE: Rule<TickActivityContext> =
    Rule("blocksandstuff:fire_tick") { ctx ->
        if (!InstanceOptionsProvider.getForInstance(ctx.instance).fireTick) return@Rule RuleResult.Next
        if (ctx.block.compare(Block.SOUL_FIRE)) return@Rule RuleResult.Next

        val random = Random()
        if (random.nextInt(FIRE_TICK_CHANCE) != 0) return@Rule RuleResult.Next

        if (!canFireSurvive(ctx.instance, ctx.blockPosition)) {
            ctx.replaceBlock(Block.AIR)
            return@Rule RuleResult.Next
        }

        val age = ctx.block.getProperty("age")?.toIntOrNull() ?: 0
        val hasFuel = hasEternalFuel(ctx.instance, ctx.blockPosition) || hasFlammableNeighbor(ctx.instance, ctx.blockPosition)
        if (age >= FIRE_MAX_AGE && !hasFuel) {
            ctx.replaceBlock(Block.AIR)
            return@Rule RuleResult.Next
        }

        meltAdjacentIce(ctx)

        if (random.nextInt(FIRE_SPREAD_CHANCE) == 0) {
            spreadFire(ctx.instance, ctx.blockPosition, random)
        }
        ctx.replaceBlock(ctx.block.withProperty("age", minOf(FIRE_MAX_AGE, age + 1).toString()))
        RuleResult.Next
    }

private fun spreadFire(
    instance: Instance,
    position: Point,
    random: Random,
) {
    val target =
        position.add(
            (random.nextInt(3) - 1).toDouble(),
            (random.nextInt(3) - 1).toDouble(),
            (random.nextInt(3) - 1).toDouble(),
        )
    val targetBlock = instance.getBlock(target)

    if (isFlammable(targetBlock)) {
        instance.setBlock(target, fireFor(instance, target))
        return
    }

    if (!targetBlock.air()) return
    if (!hasFlammableNeighbor(instance, target)) return

    instance.setBlock(target, fireFor(instance, target))
}

private fun fireFor(
    instance: Instance,
    position: Point,
): Block {
    val below = instance.getBlock(position.add(0.0, -1.0, 0.0))
    if (below.compare(Block.SOUL_SAND) || below.compare(Block.SOUL_SOIL)) return Block.SOUL_FIRE

    var fire = Block.FIRE.withProperty("age", "0")
    for (face in HORIZONTAL_FACES) {
        fire = fire.withProperty(
            face.name.lowercase(),
            isFlammable(instance.getBlock(position.relative(face))).toString(),
        )
    }
    fire = fire.withProperty(
        "up",
        isFlammable(instance.getBlock(position.add(0.0, 1.0, 0.0))).toString(),
    )
    return fire
}

private val HORIZONTAL_FACES = listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)

private val INFINIBURN_TAGS =
    listOf(
        "minecraft:infiniburn_overworld",
        "minecraft:infiniburn_nether",
        "minecraft:infiniburn_end",
    )

private fun hasEternalFuel(
    instance: Instance,
    position: Point,
): Boolean {
    val below = instance.getBlock(position.add(0.0, -1.0, 0.0))
    return INFINIBURN_TAGS.any { BlockTags.hasTag(below, it) }
}

private fun hasFlammableNeighbor(
    instance: Instance,
    position: Point,
): Boolean = BlockFace.entries.any { isFlammable(instance.getBlock(position.relative(it))) }

internal fun canFireSurvive(
    instance: Instance,
    position: Point,
): Boolean {
    val below = instance.getBlock(position.add(0.0, -1.0, 0.0))
    if (below.compare(Block.SOUL_SAND) || below.compare(Block.SOUL_SOIL)) return true
    if (below.liquid()) return false
    if (below.collisionShape().isFaceFull(BlockFace.TOP)) return true
    return BlockFace.entries.any { isFlammable(instance.getBlock(position.relative(it))) }
}

private fun meltAdjacentIce(ctx: TickActivityContext) {
    for (face in BlockFace.entries) {
        val neighbor = ctx.blockPosition.relative(face)
        if (ctx.instance.getBlock(neighbor).compare(Block.ICE)) {
            ctx.instance.setBlock(neighbor, Block.WATER)
        }
    }
}

internal fun isFlammable(block: Block): Boolean {
    if (block.air()) return false
    val name = block.name()
    return name.contains("planks") ||
        name.contains("_log") ||
        name.contains("_wood") ||
        name.contains("leaves") ||
        name.contains("wool") ||
        name.contains("carpet") ||
        name.contains("bamboo") ||
        name.contains("fence") ||
        name.contains("bookshelf") ||
        name == "minecraft:hay_block" ||
        name == "minecraft:coal_block" ||
        name == "minecraft:dead_bush" ||
        name == "minecraft:short_grass" ||
        name == "minecraft:tall_grass"
}

fun BlockRulesetEditor.addFire() {
    listOf(Block.FIRE, Block.SOUL_FIRE).forEach { block ->
        blocks(block) { tick.append { install(FIRE_TICK_RULE) } }
    }
}
