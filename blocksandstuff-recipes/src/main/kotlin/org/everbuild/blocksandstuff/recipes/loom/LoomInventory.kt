package org.everbuild.blocksandstuff.recipes.loom

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.color.DyeColor
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryButtonClickEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.inventory.click.Click
import net.minestom.server.instance.block.banner.BannerPattern
import net.minestom.server.instance.block.banner.BannerPatternTags
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.MaterialTags
import net.minestom.server.item.component.BannerPatterns
import net.minestom.server.registry.Holder
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryDrops
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.recipes.util.excludeResultSlotsFromDrag
import org.everbuild.blocksandstuff.recipes.util.isIn
import org.everbuild.blocksandstuff.recipes.util.transferInto

class LoomInventory : Inventory(InventoryType.LOOM, Component.translatable("container.loom")), BlockInventoryDrops {
    companion object {
        const val BANNER_SLOT = 0
        const val DYE_SLOT = 1
        const val PATTERN_SLOT = 2
        const val RESULT_SLOT = 3

        /** Vanilla banns accept at most six pattern layers. */
        const val MAX_LAYERS = 6
    }

    override val dropSlots: Collection<Int> = listOf(BANNER_SLOT, DYE_SLOT, PATTERN_SLOT)

    /**
     * The banner pattern selected in the loom UI. Vanilla looms allow choosing a pattern
     * from the list without consuming a banner pattern item, so the selection must be
     * tracked here instead of relying only on the pattern slot.
     */
    private var selectedPattern: Holder<BannerPattern>? = null

    init {
        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { event ->
                if (event.slot == RESULT_SLOT) return@addListener
                if (event.slot == PATTERN_SLOT) selectedPattern = null
                updateResult()
            }
            .addListener(InventoryButtonClickEvent::class.java) { event ->
                val pattern = selectablePatterns().getOrNull(event.buttonId) ?: return@addListener
                selectedPattern = pattern
                updateResult()
            }
            .addListener(InventoryPreClickEvent::class.java) { event ->
                event.excludeResultSlotsFromDrag(RESULT_SLOT)

                val slot = event.slot
                if (slot !in 0 until size) return@addListener

                if (slot == RESULT_SLOT) {
                    event.isCancelled = true
                    takeResult(event)
                    return@addListener
                }

                val cursor = event.player.inventory.cursorItem
                if (cursor.isAir) return@addListener

                if (slotFor(cursor.material()) != slot) {
                    event.isCancelled = true
                }
            }
    }

    override fun shiftClick(player: Player, slot: Int, button: Int): Boolean {
        val playerInventory = player.inventory

        if (slot == RESULT_SLOT) return false

        if (slot < size) {
            // Shift-click inside the loom -> move the item into the player inventory.
            val clicked = getItemStack(slot)
            if (clicked.isAir) return false

            setItemStack(slot, ItemStack.AIR)
            val leftover = playerInventory.addItemStack(clicked, TransactionOption.ALL)
            if (!leftover.isAir) {
                DroppedItemFactory.maybeDropFromPlayer(player, leftover)
            }

            playerInventory.update()
            update(player)
            return true
        }

        // Shift-click from the player inventory -> route into the matching loom slot.
        val clickSlot = slot - size
        val clicked = playerInventory.getItemStack(clickSlot)
        if (clicked.isAir) return false

        val target = slotFor(clicked.material()) ?: return false
        val leftover = transferInto(clicked, this, listOf(target))

        playerInventory.setItemStack(clickSlot, leftover)
        playerInventory.update()
        update(player)
        return true
    }

    private fun updateResult() {
        setItemStack(RESULT_SLOT, computeResult())
    }

