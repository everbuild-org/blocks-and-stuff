package org.everbuild.blocksandstuff.testserver
import net.minestom.server.MinecraftServer
import kotlin.math.sin
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.Generator

class ExampleGenerator : Generator {
    fun Block.withDefaultHandler(): Block {
        if (this.handler() != null) return this
        return this.withHandler(MinecraftServer.getBlockManager().getHandler(this.key().asString()))
    }

    override fun generate(unit: GenerationUnit) {
        val modifier = unit.modifier()
        val start = unit.absoluteStart()
        val rx = start.blockX()
        val rz = start.blockZ()
        for (x in rx until 16 + rx) for (z in rz until 16 + rz) {
            val sinX = sin(x.toDouble() / 16.0 * Math.PI * 0.2)
            val sinZ = sin(z.toDouble() / 16.0 * Math.PI * 0.2)
            val combined = ((sinX + 1.0) * 15 + (sinZ + 1.0) * 15) + 10

            for (y in 0 until combined.toInt()) {
                modifier.setBlock(x, y, z, Block.GRASS_BLOCK)
            }

            if (x % 64 == 0 && z % 64 == 0) modifier.setBlock(
                x,
                combined.toInt(),
                z,
                Block.WATER.withDefaultHandler()
            )
            if (x % 64 == 9 && z % 64 == 9) modifier.setBlock(
                x,
                combined.toInt(),
                z,
                Block.TUBE_CORAL_BLOCK.withDefaultHandler()
            )
            if (x % 64 == 12 && z % 64 == 12) modifier.setBlock(
                x,
                combined.toInt(),
                z,
                Block.COPPER_BLOCK.withDefaultHandler()
            )
        }
    }
}