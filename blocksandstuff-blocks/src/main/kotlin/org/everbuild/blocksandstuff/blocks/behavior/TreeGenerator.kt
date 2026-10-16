package org.everbuild.blocksandstuff.blocks.behavior

import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.tag.Tag

/**
 * Generates a tree at [base]. Attach custom generators per instance via [TREE_GENERATORS]; Blocks &
 * Stuff falls back to its built-in port of the vanilla tree features when none is provided.
 */
fun interface TreeGenerator {
    fun generate(
        instance: Instance,
        base: Point,
        random: Random,
    )
}

/**
 * Per-instance tree generator overrides, keyed by sapling block key:
 * `instance.setTag(TREE_GENERATORS, mapOf(Block.OAK_SAPLING.key() to myGenerator))`.
 */
val TREE_GENERATORS: Tag<Map<Key, TreeGenerator>> =
    Tag.Transient("blocksandstuff:tree_generators")

internal fun defaultTreeGenerator(saplingKey: Key): TreeGenerator? = DEFAULT_TREES[saplingKey]

internal fun defaultLargeTreeGenerator(saplingKey: Key): TreeGenerator? = LARGE_TREES[saplingKey]

private interface IntProvider {
    fun sample(random: Random): Int
}

private class ConstantInt(
    private val value: Int,
) : IntProvider {
    override fun sample(random: Random): Int = value
}

private class UniformInt(
    private val min: Int,
    private val max: Int,
) : IntProvider {
    override fun sample(random: Random): Int = min + random.nextInt(max - min + 1)
}

private class WeightedIntProvider(
    private val values: List<Int>,
) : IntProvider {
    override fun sample(random: Random): Int = values[random.nextInt(values.size)]
}

private data class FoliageAttachment(
    val pos: BlockVec,
    val radiusOffset: Int,
    val doubleTrunk: Boolean,
)

