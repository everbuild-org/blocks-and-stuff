package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.group.VanillaBlockBehaviour
import org.everbuild.blocksandstuff.blocks.group.behaviour.BehaviourGroup
import org.everbuild.blocksandstuff.blocks.randomticking.getRandomTickingEventNode

object BlockBehaviorRuleRegistrations {
    private val logger = MinecraftServer.LOGGER

    @JvmStatic
    fun registerRandomTickingGlobally() {
        MinecraftServer.getGlobalEventHandler().addChild(getRandomTickingEventNode())
    }


    @JvmStatic
    fun registerDefault() {
        registerDefault(listOf())
    }

    @JvmStatic
    fun registerDefault(exclude: List<Block>) {
        register(exclude, *VanillaBlockBehaviour.ALL.toTypedArray<BehaviourGroup>())
        registerRandomTickingGlobally()
    }


    @JvmStatic
    fun register(vararg blockGroups: BehaviourGroup) {
        register(listOf(), *blockGroups)
    }

    @JvmStatic
    fun register(exclude: List<Block>, vararg blockGroups: BehaviourGroup) {
        val blockManager = MinecraftServer.getBlockManager()
        var count = 0

        for (group in blockGroups) {
            val blockGroup = group.blockGroup
            for (block in blockGroup.allMatching()) {
                if (exclude.contains(block)) continue
                count++
                val handler = group.createHandler(block)
                blockManager.registerHandler(block.key().asString()) { handler }
            }
        }

        logger.info("Registered $count handlers")
    }
}