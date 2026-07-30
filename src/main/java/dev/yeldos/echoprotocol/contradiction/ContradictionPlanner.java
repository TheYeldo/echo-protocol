package dev.yeldos.echoprotocol.contradiction;

import dev.yeldos.echoprotocol.recording.RecordedFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class ContradictionPlanner {
    public Optional<ContradictionPlan> plan(ContradictionVariant variant, List<RecordedFrame> authenticFrames,
                                            Vec3d destination, ItemStack alternativeItem, long seed) {
        if (variant == null || authenticFrames == null || authenticFrames.size() < 8) {
            return Optional.empty();
        }
        int count = Math.min(60, authenticFrames.size());
        List<RecordedFrame> source = List.copyOf(authenticFrames.subList(authenticFrames.size() - count,
                authenticFrames.size()));
        Random random = new Random(seed);
        return Optional.of(switch (variant) {
            case SPLIT_MEMORY -> split(source, seed, random, true);
            case CONFLICTING_COPIES -> split(source, seed, random, false);
            case REPEATED_ENDING -> repeated(source, seed);
            case WRONG_DESTINATION -> wrongDestination(source, destination, seed);
            case MEMORY_ARRIVED_FIRST -> arrivedFirst(source, destination, seed);
            case CONFLICTING_ITEM -> conflictingItem(source, alternativeItem, seed);
            case MISSING_SEGMENT -> missingSegment(source, seed);
        });
    }

    private static ContradictionPlan split(List<RecordedFrame> source, long seed, Random random, boolean paired) {
        int branch = Math.max(4, source.size() / 2);
        List<RecordedFrame> secondary = new ArrayList<>(source.size());
        double directionSign = random.nextBoolean() ? -1.0D : 1.0D;
        for (int index = 0; index < source.size(); index++) {
            RecordedFrame frame = source.get(index);
            if (index < branch) {
                secondary.add(frame);
                continue;
            }
            double progress = (index - branch + 1.0D) / Math.max(1.0D, source.size() - branch);
            double side = directionSign * 2.2D * progress;
            double radians = Math.toRadians(frame.bodyYaw());
            Vec3d offset = new Vec3d(Math.cos(radians) * side, 0.0D, Math.sin(radians) * side);
            secondary.add(copy(frame, frame.pos().add(offset), frame.heldItemVisual()));
        }
        int delay = paired ? 0 : 70;
        return new ContradictionPlan(paired ? ContradictionVariant.SPLIT_MEMORY
                : ContradictionVariant.CONFLICTING_COPIES, source, secondary, source.getLast().pos(), delay,
                paired, !paired, seed);
    }

    private static ContradictionPlan repeated(List<RecordedFrame> source, long seed) {
        int continuationSize = Math.min(18, source.size());
        List<RecordedFrame> tail = source.subList(source.size() - continuationSize, source.size());
        Vec3d offset = source.getFirst().pos().subtract(tail.getFirst().pos());
        List<RecordedFrame> continuation = tail.stream()
                .map(frame -> copy(frame, frame.pos().add(offset), frame.heldItemVisual())).toList();
        return new ContradictionPlan(ContradictionVariant.REPEATED_ENDING, source, continuation,
                continuation.getFirst().pos(), 55, false, false, seed);
    }

    private static ContradictionPlan wrongDestination(List<RecordedFrame> source, Vec3d destination, long seed) {
        int prefixSize = Math.max(4, source.size() / 2);
        List<RecordedFrame> prefix = new ArrayList<>(source.subList(0, prefixSize));
        RecordedFrame base = prefix.getLast();
        Vec3d goal = destination == null ? base.pos().add(2.5D, 0.0D, 0.0D) : destination;
        Vec3d delta = goal.subtract(base.pos());
        Vec3d direction = delta.lengthSquared() < 0.04D ? new Vec3d(1.0D, 0.0D, 0.0D) : delta.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        int steps = Math.min(36, Math.max(12, (int) Math.ceil(Math.sqrt(delta.lengthSquared()) / 0.18D)));
        for (int index = 1; index <= steps; index++) {
            double distance = Math.min(Math.sqrt(delta.lengthSquared()), index * 0.18D);
            prefix.add(copy(base, base.pos().add(direction.multiply(distance)), base.heldItemVisual(), yaw));
        }
        return new ContradictionPlan(ContradictionVariant.WRONG_DESTINATION, prefix, List.of(), goal,
                0, false, false, seed);
    }

    private static ContradictionPlan arrivedFirst(List<RecordedFrame> source, Vec3d destination, long seed) {
        Vec3d position = destination == null ? source.getLast().pos() : destination;
        RecordedFrame base = source.getLast();
        List<RecordedFrame> waiting = new ArrayList<>();
        for (int index = 0; index < 36; index++) {
            waiting.add(copy(base, position, base.heldItemVisual(), base.bodyYaw() + (index > 18 ? 12.0F : 0.0F)));
        }
        return new ContradictionPlan(ContradictionVariant.MEMORY_ARRIVED_FIRST, waiting, List.of(), position,
                0, false, false, seed);
    }

    private static ContradictionPlan conflictingItem(List<RecordedFrame> source, ItemStack alternativeItem, long seed) {
        ItemStack item = alternativeItem == null ? null : alternativeItem.copyWithCount(1);
        List<RecordedFrame> changed = source.stream().map(frame -> copy(frame, frame.pos(), item)).toList();
        return new ContradictionPlan(ContradictionVariant.CONFLICTING_ITEM, changed, List.of(),
                changed.getLast().pos(), 0, false, false, seed);
    }

    private static ContradictionPlan missingSegment(List<RecordedFrame> source, long seed) {
        int third = Math.max(3, source.size() / 3);
        List<RecordedFrame> first = List.copyOf(source.subList(0, third));
        List<RecordedFrame> last = List.copyOf(source.subList(source.size() - third, source.size()));
        return new ContradictionPlan(ContradictionVariant.MISSING_SEGMENT, first, last,
                last.getFirst().pos(), 20, false, true, seed);
    }

    private static RecordedFrame copy(RecordedFrame frame, Vec3d pos, ItemStack item) {
        return copy(frame, pos, item, frame.bodyYaw());
    }

    private static RecordedFrame copy(RecordedFrame frame, Vec3d pos, ItemStack item, float bodyYaw) {
        ItemStack visual = item == null ? null : item.copyWithCount(Math.min(1, item.getCount()));
        return new RecordedFrame(frame.serverTick(), pos.x, pos.y, pos.z, bodyYaw, bodyYaw, frame.pitch(),
                frame.velocityX(), frame.velocityY(), frame.velocityZ(), frame.walking(), frame.sprinting(),
                frame.sneaking(), frame.swimming(), frame.crawling(), frame.jumping(), frame.mainHandSwing(),
                frame.offHandSwing(), frame.selectedHotbarSlot(), visual, frame.onGround());
    }
}
