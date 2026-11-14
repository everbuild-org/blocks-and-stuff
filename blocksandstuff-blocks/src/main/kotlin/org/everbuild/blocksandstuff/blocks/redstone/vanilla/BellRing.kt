package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import org.everbuild.blocksandstuff.redstone.VibrationBus

/** Plays the bell sound and triggers the vanilla bell shake block event. */
internal fun ringBell(
    instance: Instance,
    position: Point,
    face: BlockFace,
) {
    instance.playSoundExcept(
        null,
        Sound.sound(SoundEvent.BLOCK_BELL_USE, Sound.Source.BLOCK, 2.0f, 1.0f),
        position,
    )
    instance.sendBlockAction(position, BELL_RING_ACTION.toByte(), face.dataValue().toByte())
    VibrationBus.emit(instance, position, BELL_FREQUENCY)
    instance.scheduler().scheduleTask(
        {
            instance.playSoundExcept(
                null,
                Sound.sound(SoundEvent.BLOCK_BELL_RESONATE, Sound.Source.BLOCK, 2.0f, 1.0f),
                position,
            )
        },
        TaskSchedule.tick(RESONATE_TICKS),
        TaskSchedule.stop(),
    )
}

private const val BELL_RING_ACTION = 1
private const val RESONATE_TICKS = 50
private const val BELL_FREQUENCY = 11
