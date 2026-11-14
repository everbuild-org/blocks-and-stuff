package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.GameMode
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import java.util.Random

private const val PATCH_RADIUS = 3

private fun boneMeal(ctx: InteractionActivityContext): ItemStack? =
    ctx.player.itemInMainHand.takeIf { it.material() == Material.BONE_MEAL }

private fun consumeBoneMeal(
    ctx: InteractionActivityContext,
    item: ItemStack,
) {
    if (ctx.player.gameMode == GameMode.CREATIVE) return
    ctx.player.itemInMainHand = if (item.amount() <= 1) ItemStack.AIR else item.withAmount(item.amount() - 1)
}

private fun successfulBoneMeal(
    ctx: InteractionActivityContext,
    item: ItemStack,
    random: Random,
    action: (Point) -> Boolean,
): RuleResult<InteractionActivityContext> {
    var grown = false
    repeat(24) {
        val target =
            ctx.blockPosition.add(
                (random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS).toDouble(),
                (random.nextInt(3) - 1).toDouble(),
                (random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS).toDouble(),
            )
        if (action(target)) grown = true
    }
    if (!grown) return RuleResult.Next
    consumeBoneMeal(ctx, item)
    playSoundFor(ctx, SoundEvent.ITEM_BONE_MEAL_USE)
    return RuleResult.Consume
}

val NYLIUM_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:nylium_bone_meal") { ctx ->
        val crimson =
            when {
                ctx.block.compare(Block.CRIMSON_NYLIUM) -> true
                ctx.block.compare(Block.WARPED_NYLIUM) -> false
                else -> return@Rule RuleResult.Next
            }
        val item = boneMeal(ctx) ?: return@Rule RuleResult.Next
        val random = Random()

        successfulBoneMeal(ctx, item, random) { target ->
            if (!ctx.instance.getBlock(target).air()) return@successfulBoneMeal false
            val ground = ctx.instance.getBlock(target.sub(0.0, 1.0, 0.0))
            if (!ground.compare(Block.CRIMSON_NYLIUM) && !ground.compare(Block.WARPED_NYLIUM) && !ground.compare(Block.NETHERRACK)) {
                return@successfulBoneMeal false
            }
            ctx.instance.setBlock(target, netherFlora(crimson, random))
            true
        }
    }

val NETHERRACK_NYLIUM_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:netherrack_nylium_bone_meal") { ctx ->
        if (!ctx.block.compare(Block.NETHERRACK)) return@Rule RuleResult.Next
        val item = boneMeal(ctx) ?: return@Rule RuleResult.Next
        val random = Random()
        val nearby = adjacentNylium(ctx.instance, ctx.blockPosition) ?: return@Rule RuleResult.Next

        successfulBoneMeal(ctx, item, random) { target ->
            val ground = ctx.instance.getBlock(target.sub(0.0, 1.0, 0.0))
            if (!ground.compare(Block.NETHERRACK)) return@successfulBoneMeal false
            ctx.instance.setBlock(target.sub(0.0, 1.0, 0.0), nearby)
            if (ctx.instance.getBlock(target).air()) {
                ctx.instance.setBlock(target, netherFlora(nearby.compare(Block.CRIMSON_NYLIUM), random))
            }
            true
        }
    }

val MOSS_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:moss_bone_meal") { ctx ->
        if (!ctx.block.compare(Block.MOSS_BLOCK)) return@Rule RuleResult.Next
        val item = boneMeal(ctx) ?: return@Rule RuleResult.Next
        val random = Random()

        successfulBoneMeal(ctx, item, random) { target ->
            val below = ctx.instance.getBlock(target.sub(0.0, 1.0, 0.0))
            if (isMossable(below)) {
                ctx.instance.setBlock(target.sub(0.0, 1.0, 0.0), Block.MOSS_BLOCK)
                if (ctx.instance.getBlock(target).air()) {
                    ctx.instance.setBlock(target, mossFlora(random))
                }
                return@successfulBoneMeal true
            }
            false
        }
    }

val RED_SHRUB_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:red_shrub_bone_meal") { ctx ->
        spreadPlantLine(ctx, Block.RED_SHRUB)
    }

val SHORT_DRY_GRASS_BONE_MEAL_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:short_dry_grass_bone_meal") { ctx ->
        spreadPlantLine(ctx, Block.SHORT_DRY_GRASS)
    }

