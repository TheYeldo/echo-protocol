package dev.yeldos.echoprotocol.util;

import dev.yeldos.echoprotocol.config.EchoConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class SafeEchoPositionFinder {
    private static final double HALF_WIDTH = 0.32D;
    private static final double HEIGHT = 1.8D;

    private SafeEchoPositionFinder() {
    }

    public static Optional<Vec3d> findSpawn(ServerWorld world, ServerPlayerEntity target, Vec3d preferred, EchoConfig config) {
        if (isValid(world, target, preferred, config, true)) {
            return Optional.of(preferred);
        }
        int attempts = Math.max(1, config.safeSpawnAttempts());
        Vec3d look = target.getRotationVec(1.0F).normalize();
        for (int i = 0; i < attempts; i++) {
            double distance = MathHelper.lerp(i / (double) Math.max(1, attempts - 1), config.minimumEchoSpawnDistance(), config.maximumEchoSpawnDistance());
            double angle = Math.PI + ThreadLocalRandom.current().nextDouble(-1.25D, 1.25D);
            Vec3d rotated = rotateY(look, angle).multiply(distance);
            Vec3d candidate = dropToGround(world, target.getEntityPos().add(rotated));
            if (isValid(world, target, candidate, config, true)
                    && ViewAngle.outsideCentralView(look, candidate.add(0.0D, 1.0D, 0.0D)
                    .subtract(target.getEyePos()), 0.65D)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static Optional<Vec3d> findHidden(ServerWorld world, ServerPlayerEntity target, Vec3d origin, EchoConfig config) {
        int attempts = Math.max(1, config.safeSpawnAttempts());
        Vec3d look = target.getRotationVec(1.0F).normalize();
        for (int i = 0; i < attempts; i++) {
            double angle = Math.PI + ThreadLocalRandom.current().nextDouble(-1.8D, 1.8D);
            double distance = ThreadLocalRandom.current().nextDouble(config.minimumEchoSpawnDistance(), Math.max(config.minimumEchoSpawnDistance() + 1, config.maximumEchoSpawnDistance()));
            Vec3d candidate = dropToGround(world, target.getEntityPos().add(rotateY(look, angle).multiply(distance)));
            if (isValid(world, target, candidate, config, true) && !hasClearBlockLine(world, target, target.getEyePos(), candidate.add(0.0D, 1.2D, 0.0D))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static Optional<Vec3d> findPeripheral(ServerWorld world, ServerPlayerEntity target, EchoConfig config) {
        int attempts = Math.max(4, config.safeSpawnAttempts());
        Vec3d look = target.getRotationVec(1.0F).normalize();
        for (int i = 0; i < attempts; i++) {
            double side = ThreadLocalRandom.current().nextBoolean() ? 1.0D : -1.0D;
            double angle = side * ThreadLocalRandom.current().nextDouble(1.05D, 1.75D);
            double distance = ThreadLocalRandom.current().nextDouble(config.minimumEchoSpawnDistance(),
                    Math.max(config.minimumEchoSpawnDistance() + 1.0D, config.maximumEchoSpawnDistance()));
            Vec3d candidate = dropToGround(world, target.getEntityPos().add(rotateY(look, angle).multiply(distance)));
            if (isValid(world, target, candidate, config, true)
                    && ViewAngle.outsideCentralView(look, candidate.add(0.0D, 1.0D, 0.0D)
                    .subtract(target.getEyePos()), 0.65D)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static Optional<Vec3d> findOriginalStart(ServerWorld world, ServerPlayerEntity target, Vec3d anchor,
                                                    EchoConfig config) {
        int attempts = Math.max(12, Math.min(32, config.safeSpawnAttempts() * 2));
        double minimum = Math.max(5.0D, config.originalMinimumMovementDistance());
        double maximum = Math.min(12.0D, config.originalMaximumMovementDistance());
        maximum = Math.max(minimum, maximum);
        for (int i = 0; i < attempts; i++) {
            double angle = i * Math.PI * 2.0D / attempts + (target.getUuid().hashCode() & 7) * 0.11D;
            double distance = MathHelper.lerp((i % 5) / 4.0D, minimum, maximum);
            Vec3d candidate = dropToGround(world, anchor.add(Math.cos(angle) * distance, 0.0D,
                    Math.sin(angle) * distance));
            if (isValid(world, target, candidate, config, true)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * Administrative event commands should demonstrate the behavior, not hide the initial reveal. This finder keeps
     * the spawn in a clear forward cone while retaining the normal floor, distance, chunk and hazard checks.
     */
    public static Optional<Vec3d> findVisibleOriginalStart(ServerWorld world, ServerPlayerEntity target,
                                                           EchoConfig config) {
        Vec3d look = target.getRotationVec(1.0F).multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSquared() < 0.01D) {
            look = new Vec3d(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        double minimum = Math.max(3.5D, config.minimumEchoSpawnDistance());
        double maximum = Math.min(8.0D, config.maximumEchoSpawnDistance());
        maximum = Math.max(minimum, maximum);
        double[] angles = {0.0D, -0.28D, 0.28D, -0.52D, 0.52D, -0.78D, 0.78D};
        for (int ring = 0; ring < 3; ring++) {
            double distance = MathHelper.lerp(ring / 2.0D, minimum, maximum);
            for (double angle : angles) {
                Vec3d candidate = dropToGround(world, target.getEntityPos().add(rotateY(look, angle).multiply(distance)));
                if (isValid(world, target, candidate, config, true, false)
                        && hasClearBlockLine(world, target, target.getEyePos(), candidate.add(0.0D, 1.25D, 0.0D))) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isValid(ServerWorld world, ServerPlayerEntity target, Vec3d pos, EchoConfig config, boolean requireFloor) {
        return isValid(world, target, pos, config, requireFloor, true);
    }

    private static boolean isValid(ServerWorld world, ServerPlayerEntity target, Vec3d pos, EchoConfig config,
                                   boolean requireFloor, boolean avoidCentralView) {
        BlockPos blockPos = BlockPos.ofFloored(pos);
        if (!world.isChunkLoaded(blockPos)) {
            return false;
        }
        double distanceSq = target.squaredDistanceTo(pos);
        if (distanceSq < config.minimumEchoSpawnDistance() * config.minimumEchoSpawnDistance()
                || distanceSq > config.maximumEchoSpawnDistance() * config.maximumEchoSpawnDistance()) {
            return false;
        }
        Vec3d toCandidate = pos.add(0.0D, 1.0D, 0.0D).subtract(target.getEyePos());
        if (avoidCentralView && toCandidate.lengthSquared() > 0.001D
                && target.getRotationVec(1.0F).normalize().dotProduct(toCandidate.normalize()) > 0.92D
                && hasClearBlockLine(world, target, target.getEyePos(), pos.add(0.0D, 1.0D, 0.0D))) {
            return false;
        }
        Box box = new Box(pos.x - HALF_WIDTH, pos.y, pos.z - HALF_WIDTH, pos.x + HALF_WIDTH, pos.y + HEIGHT, pos.z + HALF_WIDTH);
        if (!world.isSpaceEmpty(box) || box.intersects(target.getBoundingBox())) {
            return false;
        }
        BlockState body = world.getBlockState(blockPos);
        BlockState head = world.getBlockState(blockPos.up());
        if (isUnsafe(body) || isUnsafe(head)) {
            return false;
        }
        if (requireFloor) {
            return hasSupportingSurface(world, pos);
        }
        return true;
    }

    private static boolean hasSupportingSurface(ServerWorld world, Vec3d pos) {
        BlockPos supportPos = BlockPos.ofFloored(pos.x, pos.y - 1.0E-4D, pos.z);
        var shape = world.getBlockState(supportPos).getCollisionShape(world, supportPos);
        if (shape.isEmpty()) {
            return false;
        }
        double localX = pos.x - Math.floor(pos.x);
        double localZ = pos.z - Math.floor(pos.z);
        double top = shape.getEndingCoord(Direction.Axis.Y, localX, localZ);
        return Double.isFinite(top) && Math.abs(supportPos.getY() + top - pos.y) <= 1.0E-3D;
    }

    private static boolean isUnsafe(BlockState state) {
        return state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.POWDER_SNOW)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.CAMPFIRE)
                || state.isOf(Blocks.SOUL_CAMPFIRE)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.NETHER_PORTAL)
                || state.isOf(Blocks.END_PORTAL);
    }

    private static Vec3d dropToGround(ServerWorld world, Vec3d start) {
        BlockPos.Mutable mutable = BlockPos.ofFloored(start).mutableCopy();
        for (int i = 0; i < 5; i++) {
            BlockPos floorPos = mutable.down().toImmutable();
            BlockState below = world.getBlockState(floorPos);
            var shape = below.getCollisionShape(world, floorPos);
            if (!shape.isEmpty()) {
                double top = shape.getEndingCoord(Direction.Axis.Y, 0.5D, 0.5D);
                if (Double.isFinite(top)) {
                    return new Vec3d(mutable.getX() + 0.5D, floorPos.getY() + top,
                            mutable.getZ() + 0.5D);
                }
            }
            mutable.move(0, -1, 0);
        }
        return start;
    }

    private static Vec3d rotateY(Vec3d vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(vec.x * cos - vec.z * sin, 0.0D, vec.x * sin + vec.z * cos).normalize();
    }

    private static boolean hasClearBlockLine(ServerWorld world, ServerPlayerEntity target, Vec3d from, Vec3d to) {
        return world.raycast(new net.minecraft.world.RaycastContext(from, to,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                target)).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }
}
