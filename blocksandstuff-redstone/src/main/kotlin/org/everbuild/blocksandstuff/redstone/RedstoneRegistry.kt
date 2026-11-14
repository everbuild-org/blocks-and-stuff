package org.everbuild.blocksandstuff.redstone

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.tag.BlockTags

/**
 * Registry of redstone components.
 *
 * Blocks can be registered by concrete [Block] or by tag. Tag membership is resolved lazily from
 * [BlockTags] and cached per block key. Call [refresh] if tags are (re)loaded after registration.
 */
class RedstoneRegistry {
    private val byBlock = HashMap<Key, ComponentFactory>()
    private val byTag = LinkedHashMap<String, ComponentFactory>()
    private val tagCache = HashMap<Key, ComponentFactory?>()

    fun register(
        selector: ComponentSelector,
        factory: ComponentFactory,
    ) {
        when (selector) {
            is ComponentSelector.ByBlock -> byBlock[selector.block.key()] = factory
            is ComponentSelector.ByTag -> byTag[selector.tag] = factory
        }
        tagCache.clear()
    }

    fun register(
        block: Block,
        factory: ComponentFactory,
    ) {
        register(ComponentSelector.ByBlock(block), factory)
    }

    /** Convenience: registers a simple, non-directional or directional [PowerSource]. */
    fun source(
        block: Block,
        source: PowerSource,
    ) = source(ComponentSelector.ByBlock(block), source)

    fun source(
        selector: ComponentSelector,
        source: PowerSource,
    ) {
        register(selector) { concrete -> SourceComponent(concrete, source) }
    }

    /** Convenience: registers a [PowerConsumer] that reacts to changing input. */
    fun consumer(
        block: Block,
        consumer: PowerConsumer,
    ) = consumer(ComponentSelector.ByBlock(block), consumer)

    fun consumer(
        selector: ComponentSelector,
        consumer: PowerConsumer,
    ) {
        register(selector) { ConsumerComponent(consumer) }
    }

    fun factoryFor(block: Block): ComponentFactory? {
        val key = block.key()
        byBlock[key]?.let { return it }
        if (tagCache.containsKey(key)) return tagCache[key]

        for ((tag, factory) in byTag) {
            if (BlockTags.getTaggedWith(tag).any { it.key() == key }) {
                tagCache[key] = factory
                return factory
            }
        }

        tagCache[key] = null
        return null
    }

    fun clearTagCache() {
        tagCache.clear()
    }
}
