package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class RedstoneWireRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler
    private val pos = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addRedstoneWire() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:redstone_wire"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun togglesIsolatedCrossAndDot() {
        instance.setBlock(pos, Block.REDSTONE_WIRE.withProperties(SIDES.associateWith { "side" }))
        Assertions.assertEquals("side", instance.getBlock(pos).getProperty("north"))

        interact()
        Assertions.assertEquals("none", instance.getBlock(pos).getProperty("north"))
        Assertions.assertEquals("none", instance.getBlock(pos).getProperty("east"))
        Assertions.assertEquals("none", instance.getBlock(pos).getProperty("south"))
        Assertions.assertEquals("none", instance.getBlock(pos).getProperty("west"))

        interact()
        Assertions.assertEquals("side", instance.getBlock(pos).getProperty("north"))
    }

    @Test
    fun connectedWireDoesNotToggle() {
        instance.setBlock(pos, Block.REDSTONE_WIRE.withProperties(SIDES.associateWith { "side" }))
        instance.setBlock(pos.add(1.0, 0.0, 0.0), Block.REDSTONE_LAMP)

        interact()

        Assertions.assertEquals("side", instance.getBlock(pos).getProperty("north"))
    }

    private fun interact() {
        val block = instance.getBlock(pos)
        val player = mock<Player> {
            on { isSneaking } doReturn false
            on { itemInMainHand } doReturn ItemStack.AIR
        }
        handler.onInteract(BlockHandler.Interaction(block, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN))
    }

    private companion object {
        private val SIDES = listOf("north", "east", "south", "west")
    }
}
