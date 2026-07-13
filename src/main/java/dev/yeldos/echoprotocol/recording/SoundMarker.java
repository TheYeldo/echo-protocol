package dev.yeldos.echoprotocol.recording;

import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SoundMarker(long serverTick, double x, double y, double z, Identifier soundId, float volume, float pitch) {
    public static SoundMarker of(long tick, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        Identifier id = Registries.SOUND_EVENT.getId(sound);
        return new SoundMarker(tick, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                id == null ? Registries.SOUND_EVENT.getId(SoundEvents.BLOCK_STONE_STEP) : id, volume, pitch);
    }

    public SoundEvent sound() {
        SoundEvent event = Registries.SOUND_EVENT.get(soundId);
        return event == null ? SoundEvents.BLOCK_STONE_STEP : event;
    }
}
