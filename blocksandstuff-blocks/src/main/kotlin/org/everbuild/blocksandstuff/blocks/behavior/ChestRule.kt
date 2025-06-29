package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.codec.Transcoder
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.event.EventListener
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.instance.block.BlockHandler.Destroy
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.play.BlockActionPacket
import net.minestom.server.utils.time.TimeUnit
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.min

class ChestRule(private val block: Block) : BlockHandler {
    override fun getKey(): Key {
        return block.key()
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val p: Player = interaction.player
        if (p.isSneaking && !p.itemInMainHand.isAir
        ) return onInteract(interaction)

        val clickedBlock = interaction.block
        val blockPos = interaction.blockPosition
        val instance = interaction.instance

        val inv = getBlockInventory(clickedBlock, blockPos, instance)

        p.openInventory(inv)

        MinecraftServer.getGlobalEventHandler().addListener(
            EventListener.builder(InventoryCloseEvent::class.java)
                .expireCount(1)
                .handler( { event: InventoryCloseEvent? ->
                    if (event != null) {
                        updateBlockInventory(inv, clickedBlock, blockPos, instance)
                        updateBlockAction(p, blockPos, clickedBlock, 0.toByte())
                    }
                })
                .build()
        )
        updateBlockAction(p, blockPos, clickedBlock, 1.toByte())
        return false
    }

    override fun onDestroy(destroy: Destroy) {
        val block = destroy.block
        if (destroy.instance.getBlock(destroy.blockPosition) === Block.AIR && block.nbt() != null) {
            if (block.nbt()!!.get("Inventory") != null) {
                val tag = block.nbt()!!.get("Inventory")
                val inventory = ITEMSTACK_CODEC.decode(Transcoder.NBT, tag!!).orElse(
                    listOf<ItemStack?>(
                        ItemStack.AIR
                    )
                )
                inventory.forEach(Consumer { itemStack: ItemStack? ->
                    val entity = ItemEntity(itemStack!!)
                    entity.setPickupDelay(1, TimeUnit.SECOND)
                    entity.scheduleRemove(5, TimeUnit.MINUTE)
                    entity.setVelocity(
                        Vec(
                            ThreadLocalRandom.current().nextDouble() * 2 - 1,
                            2.0,
                            ThreadLocalRandom.current().nextDouble() * 2 - 1
                        )
                    )
                    entity.setInstance(destroy.getInstance(), destroy.getBlockPosition().add(0.5, 0.5, 0.5))
                })
            }
        }
    }

    private fun updateBlockInventory(inv: Inventory, block: Block, blockPos: Point, instance: Instance) {
        if (block.getProperty("type").equals("left")
            || block.getProperty("type") == "right"
        ) {
            val fullItems = Arrays.asList(*inv.getItemStacks())

            val pair: ChestPair? = getChestPair(block, blockPos, instance)
            val leftChestPos: Point? = pair?.leftPos
            val leftBlock: Block? = pair?.left
            val rightChestPos: Point? = pair?.rightPos
            val rightBlock: Block? = pair?.right

            val leftHalf = fullItems.subList(0, 27)
            val rightHalf = fullItems.subList(27, 54)

            val leftNBT = CompoundBinaryTag.builder()
                .put("Inventory", ITEMSTACK_CODEC.encode(Transcoder.NBT, leftHalf).orElseThrow())
                .build()
            val rightNBT = CompoundBinaryTag.builder()
                .put("Inventory", ITEMSTACK_CODEC.encode(Transcoder.NBT, rightHalf).orElseThrow())
                .build()

            if (leftChestPos != null && leftBlock != null) {
                instance.setBlock(leftChestPos, leftBlock.withNbt(leftNBT))
            }
            if (rightChestPos != null && rightBlock != null) {
                instance.setBlock(rightChestPos, rightBlock.withNbt(rightNBT))
            }
        } else {
            instance.setBlock(
                blockPos,
                block.withNbt(
                    CompoundBinaryTag.builder().put(
                        "Inventory",
                        ITEMSTACK_CODEC.encode(
                            Transcoder.NBT,
                            listOf(*inv.getItemStacks())
                        ).orElseThrow()
                    ).build()
                )
            )
        }
    }

