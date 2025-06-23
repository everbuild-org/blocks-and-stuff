package org.everbuild.blocksandstuff.blocks.group

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup
import org.everbuild.blocksandstuff.blocks.placement.*
import java.util.function.Function

object VanillaPlacementRules : VanillaRuleset<PlacementGroup, Function<Block, BlockPlacementRule>>() {

    val ROTATED_PILLARS = group(
        all(
            byTag("minecraft:logs"),
            byBlock(Block.MUDDY_MANGROVE_ROOTS),
            byBlock(Block.BAMBOO_BLOCK),
            byBlock(Block.STRIPPED_BAMBOO_BLOCK),
            byBlock(Block.BASALT),
            byBlock(Block.POLISHED_BASALT),
            byBlock(Block.QUARTZ_PILLAR),
            byBlock(Block.PURPUR_PILLAR),
            byBlock(Block.BONE_BLOCK),
            byBlock(Block.DEEPSLATE),
            byBlock(Block.INFESTED_DEEPSLATE),
            byBlock(Block.OCHRE_FROGLIGHT),
            byBlock(Block.VERDANT_FROGLIGHT),
            byBlock(Block.PEARLESCENT_FROGLIGHT),
            byBlock(Block.HAY_BLOCK),
        ),
        ::RotatedPillarPlacementRule
    )

    val SLAB = group(
        byTag("minecraft:slabs"),
        ::SlabPlacementRule
    )

    val WORKSTATIONS = group(
        all(
            byBlock(Block.FURNACE),
            byBlock(Block.BLAST_FURNACE),
            byBlock(Block.SMOKER),
            byBlock(Block.LECTERN),
            byBlock(Block.ENDER_CHEST),
            byBlock(Block.CHISELED_BOOKSHELF),
            byBlock(Block.CARVED_PUMPKIN),
            byBlock(Block.JACK_O_LANTERN),
            byBlock(Block.BEEHIVE),
            byBlock(Block.STONECUTTER),
            byBlock(Block.LOOM),
            byBlock(Block.BEE_NEST),
            byBlock(Block.END_PORTAL_FRAME),
        ),
        ::WorkstationPlacementRule
    )

    val ROTATED_WORKSTATIONS = group(
        all(
            byBlock(Block.ANVIL),
            byBlock(Block.CHIPPED_ANVIL),
            byBlock(Block.DAMAGED_ANVIL),
        ),
        ::InverseWorkstationPlacementRule
    )

    val AMETHYST = group(
        all(
            byBlock(Block.AMETHYST_CLUSTER),
            byBlock(Block.SMALL_AMETHYST_BUD),
            byBlock(Block.MEDIUM_AMETHYST_BUD),
            byBlock(Block.LARGE_AMETHYST_BUD),
        ),
        ::AmethystPlacementRule
    )

    val BAMBOO = group(
        all(
            byBlock(Block.BAMBOO),
            byBlock(Block.BAMBOO_SAPLING),
        ),
        ::BambooPlantPlacementRule
    )

    val BANNER = group(
        all(
            byTag("minecraft:banners"),
        ),
        ::BannerPlacementRule
    )

    val FACING = group(
        all(
            byBlock(Block.BARREL)
        ),
        ::FacingPlacementRule
    )

    val SIMPLE_WATERLOGGABLE = group(
        all(
            byBlock(Block.BARRIER),
            byBlock(Block.COPPER_GRATE),
            byBlock(Block.EXPOSED_COPPER_GRATE),
            byBlock(Block.WEATHERED_COPPER_GRATE),
            byBlock(Block.OXIDIZED_COPPER_GRATE),
            byBlock(Block.WAXED_COPPER_GRATE),
            byBlock(Block.WAXED_EXPOSED_COPPER_GRATE),
            byBlock(Block.WAXED_WEATHERED_COPPER_GRATE),
            byBlock(Block.WAXED_OXIDIZED_COPPER_GRATE),
            byBlock(Block.DRIED_GHAST),
            byBlock(Block.HEAVY_CORE)
        ),
        ::SimpleWaterloggablePlacementRule
    )

    val BEDS = group(
        all(
            byTag("minecraft:beds")
        ),
        ::BedPlacementRule
    )

    val CROPS = group(
        all(
            byTag("minecraft:crops")
        ),
        ::CropPlacementRule
    )

    val BELL = group(
        byBlock(Block.BELL),
        ::BellPlacementRule
    )

