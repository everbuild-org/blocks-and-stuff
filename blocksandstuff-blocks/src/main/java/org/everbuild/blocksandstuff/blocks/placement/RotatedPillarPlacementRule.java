package org.everbuild.blocksandstuff.blocks.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.everbuild.blocksandstuff.common.utils.DirectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RotatedPillarPlacementRule extends BlockPlacementRule {
    public RotatedPillarPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        if (placementState.playerPosition() == null) return placementState.block();
        return placementState.block()
                .withProperty(
                        "axis",
                        DirectionUtils.getAxis(
                                Objects.requireNonNull(placementState.blockFace())
                                        .toDirection()
                        )
                );
    }

}
