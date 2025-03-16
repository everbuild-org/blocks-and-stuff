package org.everbuild.blocksandstuff.common.item

import kotlin.random.Random
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.time.TimeUnit

class DefaultDroppedItemFactory : DroppedItemFactory {
    override fun spawn(instance: Instance, position: Point, block: Block) {
        val item = block.registry().material() ?: return
        val entity = ItemEntity(ItemStack.of(item))
        entity.setPickupDelay(1, TimeUnit.SECOND) // 1s for natural drop
        entity.scheduleRemove(5, TimeUnit.MINUTE)
        entity.velocity = Vec(
            Random.nextDouble() * 2 - 1,
            4.0,
            Random.nextDouble() * 2 - 1
        )
        entity.setInstance(instance, position.add(0.5, 0.5, 0.5))
    }
}