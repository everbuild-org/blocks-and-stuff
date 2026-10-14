@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.everbuild.blocksandstuff.blocks.group

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.blocks.behavior.*
import org.everbuild.blocksandstuff.blocks.group.behaviour.BehaviourGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

object VanillaBlockBehaviour : VanillaRuleset<BehaviourGroup, (Block) -> BlockHandler>() {
    val RULESET: BlockRuleset =
        BlockRuleset.build {
            addStripping()
            addWaxing()
            addWorkStations()
            addDoors()
            addTrapdoors()
            addFenceGates()
            addButtons()
            addLevers()
            addPressurePlates()
            addCandles()
            addSigns()
            addCakes()
            addCandleCakes()
            addConcretePowders()
            addSkulls()
            addFarmland()
            addCorals()
            addSweetBerryBushes()
            addCopperOxidation()
        }

    override fun createGroup(
        blockGroup: BlockGroup,
        valueFunction: (Block) -> BlockHandler,
    ): BehaviourGroup = BehaviourGroup(blockGroup) { valueFunction(it) }

    fun group(
        blockGroup: BlockGroup,
        handler: BlockHandler,
    ): BehaviourGroup =
        BehaviourGroup(blockGroup) { handler }.also {
            ALL.add(it)
        }
}
