package org.everbuild.blocksandstuff.blocks.group

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.group.block.AggregateTagBlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockBlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.ExcludeBlockRule
import org.everbuild.blocksandstuff.blocks.group.block.ListBlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.TagBlockGroup

abstract class VanillaRuleset<GroupImpl, Functor> {
    val ALL: ArrayList<GroupImpl> = ArrayList()

    protected fun group(blockGroup: BlockGroup, valueFunction: Functor): GroupImpl {
        val result = createGroup(blockGroup, valueFunction)
        ALL.add(result)
        return result
    }

    protected fun all(vararg blockGroups: BlockGroup): BlockGroup {
        return AggregateTagBlockGroup(*blockGroups)
    }

    protected fun byTag(@KeyPattern tag: String): BlockGroup {
        return TagBlockGroup(Key.key(tag))
    }

    protected fun byBlock(block: Block): BlockGroup {
        return BlockBlockGroup(block)
    }

    protected fun byList(block: Collection<Block>): BlockGroup {
        return ListBlockGroup(block)
    }

    protected fun byExclusion(positive: BlockGroup, negative: BlockGroup): BlockGroup {
        return ExcludeBlockRule(positive, negative)
    }

    protected abstract fun createGroup(blockGroup: BlockGroup, valueFunction: Functor): GroupImpl
}