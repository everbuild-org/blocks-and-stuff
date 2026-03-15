package org.everbuild.blocksandstuff.recipes.smelting

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import org.everbuild.blocksandstuff.recipes.api.BlockInventoryBackend

class FurnaceInventoryBackend(blockPos: Point, instance: Instance) :
    BlockInventoryBackend(3, blockPos, instance)