    val BIG_DRIPLEAF = group(
        all(
            byBlock(Block.BIG_DRIPLEAF),
            byBlock(Block.BIG_DRIPLEAF_STEM),
        ),
        ::BigDripleafPlacementRule
    )

    val BOTTOM_SUPPORTED = group(
        all(
            byTag("minecraft:pressure_plates"),
            byBlock(Block.CAKE),
        ),
        ::SupportedBelowPlacementRule
    )

    val PIN_BOTTOM_SUPPORTED = group(
        all(
            byTag("minecraft:wool_carpets"),
        ),
        ::PinSupportedBelowPlacementRule
    )

    val BUTTONS = group(
        all(
            byTag("minecraft:buttons"),
        ),
        ::FacedFacingPlacementRule
    )

    val CACTUS = group(
        byBlock(Block.CACTUS),
        ::CactusPlacementRule
    )

    val CAMPFIRE = group(
        all(
            byBlock(Block.CAMPFIRE),
            byBlock(Block.SOUL_CAMPFIRE),
        ),
        ::CampfireBlockPlacementRule
    )

    val CANDLES = group(
        byTag("minecraft:candles"),
        ::CandlePlacementRule
    )

    val VINES_TOP = group(
        all(
            byBlock(Block.CAVE_VINES),
            byBlock(Block.CAVE_VINES),
            byBlock(Block.WEEPING_VINES),
            byBlock(Block.WEEPING_VINES_PLANT),
        ),
        ::TopAttachedVinePlacementRule
    )

    val TRAPDOOR = group(
        all(
            byTag("minecraft:trapdoors"),
        ),
        ::TrapdoorPlacementRule
    )

    val FENCE = group(
        all(
            byTag("minecraft:fences"),
        ),
        ::FencePlacementRule
    )

    val FENCE_GATE = group(
        all(
            byTag("minecraft:fence_gates"),
        ),
        ::FenceGatePlacementRule
    )

    val STAIRS = group(
        all(
            byTag("minecraft:stairs"),
        ),
        ::StairsPlacementRule
    )

    val VERTICAL_SLIM = group(
        all(
            byBlock(Block.IRON_BARS),
            byTag("blocksandstuff:glass_panes"),
        ),
        ::VerticalSlimBlockPlacementRule
    )

    val LADDERS = group(
        all(
            byBlock(Block.LADDER),
        ),
        ::LadderPlacementRule
    )

    val TORCHES = group(
        all(
            byBlock(Block.TORCH),
            byBlock(Block.SOUL_TORCH),
            byBlock(Block.REDSTONE_TORCH),
            byBlock(Block.WALL_TORCH),
            byBlock(Block.SOUL_WALL_TORCH),
            byBlock(Block.SOUL_FIRE),
        ),
        ::TorchPlacementRule
    )

    val WALLS = group(
        byTag("minecraft:walls"),
        ::WallBlockPlacementRule
    )

    val DOORS = group(
        byTag("minecraft:doors"),
        ::DoorPlacementRule
    )

    val LANTERNS = group(
        all(
            byBlock(Block.LANTERN),
            byBlock(Block.SOUL_LANTERN)
        ),
        ::LanternPlacementRule
    )

    val GLAZED_TERRACOTTA = group(
        all(
            byBlock(Block.MAGENTA_GLAZED_TERRACOTTA),
            byBlock(Block.WHITE_GLAZED_TERRACOTTA),
            byBlock(Block.LIGHT_GRAY_GLAZED_TERRACOTTA),
            byBlock(Block.GRAY_GLAZED_TERRACOTTA),
            byBlock(Block.BLACK_GLAZED_TERRACOTTA),
            byBlock(Block.BROWN_GLAZED_TERRACOTTA),
            byBlock(Block.RED_GLAZED_TERRACOTTA),
            byBlock(Block.ORANGE_GLAZED_TERRACOTTA),
            byBlock(Block.YELLOW_GLAZED_TERRACOTTA),
            byBlock(Block.LIME_GLAZED_TERRACOTTA),
            byBlock(Block.GREEN_GLAZED_TERRACOTTA),
            byBlock(Block.CYAN_GLAZED_TERRACOTTA),
            byBlock(Block.LIGHT_BLUE_GLAZED_TERRACOTTA),
            byBlock(Block.BLUE_GLAZED_TERRACOTTA),
            byBlock(Block.PURPLE_GLAZED_TERRACOTTA),
            byBlock(Block.MAGENTA_GLAZED_TERRACOTTA),
            byBlock(Block.PINK_GLAZED_TERRACOTTA)
        ),
        ::GlazedTerracottaPlacementRule
    )

