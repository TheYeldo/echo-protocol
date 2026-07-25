package dev.yeldos.echoprotocol.panic;

import dev.yeldos.echoprotocol.recording.RecordedFrame;
import net.minecraft.item.ItemStack;

import java.util.List;

public record PanicImprint(
        List<RecordedFrame> frames,
        String dimension,
        HealthCategory healthCategory,
        PanicTriggerType triggerType,
        ItemStack heldItem,
        long capturedTick
) {
    public enum HealthCategory {
        CRITICAL,
        LOW,
        STABLE
    }

    public PanicImprint {
        frames = List.copyOf(frames.subList(0, Math.min(frames.size(), 120)));
        heldItem = heldItem.copyWithCount(Math.min(1, heldItem.getCount()));
    }
}