private class TreePlacerContext(
    private val instance: Instance,
    private val log: Block,
    private val leavesProvider: (Random) -> Block,
) {
    private val placedLeaves = LinkedHashMap<String, BlockVec>()
    private val placedLogs = LinkedHashMap<String, BlockVec>()

    fun get(pos: BlockVec): Block = if (instance.isChunkLoaded(pos)) instance.getBlock(pos) else Block.STONE

    fun isAir(pos: BlockVec): Boolean = instance.isChunkLoaded(pos) && instance.getBlock(pos).air()

    fun setRaw(
        pos: BlockVec,
        block: Block,
    ) {
        if (instance.isChunkLoaded(pos)) instance.setBlock(pos, block)
    }

    fun isGrassOrDirt(pos: BlockVec): Boolean {
        val key = get(pos).key().value()
        return key.contains("dirt") || key == "grass_block" || key == "podzol" || key == "mycelium" || key == "moss_block"
    }

    fun logPositions(): List<BlockVec> = placedLogs.values.sortedBy { it.blockY() }

    fun leafPositions(): List<BlockVec> = placedLeaves.values.sortedBy { it.blockY() }

    fun isSet(pos: BlockVec): Boolean = placedLeaves.containsKey(coordKey(pos))

    fun placeLog(
        pos: BlockVec,
        axis: String? = null,
    ): Boolean {
        if (!instance.isChunkLoaded(pos)) return false
        if (!validTreePos(pos)) return false
        setLog(pos, axis)
        return true
    }

    fun placeLogIfFree(pos: BlockVec) {
        if (instance.isChunkLoaded(pos) && isFree(pos)) setLog(pos, null)
    }

    fun placeLeaf(
        pos: BlockVec,
        random: Random,
    ): Boolean {
        if (!instance.isChunkLoaded(pos)) return false
        if (isLeafAt(pos) && get(pos).getProperty("persistent") == "true") return false
        if (!validTreePos(pos)) return false
        instance.setBlock(pos, leavesProvider(random))
        placedLeaves[coordKey(pos)] = pos
        return true
    }

    fun placeVine(
        pos: BlockVec,
        property: String,
    ) {
        if (!isAir(pos)) return
        instance.setBlock(pos, Block.VINE.withProperty(property, "true"))
    }

    private fun setLog(
        pos: BlockVec,
        axis: String?,
    ) {
        val block = if (axis != null && log.getProperty("axis") != null) log.withProperty("axis", axis) else log
        instance.setBlock(pos, block)
        placedLogs[coordKey(pos)] = pos
    }

    private fun validTreePos(pos: BlockVec): Boolean {
        val block = get(pos)
        return block.air() || isLeafAt(pos) || REPLACEABLE.contains(block.key().value())
    }

    private fun isFree(pos: BlockVec): Boolean = validTreePos(pos) || isLogAt(pos)

    private fun isLogAt(pos: BlockVec): Boolean = get(pos).key().value().let { LOG_SUFFIXES.any(it::endsWith) }

    private fun isLeafAt(pos: BlockVec): Boolean = get(pos).key().value().endsWith("_leaves")

    companion object {
        private val LOG_SUFFIXES = listOf("_log", "_wood", "_stem", "_hyphae")
        private val REPLACEABLE =
            setOf("short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "snow", "vine", "glow_lichen")
    }
}

private fun BlockVec.above(n: Int): BlockVec = BlockVec(blockX(), blockY() + n, blockZ())

private fun BlockVec.offset(
    dx: Int,
    dy: Int,
    dz: Int,
): BlockVec = BlockVec(blockX() + dx, blockY() + dy, blockZ() + dz)

private fun coordKey(pos: BlockVec): String = "${pos.blockX()},${pos.blockY()},${pos.blockZ()}"

private fun manhattan(
    a: BlockVec,
    b: BlockVec,
): Int = abs(a.blockX() - b.blockX()) + abs(a.blockY() - b.blockY()) + abs(a.blockZ() - b.blockZ())

private val HORIZONTAL = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

private fun randomHorizontal(random: Random): Pair<Int, Int> = HORIZONTAL[random.nextInt(HORIZONTAL.size)]

private interface TreeDecorator {
    fun place(
        ctx: TreePlacerContext,
        random: Random,
    )
}

private object TrunkVineDecorator : TreeDecorator {
    override fun place(
        ctx: TreePlacerContext,
        random: Random,
    ) {
        for (log in ctx.logPositions()) {
            if (random.nextInt(3) > 0) ctx.placeVine(log.offset(-1, 0, 0), "east")
            if (random.nextInt(3) > 0) ctx.placeVine(log.offset(1, 0, 0), "west")
            if (random.nextInt(3) > 0) ctx.placeVine(log.offset(0, 0, -1), "south")
            if (random.nextInt(3) > 0) ctx.placeVine(log.offset(0, 0, 1), "north")
        }
    }
}

private class AlterGroundDecorator(
    private val ground: Block,
) : TreeDecorator {
    override fun place(
        ctx: TreePlacerContext,
        random: Random,
    ) {
        val logs = ctx.logPositions()
        if (logs.isEmpty()) return
        val lowestY = logs.minOf { it.blockY() }

        for (pos in logs.filter { it.blockY() == lowestY }) {
            placeCircle(ctx, pos.offset(-1, 0, -1))
            placeCircle(ctx, pos.offset(2, 0, -1))
            placeCircle(ctx, pos.offset(-1, 0, 2))
            placeCircle(ctx, pos.offset(2, 0, 2))
            repeat(5) {
                val k = random.nextInt(64)
                val l = k % 8
                val m = k / 8
                if (l == 0 || l == 7 || m == 0 || m == 7) {
                    placeCircle(ctx, pos.offset(-3 + l, 0, -3 + m))
                }
            }
        }
    }

    private fun placeCircle(
        ctx: TreePlacerContext,
        center: BlockVec,
    ) {
        for (dx in -2..2) {
            for (dz in -2..2) {
                if (abs(dx) != 2 || abs(dz) != 2) {
                    placeBlockAt(ctx, center.offset(dx, 0, dz))
                }
            }
        }
    }

    private fun placeBlockAt(
        ctx: TreePlacerContext,
        pos: BlockVec,
    ) {
        for (dy in 2 downTo -3) {
            val current = pos.offset(0, dy, 0)
            if (ctx.isGrassOrDirt(current)) {
                ctx.setRaw(current, ground)
                return
            }
            if (!ctx.isAir(current) && dy < 0) return
        }
    }
}

private class LeaveVineDecorator(
    private val probability: Float,
) : TreeDecorator {
    override fun place(
        ctx: TreePlacerContext,
        random: Random,
    ) {
        for (leaf in ctx.leafPositions()) {
            if (random.nextFloat() < probability) hangingVine(ctx, leaf.offset(-1, 0, 0), "east")
            if (random.nextFloat() < probability) hangingVine(ctx, leaf.offset(1, 0, 0), "west")
            if (random.nextFloat() < probability) hangingVine(ctx, leaf.offset(0, 0, -1), "south")
            if (random.nextFloat() < probability) hangingVine(ctx, leaf.offset(0, 0, 1), "north")
        }
    }

    private fun hangingVine(
        ctx: TreePlacerContext,
        pos: BlockVec,
        property: String,
    ) {
        ctx.placeVine(pos, property)
        var below = pos.offset(0, -1, 0)
        var remaining = 4
        while (ctx.isAir(below) && remaining > 0) {
            ctx.placeVine(below, property)
            below = below.offset(0, -1, 0)
            remaining--
        }
    }
}

private abstract class TrunkPlacer(
    private val baseHeight: Int,
    private val heightRandA: Int,
    private val heightRandB: Int,
) {
    fun treeHeight(random: Random): Int = baseHeight + random.nextInt(heightRandA + 1) + random.nextInt(heightRandB + 1)

    abstract fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment>
}

private class StraightTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
) : TrunkPlacer(baseHeight, randA, randB) {
    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        for (i in 0 until height) ctx.placeLog(origin.above(i))
        return listOf(FoliageAttachment(origin.above(height), 0, false))
    }
}

