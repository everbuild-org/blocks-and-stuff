package org.everbuild.blocksandstuff.blocks

import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.behavior.BellInteraction
import org.everbuild.blocksandstuff.blocks.behavior.BeaconMenus
import org.everbuild.blocksandstuff.blocks.behavior.BedSleepTracker
import org.everbuild.blocksandstuff.blocks.behavior.BookUseListener
import org.everbuild.blocksandstuff.blocks.behavior.CushionBehaviour
import org.everbuild.blocksandstuff.blocks.behavior.CopperDoorSync
import org.everbuild.blocksandstuff.blocks.behavior.IceBreakListener
import org.everbuild.blocksandstuff.blocks.behavior.FireStarterListener
import org.everbuild.blocksandstuff.blocks.behavior.PotentSulfurHandler
import org.everbuild.blocksandstuff.blocks.behavior.SignEditListener
import org.everbuild.blocksandstuff.blocks.group.VanillaBlockBehaviour
import org.everbuild.blocksandstuff.blocks.group.behaviour.BehaviourGroup
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider
import org.everbuild.twaddle.random_ticking.getRandomTickingEventNode
import org.everbuild.twaddle.random_ticking.randomTickSpeed
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

object BlockBehaviorRuleRegistrations {
    private val logger = ComponentLogger.logger(BlockBehaviorRuleRegistrations::class.java)

    /**
     * Installs Twaddle's external random-tick node and keeps [net.minestom.server.instance.Instance.randomTickSpeed]
     * in sync with [InstanceOptionsProvider]. Call this when registering behaviour groups manually;
     * [registerDefault] already does it.
     */
    @JvmStatic
    fun registerRandomTickingGlobally() {
        val globalHandler = MinecraftServer.getGlobalEventHandler()
        globalHandler.addChild(
            EventNode
                .type("blocksandstuff:random-tick-speed-sync", EventFilter.INSTANCE)
                .addListener(InstanceTickEvent::class.java) { event ->
                    val options = InstanceOptionsProvider.getForInstance(event.instance).randomTickSpeed
                    if (event.instance.randomTickSpeed != options) {
                        event.instance.randomTickSpeed = options
                    }
                }.setPriority(-1),
        )
        globalHandler.addChild(getRandomTickingEventNode())
    }

    @JvmStatic
    fun registerDefault() {
        registerDefault(listOf())
    }

    @JvmStatic
    fun registerDefault(exclude: List<Block>) {
        register(exclude, *VanillaBlockBehaviour.ALL.toTypedArray<BehaviourGroup>())
        BlockRuleset.applyRulesAsDefault(
            if (exclude.isEmpty()) {
                VanillaBlockBehaviour.RULESET
            } else {
                VanillaBlockBehaviour.RULESET.modify { removeBlocks(*exclude.toTypedArray()) }
            },
        )
        PlacedHandlerRegistration.registerDefault()
        MinecraftServer.getBlockManager().registerHandler("minecraft:potent_sulfur") { PotentSulfurHandler() }
        BellInteraction.enable()
        BeaconMenus.enable()
        BedSleepTracker.enable()
        IceBreakListener.enable()
        FireStarterListener.enable()
        registerRandomTickingGlobally()
        NaturalBlockInteract.enable()
        SignEditListener.enable()
        BookUseListener.enable()
        CushionBehaviour.enable()
        CopperDoorSync.enable()
    }

    @JvmStatic
    fun register(vararg blockGroups: BehaviourGroup) {
        register(listOf(), *blockGroups)
    }

    @JvmStatic
    fun register(
        exclude: List<Block>,
        vararg blockGroups: BehaviourGroup,
    ) {
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
