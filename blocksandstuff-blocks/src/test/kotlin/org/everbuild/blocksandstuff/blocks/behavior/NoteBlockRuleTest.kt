package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.blocks.BlockBehaviorRuleRegistrations
import org.everbuild.blocksandstuff.blocks.PlacedHandlerRegistration
import org.everbuild.blocksandstuff.blocks.redstone.vanilla.NoteBlockInstruments
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class NoteBlockRuleTest {
    private lateinit var instance: Instance
    private val position = Pos(4.0, 64.0, 4.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun context(block: Block): InteractionActivityContext {
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.CREATIVE
                on { itemInMainHand } doReturn ItemStack.AIR
            }
        return InteractionActivityContext(block, instance, BlockFace.TOP, position.asBlockVec(), Vec.ZERO, player, PlayerHand.MAIN)
    }

    private fun interaction(block: Block): BlockHandler.Interaction {
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.CREATIVE
                on { itemInMainHand } doReturn ItemStack.AIR
            }
        return BlockHandler.Interaction(block, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN)
    }

    @Test
    fun rightClickAdvancesPitch() {
        val block = Block.NOTE_BLOCK.withProperty("note", "0")
        instance.setBlock(position, block)

        NOTE_BLOCK_RULE.evaluate(context(block))

        Assertions.assertEquals("1", instance.getBlock(position).getProperty("note"))
    }

    @Test
    fun rightClickWrapsPitchAfterHighestNote() {
        val block = Block.NOTE_BLOCK.withProperty("note", "24")
        instance.setBlock(position, block)

        NOTE_BLOCK_RULE.evaluate(context(block))

        Assertions.assertEquals("0", instance.getBlock(position).getProperty("note"))
    }

    @Test
    fun rightClickChangesPitchWhenBlockedAbove() {
        val block = Block.NOTE_BLOCK.withProperty("note", "5")
        instance.setBlock(position, block)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.STONE)

        NOTE_BLOCK_RULE.evaluate(context(block))

        Assertions.assertEquals("6", instance.getBlock(position).getProperty("note"))
    }

    @Test
    fun ruleIsRegisteredAsBlockHandler() {
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addNoteBlocks() })

        Assertions.assertNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:note_block"))
    }

    @Test
    fun rightClickSelectsInstrumentFromBlockBelow() {
        val block = Block.NOTE_BLOCK.withProperty("note", "0")
        instance.setBlock(position, block)
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)

        NOTE_BLOCK_RULE.evaluate(context(block))

        Assertions.assertEquals("basedrum", instance.getBlock(position).getProperty("instrument"))
    }

    @Test
    fun instrumentMappingMatchesVanillaBlocks() {
        val expectations =
            mapOf(
                Block.STONE to "basedrum",
                Block.OAK_PLANKS to "bass",
                Block.SAND to "snare",
                Block.GLASS to "hat",
                Block.GOLD_BLOCK to "bell",
                Block.CLAY to "flute",
                Block.PACKED_ICE to "chime",
                Block.WHITE_WOOL to "guitar",
                Block.BONE_BLOCK to "xylophone",
                Block.IRON_BLOCK to "iron_xylophone",
                Block.SOUL_SAND to "cow_bell",
                Block.PUMPKIN to "didgeridoo",
                Block.EMERALD_BLOCK to "bit",
                Block.HAY_BLOCK to "banjo",
                Block.GLOWSTONE to "pling",
                Block.COPPER_BLOCK to "trumpet",
                Block.EXPOSED_COPPER to "trumpet_exposed",
                Block.OXIDIZED_COPPER to "trumpet_oxidized",
                Block.DIRT to "harp",
            )

        for ((block, instrument) in expectations) {
            Assertions.assertEquals(instrument, NoteBlockInstruments.forBlockBelow(block), "for ${block.key()}")
        }
    }

    @Test
    fun registerDefaultRegistersNoteBlockHandler() {
        BlockBehaviorRuleRegistrations.registerDefault()

        Assertions.assertNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:note_block"))
    }

    @Test
    fun settingNoteBlockAttachesHandler() {
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addNoteBlocks() })
        PlacedHandlerRegistration.registerDefault()

        instance.setBlock(position, Block.NOTE_BLOCK)

        Assertions.assertNotNull(instance.getBlock(position).handler())
    }

    @Test
    fun blockHandlerAdvancesPitch() {
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addNoteBlocks() })
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:note_block"))
        instance.setBlock(position, Block.NOTE_BLOCK.withHandler(handler))

        handler.onInteract(interaction(instance.getBlock(position)))

        Assertions.assertEquals("1", instance.getBlock(position).getProperty("note"))
    }
}
