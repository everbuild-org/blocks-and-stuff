package org.everbuild.blocksandstuff.blocks.behavior.tool

import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext

data class ToolTransform(
    val from: Block,
    val tool: Material,
    val to: Block,
    val preserveProperties: Boolean = false, // <- war preserveAxis
    val extra: ((ctx: InteractionActivityContext, transformed: Block) -> Unit)? = null,
)

fun toolRule(
    id: String,
    transforms: List<ToolTransform>,
): Rule<InteractionActivityContext> =
    Rule<InteractionActivityContext>(id) { ctx ->
        val held = ctx.player.itemInMainHand
        val match =
            transforms.firstOrNull {
                ctx.block.key() == it.from.key() && held.material() == it.tool
            } ?: return@Rule RuleResult.Next

        val result =
            if (match.preserveProperties) {
                ctx.block.getProperty("axis")?.let { match.to.withProperty("axis", it) } ?: match.to
            } else {
                match.to
            }

        ctx.replaceBlock(result)
        match.extra?.invoke(ctx, result)

        if (held.material().name().endsWith("_axe")) damageTool(ctx.player, held)

        RuleResult.Consume
    }

private fun damageTool(
    player: Player,
    tool: ItemStack,
) {
    if (player.gameMode == GameMode.CREATIVE) return

    val currentDamage = tool.getTag(Tag.Integer("Damage")) ?: 0
    val maxDamage = tool.get(DataComponents.MAX_DAMAGE) ?: return

    if (currentDamage + 1 >= maxDamage) {
        player.itemInMainHand = ItemStack.AIR
        player.instance?.playSound(
            Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1.0f, 1.0f),
            player.position,
        )
    } else {
        player.itemInMainHand = tool.withTag(Tag.Integer("Damage"), currentDamage + 1)
    }
}
