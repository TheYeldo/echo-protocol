package dev.yeldos.echoprotocol.audio;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record AudioResidue(
        Identifier soundId,
        AudioResidueEvent event,
        String dimension,
        BlockPos position,
        float volume,
        float pitch,
        long capturedTick
) {
    public AudioResidue {
        position = position.toImmutable();
        volume = Math.max(0.05F, Math.min(volume, 0.6F));
        pitch = Math.max(0.5F, Math.min(pitch, 1.5F));
    }
}
