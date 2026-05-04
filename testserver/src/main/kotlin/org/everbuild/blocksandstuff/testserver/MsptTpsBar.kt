package org.everbuild.blocksandstuff.testserver

import java.time.Duration
import kotlin.math.max
import kotlin.math.min
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.timer.TaskSchedule

class MsptTpsBar {
    private val bossBar: BossBar = BossBar.bossBar(
        Component.text("MSPT: 0.0 | TPS: 20.0"),
        0.0f,
        BossBar.Color.GREEN,
        BossBar.Overlay.PROGRESS
    )
    private var ticks = 0
    private var totalMspt = 0.0

    init {

        MinecraftServer.getGlobalEventHandler().addListener(
            PlayerSpawnEvent::class.java
        ) { event: PlayerSpawnEvent -> event.player.showBossBar(bossBar) }

        MinecraftServer.getGlobalEventHandler().addListener(
            ServerTickMonitorEvent::class.java
        ) { event: ServerTickMonitorEvent ->
            ticks++
            totalMspt += event.tickMonitor.tickTime
        }

        MinecraftServer.getSchedulerManager().scheduleTask(
            {
                val tps = min(20.0f, ticks.toFloat())
                val mspt = if (ticks > 0) totalMspt / ticks else 0.0

                bossBar.name(Component.text(String.format("MSPT: %.1f | TPS: %.1f", mspt, tps), getTextColor(mspt)))
                bossBar.progress(max(0.0, min(1.0, mspt / 50.0)).toFloat())
                bossBar.color(getBarColor(mspt))

                ticks = 0
                totalMspt = 0.0
            },
            TaskSchedule.duration(Duration.ofSeconds(1)),
            TaskSchedule.duration(Duration.ofSeconds(1))
        )
    }

    private fun getTextColor(mspt: Double): NamedTextColor {
        if (mspt < 40.0) return NamedTextColor.GREEN
        if (mspt < 50.0) return NamedTextColor.YELLOW
        return NamedTextColor.RED
    }

    private fun getBarColor(mspt: Double): BossBar.Color {
        if (mspt < 40.0) return BossBar.Color.GREEN
        if (mspt < 50.0) return BossBar.Color.YELLOW
        return BossBar.Color.RED
    }
}