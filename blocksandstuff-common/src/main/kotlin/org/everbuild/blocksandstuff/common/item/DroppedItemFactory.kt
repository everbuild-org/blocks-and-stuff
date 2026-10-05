package org.everbuild.blocksandstuff.common.item

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule.UpdateState
import net.minestom.server.item.ItemStack

interface DroppedItemFactory {
    fun spawn(instance: Instance, position: Point, block: Block)
    fun spawn(instance: Instance, position: Point, item: ItemStack)

    /**
     * Drops an item in front of the player, thrown in the direction they are looking.
     */
    fun spawnFromPlayer(player: Player, item: ItemStack) {
        val instance = player.instance ?: return
        spawn(instance, player.position, item)
    }

    companion object {
        @JvmStatic
        var current: DroppedItemFactory = DefaultDroppedItemFactory()
        @JvmStatic
        var doDropItems: Boolean = true

        private var playerDropListenerEnabled = false

        /**
         * Minestom's [net.minestom.server.entity.Player.dropItem] only dispatches an
         * [ItemDropEvent]; it does not spawn an item entity itself. Without a listener the
         * dropped stack is lost (e.g. the cursor item when closing an inventory).
         *
         * Call this once during server setup to route these drops through [current].
         */
        @JvmStatic
        fun enablePlayerDrops() {
            if (playerDropListenerEnabled) return
            playerDropListenerEnabled = true

            MinecraftServer.getGlobalEventHandler().addListener(ItemDropEvent::class.java) { event ->
                if (event.isCancelled) return@addListener
                maybeDropFromPlayer(event.player, event.itemStack)
            }
        }

        fun maybeDropFromPlayer(player: Player, item: ItemStack) {
            if (doDropItems) {
                current.spawnFromPlayer(player, item)
            }
        }

        fun maybeDrop(instance: Instance, position: Point, block: Block) {
            if (doDropItems) {
                current.spawn(instance, position, block)
            }
        }

        fun maybeDrop(state: UpdateState) {
            if (doDropItems) {
                current.spawn(state.instance as Instance, state.blockPosition, state.currentBlock)
            }
        }

        fun maybeDrop(instance: Instance, position: Point, item: ItemStack) {
            if (doDropItems) {
                current.spawn(instance, position, item)
            }
        }
    }
}