private class GiantTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
) : TrunkPlacer(baseHeight, randA, randB) {
    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        for (i in 0 until height) {
            ctx.placeLogIfFree(origin.offset(0, i, 0))
            if (i < height - 1) {
                ctx.placeLogIfFree(origin.offset(1, i, 0))
                ctx.placeLogIfFree(origin.offset(1, i, 1))
                ctx.placeLogIfFree(origin.offset(0, i, 1))
            }
        }
        return listOf(FoliageAttachment(origin.above(height), 0, true))
    }
}

private class MegaJungleTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
) : TrunkPlacer(baseHeight, randA, randB) {
    private val giant = GiantTrunkPlacer(baseHeight, randA, randB)

    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        val attachments = giant.placeTrunk(ctx, random, height, origin).toMutableList()
        var i = height - 2 - random.nextInt(4)
        while (i > height / 2) {
            val angle = random.nextFloat() * (Math.PI * 2).toFloat()
            var j = 0
            var k = 0
            for (l in 0 until 5) {
                j = (1.5f + cos(angle) * l).toInt()
                k = (1.5f + sin(angle) * l).toInt()
                ctx.placeLog(origin.offset(j, i - 3 + l / 2, k))
            }
            attachments.add(FoliageAttachment(origin.offset(j, i, k), -2, false))
            i -= 2 + random.nextInt(4)
        }
        return attachments
    }
}

private class ForkingTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
) : TrunkPlacer(baseHeight, randA, randB) {
    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        val attachments = mutableListOf<FoliageAttachment>()
        val (fdx, fdz) = randomHorizontal(random)
        val i = height - random.nextInt(4) - 1
        var j = 3 - random.nextInt(3)
        var k = origin.blockX()
        var l = origin.blockZ()
        var top = -1

        for (i1 in 0 until height) {
            val y = origin.blockY() + i1
            if (i1 >= i && j > 0) {
                k += fdx
                l += fdz
                j--
            }
            if (ctx.placeLog(BlockVec(k, y, l))) top = y + 1
        }
        if (top >= 0) attachments.add(FoliageAttachment(BlockVec(k, top, l), 1, false))

        k = origin.blockX()
        l = origin.blockZ()
        val (sdx, sdz) = randomHorizontal(random)
        if (sdx != fdx || sdz != fdz) {
            var l1 = i - random.nextInt(2) - 1
            var k1 = 1 + random.nextInt(3)
            var secondTop = -1
            while (l1 < height && k1 > 0) {
                if (l1 >= 1) {
                    val y = origin.blockY() + l1
                    k += sdx
                    l += sdz
                    if (ctx.placeLog(BlockVec(k, y, l))) secondTop = y + 1
                }
                l1++
                k1--
            }
            if (secondTop >= 0) attachments.add(FoliageAttachment(BlockVec(k, secondTop, l), 0, false))
        }
        return attachments
    }
}

private class BendingTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
    private val minHeightForLeaves: Int,
    private val bendLength: IntProvider,
) : TrunkPlacer(baseHeight, randA, randB) {
    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        val (dx, dz) = randomHorizontal(random)
        val i = height - 1
        var pos = origin
        val attachments = mutableListOf<FoliageAttachment>()

        for (j in 0..i) {
            if (j + 1 >= i + random.nextInt(2)) pos = pos.offset(dx, 0, dz)
            ctx.placeLog(pos)
            if (j >= minHeightForLeaves) attachments.add(FoliageAttachment(pos, 0, false))
            pos = pos.offset(0, 1, 0)
        }

        val length = bendLength.sample(random)
        for (k in 0..length) {
            ctx.placeLog(pos)
            attachments.add(FoliageAttachment(pos, 0, false))
            pos = pos.offset(dx, 0, dz)
        }
        return attachments
    }
}

private class PoplarTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
    private val trunkHeightAboveBranches: Int,
    private val branchAmount: IntProvider,
) : TrunkPlacer(baseHeight, randA, randB) {
    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        for (i in 0 until height) ctx.placeLog(origin.above(i))

        val attachments = mutableListOf<FoliageAttachment>()
        val branchBase = (height - trunkHeightAboveBranches).coerceIn(1, height - 1)
        val directions = HORIZONTAL.shuffled(random)
        val count = branchAmount.sample(random).coerceAtLeast(0)
        for (b in 0 until count) {
            val (dx, dz) = directions[b % directions.size]
            var pos = origin.above(branchBase)
            val length = 2 + random.nextInt(2)
            for (i in 0 until length) {
                pos = pos.offset(dx, if (i == 0) 0 else 1, dz)
                ctx.placeLog(pos)
            }
            attachments.add(FoliageAttachment(pos.above(1), 0, false))
        }
        attachments.add(FoliageAttachment(origin.above(height), 0, false))
        return attachments
    }
}

