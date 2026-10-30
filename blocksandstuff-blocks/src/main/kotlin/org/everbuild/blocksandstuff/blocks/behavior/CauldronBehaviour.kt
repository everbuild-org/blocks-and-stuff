package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.LivingEntity
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.PotionContents
import net.minestom.server.potion.PotionType
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.common.blockinventory.PlayerInventoryStash
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TouchActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import org.everbuild.twaddle.stacked_behaviour_block.type.touch

internal object Cauldrons {
    fun waterLevel(block: Block): Int =
        if (block.compare(Block.WATER_CAULDRON)) {
            (block.getProperty("level")?.toIntOrNull() ?: 0).coerceIn(0, 3)
        } else {
            0
        }

    fun addWaterLevel(
        instance: Instance,
        position: net.minestom.server.coordinate.Point,
    ): Boolean {
        val block = instance.getBlock(position)
        return when {
            block.compare(Block.CAULDRON) -> {
                instance.setBlock(position, Block.WATER_CAULDRON.withProperty("level", "1"))
                true
            }

            block.compare(Block.WATER_CAULDRON) && waterLevel(block) < 3 -> {
                instance.setBlock(position, block.withProperty("level", (waterLevel(block) + 1).toString()))
                true
            }

            else -> {
                false
            }
        }
    }

    fun addLava(
        instance: Instance,
        position: net.minestom.server.coordinate.Point,
    ): Boolean {
        if (!instance.getBlock(position).compare(Block.CAULDRON)) return false
        instance.setBlock(position, Block.LAVA_CAULDRON)
        return true
    }

    fun canReceiveWater(block: Block): Boolean =
        block.compare(Block.CAULDRON) || (block.compare(Block.WATER_CAULDRON) && waterLevel(block) < 3)

    fun canReceiveLava(block: Block): Boolean = block.compare(Block.CAULDRON)
}

val CAULDRON_INTERACT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:cauldron_interact") { ctx ->
        val block = ctx.block
        val item = ctx.player.getItemInHand(ctx.hand)

        when {
            block.compare(Block.CAULDRON) -> {
                when {
                    isWaterBottle(item) -> {
                        ctx.instance.setBlock(ctx.blockPosition, Block.WATER_CAULDRON.withProperty("level", "1"))
                        replaceItem(ctx, item, ItemStack.of(Material.GLASS_BOTTLE))
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BOTTLE_EMPTY)
                        RuleResult.Consume
                    }

                    item.material() == Material.WATER_BUCKET -> {
                        ctx.instance.setBlock(ctx.blockPosition, Block.WATER_CAULDRON.withProperty("level", "3"))
                        replaceItem(ctx, item, ItemStack.of(Material.BUCKET))
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BUCKET_EMPTY)
                        RuleResult.Consume
                    }

                    item.material() == Material.LAVA_BUCKET -> {
                        ctx.instance.setBlock(ctx.blockPosition, Block.LAVA_CAULDRON)
                        replaceItem(ctx, item, ItemStack.of(Material.BUCKET))
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BUCKET_EMPTY_LAVA)
                        RuleResult.Consume
                    }

                    item.material() == Material.POWDER_SNOW_BUCKET -> {
                        ctx.instance.setBlock(
                            ctx.blockPosition,
                            Block.POWDER_SNOW_CAULDRON.withProperty("level", "3"),
                        )
                        replaceItem(ctx, item, ItemStack.of(Material.BUCKET))
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BUCKET_EMPTY_POWDER_SNOW)
                        RuleResult.Consume
                    }

                    else -> {
                        RuleResult.Next
                    }
                }
            }

            block.compare(Block.WATER_CAULDRON) -> {
                val level = Cauldrons.waterLevel(block)
                when {
                    item.material() == Material.WATER_BUCKET -> {
                        if (level >= 3) return@Rule RuleResult.Next
                        ctx.instance.setBlock(ctx.blockPosition, block.withProperty("level", "3"))
                        replaceItem(ctx, item, ItemStack.of(Material.BUCKET))
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BUCKET_EMPTY)
                        RuleResult.Consume
                    }

                    item.material() == Material.BUCKET -> {
                        if (level < 3) return@Rule RuleResult.Next
                        ctx.instance.setBlock(ctx.blockPosition, Block.CAULDRON)
                        replaceItem(ctx, item, ItemStack.of(Material.WATER_BUCKET))
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BUCKET_FILL)
                        RuleResult.Consume
                    }

                    isWaterBottle(item) -> {
                        if (level >= 3) return@Rule RuleResult.Next
                        ctx.instance.setBlock(ctx.blockPosition, block.withProperty("level", (level + 1).toString()))
                        replaceItem(ctx, item, ItemStack.of(Material.GLASS_BOTTLE))
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BOTTLE_EMPTY)
                        RuleResult.Consume
                    }

                    item.material() == Material.GLASS_BOTTLE -> {
                        if (level <= 0) return@Rule RuleResult.Next
                        val replacement = if (level == 1) Block.CAULDRON else block.withProperty("level", (level - 1).toString())
                        ctx.instance.setBlock(ctx.blockPosition, replacement)
                        replaceItem(ctx, item, waterBottle())
                        playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BOTTLE_FILL)
                        RuleResult.Consume
                    }

                    else -> {
                        RuleResult.Next
                    }
                }
            }

            block.compare(Block.LAVA_CAULDRON) -> {
                if (item.material() == Material.BUCKET) {
                    ctx.instance.setBlock(ctx.blockPosition, Block.CAULDRON)
                    replaceItem(ctx, item, ItemStack.of(Material.LAVA_BUCKET))
                    playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BUCKET_FILL_LAVA)
                    RuleResult.Consume
                } else {
                    RuleResult.Next
                }
            }

            block.compare(Block.POWDER_SNOW_CAULDRON) -> {
                if (item.material() == Material.BUCKET) {
                    ctx.instance.setBlock(ctx.blockPosition, Block.CAULDRON)
                    replaceItem(ctx, item, ItemStack.of(Material.POWDER_SNOW_BUCKET))
                    playSound(ctx.instance, ctx.blockPosition, SoundEvent.ITEM_BUCKET_FILL_POWDER_SNOW)
                    RuleResult.Consume
                } else {
                    RuleResult.Next
                }
            }

            else -> {
                RuleResult.Next
            }
        }
    }

