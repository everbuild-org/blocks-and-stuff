package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.time.TimeUnit
import kotlin.random.Random

internal fun spawnItemEntity(
    instance: Instance,
    position: Point,
    facing: BlockFace,
    stack: ItemStack,
) {
    val direction = facing.toDirection()
    val item = ItemEntity(stack)
    item.setPickupDelay(10, TimeUnit.SERVER_TICK)
    item.scheduleRemove(5, TimeUnit.MINUTE)
    item.velocity =
        Vec(
            direction.normalX() * 0.6 + randomOffset(),
            direction.normalY() * 0.6 + randomOffset() + 0.1,
            direction.normalZ() * 0.6 + randomOffset(),
        )
    item.setInstance(
        instance,
        position.add(0.5, 0.5, 0.5).add(direction.vec().mul(0.6)),
    )
}

internal fun facingOf(block: Block): BlockFace =
    when (block.getProperty("facing")) {
        "up" -> BlockFace.TOP
        "north" -> BlockFace.NORTH
        "south" -> BlockFace.SOUTH
        "east" -> BlockFace.EAST
        "west" -> BlockFace.WEST
        else -> BlockFace.BOTTOM
    }

private fun randomOffset(): Double = (Random.nextDouble() - 0.5) * 0.2