private class PoplarFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    private val height: IntProvider,
    private val sideHoleChance: Float,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = this.height.sample(random)

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        val pos = attachment.pos.above(offset)
        val centerY = (foliageHeight - 1) / 2.0
        val halfHeight = foliageHeight / 2.0
        for (y in 0 until foliageHeight) {
            val t = (y - centerY) / (halfHeight + 0.5)
            val layerRadius = (foliageRadius * sqrt(maxOf(0.0, 1.0 - t * t))).toInt()
            if (layerRadius <= 0) {
                ctx.placeLeaf(pos.offset(0, y, 0), random)
                continue
            }
            val outer = (layerRadius - 0.5) * (layerRadius - 0.5)
            for (x in -layerRadius..layerRadius) {
                for (z in -layerRadius..layerRadius) {
                    val distance = (x * x + z * z).toDouble()
                    if (distance > layerRadius * layerRadius) continue
                    if (distance > outer && random.nextFloat() < sideHoleChance) continue
                    ctx.placeLeaf(pos.offset(x, y, z), random)
                }
            }
        }
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean = false
}

private class DarkOakTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
) : TrunkPlacer(baseHeight, randA, randB) {
    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        val attachments = mutableListOf<FoliageAttachment>()
        val (dx, dz) = randomHorizontal(random)
        val i = height - random.nextInt(4)
        var j = 2 - random.nextInt(3)
        val k = origin.blockX()
        val l = origin.blockY()
        val i1 = origin.blockZ()
        var j1 = k
        var k1 = i1
        val l1 = l + height - 1

        for (i2 in 0 until height) {
            if (i2 >= i && j > 0) {
                j1 += dx
                k1 += dz
                j--
            }
            val y = l + i2
            val pos = BlockVec(j1, y, k1)
            if (ctx.get(pos).air() || ctx.get(pos).key().value().endsWith("_leaves")) {
                ctx.placeLog(pos)
                ctx.placeLog(pos.offset(1, 0, 0))
                ctx.placeLog(pos.offset(0, 0, 1))
                ctx.placeLog(pos.offset(1, 0, 1))
            }
        }
        attachments.add(FoliageAttachment(BlockVec(j1, l1, k1), 0, true))

        for (l2 in -1..2) {
            for (i3 in -1..2) {
                if ((l2 < 0 || l2 > 1 || i3 < 0 || i3 > 1) && random.nextInt(3) <= 0) {
                    val j3 = random.nextInt(3) + 2
                    for (k2 in 0 until j3) {
                        ctx.placeLog(BlockVec(k + l2, l1 - k2 - 1, i1 + i3))
                    }
                    attachments.add(FoliageAttachment(BlockVec(k + l2, l1, i1 + i3), 0, false))
                }
            }
        }
        return attachments
    }
}

private class CherryTrunkPlacer(
    baseHeight: Int,
    randA: Int,
    randB: Int,
    private val branchCount: IntProvider,
    private val branchHorizontalLength: IntProvider,
    private val branchStartOffsetFromTop: IntProvider,
    private val branchEndOffsetFromTop: IntProvider,
) : TrunkPlacer(baseHeight, randA, randB) {
    override fun placeTrunk(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
    ): List<FoliageAttachment> {
        val startA = (height - 1 + branchStartOffsetFromTop.sample(random)).coerceAtLeast(0)
        var startB = (height - 1 + branchStartOffsetFromTop.sample(random) - 1).coerceAtLeast(0)
        if (startB >= startA) startB++

        val count = branchCount.sample(random)
        val three = count == 3
        val two = count >= 2
        val trunkHeight = when {
            three -> height
            two -> maxOf(startA, startB) + 1
            else -> startA + 1
        }

        for (i in 0 until trunkHeight) ctx.placeLog(origin.above(i))

        val attachments = mutableListOf<FoliageAttachment>()
        if (three) attachments.add(FoliageAttachment(origin.above(trunkHeight), 0, false))

        val (dx, dz) = randomHorizontal(random)
        attachments.add(generateBranch(ctx, random, height, origin, dx, dz, startA, startA < trunkHeight - 1))
        if (two) attachments.add(generateBranch(ctx, random, height, origin, -dx, -dz, startB, startB < trunkHeight - 1))
        return attachments
    }

    private fun generateBranch(
        ctx: TreePlacerContext,
        random: Random,
        height: Int,
        origin: BlockVec,
        dx: Int,
        dz: Int,
        start: Int,
        isTop: Boolean,
    ): FoliageAttachment {
        var pos = origin.above(start)
        val endY = height - 1 + branchEndOffsetFromTop.sample(random)
        val flag = isTop || endY < start
        val horizontal = branchHorizontalLength.sample(random) + (if (flag) 1 else 0)
        val end = BlockVec(origin.blockX() + dx * horizontal, origin.blockY() + endY, origin.blockZ() + dz * horizontal)
        val axis = if (dx != 0) "x" else "z"

        repeat(if (flag) 2 else 1) {
            pos = pos.offset(dx, 0, dz)
            ctx.placeLog(pos, axis)
        }

        val goingUp = end.blockY() > pos.blockY()
        while (true) {
            val distance = manhattan(pos, end)
            if (distance == 0) return FoliageAttachment(end.above(1), 0, false)

            val verticalChance = abs(end.blockY() - pos.blockY()).toFloat() / distance
            val vertical = random.nextFloat() < verticalChance
            pos = if (vertical) pos.offset(0, if (goingUp) 1 else -1, 0) else pos.offset(dx, 0, dz)
            ctx.placeLog(pos, if (vertical) null else axis)
        }
    }
}

