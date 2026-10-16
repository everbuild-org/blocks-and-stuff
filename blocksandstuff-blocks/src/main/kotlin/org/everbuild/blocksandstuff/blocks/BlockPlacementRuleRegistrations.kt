package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.group.VanillaPlacementRules
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup
import org.everbuild.blocksandstuff.blocks.util.applyItemBlockEntityData
import org.everbuild.blocksandstuff.blocks.util.applyItemContainerContents

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
                blockManager.registerBlockPlacementRule(withBlockEntityData(placementGroup.createRule(block)))
            }
        }

        logger.info("Registered $count block placement rules")
    }

    private fun withBlockEntityData(rule: BlockPlacementRule): BlockPlacementRule {
        val block = rule.block
        return object : BlockPlacementRule(block) {
            override fun blockPlace(placementState: PlacementState): Block? =
                rule.blockPlace(placementState)
                    ?.applyItemBlockEntityData(placementState.usedItemStack)
                    ?.applyItemContainerContents(placementState.usedItemStack)

            override fun blockUpdate(updateState: UpdateState): Block = rule.blockUpdate(updateState)

            override fun isSelfReplaceable(replacement: Replacement): Boolean =
                rule.isSelfReplaceable(replacement)

            override fun maxUpdateDistance(): Int = rule.maxUpdateDistance()
        }
    }
}
