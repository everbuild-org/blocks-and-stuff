package org.everbuild.blocksandstuff.recipes

import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.block.BlockManager
import net.minestom.server.network.packet.client.play.ClientClickWindowButtonPacket
import org.everbuild.blocksandstuff.recipes.api.ItemController
import org.everbuild.blocksandstuff.recipes.api.StashController
import org.everbuild.blocksandstuff.recipes.grid.CraftingTableHandler
import org.everbuild.blocksandstuff.recipes.grid.PlayerInventoryCraftingGridService
import org.everbuild.blocksandstuff.recipes.loader.FuelLoader
import org.everbuild.blocksandstuff.recipes.loader.RecipeLoader
import org.everbuild.blocksandstuff.recipes.smelting.blast_furnace.BlastFurnaceHandler
import org.everbuild.blocksandstuff.recipes.smelting.furnace.FurnaceHandler
import org.everbuild.blocksandstuff.recipes.smelting.smoker.SmokerHandler
import org.everbuild.blocksandstuff.recipes.smithing.SmithingTableHandler
import org.everbuild.blocksandstuff.recipes.stonecutting.StonecutterHandler
import org.everbuild.blocksandstuff.recipes.util.InventoryButtonClickListener
import org.slf4j.LoggerFactory

class RecipeRegistrations {
    private val logger = LoggerFactory.getLogger(RecipeRegistrations::class.java)

    val recipeNamespaces = mutableListOf<String>()
    val fuelNamespaces = mutableListOf<String>()

    val registrations = FeatureRegistration.entries.toMutableList()

    var itemController: ItemController? = null
    var stashController: StashController? = null

    fun fuelNamespace(namespace: String): RecipeRegistrations {
        fuelNamespaces.add(namespace)
        return this
    }

    fun recipeNamespace(namespace: String): RecipeRegistrations {
        recipeNamespaces.add(namespace)
        return this
    }

    fun addRegistration(registration: FeatureRegistration): RecipeRegistrations {
        registrations.add(registration)
        return this
    }

    fun removeRegistration(registration: FeatureRegistration): RecipeRegistrations {
        registrations.remove(registration)
        return this
    }

    fun disableRegistrations(): RecipeRegistrations {
        registrations.clear()
        return this
    }

    fun itemController(controller: ItemController): RecipeRegistrations {
        itemController = controller
        return this
    }

    fun stashController(controller: StashController): RecipeRegistrations {
        stashController = controller
        return this
    }

    fun apply() {
        itemController?.let { RecipeFactory.itemController = it }
        stashController?.let { RecipeFactory.stashController = it }

        recipeNamespaces.forEach(RecipeLoader::loadAllRecipes)
        fuelNamespaces.forEach(FuelLoader::loadAllFuels)

        registrations.forEach { it.applicator(MinecraftServer.getBlockManager()) }

        MinecraftServer.getPacketListenerManager().setPlayListener(
            ClientClickWindowButtonPacket::class.java,
            InventoryButtonClickListener::inventoryButtonClickListener
        )

        logger.info("Registered ${registrations.size} recipe features, ${recipeNamespaces.size} recipe namespaces, and ${fuelNamespaces.size} fuel namespaces.")
    }

    companion object {
        @JvmStatic
        fun builder() = RecipeRegistrations()

        operator fun invoke(builder: RecipeRegistrations.() -> Unit) {
            val reg = RecipeRegistrations()
            reg.builder()
            reg.apply()
        }

        enum class FeatureRegistration(val applicator: (blocks: BlockManager) -> Unit) {
            CRAFTING({ blocks ->
                MinecraftServer.getGlobalEventHandler().addListener(
                    PlayerSpawnEvent::class.java
                ) {
                    PlayerInventoryCraftingGridService(it.player)
                }

                blocks.registerHandler("minecraft:crafting_table", ::CraftingTableHandler)
            }),
            SMITHING({ blocks ->
                blocks.registerHandler("minecraft:smithing_table", ::SmithingTableHandler)
            }),
            SMELTING({ blocks ->
                blocks.registerHandler("minecraft:furnace", ::FurnaceHandler)
                blocks.registerHandler("minecraft:blast_furnace", ::BlastFurnaceHandler)
                blocks.registerHandler("minecraft:smoker", ::SmokerHandler)
            }),
            STONECUTTING({ blocks ->
                blocks.registerHandler("minecraft:stonecutter", ::StonecutterHandler)
            })
        }
    }
}