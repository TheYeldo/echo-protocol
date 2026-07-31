package dev.yeldos.echoprotocol.original;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.OriginalEventKind;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class OriginalActionPlanFactory {
    private OriginalActionPlanFactory() {
    }

    public static OriginalActionPlan create(OriginalEventKind eventKind, OriginalMovementMode testMode,
                                            ServerWorld world, EchoEntity echo, ServerPlayerEntity player,
                                            Vec3d anchor, List<Vec3d> knownLocations, EchoConfig config) {
        Vec3d interaction = OriginalRoutePlanner.findRoutableStandingNear(world, echo, anchor, 1.0D, 2.4D,
                        config.originalMaximumWaypoints())
                .orElse(anchor);
        Vec3d destination = meaningfulDestination(world, echo, player, anchor, knownLocations, config);
        Vec3d exit = exitDestination(world, echo, player, anchor, config);
        if (testMode != null) {
            return movementTest(testMode, interaction, destination, exit, player);
        }
        return switch (eventKind) {
            case OCCUPIED_PLACE -> occupiedPlace(interaction, exit);
            case ALREADY_HOME -> alreadyHome(destination, exit);
            case YOUR_BED -> yourBed(interaction, exit, player);
            case WRONG_OWNER -> wrongOwner(interaction, exit, player);
            case EARLIER_THAN_YOU -> earlierThanYou(destination, exit);
            case FAMILIAR_ITEM -> familiarItem(interaction, exit);
            case WAITING -> waiting(destination, exit);
            case EMPTY_ROOM -> emptyRoom(interaction, destination);
            case CONFRONTATION -> confrontation(exit);
        };
    }

    private static OriginalActionPlan occupiedPlace(Vec3d interaction, Vec3d exit) {
        return plan(
                rotate(interaction),
                move(OriginalAction.WALK, interaction, 180),
                timed(OriginalAction.LOOK_AT_ANCHOR, interaction, 20, 36, OriginalPauseReason.EXAMINING),
                pause(45, 90, OriginalPauseReason.CLAIMING_SPACE, true),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 24, 42, OriginalPauseReason.WATCHING),
                move(OriginalAction.WALK, exit, 180),
                waitUnobserved(20, 65),
                disappear());
    }

    private static OriginalActionPlan alreadyHome(Vec3d destination, Vec3d exit) {
        return plan(
                waitObserved(12, 120),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 20, 34, OriginalPauseReason.WATCHING),
                move(OriginalAction.FAST_WALK, destination, 180),
                timed(OriginalAction.PLACE_BLOCK, destination, 8, 14, OriginalPauseReason.EXAMINING),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 10, 18, OriginalPauseReason.WATCHING),
                move(OriginalAction.FAST_WALK, exit, 180),
                waitUnobserved(15, 60),
                disappear());
    }

    private static OriginalActionPlan yourBed(Vec3d bedSide, Vec3d exit, ServerPlayerEntity player) {
        Vec3d crossing = midpoint(player.getPos(), bedSide);
        return plan(
                rotate(bedSide),
                move(OriginalAction.FAST_WALK, bedSide, 180),
                timed(OriginalAction.LOOK_AT_ANCHOR, bedSide, 18, 30, OriginalPauseReason.EXAMINING),
                timed(OriginalAction.PLACE_BLOCK, bedSide, 8, 14, OriginalPauseReason.EXAMINING),
                timed(OriginalAction.CROUCH, bedSide, 35, 65, OriginalPauseReason.CLAIMING_SPACE),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 28, 50, OriginalPauseReason.WATCHING),
                move(OriginalAction.APPROACH, crossing, 120),
                move(OriginalAction.FAST_WALK, exit, 180),
                waitUnobserved(12, 50),
                disappear());
    }

    private static OriginalActionPlan wrongOwner(Vec3d storage, Vec3d exit, ServerPlayerEntity player) {
        return plan(
                move(OriginalAction.WALK, storage, 180),
                timed(OriginalAction.LOOK_AT_ANCHOR, storage, 18, 30, OriginalPauseReason.EXAMINING),
                timed(OriginalAction.CHANGE_ITEM, storage, 8, 12, OriginalPauseReason.EXAMINING),
                timed(OriginalAction.SWING_HAND, storage, 8, 14, OriginalPauseReason.EXAMINING),
                pause(25, 45, OriginalPauseReason.CLAIMING_SPACE, false),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 20, 34, OriginalPauseReason.WATCHING),
                move(OriginalAction.APPROACH, player.getPos(), 80),
                move(OriginalAction.WALK, exit, 180),
                disappear());
    }

    private static OriginalActionPlan earlierThanYou(Vec3d destination, Vec3d exit) {
        return plan(
                timed(OriginalAction.LOOK_AT_ANCHOR, destination, 18, 35, OriginalPauseReason.WAITING_TO_BE_NOTICED),
                waitObserved(15, 120),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 12, 24, OriginalPauseReason.WATCHING),
                move(OriginalAction.FAST_WALK, destination, 180),
                move(OriginalAction.WALK, exit, 180),
                waitUnobserved(12, 50),
                disappear());
    }

    private static OriginalActionPlan familiarItem(Vec3d relatedLocation, Vec3d exit) {
        return plan(
                move(OriginalAction.WALK, relatedLocation, 180),
                timed(OriginalAction.LOOK_AT_ANCHOR, relatedLocation, 22, 38, OriginalPauseReason.EXAMINING),
                timed(OriginalAction.SWING_HAND, relatedLocation, 8, 14, OriginalPauseReason.EXAMINING),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 24, 40, OriginalPauseReason.WATCHING),
                timed(OriginalAction.CHANGE_ITEM, null, 8, 12, OriginalPauseReason.EXAMINING),
                move(OriginalAction.WALK, exit, 180),
                disappear());
    }

    private static OriginalActionPlan waiting(Vec3d crossing, Vec3d exit) {
        return plan(
                waitObserved(12, 120),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 14, 26, OriginalPauseReason.WATCHING),
                move(OriginalAction.WALK, crossing, 180),
                timed(OriginalAction.PLACE_BLOCK, crossing, 8, 14, OriginalPauseReason.EXAMINING),
                pause(28, 50, OriginalPauseReason.BLOCKING_ROUTE, true),
                move(OriginalAction.FAST_WALK, exit, 160),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 12, 22, OriginalPauseReason.WATCHING),
                disappear());
    }

    private static OriginalActionPlan emptyRoom(Vec3d doorway, Vec3d roomInterior) {
        return plan(
                rotate(doorway),
                move(OriginalAction.WALK, doorway, 150),
                timed(OriginalAction.SWING_HAND, doorway, 8, 14, OriginalPauseReason.EXAMINING),
                move(OriginalAction.FAST_WALK, roomInterior, 180),
                waitUnobserved(8, 80),
                disappear());
    }

    private static OriginalActionPlan confrontation(Vec3d exit) {
        return plan(
                timed(OriginalAction.LOOK_AT_PLAYER, null, 18, 24, OriginalPauseReason.CONFRONTING),
                move(OriginalAction.APPROACH, null, 180),
                timed(OriginalAction.SWING_HAND, null, 8, 14, OriginalPauseReason.CONFRONTING),
                pause(45, 85, OriginalPauseReason.CONFRONTING, true),
                move(OriginalAction.FAST_WALK, exit, 180),
                timed(OriginalAction.LOOK_AT_PLAYER, null, 12, 20, OriginalPauseReason.WATCHING),
                waitUnobserved(15, 60),
                disappear());
    }

    private static OriginalActionPlan movementTest(OriginalMovementMode mode, Vec3d anchor, Vec3d destination,
                                                   Vec3d exit, ServerPlayerEntity player) {
        return switch (mode) {
            case WALK -> plan(move(OriginalAction.WALK, destination, 200), pause(20, 20,
                    OriginalPauseReason.LISTENING, false), disappear());
            case FAST_WALK -> plan(move(OriginalAction.FAST_WALK, destination, 160), pause(20, 20,
                    OriginalPauseReason.LISTENING, false), disappear());
            case APPROACH -> plan(move(OriginalAction.APPROACH, player.getPos(), 180), pause(40, 40,
                    OriginalPauseReason.CONFRONTING, false), disappear());
            case RETREAT -> plan(move(OriginalAction.RETREAT, exit, 180), disappear());
            case PATROL -> plan(move(OriginalAction.WALK, anchor, 160), move(OriginalAction.FAST_WALK, destination, 180),
                    move(OriginalAction.SLOW_WALK, exit, 180), disappear());
            case DOORWAY -> plan(move(OriginalAction.WALK, anchor, 160), waitUnobserved(12, 50),
                    move(OriginalAction.WALK, destination, 180), disappear());
            case BED -> yourBed(anchor, exit, player);
        };
    }

    private static Vec3d meaningfulDestination(ServerWorld world, EchoEntity echo, ServerPlayerEntity player,
                                                Vec3d anchor, List<Vec3d> knownLocations, EchoConfig config) {
        for (Vec3d location : knownLocations) {
            if (horizontalDistance(location, echo.getPos()) >= config.originalMinimumMovementDistance()
                    && horizontalDistance(location, echo.getPos()) <= config.originalMaximumMovementDistance()) {
                var routable = OriginalRoutePlanner.findRoutableStandingNear(world, echo, location, 1.0D, 2.5D,
                        config.originalMaximumWaypoints());
                if (routable.isPresent()) {
                    return routable.get();
                }
            }
        }
        Vec3d direction = player.getRotationVec(1.0F).multiply(config.originalMinimumMovementDistance() + 2.0D);
        Vec3d preferred = anchor.add(direction.x, 0.0D, direction.z);
        Vec3d resolved = OriginalRoutePlanner.findRoutableStandingNear(world, echo, preferred, 0.0D, 2.5D,
                config.originalMaximumWaypoints()).orElse(null);
        if (resolved != null && horizontalDistance(resolved, echo.getPos()) >= config.originalMinimumMovementDistance()
                && horizontalDistance(resolved, echo.getPos()) <= config.originalMaximumMovementDistance()) {
            return resolved;
        }
        return OriginalRoutePlanner.findRoutableStandingNear(world, echo, echo.getPos(),
                config.originalMinimumMovementDistance(), Math.min(8.0D, config.originalMaximumMovementDistance()),
                config.originalMaximumWaypoints())
                .orElse(anchor);
    }

    private static Vec3d exitDestination(ServerWorld world, EchoEntity echo, ServerPlayerEntity player,
                                         Vec3d anchor, EchoConfig config) {
        Vec3d away = anchor.subtract(player.getPos());
        if (away.lengthSquared() < 0.01D) {
            away = player.getRotationVec(1.0F).multiply(-1.0D);
        }
        double distance = Math.min(config.originalMaximumMovementDistance(),
                Math.max(config.originalMinimumMovementDistance(), 6.0D));
        Vec3d preferred = echo.getPos().add(away.normalize().multiply(distance));
        return OriginalRoutePlanner.findRoutableStandingNear(world, echo, preferred, 0.0D, 2.5D,
                        config.originalMaximumWaypoints())
                .or(() -> OriginalRoutePlanner.findRoutableStandingNear(world, echo, echo.getPos(),
                        config.originalMinimumMovementDistance(), distance, config.originalMaximumWaypoints()))
                .orElse(anchor);
    }

    private static OriginalMovementSegment rotate(Vec3d target) {
        return timed(OriginalAction.ROTATE, target, 4, 45, OriginalPauseReason.ORIENTING);
    }

    private static OriginalMovementSegment move(OriginalAction action, Vec3d target, int maximumTicks) {
        return OriginalMovementSegment.move(action, target, maximumTicks);
    }

    private static OriginalMovementSegment timed(OriginalAction action, Vec3d target, int minimum, int maximum,
                                                  OriginalPauseReason reason) {
        return OriginalMovementSegment.timed(action, target, minimum, maximum, reason);
    }

    private static OriginalMovementSegment pause(int minimum, int maximum, OriginalPauseReason reason,
                                                  boolean interruptWhenApproached) {
        return OriginalMovementSegment.pause(minimum, maximum, reason, interruptWhenApproached);
    }

    private static OriginalMovementSegment waitObserved(int minimum, int maximum) {
        return timed(OriginalAction.WAIT_UNTIL_OBSERVED, null, minimum, maximum,
                OriginalPauseReason.WAITING_TO_BE_NOTICED);
    }

    private static OriginalMovementSegment waitUnobserved(int minimum, int maximum) {
        return timed(OriginalAction.WAIT_UNTIL_UNOBSERVED, null, minimum, maximum,
                OriginalPauseReason.WAITING_FOR_PRIVACY);
    }

    private static OriginalMovementSegment disappear() {
        return timed(OriginalAction.DISAPPEAR, null, 12, 28, OriginalPauseReason.WAITING_FOR_PRIVACY);
    }

    private static OriginalActionPlan plan(OriginalMovementSegment... segments) {
        List<OriginalMovementSegment> bounded = new ArrayList<>(List.of(segments));
        return new OriginalActionPlan(bounded);
    }

    private static Vec3d midpoint(Vec3d left, Vec3d right) {
        return new Vec3d((left.x + right.x) * 0.5D, Math.min(left.y, right.y), (left.z + right.z) * 0.5D);
    }

    private static double horizontalDistance(Vec3d left, Vec3d right) {
        double x = left.x - right.x;
        double z = left.z - right.z;
        return Math.sqrt(x * x + z * z);
    }
}
