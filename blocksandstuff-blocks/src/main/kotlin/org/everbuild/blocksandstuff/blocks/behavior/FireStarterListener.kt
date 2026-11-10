package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.damageTool

object FireStarterListener {
    private var enabled = false

    fun enable() {
        if (enabled) return
        enabled = true

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent::class.java) { event ->
            if (event.isCancelled) return@addListener
            if (tryIgnite(event.instance, event.player, event.block, event.blockPosition, event.blockFace)) {
                event.isBlockingItemUse = true
            }
        }
    }

    internal fun tryIgnite(
        instance: Instance,
        player: Player,
        clickedBlock: Block,
        clickedPosition: Point,
        blockFace: BlockFace,
    ): Boolean {
        val held = player.itemInMainHand
        val material = held.material()
        if (material != Material.FLINT_AND_STEEL && material != Material.FIRE_CHARGE) return false
        if (isHandledByBlockRule(clickedBlock)) return false

        val target = clickedPosition.relative(blockFace)
        val targetBlock = instance.getBlock(target)
        if (!targetBlock.air()) return false

        val below = instance.getBlock(target.add(0.0, -1.0, 0.0))
        val soulFire = below.compare(Block.SOUL_SAND) || below.compare(Block.SOUL_SOIL)
        if (!soulFire && !canFireSurviveAt(instance, target)) return false

        val fire = if (soulFire) Block.SOUL_FIRE else fireState(instance, target)
        instance.setBlock(target, fire)
        instance.playSoundExcept(
            null,
            Sound.sound(
                if (material == Material.FIRE_CHARGE) SoundEvent.ITEM_FIRECHARGE_USE else SoundEvent.ITEM_FLINTANDSTEEL_USE,
                Sound.Source.BLOCK,
                1.0f,
                1.0f,
            ),
            target,
        )

        if (material == Material.FLINT_AND_STEEL) {
            damageTool(player, held)
        } else if (player.gameMode != GameMode.CREATIVE) {
            player.itemInMainHand =
                if (held.amount() <= 1) ItemStack.AIR else held.withAmount(held.amount() - 1)
        }
        return true
    }

    private fun isHandledByBlockRule(block: Block): Boolean =
        block.compare(Block.CAMPFIRE) ||
            block.compare(Block.SOUL_CAMPFIRE) ||
            block.compare(Block.TNT) ||
            block.name().contains("candle")

    private fun fireState(
        instance: Instance,
        position: Point,
    ): Block {
        var fire = Block.FIRE.withProperty("age", "0")
        fire = fire.withProperty("up", isFlammableBlock(instance.getBlock(position.add(0.0, 1.0, 0.0))).toString())
        for (face in HORIZONTAL_FACES) {
            fire = fire.withProperty(
                face.name.lowercase(),
                isFlammableBlock(instance.getBlock(position.relative(face))).toString(),
            )
        }
        return fire
    }

    private fun canFireSurviveAt(
        instance: Instance,
        position: Point,
    ): Boolean {
        val below = instance.getBlock(position.add(0.0, -1.0, 0.0))
        if (below.liquid()) return false
        if (below.collisionShape().isFaceFull(BlockFace.TOP)) return true
        return BlockFace.entries.any { isFlammableBlock(instance.getBlock(position.relative(it))) }
    }

    private fun isFlammableBlock(block: Block): Boolean {
        if (block.air()) return false
        val name = block.name()
        return name.contains("planks") ||
            name.contains("_log") ||
            name.contains("_wood") ||
            name.contains("leaves") ||
            name.contains("wool") ||
            name.contains("carpet") ||
            name.contains("bamboo") ||
            name.contains("fence") ||
            name.contains("bookshelf") ||
            name == "minecraft:hay_block" ||
            name == "minecraft:coal_block" ||
            name == "minecraft:dead_bush" ||
            name == "minecraft:short_grass" ||
            name == "minecraft:tall_grass"
    }

    private val HORIZONTAL_FACES =
        listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
}
