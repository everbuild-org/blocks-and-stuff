package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.inventory.type.BeaconInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.client.play.ClientSetBeaconEffectPacket
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val BEACON_BASE_BLOCKS =
    setOf(
        Block.IRON_BLOCK,
        Block.GOLD_BLOCK,
        Block.DIAMOND_BLOCK,
        Block.EMERALD_BLOCK,
        Block.NETHERITE_BLOCK,
    )

private val BEACON_PAYMENT_ITEMS =
    setOf(
        Material.IRON_INGOT,
        Material.GOLD_INGOT,
        Material.DIAMOND,
        Material.EMERALD,
        Material.NETHERITE_INGOT,
    )

private const val MAX_BEACON_LEVEL = 4

val BEACON_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:beacon") { ctx ->
        if (ctx.player.isSneaking) return@Rule RuleResult.Next

        val level = beaconLevel(ctx.instance, ctx.blockPosition)
        val menu = BeaconMenu(ctx.instance, ctx.blockPosition, level)
        ctx.player.openInventory(menu)
        menu.setPowerLevel(level.toShort())
        RuleResult.Consume
    }

val CONDUIT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:conduit") { ctx ->
        if (ctx.block.getProperty("waterlogged") != "true") return@Rule RuleResult.Next

        ctx.player.addEffect(Potion(potion("conduit_power"), 0, 13 * 20, 0.toByte()))
        playSoundFor(ctx, SoundEvent.BLOCK_CONDUIT_ACTIVATE)
        RuleResult.Consume
    }

class BeaconMenu(
    val instance: Instance,
    val position: Point,
    val level: Int,
) : BeaconInventory(Component.translatable("container.beacon")) {
    init {
        setPowerLevel(level.toShort())
    }
}

object BeaconMenus {
    private var enabled = false

    fun enable() {
        if (enabled) return
        enabled = true

        MinecraftServer.getPacketListenerManager().setPlayListener(
            ClientSetBeaconEffectPacket::class.java,
        ) { packet, player ->
            val menu = player.openInventory as? BeaconMenu ?: return@setPlayListener
            applySelection(menu, player, packet)
        }
        MinecraftServer.getGlobalEventHandler().addListener(InventoryCloseEvent::class.java) { event ->
            val menu = event.inventory as? BeaconMenu ?: return@addListener
            returnPayment(event.player, menu)
        }
    }

    private fun applySelection(
        menu: BeaconMenu,
        player: Player,
        packet: ClientSetBeaconEffectPacket,
    ) {
        if (menu.level <= 0) return
        if (!isPayment(menu.getItemStack(0))) return

        val primary = packet.primaryEffect ?: return
        if (!isValidPrimary(primary, menu.level)) return
        val secondary = packet.secondaryEffect?.takeIf { menu.level >= MAX_BEACON_LEVEL && isValidSecondary(it, menu.level) }

        menu.setFirstPotionEffect(primary)
        menu.setSecondPotionEffect(secondary)

        if (player.gameMode != GameMode.CREATIVE) {
            consumePayment(menu)
        }

        val duration = (11 + (menu.level - 1) * 10) * 20
        val range = 10.0 + menu.level * 10.0
        menu.instance.getPlayers().forEach { target ->
            if (target.position.distance(menu.position) > range) return@forEach
            target.addEffect(Potion(primary, 0, duration, 0.toByte()))
            secondary?.let { target.addEffect(Potion(it, if (it == primary) 1 else 0, duration, 0.toByte())) }
        }

        menu.instance.playSoundExcept(
            null,
            Sound.sound(SoundEvent.BLOCK_BEACON_POWER_SELECT, Sound.Source.BLOCK, 1.0f, 1.0f),
            menu.position,
        )
    }

    private fun returnPayment(
        player: Player,
        menu: BeaconMenu,
    ) {
        val payment = menu.getItemStack(0)
        if (payment.isAir) return
        menu.setItemStack(0, ItemStack.AIR)

        val rest = player.inventory.addItemStack(payment, TransactionOption.ALL)
        if (!rest.isAir) {
            DroppedItemFactory.maybeDrop(player.instance ?: return, player.position, rest)
        }
    }

    private fun consumePayment(menu: BeaconMenu) {
        val payment = menu.getItemStack(0)
        menu.setItemStack(0, if (payment.amount() <= 1) ItemStack.AIR else payment.withAmount(payment.amount() - 1))
    }

    private fun isPayment(item: ItemStack): Boolean = !item.isAir && item.material() in BEACON_PAYMENT_ITEMS

    private fun isValidPrimary(
        effect: PotionEffect,
        level: Int,
    ): Boolean {
        val key = effect.key().value()
        if (key == "strength" && level < 3) return false
        return key in setOf("speed", "haste", "resistance", "jump_boost", "strength")
    }

    private fun isValidSecondary(
        effect: PotionEffect,
        level: Int,
    ): Boolean {
        if (level < MAX_BEACON_LEVEL) return false
        val key = effect.key().value()
        return key == "regeneration" || key in setOf("speed", "haste", "resistance", "jump_boost", "strength")
    }
}

internal fun beaconLevel(
    instance: Instance,
    position: Point,
): Int {
    var level = 0
    for (layer in 1..MAX_BEACON_LEVEL) {
        val y = position.blockY() - layer
        val radius = layer
        if (!layerIsValid(instance, position.blockX(), y, position.blockZ(), radius)) break
        level = layer
    }
    return level
}

private fun layerIsValid(
    instance: Instance,
    centerX: Int,
    y: Int,
    centerZ: Int,
    radius: Int,
): Boolean {
    for (x in centerX - radius..centerX + radius) {
        for (z in centerZ - radius..centerZ + radius) {
            if (instance.getBlock(x, y, z) !in BEACON_BASE_BLOCKS) return false
        }
    }
    return true
}

private fun potion(key: String): PotionEffect =
    PotionEffect.fromKey("minecraft:$key") ?: error("unknown potion effect: $key")

fun BlockRulesetEditor.addBeaconAndConduit() {
    blocks(Block.BEACON) { interact.append { install(BEACON_RULE) } }
    blocks(Block.CONDUIT) { interact.append { install(CONDUIT_RULE) } }
}
