package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TouchActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import org.everbuild.twaddle.stacked_behaviour_block.type.touch
import java.util.Random
import kotlin.math.abs

val SWEET_BERRY_BUSH_RANDOM_TICK_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:sweet_berry_bush_random_tick") { ctx ->
        val age = ctx.block.getProperty("age")?.toIntOrNull() ?: 0
        if (age >= 3) return@Rule RuleResult.Next
        if (ctx.instance.getSkyLight(ctx.blockPosition.blockX, ctx.blockPosition.blockY, ctx.blockPosition.blockZ) < 9) {
            return@Rule RuleResult.Next
        }
        if (Random().nextInt(5) == 0) {
            ctx.replaceBlock(ctx.block.withProperty("age", (age + 1).toString()))
        }
        RuleResult.Next
    }

val SWEET_BERRY_BUSH_INTERACT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:sweet_berry_bush_interact") { ctx ->
        val player = ctx.player
        val itemInHand = player.itemInMainHand
        val age = ctx.block.getProperty("age")?.toIntOrNull() ?: 0

        if (age > 1) {
            val count = 1 + Random().nextInt(2) + if (age == 3) 1 else 0
            DroppedItemFactory.maybeDrop(
                ctx.instance,
                ctx.blockPosition,
                ItemStack.of(Material.SWEET_BERRIES, count),
            )
            ctx.replaceBlock(ctx.block.withProperty("age", "1"))
        } else if (itemInHand.material() == Material.BONE_MEAL) {
            ctx.replaceBlock(ctx.block.withProperty("age", (age + 1).toString()))
            if (player.gameMode != GameMode.CREATIVE) {
                player.itemInMainHand = itemInHand.withAmount(itemInHand.amount() - 1)
            }
        }

        RuleResult.Consume
    }

val SWEET_BERRY_BUSH_TOUCH_RULE: Rule<TouchActivityContext> =
    Rule("blocksandstuff:sweet_berry_bush_touch") { ctx ->
        val entity = ctx.touching
        if (entity is LivingEntity && entity.entityType != EntityType.FOX && entity.entityType != EntityType.BEE) {
            if (ctx.block.getProperty("age")?.toIntOrNull() != 0) {
                val movementDistance = entity.previousPosition.sub(entity.position)
                if (movementDistance.x() > 0 || movementDistance.z() > 0) {
                    val dx = abs(movementDistance.x())
                    val dz = abs(movementDistance.z())
                    if (dx >= 0.003F || dz >= 0.003F) {
                        entity.damage(
                            Damage(
                                DamageType.SWEET_BERRY_BUSH,
                                ctx.touching,
                                null,
                                ctx.blockPosition,
                                1.0F,
                            ),
                        )
                    }
                }
            }
        }
        RuleResult.Next
    }

fun BlockRulesetEditor.addSweetBerryBushes() {
    blocks(Block.SWEET_BERRY_BUSH) {
        interact.append { install(SWEET_BERRY_BUSH_INTERACT_RULE) }
        touch.append { install(SWEET_BERRY_BUSH_TOUCH_RULE) }
        randomTick.append { install(SWEET_BERRY_BUSH_RANDOM_TICK_RULE) }
    }
}