private fun spreadPlantLine(
    ctx: InteractionActivityContext,
    plant: Block,
): RuleResult<InteractionActivityContext> {
    if (!ctx.block.compare(plant)) return RuleResult.Next
    val item = boneMeal(ctx) ?: return RuleResult.Next
    val random = Random()
    val directions = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0).shuffled(random)

    for ((dx, dz) in directions) {
        val target = ctx.blockPosition.add(dx.toDouble(), 0.0, dz.toDouble())
        if (!ctx.instance.getBlock(target).air()) continue
        val ground = ctx.instance.getBlock(target.sub(0.0, 1.0, 0.0))
        if (ground.air() || ground.liquid()) continue
        ctx.instance.setBlock(target, plant)
        consumeBoneMeal(ctx, item)
        playSoundFor(ctx, SoundEvent.ITEM_BONE_MEAL_USE)
        return RuleResult.Consume
    }
    return RuleResult.Next
}

val FUNGUS_GROWTH_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:fungus_growth") { ctx ->
        val crimson =
            when {
                ctx.block.compare(Block.CRIMSON_FUNGUS) -> true
                ctx.block.compare(Block.WARPED_FUNGUS) -> false
                else -> return@Rule RuleResult.Next
            }
        val item = boneMeal(ctx) ?: return@Rule RuleResult.Next
        val base = ctx.instance.getBlock(ctx.blockPosition.sub(0.0, 1.0, 0.0))
        if (!isValidFungusBase(base, crimson)) return@Rule RuleResult.Next

        consumeBoneMeal(ctx, item)
        playSoundFor(ctx, SoundEvent.ITEM_BONE_MEAL_USE)
        if (Random().nextFloat() < 0.4f) {
            growHugeFungus(ctx.instance, ctx.blockPosition, crimson, Random())
        }
        RuleResult.Consume
    }

private fun isValidFungusBase(
    block: Block,
    crimson: Boolean,
): Boolean =
    if (crimson) {
        block.compare(Block.CRIMSON_NYLIUM)
    } else {
        block.compare(Block.WARPED_NYLIUM)
    }

private fun growHugeFungus(
    instance: Instance,
    base: Point,
    crimson: Boolean,
    random: Random,
): Boolean {
    val stem = if (crimson) Block.CRIMSON_STEM else Block.WARPED_STEM
    val wart = if (crimson) Block.NETHER_WART_BLOCK else Block.WARPED_WART_BLOCK
    val height = growthHeight(random)

    instance.setBlock(base, Block.AIR)
    generateStem(instance, random, base, height, stem)
    generateHat(instance, random, base, height, wart, vines = crimson)
    return true
}

private fun generateStem(
    instance: Instance,
    random: Random,
    base: Point,
    stemHeight: Int,
    stem: Block,
) {
    for (y in 0 until stemHeight) {
        val position = base.add(0.0, y.toDouble(), 0.0)
        if (!isReplaceable(instance.getBlock(position))) continue
        instance.setBlock(position, stem)
    }
}

private fun generateHat(
    instance: Instance,
    random: Random,
    base: Point,
    hatHeight: Int,
    wart: Block,
    vines: Boolean,
) {
    val hatStart = minOf(random.nextInt(1 + hatHeight / 3) + 5, hatHeight)
    val hatBottom = hatHeight - hatStart

    for (y in hatBottom..hatHeight) {
        var radius = if (y < hatHeight - random.nextInt(3)) 2 else 1
        if (hatStart > 8 && y < hatBottom + 4) radius = 3

        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val edgeX = dx == -radius || dx == radius
                val edgeZ = dz == -radius || dz == radius
                val interior = !edgeX && !edgeZ && y != hatHeight
                val corner = edgeX && edgeZ
                val lower = y < hatBottom + 3
                val position = base.add(dx.toDouble(), y.toDouble(), dz.toDouble())
                if (!isReplaceable(instance.getBlock(position))) continue

                if (lower) {
                    if (!interior) placeWithOptionalVines(instance, random, position, wart, vines)
                } else if (interior) {
                    placeHatBlock(instance, random, position, wart, 0.1f, 0.2f, if (vines) 0.1f else 0.0f)
                } else if (corner) {
                    placeHatBlock(instance, random, position, wart, 0.01f, 0.7f, if (vines) 0.083f else 0.0f)
                } else {
                    placeHatBlock(instance, random, position, wart, 5.0E-4f, 0.98f, if (vines) 0.07f else 0.0f)
                }
            }
        }
    }
}

private fun placeHatBlock(
    instance: Instance,
    random: Random,
    position: Point,
    wart: Block,
    decorationChance: Float,
    generationChance: Float,
    vineChance: Float,
) {
    if (random.nextFloat() < decorationChance) {
        instance.setBlock(position, Block.SHROOMLIGHT)
    } else if (random.nextFloat() < generationChance) {
        instance.setBlock(position, wart)
        if (random.nextFloat() < vineChance) generateVines(instance, position, random)
    }
}

