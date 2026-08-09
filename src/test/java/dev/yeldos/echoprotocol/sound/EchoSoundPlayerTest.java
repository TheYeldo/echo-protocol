package dev.yeldos.echoprotocol.sound;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoSoundPlayerTest {
    @Test
    void distantSpawnCueKeepsDirectionButStaysAudible() {
        Vec3d listener = new Vec3d(2.0D, 64.0D, -3.0D);
        Vec3d distant = listener.add(24.0D, 0.0D, 0.0D);

        Vec3d audible = EchoSoundPlayer.audiblePosition(listener, distant);

        assertEquals(12.0D, audible.distanceTo(listener), 1.0E-6D);
        assertEquals(listener.add(12.0D, 0.0D, 0.0D), audible);
    }

    @Test
    void nearbyCueRetainsItsRealSource() {
        Vec3d listener = Vec3d.ZERO;
        Vec3d nearby = new Vec3d(4.0D, -2.0D, 5.0D);
        assertEquals(nearby, EchoSoundPlayer.audiblePosition(listener, nearby));
    }
}
