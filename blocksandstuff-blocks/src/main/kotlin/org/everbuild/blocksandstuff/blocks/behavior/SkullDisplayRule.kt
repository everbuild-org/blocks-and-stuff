package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.stacked_behaviour_block.configuration.blockEntity
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor

fun BlockRulesetEditor.addSkulls() {
    blocks(Block.PLAYER_HEAD) {
        blockEntity {
            tags += Tag.NBT("profile")
            tags += Tag.String("note_block_sound")
            tags += Tag.NBT("custom_name")
        }
    }
}
