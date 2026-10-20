package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.key.Key
// <@AI_UNREVIEWED>
import net.kyori.adventure.sound.Sound
// </<@AI_UNREVIEWED>
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
// <@AI_UNREVIEWED>
import net.minestom.server.sound.SoundEvent
// </<@AI_UNREVIEWED>
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import java.util.concurrent.ThreadLocalRandom

/**
 * Piston and sticky piston. Follows power transitions (extend on rising edge, retract on falling
 * edge) and pushes up to [MAX_PUSH] blocks. Slime/honey dragging and moving-block animation are not
 * modelled; the piston updates instantly.
 */
class PistonComponent(
    private val sticky: Boolean,
) : RedstoneComponent {
    private var lastPowered = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val powered = isPowered(ctx)
        if (powered == lastPowered) return ComponentEvaluation.None
        lastPowered = powered
        return ComponentEvaluation.Trigger { context -> if (powered) extend(context) else retract(context) }
    }

    private fun extend(ctx: UpdateContext) {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return
        var cursor = ctx.position.relative(facing)

        val pushed = ArrayList<Pair<Point, Block>>()
        while (true) {
            val candidate = ctx.instance.getBlock(cursor)
            if (candidate.air() || candidate.compare(Block.PISTON_HEAD)) break
            if (isImmovable(candidate)) return
            pushed.add(cursor to candidate)
            if (pushed.size >= MAX_PUSH) return
            cursor = cursor.relative(facing)
        }

        for (index in pushed.indices.reversed()) {
            val (origin, moved) = pushed[index]
            ctx.instance.setBlock(origin.relative(facing), moved)
            ctx.instance.setBlock(origin, Block.AIR)
        }

        ctx.instance.setBlock(ctx.position.relative(facing), head(block, facing))
        ctx.setState(ctx.position, block.withProperty("extended", "true"))
        playSound(ctx, SoundEvent.BLOCK_PISTON_EXTEND)
    }

    private fun retract(ctx: UpdateContext) {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return
        val headPosition = ctx.position.relative(facing)
        val head = ctx.instance.getBlock(headPosition)
        if (head.compare(Block.PISTON_HEAD)) {
            ctx.instance.setBlock(headPosition, Block.AIR)
        }

        if (sticky) {
            val pullPosition = headPosition.relative(facing)
            val pulled = ctx.instance.getBlock(pullPosition)
            if (!pulled.air() && !isImmovable(pulled) && !pulled.compare(Block.PISTON_HEAD)) {
                ctx.instance.setBlock(pullPosition, Block.AIR)
                ctx.instance.setBlock(headPosition, pulled)
            }
        }

        ctx.setState(ctx.position, block.withProperty("extended", "false"))
        playSound(ctx, SoundEvent.BLOCK_PISTON_CONTRACT)
    }

    private fun playSound(
        ctx: UpdateContext,
        event: SoundEvent,
    ) {
        ctx.instance.playSoundExcept(
            null,
            Sound.sound(event, Sound.Source.BLOCK, 0.5f, 0.6f + ThreadLocalRandom.current().nextFloat() * 0.25f),
            ctx.position,
        )
    }

    private fun head(
        block: Block,
        facing: BlockFace,
    ): Block =
        Block.PISTON_HEAD
            .withProperty("facing", facing.name.lowercase())
            .withProperty("type", if (sticky) "sticky" else "normal")

    private companion object {
        private const val MAX_PUSH = 12

        private val immovableKeys: Set<Key> =
            setOf(
                Key.key("minecraft:bedrock"),
                Key.key("minecraft:obsidian"),
                Key.key("minecraft:crying_obsidian"),
                Key.key("minecraft:ancient_debris"),
                Key.key("minecraft:reinforced_deepslate"),
                Key.key("minecraft:enchanting_table"),
                Key.key("minecraft:beacon"),
                Key.key("minecraft:end_portal"),
                Key.key("minecraft:end_portal_frame"),
                Key.key("minecraft:ender_chest"),
                Key.key("minecraft:nether_portal"),
                Key.key("minecraft:respawn_anchor"),
                Key.key("minecraft:barrier"),
                Key.key("minecraft:command_block"),
                Key.key("minecraft:chain_command_block"),
                Key.key("minecraft:repeating_command_block"),
                Key.key("minecraft:structure_block"),
                Key.key("minecraft:jigsaw"),
                Key.key("minecraft:moving_piston"),
            )

        private fun isImmovable(block: Block): Boolean = block.key() in immovableKeys
    }
}
