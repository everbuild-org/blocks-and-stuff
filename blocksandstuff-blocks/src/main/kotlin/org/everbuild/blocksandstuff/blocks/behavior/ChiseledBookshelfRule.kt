package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.GameMode
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
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

private const val BOOK_SLOTS = 6

private fun isBook(material: Material): Boolean =
    material == Material.BOOK ||
        material == Material.WRITABLE_BOOK ||
        material == Material.WRITTEN_BOOK ||
        material == Material.ENCHANTED_BOOK ||
        material == Material.KNOWLEDGE_BOOK

val CHISELED_BOOKSHELF_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:chiseled_bookshelf") { ctx ->
        if (!ctx.block.compare(Block.CHISELED_BOOKSHELF)) return@Rule RuleResult.Next
        val current = ctx.instance.getBlock(ctx.blockPosition)
        val items = current.readBlockEntityItems()
        val held = ctx.player.itemInMainHand

        if (held.isAir) {
            val slot =
                targetedSlot(ctx, current)?.takeIf { items.containsKey(it) }
                    ?: (BOOK_SLOTS - 1 downTo 0).firstOrNull { items.containsKey(it) }
                    ?: return@Rule RuleResult.Next
            val stored = items.getValue(slot)
            ctx.instance.setBlock(
                ctx.blockPosition,
                current
                    .withBlockEntityItems(items - slot)
                    .withProperty(slotProperty(slot), "false"),
            )
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, stored)
            playSoundFor(ctx, SoundEvent.BLOCK_CHISELED_BOOKSHELF_PICKUP)
            return@Rule RuleResult.Consume
        }

        if (!isBook(held.material())) return@Rule RuleResult.Next
        val slot =
            targetedSlot(ctx, current)?.takeIf { !items.containsKey(it) }
                ?: (0 until BOOK_SLOTS).firstOrNull { !items.containsKey(it) }
                ?: return@Rule RuleResult.Next

        ctx.instance.setBlock(
            ctx.blockPosition,
            current
                .withBlockEntityItems(items + (slot to held.withAmount(1)))
                .withProperty(slotProperty(slot), "true"),
        )
        if (ctx.player.gameMode != GameMode.CREATIVE) {
            ctx.player.itemInMainHand = if (held.amount() <= 1) ItemStack.AIR else held.withAmount(held.amount() - 1)
        }
        playSoundFor(
            ctx,
            if (held.material() == Material.ENCHANTED_BOOK) {
                SoundEvent.BLOCK_CHISELED_BOOKSHELF_INSERT_ENCHANTED
            } else {
                SoundEvent.BLOCK_CHISELED_BOOKSHELF_INSERT
            },
        )
        RuleResult.Consume
    }

val CHISELED_BOOKSHELF_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:chiseled_bookshelf_destroy") { ctx ->
        if (!ctx.block.compare(Block.CHISELED_BOOKSHELF)) return@Rule RuleResult.Next
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next

        ctx.block.readBlockEntityItems().values.forEach {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, it)
        }
        RuleResult.Next
    }

private fun slotProperty(slot: Int): String = "slot_${slot}_occupied"

private fun targetedSlot(
    ctx: InteractionActivityContext,
    block: Block,
): Int? {
    val facing = block.getProperty("facing") ?: "north"
    val cursor = ctx.cursorPosition
    val along =
        when (facing) {
            "north" -> 1.0 - cursor.x()
            "south" -> cursor.x()
            "west" -> cursor.z()
            "east" -> 1.0 - cursor.z()
            else -> 0.5
        }
    val column =
        when {
            along < 0.375 -> 0
            along < 0.6875 -> 1
            else -> 2
        }
    val row = if (cursor.y() >= 0.5) 0 else 1
    return column + row * 3
}

fun BlockRulesetEditor.addChiseledBookshelves() {
    blocks(Block.CHISELED_BOOKSHELF) {
        blockEntity {
            tags += Tag.NBT("Items").list()
            tags += Tag.Integer("last_interacted_slot")
        }
        interact.append { install(CHISELED_BOOKSHELF_RULE) }
        destroy.append { install(CHISELED_BOOKSHELF_DESTROY_RULE) }
    }
}