    private fun computeResult(): ItemStack {
        val banner = getItemStack(BANNER_SLOT)
        val dye = getItemStack(DYE_SLOT)
        if (banner.isAir || dye.isAir) return ItemStack.AIR

        val baseColor = banner.get(DataComponents.BASE_COLOR) ?: baseColorOf(banner.material())
            ?: return ItemStack.AIR
        val dyeColor = dyeColorOf(dye.material()) ?: return ItemStack.AIR

        // A banner pattern item takes precedence, otherwise the pattern selected in the UI is used.
        val patternHolder = patternItemPattern() ?: selectedPattern ?: return ItemStack.AIR

        val existing = banner.get(DataComponents.BANNER_PATTERNS)?.layers() ?: emptyList()
        if (existing.size >= MAX_LAYERS) return ItemStack.AIR

        val layers = existing + BannerPatterns.Layer(patternHolder, dyeColor)
        return banner
            .with(DataComponents.BANNER_PATTERNS, BannerPatterns(layers))
            .with(DataComponents.BASE_COLOR, baseColor)
            .withAmount(1)
    }

    /**
     * The patterns offered by the current banner pattern item, or the vanilla default list
     * (all patterns that do not require an item) when the slot is empty.
     */
    private fun selectablePatterns(): List<Holder<BannerPattern>> {
        val pattern = getItemStack(PATTERN_SLOT)
        if (!pattern.isAir) {
            return pattern.get(DataComponents.PROVIDES_BANNER_PATTERNS)?.toList() ?: emptyList()
        }

        val registry = MinecraftServer.getBannerPatternRegistry()
        val noItemRequired = registry.getTag(BannerPatternTags.NO_ITEM_REQUIRED) ?: return emptyList()
        // Iterate the tag itself instead of the registry so the order matches the tag
        // arrangement sent to the client, which the button ids are based on.
        return noItemRequired.mapNotNull { registry.get(it) }
    }

    private fun patternItemPattern(): Holder<BannerPattern>? {
        val pattern = getItemStack(PATTERN_SLOT)
        if (pattern.isAir) return null
        return pattern.get(DataComponents.PROVIDES_BANNER_PATTERNS)?.firstOrNull()
    }

    private fun takeResult(event: InventoryPreClickEvent) {
        val result = getItemStack(RESULT_SLOT)
        if (result.isAir) return

        val player = event.player
        val shift = event.click is Click.LeftShift || event.click is Click.RightShift

        // Vanilla: banner and dye are consumed, the banner pattern item is not.
        val banner = getItemStack(BANNER_SLOT)
        val dye = getItemStack(DYE_SLOT)

        if (shift) {
            // Craft the whole input at once instead of only a single banner.
            val crafts = minOf(banner.amount(), dye.amount())
            val leftover = player.inventory.addItemStack(result.withAmount(crafts), TransactionOption.ALL)
            if (!leftover.isAir) {
                DroppedItemFactory.maybeDropFromPlayer(player, leftover)
            }
            setItemStack(BANNER_SLOT, banner.withAmount(banner.amount() - crafts))
            setItemStack(DYE_SLOT, dye.withAmount(dye.amount() - crafts))
        } else {
            val cursor = player.inventory.cursorItem
            if (!cursor.isAir &&
                (!cursor.isSimilar(result) || cursor.amount() + result.amount() > cursor.maxStackSize())
            ) {
                return
            }
            val amount = (if (cursor.isAir) 0 else cursor.amount()) + result.amount()
            player.inventory.cursorItem = result.withAmount(amount)
            setItemStack(BANNER_SLOT, banner.withAmount(banner.amount() - 1))
            setItemStack(DYE_SLOT, dye.withAmount(dye.amount() - 1))
        }

        player.inventory.update()
        update(player)
        updateResult()
    }

    private fun slotFor(material: Material): Int? = when {
        material.isIn(MaterialTags.BANNERS) -> BANNER_SLOT
        material.isIn(MaterialTags.LOOM_DYES) -> DYE_SLOT
        material.isIn(MaterialTags.LOOM_PATTERNS) -> PATTERN_SLOT
        else -> null
    }

    private fun dyeColorOf(material: Material): DyeColor? {
        val name = material.key().value()
        if (!name.endsWith("_dye")) return null
        return runCatching { DyeColor.valueOf(name.removeSuffix("_dye").uppercase()) }.getOrNull()
    }

    private fun baseColorOf(material: Material): DyeColor? {
        val name = material.key().value()
        if (!name.endsWith("_banner")) return null
        return runCatching { DyeColor.valueOf(name.removeSuffix("_banner").uppercase()) }.getOrNull()
    }
}
