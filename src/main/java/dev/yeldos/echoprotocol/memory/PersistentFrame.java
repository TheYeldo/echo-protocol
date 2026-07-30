package dev.yeldos.echoprotocol.memory;

import dev.yeldos.echoprotocol.recording.RecordedFrame;
import net.minecraft.item.ItemStack;

public record PersistentFrame(
        long tickOffset,
        double x,
        double y,
        double z,
        float bodyYaw,
        float headYaw,
        float pitch,
        boolean walking,
        boolean sprinting,
        boolean sneaking,
        boolean swimming,
        boolean crawling,
        boolean jumping,
        boolean onGround
) {
    public PersistentFrame {
        tickOffset = Math.max(0L, tickOffset);
        x = finite(x);
        y = finite(y);
        z = finite(z);
        bodyYaw = finite(bodyYaw);
        headYaw = finite(headYaw);
        pitch = finite(pitch);
    }

    public static PersistentFrame from(RecordedFrame frame, long firstTick) {
        return new PersistentFrame(Math.max(0L, frame.serverTick() - firstTick), frame.x(), frame.y(), frame.z(),
                frame.bodyYaw(), frame.headYaw(), frame.pitch(), frame.walking(), frame.sprinting(), frame.sneaking(),
                frame.swimming(), frame.crawling(), frame.jumping(), frame.onGround());
    }

    public RecordedFrame toRecordedFrame(long baseTick) {
        return new RecordedFrame(baseTick + tickOffset, x, y, z, bodyYaw, headYaw, pitch,
                0.0D, 0.0D, 0.0D, walking, sprinting, sneaking, swimming, crawling, jumping,
                false, false, 0, ItemStack.EMPTY, onGround);
    }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
    private static float finite(float value) { return Float.isFinite(value) ? value : 0.0F; }
}
