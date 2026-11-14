package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.util.PrimedTnt
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import java.util.Random

fun interface DispenseBehavior {
    /** @return true when the item was handled, false to fall back to dropper-style ejection. */
    fun dispense(context: DispenseContext): Boolean
}

class DispenseContext internal constructor(
    val instance: Instance,
    val position: Point,
    val inventory: PhysicalInventory,
    val slot: Int,
    val item: ItemStack,
    val facing: BlockFace,
) {
    val target: Point get() = position.relative(facing)

    fun setSlot(replacement: ItemStack) {
        inventory.transact { set -> set(slot, replacement) }
        notifyRedstone()
    }

    fun consumeOne() {
        inventory.transact { set ->
            val current = inventory.getItemStack(slot)
            set(slot, if (current.amount() > 1) current.withAmount(current.amount() - 1) else ItemStack.AIR)
        }
        notifyRedstone()
    }

    fun ejectOne() {
        val current = inventory.getItemStack(slot)
        if (current.isAir) return

        consumeOne()
        spawnItemEntity(instance, position, facing, current.withAmount(1))
    }

    fun placeFluid(fluid: Block): Boolean {
        val target = position.relative(facing)
        val current = instance.getBlock(target)
        if (!current.air() && !current.replaceable()) return false

        return instance.placeBlock(BlockHandler.Placement(fluid, current, instance, target))
    }

    fun pickUpFluid(fluid: Block): Boolean {
        val target = position.relative(facing)
        val current = instance.getBlock(target)
        if (!current.compare(fluid) || !isSource(current)) return false

        instance.setBlock(target, Block.AIR, true)
        return true
    }

    private fun isSource(block: Block): Boolean = (block.getProperty("level")?.toIntOrNull() ?: 0) == 0

    private fun notifyRedstone() {
        RedstoneRegistrations.notifyContainerChanged(instance, position)
    }
}

private val projectileTypes =
    mapOf(
        Material.ARROW to "minecraft:arrow",
        Material.SPECTRAL_ARROW to "minecraft:spectral_arrow",
        Material.TIPPED_ARROW to "minecraft:arrow",
        Material.SNOWBALL to "minecraft:snowball",
        Material.EGG to "minecraft:egg",
        Material.EXPERIENCE_BOTTLE to "minecraft:experience_bottle",
    )

private val ageCropKeys by lazy {
    (
        CROP_BLOCKS +
            listOf(
                Block.NETHER_WART,
                Block.TORCHFLOWER_CROP,
                Block.PITCHER_CROP,
                Block.PUMPKIN_STEM,
                Block.MELON_STEM,
            )
    ).map { it.key() }.toSet()
}

private val saplingKeys by lazy {
    (SAPLING_BLOCKS + listOf(Block.AZALEA, Block.FLOWERING_AZALEA)).map { it.key() }.toSet()
}

private fun projectileType(material: Material): EntityType? = projectileTypes[material]?.let(EntityType::fromKey)

private fun vehicleType(material: Material): EntityType? {
    val key = material.key().value()
    if (key != "minecart" && !key.endsWith("_minecart") && !key.endsWith("_boat")) return null
    return EntityType.fromKey("minecraft:$key")
}

private fun spawnEggType(material: Material): EntityType? {
    val key = material.key().value()
    if (!key.endsWith("_spawn_egg")) return null
    return EntityType.fromKey("minecraft:${key.removeSuffix("_spawn_egg")}")
}

private fun ignite(context: DispenseContext): Boolean {
    val block = context.instance.getBlock(context.target)
    if (block.compare(Block.TNT)) {
        context.instance.setBlock(context.target, Block.AIR)
        PrimedTnt.spawn(context.instance, context.target)
        context.consumeOne()
        return true
    }
    if ((block.air() || block.replaceable()) && context.instance.getBlock(context.target.sub(0.0, 1.0, 0.0)).solid()) {
        context.instance.setBlock(context.target, Block.FIRE)
        context.consumeOne()
        return true
    }
    return false
}

