package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.GameMode
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.util.readBlockEntityItems
import org.everbuild.blocksandstuff.blocks.util.withBlockEntityItems
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.configuration.blockEntity
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import kotlin.math.floor

internal val SHELF_BLOCKS: List<Block> =
    listOf(
        Block.OAK_SHELF,
        Block.SPRUCE_SHELF,
        Block.BIRCH_SHELF,
        Block.JUNGLE_SHELF,
        Block.ACACIA_SHELF,
        Block.DARK_OAK_SHELF,
        Block.MANGROVE_SHELF,
        Block.CHERRY_SHELF,
        Block.PALE_OAK_SHELF,
        Block.POPLAR_SHELF,
        Block.BAMBOO_SHELF,
        Block.CRIMSON_SHELF,
        Block.WARPED_SHELF,
    )

private const val SHELF_SLOTS = 3

val SHELF_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:shelf") { ctx ->
        if (SHELF_BLOCKS.none { it.key() == ctx.block.key() }) return@Rule RuleResult.Next
        if (ctx.hand == PlayerHand.OFF) return@Rule RuleResult.Next

        val current = ctx.instance.getBlock(ctx.blockPosition)
        val slot = targetedSlot(ctx, current) ?: return@Rule RuleResult.Next
        val player = ctx.player
        val inventory = player.inventory
        val heldSlot = player.heldSlot.toInt()
        val held = inventory.getItemStack(heldSlot)
        val items = current.readBlockEntityItems().toMutableMap()
        val powered = current.getProperty("powered") == "true"

        if (!powered) {
            val removed = items[slot]
            if (removed == null && held.isAir) return@Rule RuleResult.Next

            items.remove(slot)
            if (!held.isAir) items[slot] = held

            val newHeld =
                if (player.gameMode == GameMode.CREATIVE && removed == null) {
                    held
                } else {
                    removed ?: ItemStack.AIR
                }
            inventory.setItemStack(heldSlot, newHeld)
            ctx.instance.setBlock(ctx.blockPosition, current.withBlockEntityItems(items))
            val sound =
                when {
                    removed != null && held.isAir -> SoundEvent.BLOCK_SHELF_TAKE_ITEM
                    removed != null -> SoundEvent.BLOCK_SHELF_SINGLE_SWAP
                    else -> SoundEvent.BLOCK_SHELF_PLACE_ITEM
                }
            playSoundFor(ctx, sound)
            return@Rule RuleResult.Consume
        }

        var anySwapped = false
        for (offset in 0 until SHELF_SLOTS) {
            val hotbarSlot = SHELF_SLOTS * 2 + offset
            val placed = inventory.getItemStack(hotbarSlot)
            val removed = items[offset]
            if (!placed.isAir || removed != null) anySwapped = true
            items.remove(offset)
            if (!placed.isAir) items[offset] = placed
            inventory.setItemStack(hotbarSlot, removed ?: ItemStack.AIR)
        }
        if (anySwapped) {
            ctx.instance.setBlock(ctx.blockPosition, current.withBlockEntityItems(items))
            playSoundFor(ctx, SoundEvent.BLOCK_SHELF_MULTI_SWAP)
        }
        RuleResult.Consume
    }

internal val SHELF_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:shelf_destroy") { ctx ->
        if (SHELF_BLOCKS.none { it.key() == ctx.block.key() }) return@Rule RuleResult.Next
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next

        ctx.block.readBlockEntityItems().values.forEach {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, it)
        }
        RuleResult.Next
    }

private fun targetedSlot(
    ctx: InteractionActivityContext,
    block: Block,
): Int? {
    val facing = block.getProperty("facing") ?: return null
    if (ctx.blockFace.name.lowercase() != facing) return null

    val cursor = ctx.cursorPosition
    val relativeX =
        when (facing) {
            "north" -> 1.0 - cursor.x()
            "south" -> cursor.x()
            "west" -> cursor.z()
            "east" -> 1.0 - cursor.z()
            else -> return null
        }
    return section(relativeX, SHELF_SLOTS)
}

private fun section(
    position: Double,
    max: Int,
): Int {
    val targetedPixel = position * 16.0
    val sectionSize = 16.0 / max
    return floor(targetedPixel / sectionSize).toInt().coerceIn(0, max - 1)
}

fun BlockRulesetEditor.addShelves() {
    SHELF_BLOCKS.forEach { shelf ->
        blocks(shelf) {
            blockEntity { tags += Tag.NBT("Items").list() }
            interact.append { install(SHELF_RULE) }
            destroy.append { install(SHELF_DESTROY_RULE) }
        }
    }
}
