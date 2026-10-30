package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Transcoder
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.MaterialTags
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.recipes.smelting.campfire.CampfireCookingRecipe
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.configuration.blockEntity
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TouchActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import org.everbuild.twaddle.stacked_behaviour_block.type.tick
import org.everbuild.twaddle.stacked_behaviour_block.type.touch
import kotlin.math.max

private const val CAMPFIRE_SLOTS = 4
private const val DEFAULT_COOK_TIME = 600

private val CAMPFIRE_ITEMS_TAG: Tag<List<BinaryTag>> = Tag.NBT("Items").list()
private val CAMPFIRE_COOKING_TIMES_TAG: Tag<BinaryTag> = Tag.NBT("CookingTimes")
private val CAMPFIRE_COOKING_TOTAL_TIMES_TAG: Tag<BinaryTag> = Tag.NBT("CookingTotalTimes")

val CAMPFIRE_INTERACT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:campfire_interact") { ctx ->
        val block = ctx.block
        val lit = block.getProperty("lit") == "true"
        val waterlogged = block.getProperty("waterlogged") == "true"
        val item = ctx.player.getItemInHand(ctx.hand)

        when {
            !lit && !waterlogged && isLighter(item) -> {
                ctx.replaceBlock(block.withProperty("lit", "true"))
                ctx.instance.playSoundExcept(
                    null,
                    Sound.sound(lighterSound(item), Sound.Source.BLOCK, 1.0f, 1.0f),
                    ctx.blockPosition,
                )
                consumeLighter(ctx.player, ctx.hand, item)
                RuleResult.Consume
            }

            lit && ctx.blockFace != BlockFace.BOTTOM && isDousingTool(item) -> {
                ctx.replaceBlock(block.withProperty("lit", "false"))
                ctx.instance.playSoundExcept(
                    null,
                    Sound.sound(SoundEvent.BLOCK_FIRE_EXTINGUISH, Sound.Source.BLOCK, 1.0f, 1.0f),
                    ctx.blockPosition,
                )
                RuleResult.Consume
            }

            else -> {
                if (!addFood(ctx, item)) return@Rule RuleResult.Next
                RuleResult.Consume
            }
        }
    }

val CAMPFIRE_TICK_RULE: Rule<TickActivityContext> =
    Rule("blocksandstuff:campfire_tick") { ctx ->
        var block = ctx.block
        if (block.getProperty("waterlogged") == "true" && block.getProperty("lit") == "true") {
            block = block.withProperty("lit", "false")
        }

        val items = readItems(block)
        val times = readIntArray(block, CAMPFIRE_COOKING_TIMES_TAG)
        val totals = readIntArray(block, CAMPFIRE_COOKING_TOTAL_TIMES_TAG)
        var changed = block != ctx.block

        if (items.isEmpty()) {
            if (changed) ctx.instance.setBlock(ctx.blockPosition, block, false)
            return@Rule RuleResult.Next
        }

        val lit = block.getProperty("lit") == "true"
        for (slot in 0 until CAMPFIRE_SLOTS) {
            val item = items[slot]
            if (item == null) {
                if (times[slot] != 0 || totals[slot] != 0) {
                    times[slot] = 0
                    totals[slot] = 0
                    changed = true
                }
                continue
            }

            if (!lit) {
                if (times[slot] > 0) {
                    times[slot] = max(0, times[slot] - 2)
                    changed = true
                }
                continue
            }

            if (totals[slot] <= 0) {
                totals[slot] = findCampfireRecipe(item)?.burnTime ?: DEFAULT_COOK_TIME
                changed = true
            }

            times[slot]++
            changed = true
            if (times[slot] >= totals[slot]) {
                val result = findCampfireRecipe(item)?.result ?: item
                DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition.add(0.0, 1.0, 0.0), result)
                items.remove(slot)
                times[slot] = 0
                totals[slot] = 0
            }
        }

        if (changed) {
            ctx.instance.setBlock(ctx.blockPosition, block.withCampfireState(items, times, totals), false)
        }
        RuleResult.Next
    }

val CAMPFIRE_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:campfire_destroy") { ctx ->
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next

        readItems(ctx.block).values.forEach {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, it)
        }
        RuleResult.Next
    }

val CAMPFIRE_TOUCH_RULE: Rule<TouchActivityContext> =
    Rule("blocksandstuff:campfire_touch") { ctx ->
        val lit = ctx.block.getProperty("lit") == "true"
        val waterlogged = ctx.block.getProperty("waterlogged") == "true"
        val entity = ctx.touching
        if (lit && !waterlogged && entity is LivingEntity) {
            entity.damage(Damage(DamageType.CAMPFIRE, entity, null, ctx.blockPosition, 1.0f))
        }
        RuleResult.Next
    }

