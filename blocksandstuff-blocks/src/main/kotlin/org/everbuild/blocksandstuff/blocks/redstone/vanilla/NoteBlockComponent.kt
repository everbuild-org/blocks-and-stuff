package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import kotlin.math.pow

/**
 * Note block. Plays its current instrument and pitch on a rising redstone edge and mirrors the
 * input in the `powered` state. The instrument is read from the block state, matching vanilla's
 * block-below based instrument selection.
 */
class NoteBlockComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val instrument = NoteBlockInstruments.forBlockBelow(ctx.block(ctx.position.relative(BlockFace.BOTTOM)))
        var updated = block
        if (block.getProperty("instrument") != instrument) updated = updated.withProperty("instrument", instrument)

        val powered = isPowered(ctx)
        val wasPowered = block.getProperty("powered") == "true"
        if (powered != wasPowered) {
            if (powered) NoteBlockSound.play(ctx.instance, ctx.position, updated)
            updated = updated.withProperty("powered", powered.toString())
        }
        return if (updated.stateId() == block.stateId()) ComponentEvaluation.None else ComponentEvaluation.SetState(updated)
    }
}

internal object NoteBlockSound {
    fun play(
        instance: Instance,
        position: Point,
        block: Block,
    ) {
        if (isSilenced(instance, position)) return
        val above = instance.getBlock(position.add(0.0, 1.0, 0.0))
        val headInstrument = NoteBlockInstruments.fromHead(above)
        if (headInstrument == "custom_head") return

        val note = block.getProperty("note")?.toIntOrNull()?.coerceIn(0, 24) ?: 0
        val pitch = if (headInstrument != null) 1.0f else 2.0.pow((note - 12) / 12.0).toFloat()
        val instrument = headInstrument ?: NoteBlockInstruments.forBlockBelow(instance.getBlock(position.add(0.0, -1.0, 0.0)))

        instance.playSoundExcept(
            null,
            Sound.sound(sound(instrument), Sound.Source.RECORD, 3.0f, pitch),
            position,
        )
        if (headInstrument == null) {
            instance.sendGroupedPacket(
                ParticlePacket(Particle.NOTE, position.add(0.5, 1.2, 0.5), Vec.ZERO, 1.0f, 1),
            )
        }
    }

    private fun isSilenced(
        instance: Instance,
        position: Point,
    ): Boolean {
        val above = instance.getBlock(position.add(0.0, 1.0, 0.0))
        if (above.air()) return false
        return NoteBlockInstruments.fromHead(above) == null
    }

    private fun sound(instrument: String): SoundEvent =
        when (instrument) {
            "basedrum" -> SoundEvent.BLOCK_NOTE_BLOCK_BASEDRUM
            "snare" -> SoundEvent.BLOCK_NOTE_BLOCK_SNARE
            "hat" -> SoundEvent.BLOCK_NOTE_BLOCK_HAT
            "bass" -> SoundEvent.BLOCK_NOTE_BLOCK_BASS
            "flute" -> SoundEvent.BLOCK_NOTE_BLOCK_FLUTE
            "bell" -> SoundEvent.BLOCK_NOTE_BLOCK_BELL
            "guitar" -> SoundEvent.BLOCK_NOTE_BLOCK_GUITAR
            "chime" -> SoundEvent.BLOCK_NOTE_BLOCK_CHIME
            "xylophone" -> SoundEvent.BLOCK_NOTE_BLOCK_XYLOPHONE
            "iron_xylophone" -> SoundEvent.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE
            "cow_bell" -> SoundEvent.BLOCK_NOTE_BLOCK_COW_BELL
            "didgeridoo" -> SoundEvent.BLOCK_NOTE_BLOCK_DIDGERIDOO
            "bit" -> SoundEvent.BLOCK_NOTE_BLOCK_BIT
            "banjo" -> SoundEvent.BLOCK_NOTE_BLOCK_BANJO
            "pling" -> SoundEvent.BLOCK_NOTE_BLOCK_PLING
            "zombie" -> SoundEvent.BLOCK_NOTE_BLOCK_IMITATE_ZOMBIE
            "skeleton" -> SoundEvent.BLOCK_NOTE_BLOCK_IMITATE_SKELETON
            "creeper" -> SoundEvent.BLOCK_NOTE_BLOCK_IMITATE_CREEPER
            "dragon" -> SoundEvent.BLOCK_NOTE_BLOCK_IMITATE_ENDER_DRAGON
            "wither_skeleton" -> SoundEvent.BLOCK_NOTE_BLOCK_IMITATE_WITHER_SKELETON
            "piglin" -> SoundEvent.BLOCK_NOTE_BLOCK_IMITATE_PIGLIN
            "trumpet" -> SoundEvent.BLOCK_NOTE_BLOCK_TRUMPET
            "trumpet_exposed" -> SoundEvent.BLOCK_NOTE_BLOCK_TRUMPET_EXPOSED
            "trumpet_weathered" -> SoundEvent.BLOCK_NOTE_BLOCK_TRUMPET_WEATHERED
            "trumpet_oxidized" -> SoundEvent.BLOCK_NOTE_BLOCK_TRUMPET_OXIDIZED
            else -> SoundEvent.BLOCK_NOTE_BLOCK_HARP
        }
}