    val CHAINS = group(
        byBlock(Block.CHAIN),
        ::ChainPlacementRule
    )

    val TALL_FLOWERS = group(
        all(
            byBlock(Block.PEONY),
            byBlock(Block.TALL_GRASS),
            byBlock(Block.LARGE_FERN),
            byBlock(Block.SUNFLOWER),
            byBlock(Block.LILAC),
            byBlock(Block.ROSE_BUSH),
        ),
        ::TallFlowerPlacementRule
    )

    val SIGNS = group(
        all(
            byTag("minecraft:all_signs"),
        ),
        ::SignPlacementRule
    )

    val CHESTS = group(
        all(
            byTag("minecraft:chests"),
            byBlock(Block.CHEST),
            byBlock(Block.TRAPPED_CHEST),
        ),
        ::ChestPlacementRule
    )

    val HOPPERS = group(
        all(
            byBlock(Block.HOPPER)
        ),
        ::HopperPlacementRule
    )

    val SHULKERBOXES = group(
        all(
            byTag("minecraft:shulker_boxes")
        ),
        ::ShulkerPlacementRule
    )

    val FLOOR_FLOWER = group(
        all(
            byBlock(Block.WILDFLOWERS),
            byBlock(Block.LEAF_LITTER),
            byBlock(Block.PINK_PETALS)
        ),
        ::FloorFillerPlacementRule
    )

    val CORALS = group(
        all(
            byTag("minecraft:corals"),
            byBlock(Block.DEAD_TUBE_CORAL),
            byBlock(Block.DEAD_BRAIN_CORAL),
            byBlock(Block.DEAD_BUBBLE_CORAL),
            byBlock(Block.DEAD_FIRE_CORAL),
            byBlock(Block.DEAD_HORN_CORAL),
            byBlock(Block.DEAD_TUBE_CORAL_FAN),
            byBlock(Block.DEAD_BRAIN_CORAL_FAN),
            byBlock(Block.DEAD_BUBBLE_CORAL_FAN),
            byBlock(Block.DEAD_FIRE_CORAL_FAN),
            byBlock(Block.DEAD_HORN_CORAL_FAN),
        ),
        ::CoralPlacementRule
    )

    val WALL_CORALS = group(
        all(
            byBlock(Block.TUBE_CORAL_WALL_FAN),
            byBlock(Block.BRAIN_CORAL_WALL_FAN),
            byBlock(Block.BUBBLE_CORAL_WALL_FAN),
            byBlock(Block.FIRE_CORAL_WALL_FAN),
            byBlock(Block.HORN_CORAL_WALL_FAN),
            byBlock(Block.DEAD_TUBE_CORAL_WALL_FAN),
            byBlock(Block.DEAD_BRAIN_CORAL_WALL_FAN),
            byBlock(Block.DEAD_BUBBLE_CORAL_WALL_FAN),
            byBlock(Block.DEAD_FIRE_CORAL_WALL_FAN),
            byBlock(Block.DEAD_HORN_CORAL_WALL_FAN)
        ),
        ::WallCoralPlacementRule
    )

    val HEADS = group(
        all(
            byBlock(Block.SKELETON_SKULL),
            byBlock(Block.WITHER_SKELETON_SKULL),
            byBlock(Block.ZOMBIE_HEAD),
            byBlock(Block.CREEPER_HEAD),
            byBlock(Block.DRAGON_HEAD),
            byBlock(Block.PLAYER_HEAD),
            byBlock(Block.PIGLIN_HEAD)
        ),
        ::HeadPlacementRule
    )

    val SUGAR_CANE = group(
        byBlock(Block.SUGAR_CANE),
        ::SugarCanePlacementRule
    )

    val GROUNDED_PLANTS = group(
        all(
            byTag("minecraft:saplings"),
            byTag("minecraft:small_flowers"),
        ),
        ::GroundedPlantBlockPlacementRule
    )

    val CRAFTER = group(
        byBlock(Block.CRAFTER),
        ::CrafterPlacementRule
    )

    val LEVER = group(
        byBlock(Block.LEVER),
        ::LeverPlacementRule
    )

    val REDSTONE_STUFF = group(
        all(
            byBlock(Block.COMPARATOR),
            byBlock(Block.REPEATER),
        ),
        ::RedstoneStuffPlacementRule
    )

    override fun createGroup(
        blockGroup: BlockGroup,
        valueFunction: Function<Block, BlockPlacementRule>
    ): PlacementGroup {
        return PlacementGroup(blockGroup, valueFunction)
    }
}
