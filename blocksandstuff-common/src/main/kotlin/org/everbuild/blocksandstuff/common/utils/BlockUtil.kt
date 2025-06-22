package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block

private val blocks = MinecraftServer.getBlockManager()

fun Block.withDefaultHandler(): Block {
    if (this.handler() != null) return this
    return this.withHandler(blocks.getHandler(this.key().asString()))
}