private abstract class FoliagePlacer(
    private val radiusProvider: IntProvider,
    private val offsetProvider: IntProvider,
) {
    open fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = 0

    open fun foliageRadius(
        random: Random,
        height: Int,
    ): Int = radiusProvider.sample(random)

    fun offset(random: Random): Int = offsetProvider.sample(random)

    abstract fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    )

    protected abstract fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean

    protected open fun shouldSkipLocationSigned(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean {
        val i = if (doubleTrunk) minOf(abs(dx), abs(dx - 1)) else abs(dx)
        val j = if (doubleTrunk) minOf(abs(dz), abs(dz - 1)) else abs(dz)
        return shouldSkipLocation(random, i, dy, j, radius, doubleTrunk)
    }

    protected fun placeLeavesRow(
        ctx: TreePlacerContext,
        random: Random,
        pos: BlockVec,
        radius: Int,
        dy: Int,
        doubleTrunk: Boolean,
    ) {
        val extra = if (doubleTrunk) 1 else 0
        for (dx in -radius..radius + extra) {
            for (dz in -radius..radius + extra) {
                if (!shouldSkipLocationSigned(random, dx, dy, dz, radius, doubleTrunk)) {
                    ctx.placeLeaf(pos.offset(dx, dy, dz), random)
                }
            }
        }
    }

    protected fun placeLeavesRowWithHangingLeavesBelow(
        ctx: TreePlacerContext,
        random: Random,
        pos: BlockVec,
        radius: Int,
        dy: Int,
        doubleTrunk: Boolean,
        hangingLeavesChance: Float,
        hangingLeavesExtensionChance: Float,
    ) {
        placeLeavesRow(ctx, random, pos, radius, dy, doubleTrunk)
        val extra = if (doubleTrunk) 1 else 0

        for ((dx, dz) in HORIZONTAL) {
            val clockwiseX = -dz
            val clockwiseZ = dx
            val j = if (clockwiseX > 0 || (clockwiseX == 0 && clockwiseZ > 0)) radius + extra else radius
            var current = pos.offset(clockwiseX * j, dy - 1, clockwiseZ * j).offset(-dx * radius, 0, -dz * radius)
            var k = -radius
            while (k < radius + extra) {
                val above = current.offset(0, 1, 0)
                val flag = ctx.isSet(above)
                if (flag && tryPlaceExtension(ctx, random, hangingLeavesChance, pos.offset(0, -1, 0), current)) {
                    val down = current.offset(0, -1, 0)
                    tryPlaceExtension(ctx, random, hangingLeavesExtensionChance, pos.offset(0, -1, 0), down)
                }
                k++
                current = current.offset(dx, 0, dz)
            }
        }
    }

    private fun tryPlaceExtension(
        ctx: TreePlacerContext,
        random: Random,
        chance: Float,
        origin: BlockVec,
        pos: BlockVec,
    ): Boolean {
        if (manhattan(pos, origin) >= 7) return false
        if (random.nextFloat() > chance) return false
        return ctx.placeLeaf(pos, random)
    }
}

private class BlobFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    private val height: Int,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = this.height

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        for (i in offset downTo offset - foliageHeight) {
            val j = maxOf(foliageRadius + attachment.radiusOffset - 1 - i / 2, 0)
            placeLeavesRow(ctx, random, attachment.pos, j, i, attachment.doubleTrunk)
        }
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean = dx == radius && dz == radius && (random.nextInt(2) == 0 || dy == 0)
}

