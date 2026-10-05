package org.everbuild.blocksandstuff.common.item

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.time.TimeUnit
import kotlin.random.Random

class DefaultDroppedItemFactory : DroppedItemFactory {
    override fun spawn(instance: Instance, position: Point, block: Block) {
        val item = block.material() ?: return
        spawn(instance, position, ItemStack.of(item))
    }

    override fun spawn(
        instance: Instance,
        position: Point,
        item: ItemStack
    ) {
        val entity = ItemEntity(item)
        entity.setPickupDelay(1, TimeUnit.SECOND) // 1s for natural drop
        entity.scheduleRemove(5, TimeUnit.MINUTE)
        entity.velocity = Vec(
            Random.nextDouble() * 2 - 1,
            2.0,
            Random.nextDouble() * 2 - 1
        )
        entity.setInstance(instance, position.add(0.5, 0.5, 0.5))
    }

    override fun spawnFromPlayer(player: Player, item: ItemStack) {
        val instance = player.instance ?: return

        // Minestom velocity is in blocks per second, vanilla throws at 0.3 blocks/tick (x20).
        val look = player.position.direction()
        val speed = 6.0
        val entity = ItemEntity(item)
        entity.setPickupDelay(1, TimeUnit.SECOND)
        entity.scheduleRemove(5, TimeUnit.MINUTE)
        entity.velocity = Vec(
            look.x() * speed,
            look.y() * speed + 2.0,
            look.z() * speed,
        )
        entity.setInstance(
            instance,
            player.position.add(look.x() * 0.3, 1.2, look.z() * 0.3)
        )
    }
}