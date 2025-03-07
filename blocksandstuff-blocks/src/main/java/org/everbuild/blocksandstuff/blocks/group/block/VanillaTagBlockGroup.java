package org.everbuild.blocksandstuff.blocks.group.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;

import java.util.Collection;
import java.util.Objects;

public class VanillaTagBlockGroup implements BlockGroup {
    private final Key key;

    public VanillaTagBlockGroup(Key key) {
        this.key = key;
    }

    @Override
    public Collection<Block> allMatching() {
        Tag tag = MinecraftServer.getTagManager().getTag(Tag.BasicType.BLOCKS, key.asString());
        assert tag != null;

        return tag
                .getValues()
                .stream()
                .map(NamespaceID::from)
                .map(Block::fromNamespaceId)
                .filter(Objects::nonNull)
                .toList();
    }
}