private class AcaciaFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
) : FoliagePlacer(radius, offset) {
    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        val pos = attachment.pos.above(offset)
        val flag = attachment.doubleTrunk
        placeLeavesRow(ctx, random, pos, foliageRadius + attachment.radiusOffset, -1 - foliageHeight, flag)
        placeLeavesRow(ctx, random, pos, foliageRadius - 1, -foliageHeight, flag)
        placeLeavesRow(ctx, random, pos, foliageRadius + attachment.radiusOffset - 1, 0, flag)
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean =
        if (dy == 0) {
            (dx > 1 || dz > 1) && dx != 0 && dz != 0
        } else {
            dx == radius && dz == radius && radius > 0
        }
}

private class DarkOakFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = 4

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        val pos = attachment.pos.above(offset)
        val double = attachment.doubleTrunk
        if (double) {
            placeLeavesRow(ctx, random, pos, foliageRadius + 2, -1, double)
            placeLeavesRow(ctx, random, pos, foliageRadius + 3, 0, double)
            placeLeavesRow(ctx, random, pos, foliageRadius + 2, 1, double)
            if (random.nextBoolean()) placeLeavesRow(ctx, random, pos, foliageRadius, 2, double)
        } else {
            placeLeavesRow(ctx, random, pos, foliageRadius + 2, -1, double)
            placeLeavesRow(ctx, random, pos, foliageRadius + 1, 0, double)
        }
    }

    override fun shouldSkipLocationSigned(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean {
        val useFallback =
            dy != 0 || !doubleTrunk ||
                (dx != -radius && dx < radius) || (dz != -radius && dz < radius)
        return if (useFallback) {
            super.shouldSkipLocationSigned(random, dx, dy, dz, radius, doubleTrunk)
        } else {
            true
        }
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean =
        if (dy == -1 && !doubleTrunk) {
            dx == radius && dz == radius
        } else {
            dy == 1 && dx + dz > radius * 2 - 2
        }
}

private class SpruceFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    private val trunkHeight: IntProvider,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = maxOf(4, height - trunkHeight.sample(random))

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        val pos = attachment.pos
        var i = random.nextInt(2)
        var j = 1
        var k = 0
        for (l in offset downTo -foliageHeight) {
            placeLeavesRow(ctx, random, pos, i, l, attachment.doubleTrunk)
            if (i >= j) {
                i = k
                k = 1
                j = minOf(j + 1, foliageRadius + attachment.radiusOffset)
            } else {
                i++
            }
        }
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean = dx == radius && dz == radius && radius > 0
}

private class MegaJungleFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    private val height: Int,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = this.height

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        val i = if (attachment.doubleTrunk) foliageHeight else 1 + random.nextInt(2)
        for (j in offset downTo offset - i) {
            val k = foliageRadius + attachment.radiusOffset + 1 - j
            placeLeavesRow(ctx, random, attachment.pos, k, j, attachment.doubleTrunk)
        }
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean = dx + dz >= 7 || dx * dx + dz * dz > radius * radius
}

private class MegaPineFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    private val crownHeight: IntProvider,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = crownHeight.sample(random)

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        val pos = attachment.pos
        var previous = 0
        for (y in pos.blockY() - foliageHeight + offset..pos.blockY() + offset) {
            val k = pos.blockY() - y
            val l = foliageRadius + attachment.radiusOffset + floor(k.toFloat() / foliageHeight * 3.5f).toInt()
            val radius =
                if (k > 0 && l == previous && (y and 1) == 0) {
                    l + 1
                } else {
                    l
                }
            placeLeavesRow(ctx, random, BlockVec(pos.blockX(), y, pos.blockZ()), radius, 0, attachment.doubleTrunk)
            previous = l
        }
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean = dx + dz >= 7 || dx * dx + dz * dz > radius * radius
}

private class CherryFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    private val height: IntProvider,
    private val wideBottomLayerHoleChance: Float,
    private val cornerHoleChance: Float,
    private val hangingLeavesChance: Float,
    private val hangingLeavesExtensionChance: Float,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = this.height.sample(random)

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        val double = attachment.doubleTrunk
        val pos = attachment.pos.above(offset)
        val i = foliageRadius + attachment.radiusOffset - 1
        placeLeavesRow(ctx, random, pos, i - 2, foliageHeight - 3, double)
        placeLeavesRow(ctx, random, pos, i - 1, foliageHeight - 4, double)

        for (j in foliageHeight - 5 downTo 0) {
            placeLeavesRow(ctx, random, pos, i, j, double)
        }

        placeLeavesRowWithHangingLeavesBelow(ctx, random, pos, i, -1, double, hangingLeavesChance, hangingLeavesExtensionChance)
        placeLeavesRowWithHangingLeavesBelow(ctx, random, pos, i - 1, -2, double, hangingLeavesChance, hangingLeavesExtensionChance)
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean {
        if (dy == -1 && (dx == radius || dz == radius) && random.nextFloat() < wideBottomLayerHoleChance) {
            return true
        }
        val corner = dx == radius && dz == radius
        return if (radius > 2) {
            corner || dx + dz > radius * 2 - 2 && random.nextFloat() < cornerHoleChance
        } else {
            corner && random.nextFloat() < cornerHoleChance
        }
    }
}

