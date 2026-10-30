package org.everbuild.blocksandstuff.common.item

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule.UpdateState
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.time.TimeUnit

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
        private var itemPickupListenerEnabled = false

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

        /**
         * Minestom only removes the [ItemEntity] on pickup and sends the collect animation; it
         * does not move the stack into the player's inventory. This listener fills the inventory,
         * cancels the pickup when nothing fits, and spawns the leftover as a new entity on a
         * partial pickup.
         */
        @JvmStatic
        fun enableItemPickup() {
            if (itemPickupListenerEnabled) return
            itemPickupListenerEnabled = true

            MinecraftServer.getGlobalEventHandler().addListener(PickupItemEvent::class.java) { event ->
                val player = event.entity as? Player ?: return@addListener
                val item = event.itemStack
                val rest = player.inventory.addItemStack(item, TransactionOption.ALL)

                if (rest.isAir) return@addListener
                if (rest.amount() == item.amount()) {
                    event.isCancelled = true
                    return@addListener
                }

                val leftover = ItemEntity(rest)
                leftover.setInstance(event.instance, event.itemEntity.position)
                leftover.setPickupDelay(1, TimeUnit.SECOND)
                leftover.scheduleRemove(5, TimeUnit.MINUTE)
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