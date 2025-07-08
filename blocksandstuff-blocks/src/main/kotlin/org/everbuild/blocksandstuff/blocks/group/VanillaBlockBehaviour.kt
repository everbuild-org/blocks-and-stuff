package org.everbuild.blocksandstuff.blocks.group

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.blocks.behavior.*
import org.everbuild.blocksandstuff.blocks.group.behaviour.BehaviourGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup

object VanillaBlockBehaviour : VanillaRuleset<BehaviourGroup, (Block) -> BlockHandler>() {
    val CRAFTING_TABLE = group(
        byBlock(Block.CRAFTING_TABLE),
        GenericWorkStationRule(
            Block.CRAFTING_TABLE,
            InventoryType.CRAFTING,
            "container.crafting"
        )
    )

    val ANVIL = group(
        byBlock(Block.ANVIL),
        GenericWorkStationRule(
            Block.ANVIL,
            InventoryType.ANVIL,
            "container.repair"
        )
    )

    val BREWING_STAND = group(
        byBlock(Block.BREWING_STAND),
        GenericWorkStationRule(
            Block.BREWING_STAND,
            InventoryType.BREWING_STAND,
            "container.brewing"
        )
    )

    val LOOM = group(
        byBlock(Block.LOOM),
        GenericWorkStationRule(
            Block.LOOM,
            InventoryType.LOOM,
            "container.loom"
        )
    )

    val GRINDSTONE = group(
        byBlock(Block.GRINDSTONE),
        GenericWorkStationRule(
            Block.GRINDSTONE,
            InventoryType.GRINDSTONE,
            "container.grindstone"
        )
    )

    val SMITHING_TABLE = group(
        byBlock(Block.SMITHING_TABLE),
        GenericWorkStationRule(
            Block.SMITHING_TABLE,
            InventoryType.SMITHING,
            "container.upgrade"
        )
    )

    val CARTOGRAPHY_TABLE = group(
        byBlock(Block.CARTOGRAPHY_TABLE),
        GenericWorkStationRule(
            Block.CARTOGRAPHY_TABLE,
            InventoryType.CARTOGRAPHY,
            "container.cartography_table"
        )
    )

    val STONECUTTER = group(
        byBlock(Block.STONECUTTER),
        GenericWorkStationRule(
            Block.STONECUTTER,
            InventoryType.STONE_CUTTER,
            "container.stonecutter"
        )
    )

    val ENCHANTING_TABLE = group(
        byBlock(Block.ENCHANTING_TABLE),
        GenericWorkStationRule(
            Block.ENCHANTING_TABLE,
            InventoryType.ENCHANTMENT,
            "container.enchant"
        )
    )

    val TRAPDOOR = group(
        byTag("minecraft:wooden_trapdoors"),
        ::WoodenTrapDoorOpenRule
    )

    val FENCE_GATE = group(
        byTag("minecraft:fence_gates"),
        ::GateOpenRule
    )

    val COPPER = group(
        byList(CopperOxidationRule.Companion.getOxidizableBlocks()),
        ::CopperOxidationRule
    )

    val WOODEN_DOORS = group(
        byExclusion(
            byTag("minecraft:doors"),
            byBlock(Block.IRON_DOOR)
        ),
        ::DoorOpenRule
    )

    val SIGNS = group(
        byTag("minecraft:all_signs"),
        ::SignEditRule
    )

    val CAKE = group(
        byBlock(Block.CAKE),
        ::CakeEatRule
    )

    val CANDLE_CAKE = group(
        byTag("minecraft:candle_cakes"),
        ::CandleCakeRule
    )

    val STRIPPABLE_WOOD = group(
        byList(StrippingBehaviorRule.getStrippableBlocks()),
        ::StrippingBehaviorRule
    )

    val CONCRETE_POWDER = group(
        all(
            byBlock(Block.WHITE_CONCRETE_POWDER),
            byBlock(Block.ORANGE_CONCRETE_POWDER),
            byBlock(Block.MAGENTA_CONCRETE_POWDER),
            byBlock(Block.LIGHT_BLUE_CONCRETE_POWDER),
            byBlock(Block.YELLOW_CONCRETE_POWDER),
            byBlock(Block.LIME_CONCRETE_POWDER),
            byBlock(Block.PINK_CONCRETE_POWDER),
            byBlock(Block.GRAY_CONCRETE_POWDER),
            byBlock(Block.LIGHT_GRAY_CONCRETE_POWDER),
            byBlock(Block.CYAN_CONCRETE_POWDER),
            byBlock(Block.PURPLE_CONCRETE_POWDER),
            byBlock(Block.BLUE_CONCRETE_POWDER),
            byBlock(Block.BROWN_CONCRETE_POWDER),
            byBlock(Block.GREEN_CONCRETE_POWDER),
            byBlock(Block.RED_CONCRETE_POWDER),
            byBlock(Block.BLACK_CONCRETE_POWDER)
        ),
        ::ConcretePowderRule
    )

    override fun createGroup(
        blockGroup: BlockGroup,
        valueFunction: (Block) -> BlockHandler
    ): BehaviourGroup = BehaviourGroup(blockGroup) { it -> valueFunction(it) }

    fun group(blockGroup: BlockGroup, handler: BlockHandler): BehaviourGroup {
        return BehaviourGroup(blockGroup) { handler }.also {
            ALL.add(it)
        }
    }
}