private class RandomSpreadFoliagePlacer(
    radius: IntProvider,
    offset: IntProvider,
    private val foliageHeightProvider: IntProvider,
    private val leafPlacementAttempts: Int,
) : FoliagePlacer(radius, offset) {
    override fun foliageHeight(
        random: Random,
        height: Int,
    ): Int = foliageHeightProvider.sample(random)

    override fun createFoliage(
        ctx: TreePlacerContext,
        random: Random,
        attachment: FoliageAttachment,
        foliageHeight: Int,
        foliageRadius: Int,
        offset: Int,
    ) {
        if (foliageRadius <= 0 || foliageHeight <= 0) return
        val pos = attachment.pos
        repeat(leafPlacementAttempts) {
            val dx = random.nextInt(foliageRadius) - random.nextInt(foliageRadius)
            val dy = random.nextInt(foliageHeight) - random.nextInt(foliageHeight)
            val dz = random.nextInt(foliageRadius) - random.nextInt(foliageRadius)
            ctx.placeLeaf(pos.offset(dx, dy, dz), random)
        }
    }

    override fun shouldSkipLocation(
        random: Random,
        dx: Int,
        dy: Int,
        dz: Int,
        radius: Int,
        doubleTrunk: Boolean,
    ): Boolean = false
}

private class VanillaTree(
    private val log: Block,
    private val leaves: Block,
    private val trunk: TrunkPlacer,
    private val foliage: FoliagePlacer,
    private val decorators: List<TreeDecorator> = emptyList(),
) : TreeGenerator {
    override fun generate(
        instance: Instance,
        base: Point,
        random: Random,
    ) {
        val ctx = TreePlacerContext(instance, log) { leaves }
        val origin = BlockVec(base.blockX(), base.blockY(), base.blockZ())
        val height = trunk.treeHeight(random)
        val foliageHeight = foliage.foliageHeight(random, height)
        val foliageRadius = foliage.foliageRadius(random, height - foliageHeight)
        val offset = foliage.offset(random)
        trunk.placeTrunk(ctx, random, height, origin).forEach {
            foliage.createFoliage(ctx, random, it, foliageHeight, foliageRadius, offset)
        }
        decorators.forEach { it.place(ctx, random) }
    }
}

/**
 * Poplar (26.3): a tall straight trunk with a large canopy; the leaf colour is picked randomly
 * from red/orange/yellow. Vanilla's dedicated poplar placers are not public yet, so this is an
 * approximation of the shape.
 */
private class PoplarTree : TreeGenerator {
    override fun generate(
        instance: Instance,
        base: Point,
        random: Random,
    ) {
        val leaves = POPLAR_LEAVES[random.nextInt(POPLAR_LEAVES.size)]
        val ctx = TreePlacerContext(instance, Block.POPLAR_LOG) { leaves }
        val origin = BlockVec(base.blockX(), base.blockY(), base.blockZ())
        val trunk = PoplarTrunkPlacer(7, 4, 0, trunkHeightAboveBranches = 4, branchAmount = UniformInt(1, 4))
        val radius = WeightedIntProvider(listOf(5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 8))
        val foliage = PoplarFoliagePlacer(radius, ConstantInt(0), UniformInt(5, 6), 0.15f)
        val height = trunk.treeHeight(random)
        val foliageHeight = foliage.foliageHeight(random, height)
        val foliageRadius = foliage.foliageRadius(random, height - foliageHeight)
        val offset = foliage.offset(random)
        trunk.placeTrunk(ctx, random, height, origin).forEach {
            foliage.createFoliage(ctx, random, it, foliageHeight, foliageRadius, offset)
        }
    }
}

/**
 * Azalea / flowering azalea (bonemeal): bending trunk + random spread foliage, leaves are a
 * weighted mix of azalea (3) and flowering azalea (1) leaves.
 */
private class AzaleaTree : TreeGenerator {
    override fun generate(
        instance: Instance,
        base: Point,
        random: Random,
    ) {
        val ctx = TreePlacerContext(instance, Block.OAK_LOG) { r ->
            if (r.nextInt(4) == 0) Block.FLOWERING_AZALEA_LEAVES else Block.AZALEA_LEAVES
        }
        val origin = BlockVec(base.blockX(), base.blockY(), base.blockZ())
        // Vanilla AZALEA_TREE: dirt(ROOTED_DIRT).forceDirt()
        instance.setBlock(origin.offset(0, -1, 0), Block.ROOTED_DIRT)
        val trunk = BendingTrunkPlacer(4, 2, 0, minHeightForLeaves = 3, bendLength = UniformInt(1, 2))
        val foliage = RandomSpreadFoliagePlacer(ConstantInt(3), ConstantInt(0), ConstantInt(2), 50)
        val height = trunk.treeHeight(random)
        val foliageHeight = foliage.foliageHeight(random, height)
        val foliageRadius = foliage.foliageRadius(random, height - foliageHeight)
        val offset = foliage.offset(random)
        trunk.placeTrunk(ctx, random, height, origin).forEach {
            foliage.createFoliage(ctx, random, it, foliageHeight, foliageRadius, offset)
        }
    }
}

