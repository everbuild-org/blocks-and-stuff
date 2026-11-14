package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SkullDisplayTest {
    @BeforeEach
    fun setup() {
        MinecraftServer.init()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun exposesProfileTags() {
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addSkulls() })

        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler(Block.PLAYER_HEAD.key().asString()))
        val keys = handler.getBlockEntityTags().map { it.key }

        Assertions.assertTrue(keys.contains("profile"), "expected profile tag, got $keys")
        Assertions.assertTrue(keys.contains("note_block_sound"))
        Assertions.assertTrue(keys.contains("custom_name"))
    }
}
