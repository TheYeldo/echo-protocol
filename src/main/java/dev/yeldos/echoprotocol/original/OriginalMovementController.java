package dev.yeldos.echoprotocol.original;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

public final class OriginalMovementController {
    private final EchoConfig config;
    private final OriginalStuckTracker stuckTracker = new OriginalStuckTracker();
    private Vec3d destination;
    private List<Vec3d> waypoints = List.of();
    private int waypointIndex;
    private double currentSpeed;
    private int replanCount;
    private int blockedAttempts;
    private boolean active;

    public OriginalMovementController(EchoConfig config) {
        this.config = config;
    }

    public void begin(EchoEntity echo, Vec3d target) {
        destination = target;
        waypoints = List.of();
        waypointIndex = 0;
        blockedAttempts = 0;
        active = true;
        stuckTracker.reset(echo.getPos());
    }

    public void updateDynamicDestination(EchoEntity echo, Vec3d target) {
        if (destination == null || horizontalDistance(destination, target) > 1.0D) {
            destination = target;
            waypoints = List.of();
            waypointIndex = 0;
            stuckTracker.reset(echo.getPos());
        }
    }

    public OriginalMovementResult tick(EchoEntity echo, OriginalAction action) {
        if (!(echo.getWorld() instanceof ServerWorld world) || destination == null) {
            stop(echo);
            return OriginalMovementResult.NO_ROUTE;
        }
        if (waypoints.isEmpty() || waypointIndex >= waypoints.size()) {
            Optional<List<Vec3d>> route = OriginalRoutePlanner.plan(world, echo, destination,
                    config.originalMaximumWaypoints());
            if (route.isEmpty()) {
                stop(echo);
                return OriginalMovementResult.NO_ROUTE;
            }
            waypoints = route.get();
            waypointIndex = 0;
            stuckTracker.reset(echo.getPos());
        }

        Vec3d waypoint = waypoints.get(waypointIndex);
        double remaining = echo.getPos().distanceTo(waypoint);
        double arrivalRadius = waypointIndex == waypoints.size() - 1
                ? config.originalArrivalRadius() : Math.min(0.30D, config.originalArrivalRadius());
        if (remaining <= arrivalRadius) {
            waypointIndex++;
            if (waypointIndex >= waypoints.size()) {
                stop(echo);
                return OriginalMovementResult.ARRIVED;
            }
            waypoint = waypoints.get(waypointIndex);
            remaining = echo.getPos().distanceTo(waypoint);
        }

        Vec3d delta = waypoint.subtract(echo.getPos());
        float turnDifference = echo.turnBodyToward(delta, config.originalBodyTurnSpeedDegrees());
        if (turnDifference > 52.0F && currentSpeed < config.originalSlowWalkSpeed() * 0.7D) {
            currentSpeed = Math.max(0.0D, currentSpeed - config.originalDeceleration());
            echo.stopOriginalMotion();
            return OriginalMovementResult.MOVING;
        }

        double desiredSpeed = speedFor(action);
        double brakingDistance = currentSpeed * currentSpeed / Math.max(0.001D, 2.0D * config.originalDeceleration());
        if (remaining <= brakingDistance + arrivalRadius) {
            desiredSpeed = Math.max(0.025D, desiredSpeed * MathHelper.clamp(
                    (remaining - arrivalRadius) / Math.max(0.25D, brakingDistance), 0.18D, 1.0D));
        }
        if (currentSpeed < desiredSpeed) {
            currentSpeed = Math.min(desiredSpeed, currentSpeed + config.originalAcceleration());
        } else {
            currentSpeed = Math.max(desiredSpeed, currentSpeed - config.originalDeceleration());
        }
        currentSpeed = Math.min(currentSpeed, config.originalMaximumSpeed());

        Vec3d direction = delta.normalize();
        Vec3d step = direction.multiply(Math.min(currentSpeed, Math.max(0.0D, remaining - arrivalRadius * 0.35D)));
        if (step.lengthSquared() < 0.000001D) {
            stop(echo);
            return OriginalMovementResult.ARRIVED;
        }
        Vec3d next = echo.getPos().add(step);
        if (!OriginalRoutePlanner.safeStep(world, echo, echo.getPos(), next)) {
            blockedAttempts++;
            replanCount++;
            waypoints = List.of();
            waypointIndex = 0;
            currentSpeed = Math.max(0.0D, currentSpeed - config.originalDeceleration());
            echo.stopOriginalMotion();
            return blockedAttempts >= 2 ? OriginalMovementResult.STUCK : OriginalMovementResult.BLOCKED;
        }

        boolean running = action == OriginalAction.FAST_WALK || action == OriginalAction.APPROACH
                || (action == OriginalAction.RETREAT && currentSpeed > config.originalWalkSpeed());
        echo.moveOriginalStep(step, running);
        if (stuckTracker.tick(echo.getPos(), config.originalStuckWindowTicks(),
                config.originalStuckMinimumProgress())) {
            replanCount++;
            waypoints = List.of();
            waypointIndex = 0;
            currentSpeed = 0.0D;
            echo.stopOriginalMotion();
            return OriginalMovementResult.STUCK;
        }
        return OriginalMovementResult.MOVING;
    }

    public void stop(EchoEntity echo) {
        currentSpeed = 0.0D;
        active = false;
        echo.stopOriginalMotion();
    }

    public void invalidateRoute(EchoEntity echo) {
        waypoints = List.of();
        waypointIndex = 0;
        currentSpeed = 0.0D;
        replanCount++;
        stuckTracker.reset(echo.getPos());
    }

    private double speedFor(OriginalAction action) {
        return switch (action) {
            case SLOW_WALK -> config.originalSlowWalkSpeed();
            case FAST_WALK -> config.originalFastWalkSpeed();
            case APPROACH -> Math.max(config.originalWalkSpeed(), config.originalFastWalkSpeed() * 0.92D);
            case RETREAT -> Math.max(config.originalWalkSpeed(), config.originalFastWalkSpeed() * 0.86D);
            default -> config.originalWalkSpeed();
        };
    }

    public boolean active() { return active; }
    public double currentSpeed() { return currentSpeed; }
    public Vec3d destination() { return destination; }
    public int waypointIndex() { return waypointIndex; }
    public int waypointCount() { return waypoints.size(); }
    public double distanceRemaining(EchoEntity echo) {
        return destination == null ? 0.0D : echo.getPos().distanceTo(destination);
    }
    public int replanCount() { return replanCount; }
    public int stuckCount() { return stuckTracker.stuckCount(); }

    private static double horizontalDistance(Vec3d left, Vec3d right) {
        double x = left.x - right.x;
        double z = left.z - right.z;
        return Math.sqrt(x * x + z * z);
    }
}