val CAULDRON_TOUCH_RULE: Rule<TouchActivityContext> =
    Rule("blocksandstuff:cauldron_touch") { ctx ->
        if (!ctx.block.compare(Block.WATER_CAULDRON)) return@Rule RuleResult.Next
        val entity = ctx.touching
        if (entity is LivingEntity && entity.fireTicks > 0) {
            entity.setFireTicks(0)
        }
        RuleResult.Next
    }

fun BlockRulesetEditor.addCauldrons() {
    listOf(Block.CAULDRON, Block.WATER_CAULDRON, Block.LAVA_CAULDRON, Block.POWDER_SNOW_CAULDRON)
        .forEach { cauldron ->
            blocks(cauldron) {
                interact.append { install(CAULDRON_INTERACT_RULE) }
                touch.append { install(CAULDRON_TOUCH_RULE) }
            }
        }
}

private fun isWaterBottle(item: ItemStack): Boolean =
    item.material() == Material.POTION &&
        item.get(DataComponents.POTION_CONTENTS)?.potion() == PotionType.WATER

private fun waterBottle(): ItemStack = ItemStack.of(Material.POTION).with(DataComponents.POTION_CONTENTS, PotionContents(PotionType.WATER))

private fun replaceItem(
    ctx: InteractionActivityContext,
    original: ItemStack,
    replacement: ItemStack,
) {
    if (ctx.player.gameMode == GameMode.CREATIVE) return
    if (original.amount() > 1) {
        ctx.player.setItemInHand(ctx.hand, original.withAmount(original.amount() - 1))
        PlayerInventoryStash.add(ctx.player, replacement)
    } else {
        ctx.player.setItemInHand(ctx.hand, replacement)
    }
}

private fun playSound(
    instance: Instance,
    position: net.minestom.server.coordinate.Point,
    sound: SoundEvent,
) {
    instance.playSoundExcept(null, Sound.sound(sound, Sound.Source.BLOCK, 1.0f, 1.0f), position)
}
