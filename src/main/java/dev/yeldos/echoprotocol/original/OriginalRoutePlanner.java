package dev.yeldos.echoprotocol.original;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.interaction.EchoWorldInteraction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public final class OriginalRoutePlanner {
    private static final int MAXIMUM_EXAMINED_NODES = 320;
    private static final int MAXIMUM_LOCAL_RADIUS = 16;
    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private OriginalRoutePlanner() {
    }

    public static Optional<List<Vec3d>> plan(ServerWorld world, EchoEntity echo, Vec3d requestedTarget,
                                             int maximumWaypoints) {
        BlockPos start = BlockPos.ofFloored(echo.getEntityPos());
        Optional<Vec3d> resolved = resolveTarget(world, echo, requestedTarget);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        Vec3d goalPosition = resolved.get();
        BlockPos goal = BlockPos.ofFloored(goalPosition);
        if (start.getSquaredDistance(goal) > MAXIMUM_LOCAL_RADIUS * MAXIMUM_LOCAL_RADIUS) {
            return Optional.empty();
        }
        if (safeSegment(world, echo, echo.getEntityPos(), goalPosition)) {
            return Optional.of(List.of(goalPosition));
        }

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::score));
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Double> cost = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        BlockPos immutableStart = start.toImmutable();
        cost.put(immutableStart, 0.0D);
        open.add(new Node(immutableStart, heuristic(immutableStart, goal)));
        BlockPos reached = null;
        int examined = 0;

        while (!open.isEmpty() && examined++ < MAXIMUM_EXAMINED_NODES) {
            BlockPos current = open.poll().position();
            if (!closed.add(current)) {
                continue;
            }
            if (horizontalSquared(current, goal) <= 1.0D && Math.abs(current.getY() - goal.getY()) <= 1) {
                reached = current;
                break;
            }
            for (BlockPos next : neighbors(world, echo, current, immutableStart)) {
                if (closed.contains(next)) {
                    continue;
                }
                double nextCost = cost.get(current) + movementCost(current, next);
                if (nextCost >= cost.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    continue;
                }
                cameFrom.put(next, current);
                cost.put(next, nextCost);
                open.add(new Node(next, nextCost + heuristic(next, goal)));
            }
        }
        if (reached == null) {
            return Optional.empty();
        }
        List<Vec3d> raw = reconstruct(world, echo, cameFrom, immutableStart, reached);
        List<Vec3d> compressed = compress(world, echo, echo.getEntityPos(), raw, Math.max(1, maximumWaypoints));
        return compressed.isEmpty() ? Optional.empty() : Optional.of(compressed);
    }

    public static Optional<Vec3d> findStandingNear(ServerWorld world, EchoEntity echo, Vec3d center,
                                                   double minimumDistance, double maximumDistance) {
        Vec3d best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        int rings = Math.max(1, (int) Math.ceil(maximumDistance - minimumDistance) + 1);
        for (int ring = 0; ring < rings; ring++) {
            double distance = minimumDistance + (maximumDistance - minimumDistance) * ring / Math.max(1, rings - 1);
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI * 2.0D / 16.0D;
                Vec3d candidate = groundCandidate(center.add(Math.cos(angle) * distance, 0.0D,
                        Math.sin(angle) * distance));
                Optional<Vec3d> standing = resolveTarget(world, echo, candidate);
                if (standing.isEmpty()) {
                    continue;
                }
                Vec3d resolved = standing.get();
                double score = Math.abs(horizontalDistance(resolved, center) - minimumDistance)
                        + horizontalDistance(resolved, echo.getEntityPos()) * 0.02D;
                if (score < bestScore) {
                    best = resolved;
                    bestScore = score;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static Optional<Vec3d> findRoutableStandingNear(ServerWorld world, EchoEntity echo, Vec3d center,
                                                           double minimumDistance, double maximumDistance,
                                                           int maximumWaypoints) {
        int rings = Math.max(2, (int) Math.ceil(maximumDistance - minimumDistance) + 2);
        int routeAttempts = 0;
        for (int ring = 0; ring < rings; ring++) {
            double distance = minimumDistance + (maximumDistance - minimumDistance) * ring / Math.max(1, rings - 1);
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI * 2.0D / 16.0D;
                Vec3d candidate = groundCandidate(center.add(Math.cos(angle) * distance, 0.0D,
                        Math.sin(angle) * distance));
                Optional<Vec3d> standing = resolveTarget(world, echo, candidate);
                if (standing.isEmpty()) {
                    continue;
                }
                Vec3d resolved = standing.get();
                if (horizontalDistance(resolved, echo.getEntityPos()) < 0.8D) {
                    continue;
                }
                if (routeAttempts++ >= 12) {
                    return Optional.empty();
                }
                if (plan(world, echo, resolved, maximumWaypoints).isPresent()) {
                    return Optional.of(resolved);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean safeStep(ServerWorld world, EchoEntity echo, Vec3d from, Vec3d to) {
        return safeTransit(world, echo, to) && safeSegment(world, echo, from, to);
    }

    private static Optional<Vec3d> resolveTarget(ServerWorld world, EchoEntity echo, Vec3d requested) {
        BlockPos center = BlockPos.ofFloored(requested);
        for (int horizontalRadius = 0; horizontalRadius <= 2; horizontalRadius++) {
            for (int y = 1; y >= -1; y--) {
                for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                    for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                        if (horizontalRadius > 0 && Math.max(Math.abs(x), Math.abs(z)) != horizontalRadius) {
                            continue;
                        }
                        BlockPos candidate = center.add(x, y, z);
                        Optional<Vec3d> standing = standingPosition(world, echo, candidate);
                        if (standing.isPresent()) {
                            return standing;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static List<BlockPos> neighbors(ServerWorld world, EchoEntity echo, BlockPos current, BlockPos start) {
        List<BlockPos> result = new ArrayList<>(8);
        Optional<Vec3d> currentPosition = standingPosition(world, echo, current);
        if (currentPosition.isEmpty()) {
            return result;
        }
        for (int[] direction : DIRECTIONS) {
            for (int yOffset : new int[]{0, 1, -1}) {
                Optional<Vec3d> resolved = standingPosition(world, echo,
                        current.add(direction[0], yOffset, direction[1]));
                if (resolved.isEmpty()) {
                    continue;
                }
                BlockPos next = BlockPos.ofFloored(resolved.get()).toImmutable();
                if (horizontalSquared(next, start) > MAXIMUM_LOCAL_RADIUS * MAXIMUM_LOCAL_RADIUS
                        || next.equals(current)) {
                    continue;
                }
                if (safeSegment(world, echo, currentPosition.get(), resolved.get())) {
                    result.add(next.toImmutable());
                    break;
                }
            }
        }
        return result;
    }

    private static Optional<Vec3d> standingPosition(ServerWorld world, EchoEntity echo, BlockPos candidate) {
        double x = candidate.getX() + 0.5D;
        double z = candidate.getZ() + 0.5D;
        for (int offset = 0; offset >= -2; offset--) {
            BlockPos support = new BlockPos(candidate.getX(), candidate.getY() + offset, candidate.getZ());
            if (!world.isChunkLoaded(support)) {
                continue;
            }
            var shape = world.getBlockState(support).getCollisionShape(world, support);
            if (shape.isEmpty()) {
                continue;
            }
            double top = shape.getEndingCoord(Direction.Axis.Y, 0.5D, 0.5D);
            if (!Double.isFinite(top)) {
                continue;
            }
            Vec3d position = new Vec3d(x, support.getY() + top, z);
            if (Math.abs(position.y - candidate.getY()) <= 1.05D && safeStanding(world, echo, position)) {
                return Optional.of(position);
            }
        }
        return Optional.empty();
    }

    private static boolean safeStanding(ServerWorld world, EchoEntity echo, Vec3d position) {
        BlockPos feet = BlockPos.ofFloored(position);
        if (!world.isChunkLoaded(feet)) {
            return false;
        }
        BlockState feetState = world.getBlockState(feet);
        BlockState headState = world.getBlockState(feet.up());
        BlockPos supportPos = BlockPos.ofFloored(position.x, position.y - 1.0E-4D, position.z);
        BlockState supportState = world.getBlockState(supportPos);
        var supportShape = supportState.getCollisionShape(world, supportPos);
        if (unsafe(feetState) || unsafe(headState) || unsafe(supportState) || supportShape.isEmpty()) {
            return false;
        }
        double supportTop = supportShape.getEndingCoord(Direction.Axis.Y,
                position.x - Math.floor(position.x), position.z - Math.floor(position.z));
        if (!Double.isFinite(supportTop)
                || Math.abs(supportPos.getY() + supportTop - position.y) > 1.0E-3D) {
            return false;
        }
        Vec3d offset = position.subtract(echo.getEntityPos());
        Box moved = echo.getBoundingBox().offset(offset);
        return world.isSpaceEmpty(echo, moved)
                || EchoWorldInteraction.collisionContainsOnlyPassages(world, moved);
    }

    private static boolean safeSegment(ServerWorld world, EchoEntity echo, Vec3d from, Vec3d to) {
        double distance = from.distanceTo(to);
        int samples = Math.max(1, (int) Math.ceil(distance / 0.22D));
        for (int i = 1; i <= samples; i++) {
            Vec3d sample = from.lerp(to, i / (double) samples);
            if (!safeTransit(world, echo, sample)) {
                return false;
            }
        }
        return true;
    }

    private static boolean safeTransit(ServerWorld world, EchoEntity echo, Vec3d position) {
        BlockPos feet = BlockPos.ofFloored(position);
        if (!world.isChunkLoaded(feet)) {
            return false;
        }
        BlockState feetState = world.getBlockState(feet);
        BlockState headState = world.getBlockState(feet.up());
        if (!supportedAtOrBelow(world, position, 2.05D) || unsafe(feetState) || unsafe(headState)) {
            return false;
        }
        Vec3d offset = position.subtract(echo.getEntityPos());
        Box moved = echo.getBoundingBox().offset(offset);
        return world.isSpaceEmpty(echo, moved)
                || EchoWorldInteraction.collisionContainsOnlyPassages(world, moved);
    }

    private static boolean supportedAtOrBelow(ServerWorld world, Vec3d position, double maximumDrop) {
        BlockPos topCandidate = BlockPos.ofFloored(position.x, position.y - 1.0E-4D, position.z);
        double localX = position.x - Math.floor(position.x);
        double localZ = position.z - Math.floor(position.z);
        for (int depth = 0; depth <= 2; depth++) {
            BlockPos support = topCandidate.down(depth);
            BlockState state = world.getBlockState(support);
            if (unsafe(state)) {
                return false;
            }
            var shape = state.getCollisionShape(world, support);
            if (shape.isEmpty()) {
                continue;
            }
            double top = shape.getEndingCoord(Direction.Axis.Y, localX, localZ);
            if (!Double.isFinite(top)) {
                continue;
            }
            double drop = position.y - (support.getY() + top);
            return drop >= -1.0E-3D && drop <= maximumDrop;
        }
        return false;
    }

    private static boolean unsafe(BlockState state) {
        return state.isOf(Blocks.LAVA) || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.CACTUS) || state.isOf(Blocks.POWDER_SNOW)
                || state.isOf(Blocks.NETHER_PORTAL) || state.isOf(Blocks.END_PORTAL)
                || state.isOf(Blocks.MAGMA_BLOCK) || state.isOf(Blocks.CAMPFIRE)
                || state.isOf(Blocks.SOUL_CAMPFIRE) || state.isOf(Blocks.SWEET_BERRY_BUSH);
    }

    private static List<Vec3d> reconstruct(ServerWorld world, EchoEntity echo,
                                           Map<BlockPos, BlockPos> cameFrom, BlockPos start, BlockPos reached) {
        LinkedList<Vec3d> path = new LinkedList<>();
        BlockPos cursor = reached;
        while (!cursor.equals(start)) {
            Optional<Vec3d> position = standingPosition(world, echo, cursor);
            if (position.isEmpty()) {
                return List.of();
            }
            path.addFirst(position.get());
            cursor = cameFrom.get(cursor);
            if (cursor == null) {
                return List.of();
            }
        }
        return path;
    }

    private static List<Vec3d> compress(ServerWorld world, EchoEntity echo, Vec3d start, List<Vec3d> raw,
                                        int maximumWaypoints) {
        List<Vec3d> result = new ArrayList<>();
        Vec3d cursor = start;
        int index = 0;
        while (index < raw.size() && result.size() < maximumWaypoints) {
            int farthest = index;
            for (int candidate = raw.size() - 1; candidate >= index; candidate--) {
                if (safeSegment(world, echo, cursor, raw.get(candidate))) {
                    farthest = candidate;
                    break;
                }
            }
            Vec3d waypoint = raw.get(farthest);
            result.add(waypoint);
            cursor = waypoint;
            index = farthest + 1;
        }
        return index >= raw.size() ? result : List.of();
    }

    private static Vec3d groundCandidate(Vec3d value) {
        return new Vec3d(value.x, Math.floor(value.y), value.z);
    }

    private static double horizontalSquared(BlockPos left, BlockPos right) {
        double x = left.getX() - right.getX();
        double z = left.getZ() - right.getZ();
        return x * x + z * z;
    }

    private static double horizontalDistance(Vec3d left, Vec3d right) {
        double x = left.x - right.x;
        double z = left.z - right.z;
        return Math.sqrt(x * x + z * z);
    }

    private static double movementCost(BlockPos from, BlockPos to) {
        double diagonal = from.getX() != to.getX() && from.getZ() != to.getZ() ? 1.414D : 1.0D;
        return diagonal + Math.abs(from.getY() - to.getY()) * 0.35D;
    }

    private static double heuristic(BlockPos from, BlockPos goal) {
        return Math.sqrt(horizontalSquared(from, goal)) + Math.abs(from.getY() - goal.getY()) * 0.5D;
    }

    private record Node(BlockPos position, double score) {
    }
}
