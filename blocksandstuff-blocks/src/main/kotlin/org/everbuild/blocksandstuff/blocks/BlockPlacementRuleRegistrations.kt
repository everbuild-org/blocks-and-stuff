package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import org.everbuild.blocksandstuff.blocks.group.VanillaPlacementRules
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup

object BlockPlacementRuleRegistrations {
    @JvmStatic
    fun registerDefault() {
        register(*VanillaPlacementRules.ALL.toTypedArray<PlacementGroup>())
    }

    @JvmStatic
    fun register(vararg blockGroups: PlacementGroup) {
        val blockManager = MinecraftServer.getBlockManager()

        for (placementGroup in blockGroups) {
            val blockGroup = placementGroup.blockGroup
            for (block in blockGroup.allMatching()) {
                blockManager.registerBlockPlacementRule(placementGroup.createRule(block))
            }
        }
    }
}