private fun placeWithOptionalVines(
    instance: Instance,
    random: Random,
    position: Point,
    wart: Block,
    vines: Boolean,
) {
    if (instance.getBlock(position.sub(0.0, 1.0, 0.0)).key() == wart.key()) {
        instance.setBlock(position, wart)
    } else if (random.nextDouble() < 0.15) {
        instance.setBlock(position, wart)
        if (vines && random.nextInt(11) == 0) generateVines(instance, position, random)
    }
}

private fun generateVines(
    instance: Instance,
    position: Point,
    random: Random,
) {
    var cursor = position.sub(0.0, 1.0, 0.0)
    if (!instance.getBlock(cursor).air()) return

    var length = 1 + random.nextInt(5)
    if (random.nextInt(7) == 0) length *= 2
    val age = (23 + random.nextInt(3)).toString()

    for (i in 0 until length) {
        if (!instance.getBlock(cursor).air()) return
        val block =
            if (i == length - 1) {
                Block.WEEPING_VINES.withProperty("age", age)
            } else {
                Block.WEEPING_VINES_PLANT
            }
        instance.setBlock(cursor, block)
        cursor = cursor.sub(0.0, 1.0, 0.0)
    }
}

private fun isReplaceable(block: Block): Boolean = block.air() || block.replaceable()

private fun growthHeight(random: Random): Int {
    var height = 4 + random.nextInt(10)
    if (random.nextInt(12) == 0) height *= 2
    return height
}

private fun netherFlora(
    crimson: Boolean,
    random: Random,
): Block =
    if (crimson) {
        when (random.nextInt(4)) {
            0 -> Block.CRIMSON_ROOTS
            1 -> Block.CRIMSON_FUNGUS
            2 -> Block.NETHER_SPROUTS
            else -> Block.CRIMSON_ROOTS
        }
    } else {
        when (random.nextInt(4)) {
            0 -> Block.WARPED_ROOTS
            1 -> Block.WARPED_FUNGUS
            2 -> Block.NETHER_SPROUTS
            else -> Block.WARPED_ROOTS
        }
    }

private fun mossFlora(random: Random): Block =
    when (random.nextInt(6)) {
        0 -> Block.MOSS_CARPET
        1 -> Block.SHORT_GRASS
        2 -> Block.AZALEA
        3 -> Block.FLOWERING_AZALEA
        else -> Block.SHORT_GRASS
    }

private fun isMossable(block: Block): Boolean =
    block.compare(Block.STONE) ||
        block.compare(Block.DEEPSLATE) ||
        block.compare(Block.DIRT) ||
        block.compare(Block.GRASS_BLOCK) ||
        block.compare(Block.TUFF) ||
        block.compare(Block.CALCITE)

private fun adjacentNylium(
    instance: Instance,
    position: Point,
): Block? {
    val offsets = listOf(Triple(1, 0, 0), Triple(-1, 0, 0), Triple(0, 0, 1), Triple(0, 0, -1))
    for ((dx, dy, dz) in offsets) {
        val block = instance.getBlock(position.add(dx.toDouble(), dy.toDouble(), dz.toDouble()))
        if (block.compare(Block.CRIMSON_NYLIUM) || block.compare(Block.WARPED_NYLIUM)) return block
    }
    return null
}

fun BlockRulesetEditor.addNetherFlora() {
    blocks(Block.CRIMSON_NYLIUM) { interact.append { install(NYLIUM_BONE_MEAL_RULE) } }
    blocks(Block.WARPED_NYLIUM) { interact.append { install(NYLIUM_BONE_MEAL_RULE) } }
    blocks(Block.NETHERRACK) { interact.append { install(NETHERRACK_NYLIUM_BONE_MEAL_RULE) } }
    blocks(Block.MOSS_BLOCK) { interact.append { install(MOSS_BONE_MEAL_RULE) } }
    blocks(Block.RED_SHRUB) { interact.append { install(RED_SHRUB_BONE_MEAL_RULE) } }
    blocks(Block.SHORT_DRY_GRASS) { interact.append { install(SHORT_DRY_GRASS_BONE_MEAL_RULE) } }
    listOf(Block.CRIMSON_FUNGUS, Block.WARPED_FUNGUS).forEach { fungus ->
        blocks(fungus) { interact.append { install(FUNGUS_GROWTH_RULE) } }
    }
}
