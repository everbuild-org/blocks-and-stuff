package org.everbuild.blocksandstuff.fluids.pickup

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.utils.eyePosition
import org.everbuild.blocksandstuff.fluids.MinestomFluids
import org.everbuild.blocksandstuff.fluids.findBlockFace
import org.everbuild.blocksandstuff.fluids.impl.LavaFluid
import org.everbuild.blocksandstuff.fluids.impl.WaterFluid
import org.everbuild.blocksandstuff.fluids.raycastForFluid

fun getFluidPickupEventNode() = EventNode.all("fluid-pickup")
    .addListener(PlayerUseItemEvent::class.java) { event: PlayerUseItemEvent ->
        // Nur MAIN_HAND behandeln, sonst läuft die Logik oft doppelt (MAIN_HAND + OFF_HAND Event).
        val player = event.player as Player
        if (event.player.getItemInHand(event.hand) != player.itemInMainHand) return@addListener

        val held = event.player.itemInMainHand
        if (held.material() != Material.BUCKET) return@addListener

        val instance = event.player.instance

        val liquidBlock =
            raycastForFluid(
                event.player,
                event.player.eyePosition(),
                event.player.position.direction(),
                event.player.getAttributeValue(Attribute.BLOCK_INTERACTION_RANGE)
            ) ?: return@addListener

        val blockFace = findBlockFace(player = event.player, liquidBlock) ?: return@addListener

        val source = event.instance.getBlock(liquidBlock)
        val pickupEvent = FluidPickupEvent(
            event.instance,
            event.player,
            source,
            BlockVec(liquidBlock.x(), liquidBlock.y(), liquidBlock.z()),
            Block.AIR
        )

        MinecraftServer.getGlobalEventHandler().callCancellable(pickupEvent) {

            // Flüssigkeit im World-Block entfernen (durch AIR ersetzen)
            instance.placeBlock(
                PlayerPlacement(
                    pickupEvent.blockToPlace,
                    source,
                    instance,
                    liquidBlock,
                    event.player,
                    event.hand,
                    blockFace,
                    liquidBlock.x().toFloat(),
                    liquidBlock.y().toFloat(),
                    liquidBlock.z().toFloat(),
                )
            )

            val fluid = MinestomFluids.getFluidInstanceOnBlock(pickupEvent.sourceBlock)
            val filledMaterial = when (fluid) {
                is WaterFluid -> Material.WATER_BUCKET
                is LavaFluid -> Material.LAVA_BUCKET
                else -> null
            } ?: return@callCancellable
            event.isCancelled = true

            if (event.player.gameMode == GameMode.CREATIVE) {
                event.player.inventory.setEquipment(
                    EquipmentSlot.MAIN_HAND,
                    event.player.heldSlot,
                    ItemStack.of(filledMaterial)
                )
                return@callCancellable
            }

            val amount = held.amount()
            val filled = ItemStack.of(filledMaterial)

            if (amount <= 1) {
                event.player.inventory.setEquipment(
                    EquipmentSlot.MAIN_HAND,
                    event.player.heldSlot,
                    filled
                )
            } else {
                event.player.inventory.setEquipment(
                    EquipmentSlot.MAIN_HAND,
                    event.player.heldSlot,
                    held.withAmount(amount - 1)
                )

                val leftover = event.player.inventory.addItemStack(filled, TransactionOption.ALL)
                if (!leftover.isAir) {
                    event.player.dropItem(leftover)
                }
            }
        }
    }
