package dev.yeldos.echoprotocol.falsememory;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.panic.PanicImprint;
import dev.yeldos.echoprotocol.recording.PlayerRecording;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.recording.ReplaySegmentSelector;
import dev.yeldos.echoprotocol.stage.FamiliarLocation;
import dev.yeldos.echoprotocol.stage.StageManager;
import dev.yeldos.echoprotocol.util.SafeEchoPositionFinder;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class FalseMemoryDirector {
    private final StageManager stageManager;
    private final FalseMemoryHistory history = new FalseMemoryHistory();

    public FalseMemoryDirector(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    public Optional<FalseMemoryPlan> createPlan(ServerPlayerEntity target, PlayerRecording recording,
                                                PanicImprint panic, EchoConfig config, long eventTick,
                                                boolean forceDeviation) {
        List<RecordedFrame> source = panic == null
                ? (recording == null ? List.of() : recording.frames())
                : panic.frames();
        int minimumFrames = Math.max(2, config.falseMemoryMinimumRealPrefixSeconds() * 20
                / config.recordingSampleIntervalTicks());
        if (source.size() < minimumFrames) {
            return Optional.empty();
        }
        long lastFrameTick = source.get(source.size() - 1).serverTick();
        long seed = target.getUuid().getMostSignificantBits() ^ target.getUuid().getLeastSignificantBits()
                ^ lastFrameTick ^ eventTick;
        Random random = new Random(seed);
        float accuracy = config.falseMemoryMinimumAccuracy()
                + random.nextFloat() * (1.0F - config.falseMemoryMinimumAccuracy());
        int prefixSeconds = config.falseMemoryMinimumRealPrefixSeconds()
                + Math.round((config.falseMemoryMaximumRealPrefixSeconds()
                - config.falseMemoryMinimumRealPrefixSeconds()) * accuracy);
        int wantedFrames = Math.min(source.size(), Math.max(minimumFrames,
                prefixSeconds * 20 / config.recordingSampleIntervalTicks()));
        List<RecordedFrame> authentic = ReplaySegmentSelector.select(source, wantedFrames, wantedFrames, random);
        if (authentic.isEmpty()) {
            return Optional.empty();
        }

        Optional<Vec3d> spawn = SafeEchoPositionFinder.findSpawn(target.getEntityWorld(), target,
                authentic.get(0).pos(), config);
        if (spawn.isEmpty()) {
            return Optional.empty();
        }
        Vec3d offset = spawn.get().subtract(authentic.get(0).pos());
        List<RecordedFrame> prefix = authentic.stream()
                .map(frame -> copy(frame, frame.pos().add(offset), frame.bodyYaw(), frame.headYaw(), frame.pitch(),
                        frame.sneaking(), frame.mainHandSwing(), frame.heldItemVisual()))
                .toList();

        int deviationCount = forceDeviation ? config.falseMemoryMaximumDeviations()
                : 1 + random.nextInt(config.falseMemoryMaximumDeviations());
        List<FalseMemoryDeviation> pool = new ArrayList<>(List.of(FalseMemoryDeviation.values()));
        Collections.shuffle(pool, random);
        List<FalseMemoryDeviation> deviations = new ArrayList<>(pool.subList(0, Math.min(deviationCount, pool.size())));
        if (panic != null && !deviations.contains(FalseMemoryDeviation.REVERSE_ROUTE)) {
            deviations.set(0, FalseMemoryDeviation.REVERSE_ROUTE);
        }
        List<RecordedFrame> fabricated = fabricate(target, prefix, recording, deviations, random, config);
        if (!FalseMemoryPlan.hasObservableDeviation(prefix.get(prefix.size() - 1), fabricated)) {
            deviations = List.of(FalseMemoryDeviation.HEAD_SHAKE);
            fabricated = fabricate(target, prefix, recording, deviations, random, config);
        }
        RecordedFrame originalFirst = authentic.get(0);
        RecordedFrame originalLast = authentic.get(authentic.size() - 1);
        String signature = target.getEntityWorld().getRegistryKey().getValue() + ":"
                + MathHelper.floor(originalFirst.x() / 4.0D) + ":" + MathHelper.floor(originalFirst.z() / 4.0D)
                + ":" + MathHelper.floor(originalLast.x() / 4.0D) + ":" + MathHelper.floor(originalLast.z() / 4.0D);
        boolean entersUnauthentic = deviations.contains(FalseMemoryDeviation.APPROACH_FAMILIAR_PLACE)
                || deviations.contains(FalseMemoryDeviation.CONTINUE_BEYOND_RECORDING);
        return Optional.of(new FalseMemoryPlan(prefix, prefix.size() - 1, fabricated, deviations, seed,
                signature, panic != null, entersUnauthentic));
    }

    public FalseMemoryHistory history() {
        return history;
    }

    public void clear() {
        history.clearAll();
    }

    public void clear(java.util.UUID playerUuid) {
        history.clear(playerUuid);
    }

    private List<RecordedFrame> fabricate(ServerPlayerEntity target, List<RecordedFrame> prefix,
                                           PlayerRecording recording, List<FalseMemoryDeviation> deviations,
                                           Random random, EchoConfig config) {
        List<RecordedFrame> result = new ArrayList<>();
        RecordedFrame branch = prefix.get(prefix.size() - 1);
        for (FalseMemoryDeviation deviation : deviations) {
            RecordedFrame base = result.isEmpty() ? branch : result.get(result.size() - 1);
            switch (deviation) {
                case CONTINUE_BEYOND_RECORDING -> continueForward(target.getEntityWorld(), base, result, 24);
                case REPEAT_MOVEMENT -> repeatMovement(prefix, result);
                case REVERSE_ROUTE -> reverseRoute(prefix, result);
                case CROUCH_AT_WRONG_PLACE -> holdPose(base, result, 18, true, false, base.heldItemVisual(), base.headYaw());
                case LOOK_AT_PLAYER -> lookAtPlayer(target, base, result);
                case HOLD_DIFFERENT_ITEM -> holdPose(base, result, 18, base.sneaking(), false,
                        chooseDifferentItem(recording, base.heldItemVisual()), base.headYaw());
                case HEAD_SHAKE -> shakeHead(base, result);
                case SWING_AT_EMPTY_SPACE -> holdPose(base, result, 16, base.sneaking(), true,
                        base.heldItemVisual(), base.headYaw());
                case APPROACH_FAMILIAR_PLACE -> approachFamiliar(target, base, result, config);
                case VANISH_WHEN_OBSERVED -> holdPose(base, result, 20, base.sneaking(), false,
                        base.heldItemVisual(), base.headYaw());
            }
            if (result.size() >= 150) {
                break;
            }
        }
        return result;
    }

    private static void continueForward(ServerWorld world, RecordedFrame base, List<RecordedFrame> out, int count) {
        double radians = Math.toRadians(base.bodyYaw());
        Vec3d direction = new Vec3d(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vec3d pos = base.pos();
        for (int i = 0; i < count; i++) {
            Vec3d next = pos.add(direction.multiply(0.12D));
            if (!isSafe(world, next)) {
                break;
            }
            out.add(copy(base, next, base.bodyYaw(), base.headYaw(), base.pitch(), false, false, base.heldItemVisual()));
            pos = next;
        }
    }

    private static void repeatMovement(List<RecordedFrame> prefix, List<RecordedFrame> out) {
        int count = Math.min(10, prefix.size());
        List<RecordedFrame> tail = prefix.subList(prefix.size() - count, prefix.size());
        Vec3d anchor = out.isEmpty() ? prefix.get(prefix.size() - 1).pos() : out.get(out.size() - 1).pos();
        Vec3d sourceAnchor = tail.get(0).pos();
        for (int repeat = 0; repeat < 2; repeat++) {
            for (RecordedFrame frame : tail) {
                Vec3d translated = anchor.add(frame.pos().subtract(sourceAnchor));
                out.add(copy(frame, translated, frame.bodyYaw(), frame.headYaw(), frame.pitch(),
                        frame.sneaking(), frame.mainHandSwing(), frame.heldItemVisual()));
            }
        }
    }

    private static void reverseRoute(List<RecordedFrame> prefix, List<RecordedFrame> out) {
        int start = Math.max(0, prefix.size() - 24);
        for (int i = prefix.size() - 1; i >= start; i--) {
            out.add(prefix.get(i));
        }
    }

    private static void lookAtPlayer(ServerPlayerEntity target, RecordedFrame base, List<RecordedFrame> out) {
        Vec3d delta = target.getEyePos().subtract(base.pos().add(0.0D, 1.62D, 0.0D));
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        holdPose(base, out, 18, base.sneaking(), false, base.heldItemVisual(), yaw, pitch);
    }

    private void approachFamiliar(ServerPlayerEntity target, RecordedFrame base, List<RecordedFrame> out, EchoConfig config) {
        FamiliarLocation location = stageManager.familiarLocations(target).stream()
                .filter(value -> value.dimension().equals(target.getEntityWorld().getRegistryKey().getValue().toString()))
                .filter(value -> target.getEntityWorld().isChunkLoaded(value.pos()))
                .filter(value -> value.pos().getSquaredDistance(BlockPos.ofFloored(base.pos())) <= 32.0D * 32.0D)
                .findFirst().orElse(null);
        if (location == null) {
            continueForward(target.getEntityWorld(), base, out, 20);
            return;
        }
        Vec3d direction = location.pos().toCenterPos().subtract(base.pos());
        if (direction.lengthSquared() < 0.25D) {
            holdPose(base, out, 16, base.sneaking(), false, base.heldItemVisual(), base.headYaw());
            return;
        }
        direction = direction.normalize();
        Vec3d pos = base.pos();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        for (int i = 0; i < 28; i++) {
            Vec3d next = pos.add(direction.multiply(0.11D));
            if (!isSafe(target.getEntityWorld(), next)) {
                break;
            }
            out.add(copy(base, next, yaw, yaw, 0.0F, false, false, base.heldItemVisual()));
            pos = next;
        }
    }

    private static void shakeHead(RecordedFrame base, List<RecordedFrame> out) {
        for (int i = 0; i < 20; i++) {
            float headYaw = base.headYaw() + (float) Math.sin(i * 0.8D) * 35.0F;
            out.add(copy(base, base.pos(), base.bodyYaw(), headYaw, base.pitch(), base.sneaking(), false,
                    base.heldItemVisual()));
        }
    }

    private static void holdPose(RecordedFrame base, List<RecordedFrame> out, int count, boolean sneaking,
                                 boolean swing, ItemStack item, float headYaw) {
        holdPose(base, out, count, sneaking, swing, item, headYaw, base.pitch());
    }

    private static void holdPose(RecordedFrame base, List<RecordedFrame> out, int count, boolean sneaking,
                                 boolean swing, ItemStack item, float headYaw, float pitch) {
        for (int i = 0; i < count; i++) {
            out.add(copy(base, base.pos(), base.bodyYaw(), headYaw, pitch, sneaking, swing && i % 7 == 0, item));
        }
    }

    private static ItemStack chooseDifferentItem(PlayerRecording recording, ItemStack current) {
        if (recording != null) {
            for (RecordedFrame frame : recording.frames().reversed()) {
                ItemStack candidate = frame.heldItemVisual();
                if (!candidate.isEmpty() && !ItemStack.areItemsEqual(candidate, current)) {
                    return candidate.copyWithCount(1);
                }
            }
        }
        return current;
    }

    private static boolean isSafe(ServerWorld world, Vec3d pos) {
        BlockPos block = BlockPos.ofFloored(pos);
        if (!world.isChunkLoaded(block)) {
            return false;
        }
        Box box = new Box(pos.x - 0.32D, pos.y, pos.z - 0.32D, pos.x + 0.32D, pos.y + 1.8D, pos.z + 0.32D);
        return world.isSpaceEmpty(box)
                && !world.getBlockState(block).isOf(Blocks.LAVA)
                && !world.getBlockState(block).isOf(Blocks.FIRE)
                && !world.getBlockState(block.down()).getCollisionShape(world, block.down()).isEmpty();
    }

    private static RecordedFrame copy(RecordedFrame frame, Vec3d pos, float bodyYaw, float headYaw, float pitch,
                                      boolean sneaking, boolean mainSwing, ItemStack item) {
        return new RecordedFrame(frame.serverTick(), pos.x, pos.y, pos.z, bodyYaw, headYaw, pitch,
                frame.velocityX(), frame.velocityY(), frame.velocityZ(), frame.walking(), frame.sprinting(),
                sneaking, frame.swimming(), frame.crawling(), frame.jumping(), mainSwing, frame.offHandSwing(),
                frame.selectedHotbarSlot(), item.copyWithCount(Math.min(1, item.getCount())), frame.onGround());
    }
}