    private fun getBlockInventory(block: Block, blockPos: Point, instance: Instance): Inventory {
        val type = block.getProperty("type")
        val inv: Inventory
        if (type.equals("left") ||
            type.equals("right")
        ) {
            inv = Inventory(InventoryType.CHEST_6_ROW, Component.text("Large Chest"))
            val items: MutableList<ItemStack?> =
                ArrayList<ItemStack?>(listOf(*arrayOfNulls<ItemStack>(54)))
            items.fill(ItemStack.AIR)

            val pair: ChestPair? = getChestPair(block, blockPos, instance)
            val leftBlock: Block? = pair?.left
            val rightBlock: Block? = pair?.right

            if (leftBlock != null && leftBlock.nbt() != null && leftBlock.nbt()!!.get("Inventory") != null) {
                val leftItems =
                    ITEMSTACK_CODEC.decode(Transcoder.NBT, leftBlock.nbt()!!.get("Inventory")!!)
                        .orElse(listOf(ItemStack.AIR))
                for (i in 0..<min(27, leftItems.size)) {
                    items.set(i, leftItems.get(i))
                }
            }

            if (rightBlock != null && rightBlock.nbt() != null && rightBlock.nbt()!!.get("Inventory") != null) {
                val rightItems = ITEMSTACK_CODEC.decode(
                    Transcoder.NBT, rightBlock.nbt()!!
                        .get("Inventory")!!
                ).orElse(listOf(ItemStack.AIR))
                for (i in 0..<min(27, rightItems.size)) {
                    items.set(27 + i, rightItems.get(i))
                }
            }

            val slot = AtomicInteger(0)
            items.forEach(Consumer { i: ItemStack? -> inv.setItemStack(slot.getAndIncrement(), i!!) })
        } else {
            inv = Inventory(InventoryType.CHEST_3_ROW, Component.text("Chest"))

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
                                Arrays.asList(*inv.getItemStacks())
                            ).orElseThrow()
                        ).build()
                    )
                )
            }
        }


        return inv
    }

    private fun updateBlockAction(p: Player, blockPos: Point?, clickedBlock: Block, value: Byte) {
        p.sendPacketToViewersAndSelf(
            BlockActionPacket(
                blockPos,
                1.toByte(),
                value,
                clickedBlock
            )
        )
    }

    private fun getConnectedChestPos(origin: Point, chestBlock: Block): Point {
        val facing = chestBlock.getProperty("facing")
        val type = chestBlock.getProperty("type")

        var dx = 0
        var dz = 0

        when (facing) {
            "north" -> {
                dx = (if (type == "left") 1 else -1)
            }

            "south" -> {
                dx = (if (type == "left") -1 else 1)
            }

            "east" -> {
                dz = (if (type == "left") 1 else -1)
            }

            "west" -> {
                dz = (if (type == "left") -1 else 1)
            }
        }

        return origin.withX(origin.x() + dx).withZ(origin.z() + dz)
    }

    @JvmRecord
    internal data class ChestPair(val leftPos: Point?, val left: Block?, val rightPos: Point?, val right: Block?)

    private fun getChestPair(block: Block, origin: Point, instance: Instance): ChestPair? {
        if (block.getProperty("type").equals("left")) {
            val rightPos = getConnectedChestPos(origin, block)
            return ChestPair(origin, block, rightPos, instance.getBlock(rightPos))
        } else if (block.getProperty("type").equals("right")) {
            val leftPos = getConnectedChestPos(origin, block)
            return ChestPair(leftPos, instance.getBlock(leftPos), origin, block)
        }
        return null
    }

    companion object {
        private val ITEMSTACK_CODEC = ItemStack.CODEC.list()
    }
}