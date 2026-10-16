package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import org.everbuild.blocksandstuff.blocks.group.VanillaBlockBehaviour
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class VanillaBlockBehaviourTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            MinecraftServer.init()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            MinecraftServer.stopCleanly()
        }
    }

    @Test
    fun buildsVanillaRuleset() {
        BlockRuleset.applyRulesAsDefault(VanillaBlockBehaviour.RULESET)
    }

    @Test
    fun registersStemGrowth() {
        BlockRuleset.applyRulesAsDefault(VanillaBlockBehaviour.RULESET)

        Assertions.assertNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:pumpkin_stem"))
        Assertions.assertNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:melon_stem"))
    }
}
