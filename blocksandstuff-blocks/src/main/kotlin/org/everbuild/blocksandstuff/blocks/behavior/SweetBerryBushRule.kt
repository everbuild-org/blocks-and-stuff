package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.randomticking.RandomTickHandler
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import java.util.*
import kotlin.math.abs

class SweetBerryBushRule(private val block: Block) : BlockHandler, RandomTickHandler {
    override fun getKey(): Key = block.key()

    override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block? {
        val instance = randomTick.instance
        val blockPosition = randomTick.blockPosition
        val age = randomTick.block.getProperty("age")?.toIntOrNull() ?: 0

        if (instance.getSkyLight(blockPosition.blockX, blockPosition.blockY, blockPosition.blockZ) < 9)
            return randomTick.block

        if (Random().nextInt(5) == 0 && age < 3) {
            return randomTick.block.withProperty("age", (age + 1).toString())
        }

        return randomTick.block
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val player = interaction.player
        val itemInHand = player.itemInMainHand
        val instance = interaction.instance
        val blockPosition = interaction.blockPosition
        val age = interaction.block.getProperty("age")?.toIntOrNull() ?: 0

        if (age < 3) {
            if (itemInHand.material().equals(Material.BONE_MEAL)) {
                instance.setBlock(blockPosition, interaction.block.withProperty("age", (age + 1).toString()))

                if (!player.gameMode.equals(GameMode.CREATIVE)) {
                    player.itemInMainHand = itemInHand.withAmount(itemInHand.amount() - 1)
                }
            }
        } else {
            DroppedItemFactory.maybeDrop(
                instance,
                blockPosition,
                ItemStack.of(Material.SWEET_BERRIES, Random().nextInt(2, 3))
            )

            instance.setBlock(blockPosition, interaction.block.withProperty("age", "1"))
        }
        return super.onInteract(interaction)
    }

    override fun onTouch(touch: BlockHandler.Touch) {
        val entity = touch.touching

        if (entity !is LivingEntity) return
        if (entity.entityType == EntityType.FOX || entity.entityType == EntityType.BEE) return

        if (touch.block.getProperty("age")?.toIntOrNull() != 0) {
            val movementDistance = entity.previousPosition.sub(entity.position)
            if (movementDistance.x() > 0 || movementDistance.z() > 0) {
                val dx = abs(movementDistance.x())
                val dz = abs(movementDistance.z())
                if (dx >= 0.003F || dz >= 0.003F) {
                    entity.damage(
                        Damage(
                            DamageType.SWEET_BERRY_BUSH,
                            touch.touching,
                            null,
                            touch.blockPosition,
                            1.0F
                        )
                    )
                }
            }
        }
        super.onTouch(touch)
    }
}