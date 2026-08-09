package dev.yeldos.echoprotocol.recording;

import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class ReplayFrames {
    private ReplayFrames() {
    }

    public static List<RecordedFrame> translated(List<RecordedFrame> frames, Vec3d destination) {
        if (frames.isEmpty()) {
            return List.of();
        }
        Vec3d offset = destination.subtract(frames.getFirst().pos());
        if (offset.lengthSquared() < 1.0E-12D) {
            return List.copyOf(frames);
        }
        return frames.stream().map(frame -> new RecordedFrame(frame.serverTick(),
                frame.x() + offset.x, frame.y() + offset.y, frame.z() + offset.z,
                frame.bodyYaw(), frame.headYaw(), frame.pitch(), frame.velocityX(), frame.velocityY(), frame.velocityZ(),
                frame.walking(), frame.sprinting(), frame.sneaking(), frame.swimming(), frame.crawling(), frame.jumping(),
                frame.mainHandSwing(), frame.offHandSwing(), frame.selectedHotbarSlot(),
                frame.heldItemVisual() == null ? null
                        : frame.heldItemVisual().copyWithCount(Math.min(1, frame.heldItemVisual().getCount())),
                frame.onGround())).toList();
    }

    public static Vec3d relativePosition(Vec3d recordedPosition, Vec3d recordedOrigin, Vec3d manifestationOrigin) {
        return manifestationOrigin.add(recordedPosition.subtract(recordedOrigin));
    }
}
