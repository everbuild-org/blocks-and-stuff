package org.everbuild.blocksandstuff.blocks.behavior

import java.util.UUID
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

/**
 * Optional, per-player ender chest storage.
 *
 * Attach an implementation to a player via [ENDER_CHEST_PERSISTENCE]. When the tag is absent,
 * Blocks & Stuff neither loads nor saves ender chest contents. The implementation is resolved
 * from the tag when the chest is opened and retained until it is closed, so a save always targets
 * the same backend that was used for the load.
 *
 * [load] receives the player's UUID rather than a live player to keep storage decoupled from the
 * entity. Returned lists are applied to the ender chest slots by index; missing entries are empty.
 */
interface EnderChestPersistence {
    suspend fun load(uuid: UUID): List<ItemStack>

    suspend fun save(
        uuid: UUID,
        items: List<ItemStack>,
    )
}

val ENDER_CHEST_PERSISTENCE: Tag<EnderChestPersistence> =
    Tag.Transient("blocksandstuff:ender_chest_persistence")