private fun boneMeal(context: DispenseContext): Boolean {
    val block = context.instance.getBlock(context.target)
    val age = block.getProperty("age")?.toIntOrNull()
    if (age != null && block.key() in ageCropKeys) {
        val maxAge = maxAgeOf(block)
        if (age >= maxAge) return false
        val nextAge = minOf(maxAge, age + 2 + Random().nextInt(4))
        context.instance.setBlock(context.target, block.withProperty("age", nextAge.toString()))
        context.consumeOne()
        return true
    }
    if (block.key() in saplingKeys) {
        if (!growSapling(context.instance, context.target, block, Random())) return false
        context.consumeOne()
        return true
    }
    return false
}

private fun spawnEntity(
    context: DispenseContext,
    type: EntityType,
    yOffset: Double,
): Boolean {
    val block = context.instance.getBlock(context.target)
    if (!block.air() && !block.replaceable()) return false
    Entity(type).setInstance(context.instance, context.target.add(0.5, yOffset, 0.5))
    context.consumeOne()
    return true
}

private fun spawnProjectile(
    context: DispenseContext,
    type: EntityType,
): Boolean {
    val direction = context.facing.toDirection().vec()
    val projectile = EntityProjectile(null, type)
    projectile.velocity = Vec(direction.x() * 1.1, direction.y() * 1.1, direction.z() * 1.1)
    projectile.setInstance(context.instance, context.target.add(0.5, 0.5, 0.5))
    context.consumeOne()
    return true
}

object DispenseBehaviors {
    private data class Entry(
        val matches: (ItemStack) -> Boolean,
        val behavior: DispenseBehavior,
    )

    private val entries = mutableListOf<Entry>()

    init {
        resetDefaults()
    }

    /** Registers or overrides the behavior for [material]. The latest registration wins. */
    fun register(
        material: Material,
        behavior: DispenseBehavior,
    ) {
        register({ it.material() == material }, behavior)
    }

    /** Registers or overrides a behavior for any matching item. The latest registration wins. */
    fun register(
        matches: (ItemStack) -> Boolean,
        behavior: DispenseBehavior,
    ) {
        entries.add(Entry(matches, behavior))
    }

    fun find(item: ItemStack): DispenseBehavior? = entries.lastOrNull { it.matches(item) }?.behavior

    fun resetDefaults() {
        entries.clear()
        register(Material.WATER_BUCKET) { context ->
            if (!context.placeFluid(Block.WATER)) return@register false
            context.setSlot(ItemStack.of(Material.BUCKET))
            true
        }
        register(Material.LAVA_BUCKET) { context ->
            if (!context.placeFluid(Block.LAVA)) return@register false
            context.setSlot(ItemStack.of(Material.BUCKET))
            true
        }
        register(Material.POWDER_SNOW_BUCKET) { context ->
            if (!context.placeFluid(Block.POWDER_SNOW)) return@register false
            context.setSlot(ItemStack.of(Material.BUCKET))
            true
        }
        register(Material.BUCKET) { context ->
            when {
                context.pickUpFluid(Block.WATER) -> {
                    context.setSlot(ItemStack.of(Material.WATER_BUCKET))
                    true
                }

                context.pickUpFluid(Block.LAVA) -> {
                    context.setSlot(ItemStack.of(Material.LAVA_BUCKET))
                    true
                }

                context.pickUpFluid(Block.POWDER_SNOW) -> {
                    context.setSlot(ItemStack.of(Material.POWDER_SNOW_BUCKET))
                    true
                }

                else -> {
                    false
                }
            }
        }
        register(Material.FLINT_AND_STEEL) { context -> ignite(context) }
        register(Material.FIRE_CHARGE) { context -> ignite(context) }
        register(Material.TNT) { context ->
            val block = context.instance.getBlock(context.target)
            if (!block.air() && !block.replaceable()) return@register false
            PrimedTnt.spawn(context.instance, context.target)
            context.consumeOne()
            true
        }
        register(Material.BONE_MEAL) { context -> boneMeal(context) }
        register({ projectileType(it.material()) != null }) { context ->
            val type = projectileType(context.item.material()) ?: return@register false
            spawnProjectile(context, type)
        }
        register({ vehicleType(it.material()) != null }) { context ->
            val type = vehicleType(context.item.material()) ?: return@register false
            spawnEntity(context, type, 0.1)
        }
        register({ spawnEggType(it.material()) != null }) { context ->
            val type = spawnEggType(context.item.material()) ?: return@register false
            spawnEntity(context, type, 0.0)
        }
    }
}
