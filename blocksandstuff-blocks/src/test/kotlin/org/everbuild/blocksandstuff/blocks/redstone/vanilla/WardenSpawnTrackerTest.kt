package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

class WardenSpawnTrackerTest {
    @Test
    fun warningLevelRaisesToFourAndCaps() {
        val player = UUID.randomUUID()
        WardenSpawnTracker.reset(player)

        Assertions.assertEquals(1, WardenSpawnTracker.blame(player, 0))
        Assertions.assertEquals(2, WardenSpawnTracker.blame(player, 200))
        Assertions.assertEquals(3, WardenSpawnTracker.blame(player, 400))
        Assertions.assertEquals(4, WardenSpawnTracker.blame(player, 600))
        Assertions.assertEquals(4, WardenSpawnTracker.blame(player, 800))
    }

    @Test
    fun warningLevelDecaysAfterTwelveThousandTicks() {
        val player = UUID.randomUUID()
        WardenSpawnTracker.reset(player)
        WardenSpawnTracker.blame(player, 0)
        WardenSpawnTracker.blame(player, 200)

        Assertions.assertEquals(1, WardenSpawnTracker.warningLevel(player, 200 + 12000))
    }

    @Test
    fun cooldownPreventsImmediateSecondWarning() {
        val player = UUID.randomUUID()
        WardenSpawnTracker.reset(player)

        Assertions.assertEquals(1, WardenSpawnTracker.blame(player, 0))
        Assertions.assertEquals(1, WardenSpawnTracker.blame(player, 100))
        Assertions.assertEquals(2, WardenSpawnTracker.blame(player, 200))
    }
}