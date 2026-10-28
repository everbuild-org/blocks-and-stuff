package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.Block

/**
 * Vanilla note-block instrument selection. The instrument is derived from the block directly
 * below the note block, or from a mob head directly above it.
 */
internal object NoteBlockInstruments {
    fun forBlockBelow(block: Block): String {
        val key = block.key().value()
        return when {
            isCopper(key) -> copperInstrument(key)
            key == "gold_block" -> "bell"
            key == "clay" -> "flute"
            key == "packed_ice" -> "chime"
            key == "bone_block" -> "xylophone"
            key == "iron_block" -> "iron_xylophone"
            key == "soul_sand" -> "cow_bell"
            key == "pumpkin" -> "didgeridoo"
            key == "emerald_block" -> "bit"
            key == "hay_block" -> "banjo"
            key == "glowstone" -> "pling"
            key.contains("wool") || key.contains("carpet") -> "guitar"
            isSandOrGravel(key) -> "snare"
            isGlass(key) -> "hat"
            isWood(key) -> "bass"
            isStone(key) -> "basedrum"
            else -> "harp"
        }
    }

    fun fromHead(block: Block): String? =
        when (block.key().value()) {
            "zombie_head", "zombie_wall_head" -> "zombie"
            "skeleton_skull", "skeleton_wall_skull" -> "skeleton"
            "wither_skeleton_skull", "wither_skeleton_wall_skull" -> "wither_skeleton"
            "creeper_head", "creeper_wall_head" -> "creeper"
            "dragon_head", "dragon_wall_head" -> "dragon"
            "piglin_head", "piglin_wall_head" -> "piglin"
            "player_head", "player_wall_head" -> "custom_head"
            else -> null
        }

    private fun isCopper(key: String): Boolean {
        val value =
            key
                .removePrefix("waxed_")
                .removePrefix("exposed_")
                .removePrefix("weathered_")
                .removePrefix("oxidized_")
        return value == "copper" ||
            value == "copper_block" ||
            value == "chiseled_copper" ||
            value == "cut_copper" ||
            value == "cut_copper_stairs" ||
            value == "cut_copper_slab" ||
            value == "copper_grate"
    }

    private fun copperInstrument(key: String): String =
        when {
            key.contains("oxidized") -> "trumpet_oxidized"
            key.contains("weathered") -> "trumpet_weathered"
            key.contains("exposed") -> "trumpet_exposed"
            else -> "trumpet"
        }

    private fun isSandOrGravel(key: String): Boolean = key in SAND_BLOCKS || key.endsWith("_concrete_powder")

    private fun isGlass(key: String): Boolean = key.contains("glass") || key in GLASS_LIKE_BLOCKS

    private fun isWood(key: String): Boolean =
        key.contains("planks") ||
            key.contains("_log") ||
            key.contains("_wood") ||
            key.contains("stripped_") ||
            key.contains("fence") ||
            key.contains("sign") ||
            key.contains("banner") ||
            key.contains("shelf") ||
            key.endsWith("_chest") ||
            key == "chest" ||
            key.contains("barrel") ||
            key.contains("bamboo") ||
            key.contains("mushroom") ||
            key in WOOD_LIKE_BLOCKS

    private fun isStone(key: String): Boolean =
        key.contains("stone") ||
            key.contains("cobble") ||
            key.contains("deepslate") ||
            key.contains("granite") ||
            key.contains("diorite") ||
            key.contains("andesite") ||
            key.contains("tuff") ||
            key.contains("calcite") ||
            key.contains("dripstone") ||
            key.contains("basalt") ||
            key.contains("blackstone") ||
            key.contains("netherrack") ||
            key.contains("obsidian") ||
            key.contains("bedrock") ||
            key.contains("purpur") ||
            key.contains("quartz") ||
            key.contains("brick") ||
            key.contains("terracotta") ||
            key.contains("prismarine") ||
            key.contains("magma") ||
            key.contains("ancient_debris") ||
            key.contains("sandstone") ||
            key in STONE_LIKE_BLOCKS

    private val SAND_BLOCKS =
        setOf("sand", "red_sand", "suspicious_sand", "gravel", "suspicious_gravel")

    private val GLASS_LIKE_BLOCKS =
        setOf("sea_lantern", "beacon", "conduit")

    private val WOOD_LIKE_BLOCKS =
        setOf(
            "crafting_table",
            "cartography_table",
            "fletching_table",
            "smithing_table",
            "loom",
            "bookshelf",
            "chiseled_bookshelf",
            "lectern",
            "jukebox",
            "note_block",
            "composter",
            "bee_nest",
            "beehive",
            "campfire",
            "soul_campfire",
            "ladder",
            "scaffolding",
            "mangrove_roots",
            "mushroom_stem",
            "brown_mushroom_block",
            "red_mushroom_block",
        )

    private val STONE_LIKE_BLOCKS =
        setOf(
            "end_stone",
            "netherite_block",
            "resin_brick",
            "mud_brick",
        )
}
