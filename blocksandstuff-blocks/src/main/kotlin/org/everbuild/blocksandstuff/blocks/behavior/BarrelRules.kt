package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.codec.Transcoder
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.event.EventListener
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.instance.block.BlockHandler.Destroy
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.time.TimeUnit
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class BarrelRules(private val block: Block) : BlockHandler {
    override fun getKey(): Key {
        return block.key()
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val p = interaction.player;
        if (p.isSneaking && !p.itemInMainHand.isAir) return onInteract(interaction)

        val clickedBlock = interaction.block
        val blockPos = interaction.blockPosition
        val instance = interaction.instance

        val inv = getBlockInventory(clickedBlock, blockPos, instance)

        p.openInventory(inv)
        instance.setBlock(blockPos, clickedBlock.withProperty("open", "true"))

        MinecraftServer.getGlobalEventHandler().addListener(
            EventListener.builder(InventoryCloseEvent::class.java)
                .expireCount(1)
                .handler({ event: InventoryCloseEvent? ->
                    if (event != null) {
                        instance.setBlock(blockPos, clickedBlock.withProperty("open", "false"))
                        updateBlockInventory(inv, clickedBlock, blockPos, instance)
                    }
                })
                .build()
        )

        return false
    }

    override fun onDestroy(destroy: Destroy) {
        val block = destroy.block
        if (destroy.instance.getBlock(destroy.getBlockPosition()) === Block.AIR && block.nbt() != null) {
            if (block.nbt()!!.get("Inventory") != null) {
                val tag = block.nbt()!!.get("Inventory")
                val inventory = ITEMSTACK_CODEC.decode(Transcoder.NBT, tag!!).orElse(
                    listOf(
                        ItemStack.AIR
                    )
                )
                inventory.forEach(Consumer { itemStack: ItemStack? ->
                    val entity = ItemEntity(itemStack!!)
                    entity.setPickupDelay(1, TimeUnit.SECOND) // 1s for natural drop
                    entity.scheduleRemove(5, TimeUnit.MINUTE)
                    entity.setVelocity(
                        Vec(
                            RANDOM.nextDouble() * 2 - 1,
                            2.0,
                            RANDOM.nextDouble() * 2 - 1
                        )
                    )
                    entity.setInstance(destroy.instance, destroy.blockPosition.add(0.5, 0.5, 0.5))
                })
            }
        }
    }

    private fun updateBlockInventory(inv: Inventory, block: Block, blockPos: Point, instance: Instance) {
        instance.setBlock(
            blockPos,
            block.withNbt(
                CompoundBinaryTag.builder().put(
                    "Inventory",
                    ITEMSTACK_CODEC.encode(Transcoder.NBT, listOf(*inv.getItemStacks()))
                        .orElseThrow()
                ).build()
            )
        )
    }

    private fun getBlockInventory(block: Block, blockPos: Point, instance: Instance): Inventory {
        val inv = Inventory(InventoryType.CHEST_3_ROW, Component.text("Barrel"))

        if (block.nbt() != null) {
            if (block.nbt()!!.get("Inventory") != null) {
                val tag = block.nbt()!!.get("Inventory")
                val inventory = ITEMSTACK_CODEC.decode(Transcoder.NBT, tag!!).orElse(
                    listOf(
                        ItemStack.AIR
                    )
                )
                val slot = AtomicInteger(0)
                inventory.forEach(Consumer { i: ItemStack? -> inv.setItemStack(slot.getAndIncrement(), i!!) })
            }
        } else {
            instance.setBlock(
                blockPos,
                block.withNbt(
                    CompoundBinaryTag.builder().put(
                        "Inventory",
                        ITEMSTACK_CODEC.encode(
                            Transcoder.NBT,
                            listOf(ItemStack.AIR)
                        ).orElseThrow()
                    ).build()
                )
            )
        }


        return inv
    }

    companion object {
        private val ITEMSTACK_CODEC = ItemStack.CODEC.list()
        private val RANDOM = ThreadLocalRandom.current();
    }
}