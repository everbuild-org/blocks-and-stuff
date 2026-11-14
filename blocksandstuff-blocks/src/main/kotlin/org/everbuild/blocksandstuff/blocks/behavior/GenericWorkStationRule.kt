package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryDrops
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryTracker
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private data class WorkStation(
    val type: InventoryType,
    val title: String,
    val returnSlots: Collection<Int>,
)

private val workStations =
    mapOf(
        Key.key("minecraft:crafting_table") to
            WorkStation(InventoryType.CRAFTING, "container.crafting", (1..9).toList()),
        Key.key("minecraft:anvil") to WorkStation(InventoryType.ANVIL, "container.repair", listOf(0, 1)),
        Key.key("minecraft:brewing_stand") to
            WorkStation(InventoryType.BREWING_STAND, "container.brewing", listOf(0, 1, 2, 3, 4)),
        Key.key("minecraft:loom") to WorkStation(InventoryType.LOOM, "container.loom", listOf(0, 1, 2)),
        Key.key("minecraft:grindstone") to WorkStation(InventoryType.GRINDSTONE, "container.grindstone", listOf(0, 1)),
        Key.key("minecraft:smithing_table") to WorkStation(InventoryType.SMITHING, "container.upgrade", listOf(0, 1, 2)),
        Key.key("minecraft:cartography_table") to
            WorkStation(InventoryType.CARTOGRAPHY, "container.cartography_table", listOf(0, 1)),
        Key.key("minecraft:stonecutter") to WorkStation(InventoryType.STONE_CUTTER, "container.stonecutter", listOf(0)),
        Key.key("minecraft:enchanting_table") to
            WorkStation(InventoryType.ENCHANTMENT, "container.enchant", listOf(0, 1)),
    )

val WORKSTATION_INTERACT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:workstation_interact") { ctx ->
        if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

        val workstation = workStations[ctx.block.key()] ?: return@Rule RuleResult.Next
        val inventory = createInventory(workstation)
        BlockInventoryTracker.track(inventory, ctx.blockPosition)
        ctx.player.openInventory(inventory)
        RuleResult.Consume
    }

val WORKSTATION_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:workstation_destroy") { ctx ->
        BlockInventoryTracker.onDestroy(ctx.instance, ctx.blockPosition, ctx.block, ctx.newBlock)
        RuleResult.Next
    }

fun BlockRulesetEditor.addWorkStations() {
    for (key in workStations.keys) {
        val block = Block.fromKey(key.asString()) ?: continue
        blocks(block) {
            interact.append { install(WORKSTATION_INTERACT_RULE) }
            destroy.append { install(WORKSTATION_DESTROY_RULE) }
        }
    }
}

private fun createInventory(workstation: WorkStation): Inventory {
    val title = Component.translatable(workstation.title)
    if (workstation.returnSlots.isEmpty()) return Inventory(workstation.type, title)
    return object : Inventory(workstation.type, title), BlockInventoryDrops {
        override val dropSlots: Collection<Int> = workstation.returnSlots
    }
}
