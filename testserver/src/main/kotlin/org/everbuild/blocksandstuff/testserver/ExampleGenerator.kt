package org.everbuild.blocksandstuff.testserver

import kotlin.math.sin
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.Generator

class ExampleGenerator : Generator {
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
                Block.WATER
            )
        }
    }
}