package dev.yeldos.echoprotocol.rendering;

import dev.yeldos.echoprotocol.config.EchoConfig;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class EchoVisualEffects {
    private EchoVisualEffects() {
    }

    public static void corruptedAfterimage(ServerPlayerEntity target, EchoConfig config, Vec3d pos) {
        if (!config.corruptionAfterimagesEnabled() || config.reducedVisualEffects()) {
            return;
        }
        int count = Math.max(1, Math.min(8, (int) (6 * config.corruptionVisualIntensity())));
        target.getServerWorld().spawnParticles(target, ParticleTypes.SCULK_SOUL, true, pos.x, pos.y + 1.0D, pos.z,
                count, 0.12D, 0.45D, 0.12D, 0.004D);
    }

    public static void disappear(ServerPlayerEntity target, EchoConfig config, Vec3d pos) {
        if (config.reducedVisualEffects()) {
            return;
        }
        target.getServerWorld().spawnParticles(target, ParticleTypes.ASH, true, pos.x, pos.y + 0.9D, pos.z,
                8, 0.22D, 0.55D, 0.22D, 0.01D);
    }
}
