package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler

private val blocks = MinecraftServer.getBlockManager()

fun Block.withDefaultHandler(): Block {
    if (this.handler() != null) return this
    return this.withHandler(MinecraftServer.getBlockManager().getHandler(this.key().asString()))
}

/** Returns the attached handler, or a fresh registered handler for this block type. */
fun Block.handlerOrRegistered(): BlockHandler? = handler() ?: MinecraftServer.getBlockManager().getHandler(this.key().asString())
