package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag

class StrippingBehaviorRule(block: Block) : BlockHandler {
    override fun getKey(): Key {
        return Key.key("blocksandstuff:stripping_behavior")
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val player = interaction.player
        val itemInHand = player.itemInMainHand

        if (!isAxe(itemInHand.material())) {
            return true
        }
        val currentBlock = interaction.instance.getBlock(interaction.blockPosition)
        val strippedBaseBlock = getStrippedVariant(currentBlock) ?: return false
        val strippedBlockWithProperties = preserveBlockProperties(currentBlock, strippedBaseBlock)
        interaction.instance.setBlock(interaction.blockPosition, strippedBlockWithProperties)
        if (player.gameMode != GameMode.CREATIVE) {
            damageAxe(player, itemInHand)
        }
        return false
    }

    private fun preserveBlockProperties(originalBlock: Block, strippedBlock: Block): Block {
        var resultBlock = strippedBlock
        val axis = originalBlock.getProperty("axis")
        if (axis != null) {
            resultBlock = resultBlock.withProperty("axis", axis)
        }
        return resultBlock
    }

    private fun isAxe(material: Material): Boolean {
        return when (material) {
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE -> true

            else -> false
        }
    }

    private fun getStrippedVariant(originalBlock: Block): Block? {
        val baseBlockName = originalBlock.name()
        return when (baseBlockName) {
            "minecraft:oak_log" -> Block.STRIPPED_OAK_LOG
            "minecraft:spruce_log" -> Block.STRIPPED_SPRUCE_LOG
            "minecraft:birch_log" -> Block.STRIPPED_BIRCH_LOG
            "minecraft:jungle_log" -> Block.STRIPPED_JUNGLE_LOG
            "minecraft:acacia_log" -> Block.STRIPPED_ACACIA_LOG
            "minecraft:dark_oak_log" -> Block.STRIPPED_DARK_OAK_LOG
            "minecraft:mangrove_log" -> Block.STRIPPED_MANGROVE_LOG
            "minecraft:cherry_log" -> Block.STRIPPED_CHERRY_LOG
            "minecraft:bamboo_block" -> Block.STRIPPED_BAMBOO_BLOCK

            "minecraft:oak_wood" -> Block.STRIPPED_OAK_WOOD
            "minecraft:spruce_wood" -> Block.STRIPPED_SPRUCE_WOOD
            "minecraft:birch_wood" -> Block.STRIPPED_BIRCH_WOOD
            "minecraft:jungle_wood" -> Block.STRIPPED_JUNGLE_WOOD
            "minecraft:acacia_wood" -> Block.STRIPPED_ACACIA_WOOD
            "minecraft:dark_oak_wood" -> Block.STRIPPED_DARK_OAK_WOOD
            "minecraft:mangrove_wood" -> Block.STRIPPED_MANGROVE_WOOD
            "minecraft:cherry_wood" -> Block.STRIPPED_CHERRY_WOOD

            "minecraft:crimson_stem" -> Block.STRIPPED_CRIMSON_STEM
            "minecraft:warped_stem" -> Block.STRIPPED_WARPED_STEM
            "minecraft:crimson_hyphae" -> Block.STRIPPED_CRIMSON_HYPHAE
            "minecraft:warped_hyphae" -> Block.STRIPPED_WARPED_HYPHAE
            else -> null
        }
    }

    private fun damageAxe(player: Player, axe: ItemStack) {
        val currentDamage = axe.getTag(Tag.Integer("Damage")) ?: 0
        if (axe.has(DataComponents.MAX_DAMAGE)) {
            val maxDamage = axe.get(DataComponents.MAX_DAMAGE)!!
            if (currentDamage + 1 >= maxDamage) {
                player.itemInMainHand = ItemStack.AIR
                val breakSound = Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1.0f, 1.0f)
                player.instance?.playSound(breakSound, player.position)
            } else {
                val damagedAxe = axe.withTag(Tag.Integer("Damage"), currentDamage + 1)
                player.itemInMainHand = damagedAxe
            }
        }
    }

    companion object {
        fun getStrippableBlocks(): List<Block> {
            return listOf(
                Block.OAK_LOG,
                Block.SPRUCE_LOG,
                Block.BIRCH_LOG,
                Block.JUNGLE_LOG,
                Block.ACACIA_LOG,
                Block.DARK_OAK_LOG,
                Block.MANGROVE_LOG,
                Block.CHERRY_LOG,
                Block.BAMBOO_BLOCK,

                Block.OAK_WOOD,
                Block.SPRUCE_WOOD,
                Block.BIRCH_WOOD,
                Block.JUNGLE_WOOD,
                Block.ACACIA_WOOD,
                Block.DARK_OAK_WOOD,
                Block.MANGROVE_WOOD,
                Block.CHERRY_WOOD,

                Block.CRIMSON_STEM,
                Block.WARPED_STEM,
                Block.CRIMSON_HYPHAE,
                Block.WARPED_HYPHAE
            )
        }
    }
}