private val POPLAR_LEAVES = listOf(Block.RED_POPLAR_LEAVES, Block.ORANGE_POPLAR_LEAVES, Block.YELLOW_POPLAR_LEAVES)

private val JUNGLE_DECORATORS: List<TreeDecorator> = listOf(TrunkVineDecorator, LeaveVineDecorator(0.25f))

private fun oak(log: Block, leaves: Block): TreeGenerator =
    VanillaTree(log, leaves, StraightTrunkPlacer(4, 2, 0), BlobFoliagePlacer(ConstantInt(2), ConstantInt(0), 3))

private val DEFAULT_TREES: Map<Key, TreeGenerator> =
    mapOf(
        Block.OAK_SAPLING.key() to oak(Block.OAK_LOG, Block.OAK_LEAVES),
        Block.BIRCH_SAPLING.key() to
            VanillaTree(Block.BIRCH_LOG, Block.BIRCH_LEAVES, StraightTrunkPlacer(5, 2, 0), BlobFoliagePlacer(ConstantInt(2), ConstantInt(0), 3)),
        Block.SPRUCE_SAPLING.key() to
            VanillaTree(
                Block.SPRUCE_LOG,
                Block.SPRUCE_LEAVES,
                StraightTrunkPlacer(5, 2, 1),
                SpruceFoliagePlacer(UniformInt(2, 3), UniformInt(0, 2), UniformInt(1, 2)),
            ),
        Block.JUNGLE_SAPLING.key() to
            VanillaTree(
                Block.JUNGLE_LOG,
                Block.JUNGLE_LEAVES,
                StraightTrunkPlacer(4, 8, 0),
                BlobFoliagePlacer(ConstantInt(2), ConstantInt(0), 3),
                JUNGLE_DECORATORS,
            ),
        Block.ACACIA_SAPLING.key() to
            VanillaTree(Block.ACACIA_LOG, Block.ACACIA_LEAVES, ForkingTrunkPlacer(5, 2, 2), AcaciaFoliagePlacer(ConstantInt(2), ConstantInt(0))),
        Block.CHERRY_SAPLING.key() to
            VanillaTree(
                Block.CHERRY_LOG,
                Block.CHERRY_LEAVES,
                CherryTrunkPlacer(
                    7,
                    1,
                    0,
                    WeightedIntProvider(listOf(1, 2, 3)),
                    UniformInt(2, 4),
                    UniformInt(-4, -3),
                    UniformInt(-1, 0),
                ),
                CherryFoliagePlacer(
                    ConstantInt(4),
                    ConstantInt(0),
                    ConstantInt(5),
                    0.25f,
                    0.5f,
                    0.16666667f,
                    0.33333334f,
                ),
            ),
        Block.POPLAR_SAPLING.key() to PoplarTree(),
        Block.AZALEA.key() to AzaleaTree(),
        Block.FLOWERING_AZALEA.key() to AzaleaTree(),
    )

private val LARGE_TREES: Map<Key, TreeGenerator> =
    mapOf(
        Block.SPRUCE_SAPLING.key() to
            VanillaTree(
                Block.SPRUCE_LOG,
                Block.SPRUCE_LEAVES,
                GiantTrunkPlacer(13, 2, 14),
                MegaPineFoliagePlacer(ConstantInt(0), ConstantInt(0), UniformInt(13, 17)),
                listOf(AlterGroundDecorator(Block.PODZOL)),
            ),
        Block.JUNGLE_SAPLING.key() to
            VanillaTree(
                Block.JUNGLE_LOG,
                Block.JUNGLE_LEAVES,
                MegaJungleTrunkPlacer(10, 2, 19),
                MegaJungleFoliagePlacer(ConstantInt(2), ConstantInt(0), 2),
                JUNGLE_DECORATORS,
            ),
        Block.DARK_OAK_SAPLING.key() to
            VanillaTree(
                Block.DARK_OAK_LOG,
                Block.DARK_OAK_LEAVES,
                DarkOakTrunkPlacer(6, 2, 1),
                DarkOakFoliagePlacer(ConstantInt(0), ConstantInt(0)),
            ),
        Block.PALE_OAK_SAPLING.key() to
            VanillaTree(
                Block.PALE_OAK_LOG,
                Block.PALE_OAK_LEAVES,
                DarkOakTrunkPlacer(6, 2, 1),
                DarkOakFoliagePlacer(ConstantInt(0), ConstantInt(0)),
            ),
    )