fun BlockRulesetEditor.addCampfires() {
    listOf(Block.CAMPFIRE, Block.SOUL_CAMPFIRE).forEach { campfire ->
        blocks(campfire) {
            blockEntity {
                tags += CAMPFIRE_ITEMS_TAG
                tags += CAMPFIRE_COOKING_TIMES_TAG
                tags += CAMPFIRE_COOKING_TOTAL_TIMES_TAG
            }
            interact.append { install(CAMPFIRE_INTERACT_RULE) }
            tick.append { install(CAMPFIRE_TICK_RULE) }
            destroy.append { install(CAMPFIRE_DESTROY_RULE) }
            touch.append { install(CAMPFIRE_TOUCH_RULE) }
        }
    }
}

private fun addFood(
    ctx: InteractionActivityContext,
    item: ItemStack,
): Boolean {
    val recipe = findCampfireRecipe(item) ?: return false
    val items = readItems(ctx.block)
    val slot = (0 until CAMPFIRE_SLOTS).firstOrNull { items[it] == null } ?: return false
    val times = readIntArray(ctx.block, CAMPFIRE_COOKING_TIMES_TAG)
    val totals = readIntArray(ctx.block, CAMPFIRE_COOKING_TOTAL_TIMES_TAG)

    items[slot] = item.withAmount(1)
    times[slot] = 0
    totals[slot] = recipe.burnTime.coerceAtLeast(1)
    ctx.instance.setBlock(ctx.blockPosition, ctx.block.withCampfireState(items, times, totals), false)
    consumeOne(ctx.player, ctx.hand, item)
    return true
}

private fun consumeOne(
    player: Player,
    hand: PlayerHand,
    item: ItemStack,
) {
    if (player.gameMode == GameMode.CREATIVE) return
    player.setItemInHand(hand, if (item.amount() <= 1) ItemStack.AIR else item.withAmount(item.amount() - 1))
}

private fun isDousingTool(item: ItemStack): Boolean =
    Material.staticRegistry().getTag(MaterialTags.DOUSES_CAMPFIRES)?.contains(item.material()) == true

private fun findCampfireRecipe(item: ItemStack): CampfireCookingRecipe? =
    MinecraftServer
        .getRecipeManager()
        .recipes
        .filterIsInstance<CampfireCookingRecipe>()
        .firstOrNull { it.matches(item) }

private fun readItems(block: Block): MutableMap<Int, ItemStack> {
    val items = mutableMapOf<Int, ItemStack>()
    block.getTag(CAMPFIRE_ITEMS_TAG)?.forEach { tag ->
        val compound = tag as? CompoundBinaryTag ?: return@forEach
        val slot = compound.getByte("Slot").toInt()
        if (slot !in 0 until CAMPFIRE_SLOTS) return@forEach
        val stack = ItemStack.CODEC.decode(Transcoder.NBT, compound).orElse(null) ?: return@forEach
        if (!stack.isAir) items[slot] = stack
    }
    return items
}

private fun readIntArray(
    block: Block,
    tag: Tag<BinaryTag>,
): IntArray {
    val values =
        block
            .getTag(tag)
            ?.let { Codec.INT_ARRAY.decode(Transcoder.NBT, it).orElse(null) }
            ?: return IntArray(CAMPFIRE_SLOTS)
    return IntArray(CAMPFIRE_SLOTS) { index -> values.getOrElse(index) { 0 } }
}

private fun Block.withCampfireState(
    items: Map<Int, ItemStack>,
    times: IntArray,
    totals: IntArray,
): Block {
    if (items.isEmpty()) {
        return withTag(CAMPFIRE_ITEMS_TAG, null)
            .withTag(CAMPFIRE_COOKING_TIMES_TAG, null)
            .withTag(CAMPFIRE_COOKING_TOTAL_TIMES_TAG, null)
    }

    val encodedItems =
        items.entries.sortedBy { it.key }.map { (slot, stack) ->
            (ItemStack.CODEC.encode(Transcoder.NBT, stack).orElseThrow() as CompoundBinaryTag)
                .putByte("Slot", slot.toByte())
        }
    return withTag(CAMPFIRE_ITEMS_TAG, encodedItems)
        .withTag(CAMPFIRE_COOKING_TIMES_TAG, encodeIntArray(times))
        .withTag(CAMPFIRE_COOKING_TOTAL_TIMES_TAG, encodeIntArray(totals))
}

private fun encodeIntArray(values: IntArray): BinaryTag = Codec.INT_ARRAY.encode(Transcoder.NBT, values).orElseThrow()
