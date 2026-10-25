package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.redstone.ComponentSelector
import org.everbuild.blocksandstuff.redstone.PowerSource
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.RedstoneRegistry

/**
 * Registers the vanilla redstone components on a [RedstoneRegistry].
 */
fun RedstoneRegistry.registerVanillaComponents() {
    PistonCleanupRegistration.registerDefault()
    register(ComponentSelector.ByBlock(Block.REDSTONE_WIRE)) { WireComponent() }
    register(ComponentSelector.ByBlock(Block.REDSTONE_TORCH)) { TorchComponent() }
    register(ComponentSelector.ByBlock(Block.REDSTONE_WALL_TORCH)) { TorchComponent() }
    register(ComponentSelector.ByBlock(Block.REPEATER)) { RepeaterComponent() }
    register(ComponentSelector.ByBlock(Block.COMPARATOR)) { ComparatorComponent() }
    register(ComponentSelector.ByBlock(Block.PISTON)) { PistonComponent(sticky = false) }
    register(ComponentSelector.ByBlock(Block.STICKY_PISTON)) { PistonComponent(sticky = true) }
    register(ComponentSelector.ByBlock(Block.MOVING_PISTON)) { MovingPistonComponent() }
    register(ComponentSelector.ByBlock(Block.OBSERVER)) { ObserverComponent() }
    register(ComponentSelector.ByBlock(Block.DAYLIGHT_DETECTOR)) { DaylightDetectorComponent() }
    register(ComponentSelector.ByBlock(Block.DISPENSER)) { DispenserComponent() }
    register(ComponentSelector.ByBlock(Block.DROPPER)) { DropperComponent() }
    register(ComponentSelector.ByBlock(Block.TNT)) { TntComponent() }
    register(ComponentSelector.ByBlock(Block.LEVER)) { SwitchComponent() }
    register(ComponentSelector.ByTag("minecraft:buttons")) { SwitchComponent() }
    register(ComponentSelector.ByTag("minecraft:pressure_plates")) { SwitchComponent() }

    source(
        Block.REDSTONE_BLOCK,
        object : PowerSource {
            override fun strength(block: Block): Int = REDSTONE_MAX_POWER

            override fun direct(block: Block): Boolean = false
        },
    )

    consumer(Block.REDSTONE_LAMP) { ctx, powered, _ ->
        ctx.block().withProperty("lit", powered.toString())
    }

    consumer(ComponentSelector.ByTag("minecraft:trapdoors")) { ctx, powered, _ ->
        ctx.block().withProperty("open", powered.toString())
    }

    consumer(ComponentSelector.ByTag("minecraft:fence_gates")) { ctx, powered, _ ->
        ctx.block().withProperty("open", powered.toString())
    }

    register(ComponentSelector.ByTag("minecraft:doors")) { DoorComponent() }
}
