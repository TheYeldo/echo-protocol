package dev.yeldos.echoprotocol.util;

import dev.yeldos.echoprotocol.config.EchoConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
            Vec3d candidate = dropToGround(world, target.getPos().add(rotated));
            if (isValid(world, target, candidate, config, true)) {
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
            Vec3d candidate = dropToGround(world, target.getPos().add(rotateY(look, angle).multiply(distance)));
            if (isValid(world, target, candidate, config, true) && !hasClearBlockLine(world, target, target.getEyePos(), candidate.add(0.0D, 1.2D, 0.0D))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static boolean isValid(ServerWorld world, ServerPlayerEntity target, Vec3d pos, EchoConfig config, boolean requireFloor) {
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
        if (toCandidate.lengthSquared() > 0.001D && target.getRotationVec(1.0F).normalize().dotProduct(toCandidate.normalize()) > 0.92D
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
            BlockState floor = world.getBlockState(blockPos.down());
            return !floor.getCollisionShape(world, blockPos.down()).isEmpty();
        }
        return true;
    }

    private static boolean isUnsafe(BlockState state) {
        return state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.POWDER_SNOW)
                || state.isOf(Blocks.NETHER_PORTAL)
                || state.isOf(Blocks.END_PORTAL);
    }

    private static Vec3d dropToGround(ServerWorld world, Vec3d start) {
        BlockPos.Mutable mutable = BlockPos.ofFloored(start).mutableCopy();
        for (int i = 0; i < 5; i++) {
            BlockState below = world.getBlockState(mutable.down());
            if (!below.getCollisionShape(world, mutable.down()).isEmpty()) {
                return Vec3d.ofBottomCenter(mutable);
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
