package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.group.VanillaPlacementRules
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup

object BlockPlacementRuleRegistrations {
    private val logger = MinecraftServer.LOGGER

    @JvmStatic
    fun registerDefault() {
        registerDefault(listOf())
    }

    @JvmStatic
    fun registerDefault(exclude: List<Block>) {
        register(exclude, *VanillaPlacementRules.ALL.toTypedArray<PlacementGroup>())
    }

    @JvmStatic
    fun register(vararg blockGroups: PlacementGroup) {
        register(listOf(), *blockGroups)
    }

    @JvmStatic
    fun register(exclude: List<Block>, vararg blockGroups: PlacementGroup) {
        val blockManager = MinecraftServer.getBlockManager()
        var count = 0

        for (placementGroup in blockGroups) {
            val blockGroup = placementGroup.blockGroup
            for (block in blockGroup.allMatching()) {
                if (exclude.contains(block)) continue
                count++
                blockManager.registerBlockPlacementRule(placementGroup.createRule(block))
            }
        }

        logger.info("Registered $count block placement rules")
    }
}
