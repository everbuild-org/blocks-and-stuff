package org.everbuild.blocksandstuff.blocks.behavior

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerLeaveBedEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private const val NIGHT_START = 13000L
private const val NIGHT_END = 23000L
private const val DAY_LENGTH = 24000L
private const val MORNING_TIME = 0L

val BED_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:bed") { ctx ->
        val part = ctx.block.getProperty("part")
        if (part != "head" && part != "foot") return@Rule RuleResult.Next

        val livingMeta = ctx.player.entityMeta as? net.minestom.server.entity.metadata.LivingEntityMeta
        if (livingMeta?.bedInWhichSleepingPosition != null) {
            BedSleepTracker.wake(ctx.player)
            return@Rule RuleResult.Consume
        }

        val time = Math.floorMod(ctx.instance.time, DAY_LENGTH)
        val isNight = time in NIGHT_START..NIGHT_END
        val isThundering = ctx.instance.weather.thunderLevel() > 0f
        if (!isNight && !isThundering) {
            ctx.player.sendActionBar(Component.translatable("block.minecraft.bed.no_sleep"))
            return@Rule RuleResult.Consume
        }

        val bedKey = bedFootKey(ctx.block, ctx.blockPosition)
        if (!BedSleepTracker.trySleep(ctx.instance, bedKey, ctx.player)) {
            ctx.player.sendActionBar(Component.translatable("block.minecraft.bed.occupied"))
            return@Rule RuleResult.Consume
        }

        ctx.player.setRespawnPoint(
            Pos(ctx.blockPosition.x() + 0.5, ctx.blockPosition.y() + 1.0, ctx.blockPosition.z() + 0.5),
        )
        ctx.player.enterBed(ctx.blockPosition)

        if (BedSleepTracker.shouldSkipNight(ctx.instance)) {
            BedSleepTracker.skipNight(ctx.instance)
        }
        RuleResult.Consume
    }

object BedSleepTracker {
    private val sleepers = ConcurrentHashMap<Instance, MutableMap<BlockVec, Player>>()
    private var enabled = false

    fun enable() {
        if (enabled) return
        enabled = true

        val handler = MinecraftServer.getGlobalEventHandler()
        handler.addListener(PlayerLeaveBedEvent::class.java) { event -> remove(event.player) }
        handler.addListener(PlayerDisconnectEvent::class.java) { event -> remove(event.player) }
    }

    fun trySleep(
        instance: Instance,
        bedKey: BlockVec,
        player: Player,
    ): Boolean {
        val beds = sleepers.computeIfAbsent(instance) { ConcurrentHashMap() }
        val occupant = beds[bedKey]
        if (occupant != null && occupant !== player) return false

        remove(player)
        beds[bedKey] = player
        return true
    }

    fun wake(player: Player) {
        player.leaveBed()
        remove(player)
    }

    fun remove(player: Player) {
        sleepers.values.forEach { beds -> beds.entries.removeIf { it.value === player } }
    }

    fun shouldSkipNight(instance: Instance): Boolean {
        val beds = sleepers[instance] ?: return false
        val onlinePlayers = instance.players.filter { it.isOnline }
        if (onlinePlayers.isEmpty()) return false

        val percentage = InstanceOptionsProvider.getForInstance(instance).playersSleepingPercentage
        val required = maxOf(1, ceil(onlinePlayers.size * percentage / 100.0).toInt())
        return beds.values.count { it.isOnline } >= required
    }

    fun skipNight(instance: Instance) {
        instance.setTime(MORNING_TIME)
        val beds = sleepers.remove(instance) ?: return
        beds.values.toSet().forEach { player ->
            if (player.isOnline) player.leaveBed()
        }
    }
}

private fun bedFootKey(
    block: Block,
    position: Point,
): BlockVec {
    if (block.getProperty("part") != "head") return position.asBlockVec()

    val facing = block.getProperty("facing") ?: return position.asBlockVec()
    val direction = Direction.valueOf(facing.uppercase())
    return position.add(direction.vec().mul(-1.0)).asBlockVec()
}

fun BlockRulesetEditor.addBeds() {
    (BlockTags.getTaggedWith("minecraft:beds") + Block.STRAW_BED)
        .distinctBy { it.key() }
        .forEach { block -> blocks(block) { interact.append { install(BED_RULE) } } }
}
