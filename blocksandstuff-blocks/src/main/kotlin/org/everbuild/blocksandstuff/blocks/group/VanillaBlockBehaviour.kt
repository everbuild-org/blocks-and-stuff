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
            addHoes()
            addWorkStations()
            addDoors()
            addTrapdoors()
            addFenceGates()
            addButtons()
            addLevers()
            addPressurePlates()
            addCandles()
            addRepeaters()
            addComparators()
            addNoteBlocks()
            addRedstoneWire()
            addDaylightDetectors()
            addSigns()
            addCakes()
            addCandleCakes()
            addComposters()
            addCampfires()
            addTntIgnition()
            addConcretePowders()
            addSkulls()
            addFarmland()
            addCorals()
            addSweetBerryBushes()
            addCopperOxidation()
            addPlantGrowth()
            addStemGrowth()
            addVerticalGrowth()
            addSaplingGrowth()
            addBoneMeal()
        }

    val FLOWER_POTS: BehaviourGroup =
        BehaviourGroup(byList(CustomPlants.allPottedBlocks() + Block.FLOWER_POT)) { FlowerPotHandler(it.key()) }
            .also { ALL.add(it) }

    val LECTERN: BehaviourGroup =
        BehaviourGroup(byBlock(Block.LECTERN)) { LecternHandler() }
            .also { ALL.add(it) }

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
