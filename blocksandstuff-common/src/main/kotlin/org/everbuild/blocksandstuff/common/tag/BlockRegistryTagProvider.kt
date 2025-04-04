package org.everbuild.blocksandstuff.common.tag

import net.minestom.server.instance.block.Block

object BlockRegistryTagProvider : RegistryTagProvider<Block>("block") {
    override fun map(key: String): Block = Block.fromKey(key)!!
}