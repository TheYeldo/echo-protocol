package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.original.OriginalAction;
import dev.yeldos.echoprotocol.original.OriginalActionPlan;
import dev.yeldos.echoprotocol.original.OriginalActionPlanFactory;
import dev.yeldos.echoprotocol.original.OriginalMovementController;
import dev.yeldos.echoprotocol.original.OriginalMovementMode;
import dev.yeldos.echoprotocol.original.OriginalMovementResult;
import dev.yeldos.echoprotocol.original.OriginalMovementSegment;
import dev.yeldos.echoprotocol.original.OriginalObservationState;
import dev.yeldos.echoprotocol.original.OriginalObservationTracker;
import dev.yeldos.echoprotocol.original.OriginalPauseReason;
import dev.yeldos.echoprotocol.original.OriginalRoutePlanner;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class OriginalEchoBehavior implements EchoBehaviorController {
    private static final int MAXIMUM_EVENT_AGE = 720;

    private final EchoEventContext context;
    private final OriginalEventKind eventKind;
    private final Vec3d anchor;
    private final List<Vec3d> knownLocations;
    private final ItemStack heldItem;
    private final OriginalMovementMode movementTest;
    private final boolean familiarAnchor;
    private final boolean bedAnchor;
    private final String threadMetadata;
    private final OriginalObservationTracker observation = new OriginalObservationTracker();
    private final OriginalMovementController movement;
    private EchoState state = EchoState.OBSERVING;
    private OriginalActionPlan plan = new OriginalActionPlan(List.of());
    private int age;
    private int segmentIndex;
    private int segmentAge;
    private int segmentDuration;
    private int movementFailures;
    private int fallbackTicks;
    private boolean textSent;
    private boolean confrontationSoundPlayed;
    private boolean confrontationDamageApplied;
    private boolean swingPlayed;
    private boolean visualItemEquipped = true;
    private boolean disappearSoundPlayed;
    private boolean unobservedAdvanceUsed;
    private boolean initialized;
    private boolean observedProgressGranted;
    private Vec3d unobservedAdvanceTarget;

    public OriginalEchoBehavior(EchoEventContext context, OriginalEventKind eventKind, Vec3d anchor,
                                List<Vec3d> knownLocations, ItemStack heldItem, OriginalMovementMode movementTest,
                                boolean familiarAnchor, boolean bedAnchor) {
        this(context, eventKind, anchor, knownLocations, heldItem, movementTest, familiarAnchor, bedAnchor, "none");
    }

    public OriginalEchoBehavior(EchoEventContext context, OriginalEventKind eventKind, Vec3d anchor,
                                List<Vec3d> knownLocations, ItemStack heldItem, OriginalMovementMode movementTest,
                                boolean familiarAnchor, boolean bedAnchor, String threadMetadata) {
        this.context = context;
        this.eventKind = eventKind;
        this.anchor = anchor;
        this.knownLocations = List.copyOf(knownLocations.subList(0, Math.min(5, knownLocations.size())));
        this.heldItem = heldItem.copyWithCount(Math.min(1, heldItem.getCount()));
        this.movementTest = movementTest;
        this.familiarAnchor = familiarAnchor;
        this.bedAnchor = bedAnchor;
        this.threadMetadata = threadMetadata == null || threadMetadata.isBlank() ? "none" : threadMetadata;
        this.movement = new OriginalMovementController(context.config());
    }

    @Override
    public EchoType type() {
        return EchoType.ORIGINAL;
    }

    @Override
    public EchoState state() {
        return state;
    }

    @Override
    public void onStarted(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null || !(echo.getWorld() instanceof ServerWorld world)) {
            echo.finishAndDiscard();
            return;
        }
        echo.setEchoState(state);
        echo.setReplayOpacity(context.config().originalNearFullOpacity() * 0.32F);
        visualItemEquipped = eventKind != OriginalEventKind.WRONG_OWNER;
        echo.setHeldItemVisual(visualItemEquipped ? heldItem : ItemStack.EMPTY);
        echo.setOriginalCrouching(false);
    }

    @Override
    public void tick(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null || !(echo.getWorld() instanceof ServerWorld world)
                || !world.isChunkLoaded(echo.getBlockPos()) || target.isDead() || target.getHealth() <= 0.0F) {
            echo.finishAndDiscard();
            return;
        }
        if (!initialized) {
            plan = OriginalActionPlanFactory.create(eventKind, movementTest, world, echo, target, anchor,
                    knownLocations, context.config());
            initialized = true;
            beginSegment(echo);
        }
        age++;
        segmentAge++;
        observation.tick(echo, target, context.config(), age);
        if (!observedProgressGranted && observation.directlyObservedTicks() >= context.config().originalObservationGraceTicks()) {
            observedProgressGranted = true;
            context.markObserved();
            if (context.awardsProgress()) {
                if (familiarAnchor) {
                    context.stageManager().grant(target, "my_place");
                }
                if (familiarAnchor && (bedAnchor || eventKind == OriginalEventKind.ALREADY_HOME)) {
                    context.stageManager().grant(target, "already_home");
                }
            }
        }
        float fullOpacity = context.config().originalNearFullOpacity();
        float reveal = MathHelper.clamp(age / 12.0F, 0.0F, 1.0F);
        echo.setReplayOpacity(MathHelper.lerp(reveal, fullOpacity * 0.32F, fullOpacity));
        maybeSendText(target);

        if (fallbackTicks > 0) {
            tickFallback(echo, target);
            return;
        }
        if (age >= MAXIMUM_EVENT_AGE || segmentIndex >= plan.segments().size()) {
            echo.finishAndDiscard();
            return;
        }

        OriginalMovementSegment segment = plan.segments().get(segmentIndex);
        boolean complete = tickSegment(echo, target, segment);
        if (complete) {
            finishSegment(echo, segment);
            segmentIndex++;
            beginSegment(echo);
        }
    }

    private boolean tickSegment(EchoEntity echo, ServerPlayerEntity target, OriginalMovementSegment segment) {
        OriginalAction action = segment.action();
        if (isMovement(action)) {
            return tickMovement(echo, target, segment);
        }
        if (action != OriginalAction.PAUSE || unobservedAdvanceTarget == null) {
            movement.stop(echo);
        }
        return switch (action) {
            case ROTATE -> tickRotate(echo, segment);
            case PAUSE -> tickPause(echo, target, segment);
            case LOOK_AT_PLAYER -> tickLookAtPlayer(echo, target);
            case LOOK_AT_ANCHOR -> tickLookAtAnchor(echo, segment);
            case CROUCH -> tickCrouch(echo, target);
            case SWING_HAND -> tickSwing(echo, segment);
            case PLACE_BLOCK -> tickPlaceBlock(echo, segment);
            case CHANGE_ITEM -> segmentAge >= segmentDuration;
            case WAIT_UNTIL_OBSERVED -> tickWaitObserved(echo, segment);
            case WAIT_UNTIL_UNOBSERVED -> tickWaitUnobserved(echo, segment);
            case DISAPPEAR -> tickDisappear(echo, target);
            default -> true;
        };
    }

    private boolean tickMovement(EchoEntity echo, ServerPlayerEntity target, OriginalMovementSegment segment) {
        OriginalAction action = segment.action();
        Vec3d desired = segment.target();
        if (action == OriginalAction.APPROACH) {
            desired = approachDestination(echo, target);
            if (Math.sqrt(target.squaredDistanceTo(echo)) <= 3.05D) {
                movement.stop(echo);
                maybeConfront(echo, target);
                return true;
            }
            movement.updateDynamicDestination(echo, desired);
        } else if (action == OriginalAction.RETREAT && desired == null) {
            desired = retreatDestination(echo, target, 5.0D);
            movement.updateDynamicDestination(echo, desired);
        }
        if (!movement.active()) {
            if (desired == null) {
                return true;
            }
            movement.begin(echo, desired);
        }

        OriginalMovementResult result = movement.tick(echo, action);
        if (age % 100 >= 64 && age % 100 <= 78 && target.squaredDistanceTo(echo) < 18.0D * 18.0D) {
            echo.turnHeadToward(target.getEyePos(), context.config().originalHeadTurnSpeedDegrees(), 8.0F, 68.0F);
        } else if (movement.destination() != null) {
            echo.turnHeadToward(movement.destination().add(0.0D, 1.35D, 0.0D),
                    context.config().originalHeadTurnSpeedDegrees(), 7.0F, 55.0F);
        }
        if (result == OriginalMovementResult.ARRIVED) {
            movementFailures = 0;
            return true;
        }
        if (result == OriginalMovementResult.BLOCKED || result == OriginalMovementResult.STUCK
                || result == OriginalMovementResult.NO_ROUTE) {
            recoverMovement(echo, target, segment, result);
        } else if (segmentAge >= segment.maximumTicks()) {
            recoverMovement(echo, target, segment, OriginalMovementResult.STUCK);
        }
        return false;
    }

    private boolean tickRotate(EchoEntity echo, OriginalMovementSegment segment) {
        Vec3d target = segment.target() == null ? anchor : segment.target();
        float difference = echo.turnBodyToward(target.subtract(echo.getPos()),
                context.config().originalBodyTurnSpeedDegrees());
        echo.turnHeadToward(target.add(0.0D, 1.2D, 0.0D), context.config().originalHeadTurnSpeedDegrees(),
                8.0F, 55.0F);
        return (segmentAge >= segment.minimumTicks() && difference <= 5.0F) || segmentAge >= segmentDuration;
    }

    private boolean tickPause(EchoEntity echo, ServerPlayerEntity target, OriginalMovementSegment segment) {
        if (unobservedAdvanceTarget != null) {
            echo.turnHeadToward(target.getEyePos(), context.config().originalHeadTurnSpeedDegrees(), 8.0F, 72.0F);
            OriginalMovementResult advance = movement.tick(echo, OriginalAction.SLOW_WALK);
            if (advance != OriginalMovementResult.MOVING) {
                unobservedAdvanceTarget = null;
                movement.stop(echo);
            }
            return false;
        }
        if (segment.pauseReason() == OriginalPauseReason.WATCHING
                || segment.pauseReason() == OriginalPauseReason.CONFRONTING
                || segment.pauseReason() == OriginalPauseReason.BLOCKING_ROUTE) {
            echo.turnHeadToward(target.getEyePos(), context.config().originalHeadTurnSpeedDegrees(), 8.0F, 72.0F);
        } else if (segment.pauseReason() == OriginalPauseReason.EXAMINING
                || segment.pauseReason() == OriginalPauseReason.CLAIMING_SPACE) {
            echo.turnHeadToward(anchor.add(0.0D, 0.8D, 0.0D), context.config().originalHeadTurnSpeedDegrees(),
                    7.0F, 55.0F);
        } else {
            echo.resetOriginalHead(context.config().originalHeadTurnSpeedDegrees(), 6.0F);
        }

        if (eventKind == OriginalEventKind.CONFRONTATION) {
            maybeConfront(echo, target);
            if (!unobservedAdvanceUsed && observation.unobservedTicks() >= context.config().originalUnobservedGraceTicks()
                    && Math.sqrt(target.squaredDistanceTo(echo)) > 2.0D) {
                unobservedAdvanceUsed = true;
                unobservedAdvanceTarget = approachDestination(echo, target, 1.8D);
                movement.begin(echo, unobservedAdvanceTarget);
                return false;
            }
        }
        return segmentAge >= segmentDuration
                || (segment.interruptWhenApproached() && segmentAge >= segment.minimumTicks() && observation.approaching());
    }

    private boolean tickLookAtPlayer(EchoEntity echo, ServerPlayerEntity target) {
        echo.turnBodyToward(target.getPos().subtract(echo.getPos()),
                context.config().originalBodyTurnSpeedDegrees() * 0.7F);
        echo.turnHeadToward(target.getEyePos(), context.config().originalHeadTurnSpeedDegrees(), 9.0F, 72.0F);
        return segmentAge >= segmentDuration;
    }

    private boolean tickLookAtAnchor(EchoEntity echo, OriginalMovementSegment segment) {
        Vec3d look = segment.target() == null ? anchor : segment.target();
        echo.turnBodyToward(look.subtract(echo.getPos()), context.config().originalBodyTurnSpeedDegrees() * 0.65F);
        echo.turnHeadToward(look.add(0.0D, 0.7D, 0.0D), context.config().originalHeadTurnSpeedDegrees(),
                7.0F, 55.0F);
        return segmentAge >= segmentDuration;
    }

    private boolean tickCrouch(EchoEntity echo, ServerPlayerEntity target) {
        echo.setOriginalCrouching(true);
        if (segmentAge > segmentDuration / 2) {
            echo.turnHeadToward(target.getEyePos(), context.config().originalHeadTurnSpeedDegrees(), 7.0F, 68.0F);
        } else {
            echo.turnHeadToward(anchor.add(0.0D, 0.5D, 0.0D), context.config().originalHeadTurnSpeedDegrees(),
                    6.0F, 50.0F);
        }
        return segmentAge >= segmentDuration;
    }

    private boolean tickSwing(EchoEntity echo, OriginalMovementSegment segment) {
        Vec3d target = segment.target() == null ? anchor : segment.target();
        echo.turnHeadToward(target.add(0.0D, 0.8D, 0.0D), context.config().originalHeadTurnSpeedDegrees(),
                7.0F, 55.0F);
        if (!swingPlayed) {
            echo.performEchoSwing(Hand.MAIN_HAND, target);
            swingPlayed = true;
        }
        return segmentAge >= segmentDuration;
    }

    private boolean tickPlaceBlock(EchoEntity echo, OriginalMovementSegment segment) {
        Vec3d target = segment.target() == null ? anchor : segment.target();
        echo.turnHeadToward(target.add(0.0D, 0.45D, 0.0D), context.config().originalHeadTurnSpeedDegrees(),
                8.0F, 60.0F);
        if (!swingPlayed) {
            ItemStack material = heldItem.getItem() instanceof BlockItem
                    ? heldItem : new ItemStack(Blocks.OAK_PLANKS);
            echo.setHeldItemVisual(material);
            echo.performEchoSwing(Hand.MAIN_HAND, target);
            swingPlayed = true;
        }
        return segmentAge >= segmentDuration;
    }

    private boolean tickWaitObserved(EchoEntity echo, OriginalMovementSegment segment) {
        echo.resetOriginalHead(context.config().originalHeadTurnSpeedDegrees() * 0.45F, 4.0F);
        return segmentAge >= segment.maximumTicks()
                || (segmentAge >= segment.minimumTicks()
                && observation.directlyObservedTicks() >= context.config().originalObservationGraceTicks());
    }

    private boolean tickWaitUnobserved(EchoEntity echo, OriginalMovementSegment segment) {
        echo.turnHeadToward(anchor.add(0.0D, 1.0D, 0.0D), context.config().originalHeadTurnSpeedDegrees() * 0.5F,
                5.0F, 55.0F);
        boolean hidden = observation.state() == OriginalObservationState.BLOCKED
                || observation.state() == OriginalObservationState.NOT_LOOKING;
        return segmentAge >= segment.maximumTicks()
                || (segmentAge >= segment.minimumTicks() && hidden
                && observation.unobservedTicks() >= context.config().originalUnobservedGraceTicks());
    }

    private boolean tickDisappear(EchoEntity echo, ServerPlayerEntity target) {
        if (!disappearSoundPlayed) {
            EchoSoundPlayer.playDisappear(target, EchoType.ORIGINAL, context.config(), echo.getPos());
            disappearSoundPlayed = true;
        }
        float progress = MathHelper.clamp(segmentAge / (float) Math.max(1, segmentDuration), 0.0F, 1.0F);
        echo.setReplayOpacity((1.0F - progress) * context.config().originalNearFullOpacity());
        if (segmentAge >= segmentDuration) {
            if (eventKind == OriginalEventKind.CONFRONTATION && context.awardsProgress()) {
                context.stageManager().grant(target, "which_one_is_real");
            }
            echo.finishAndDiscard();
            return false;
        }
        return false;
    }

    private void recoverMovement(EchoEntity echo, ServerPlayerEntity target, OriginalMovementSegment segment,
                                 OriginalMovementResult result) {
        movementFailures++;
        if (movementFailures == 1 && result != OriginalMovementResult.NO_ROUTE) {
            movement.invalidateRoute(echo);
            return;
        }
        if (movementFailures <= context.config().originalMaximumReplans()
                && echo.getWorld() instanceof ServerWorld world && segment.target() != null) {
            Vec3d alternate = OriginalRoutePlanner.findStandingNear(world, echo, segment.target(), 2.5D, 5.0D)
                    .orElse(null);
            if (alternate != null && horizontalDistance(alternate, echo.getPos()) >= 2.5D) {
                movement.begin(echo, alternate);
                return;
            }
        }
        movement.stop(echo);
        fallbackTicks = 38;
        state = EchoState.RECOGNIZING;
        echo.setEchoState(state);
    }

    private void tickFallback(EchoEntity echo, ServerPlayerEntity target) {
        movement.stop(echo);
        if (fallbackTicks > 18) {
            echo.turnBodyToward(target.getPos().subtract(echo.getPos()),
                    context.config().originalBodyTurnSpeedDegrees() * 0.65F);
            echo.turnHeadToward(target.getEyePos(), context.config().originalHeadTurnSpeedDegrees(), 7.0F, 70.0F);
        } else {
            echo.resetOriginalHead(context.config().originalHeadTurnSpeedDegrees() * 0.5F, 5.0F);
        }
        fallbackTicks--;
        if (fallbackTicks <= 0) {
            segmentIndex = Math.max(0, plan.segments().size() - 1);
            beginSegment(echo);
        }
    }

    private void beginSegment(EchoEntity echo) {
        movement.stop(echo);
        echo.setOriginalCrouching(false);
        segmentAge = 0;
        swingPlayed = false;
        if (segmentIndex >= plan.segments().size()) {
            return;
        }
        OriginalMovementSegment segment = plan.segments().get(segmentIndex);
        segmentDuration = boundedDuration(segment, segmentIndex);
        state = stateFor(segment.action());
        echo.setEchoState(state);
        if (isMovement(segment.action()) && segment.target() != null) {
            movement.begin(echo, segment.target());
        }
        if (segment.action() == OriginalAction.CHANGE_ITEM) {
            visualItemEquipped = !visualItemEquipped;
            echo.setHeldItemVisual(visualItemEquipped ? heldItem : ItemStack.EMPTY);
        }
    }

    private void finishSegment(EchoEntity echo, OriginalMovementSegment segment) {
        movement.stop(echo);
        if (segment.action() == OriginalAction.CROUCH) {
            echo.setOriginalCrouching(false);
        }
        if (segment.action() == OriginalAction.PLACE_BLOCK) {
            echo.setHeldItemVisual(visualItemEquipped ? heldItem : ItemStack.EMPTY);
        }
    }

    private int boundedDuration(OriginalMovementSegment segment, int index) {
        int minimum = segment.minimumTicks();
        int maximum = segment.maximumTicks();
        if (segment.action() == OriginalAction.PAUSE) {
            minimum = Math.max(minimum, context.config().originalMinimumPauseTicks());
            maximum = Math.min(maximum, context.config().originalMaximumPauseTicks());
            maximum = Math.max(minimum, maximum);
        }
        if (maximum <= minimum) {
            return minimum;
        }
        int seed = 31 * eventKind.ordinal() + 17 * index + context.targetUuid().hashCode();
        return minimum + Math.floorMod(seed, maximum - minimum + 1);
    }

    private void maybeSendText(ServerPlayerEntity target) {
        if (textSent || age != 20 || !context.config().originalTextEvents()
                || ThreadLocalRandom.current().nextInt(4) != 0) {
            return;
        }
        textSent = true;
        int index = ThreadLocalRandom.current().nextInt(7);
        target.sendMessage(Text.translatable("text.echoprotocol.original.message." + index), false);
        if (context.awardsProgress()) {
            context.stageManager().grant(target, "stop_following_me");
        }
    }

    private void maybeConfront(EchoEntity echo, ServerPlayerEntity target) {
        if (eventKind != OriginalEventKind.CONFRONTATION) {
            return;
        }
        if (!confrontationSoundPlayed) {
            EchoSoundPlayer.playOriginalConfrontation(target, context.config(), echo.getPos());
            confrontationSoundPlayed = true;
        }
        EchoConfig config = context.config();
        if (confrontationDamageApplied || !config.originalDamageEnabled()
                || target.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        float damage = Math.min(config.originalDamage(), Math.max(0.0F, target.getHealth() - 1.0F));
        if (damage > 0.0F && target.damage(target.getWorld(),
                echo.getDamageSources().mobAttack(echo), damage)) {
            confrontationDamageApplied = true;
        }
    }

    private static Vec3d approachDestination(EchoEntity echo, ServerPlayerEntity target) {
        return approachDestination(echo, target, 2.8D);
    }

    private static Vec3d approachDestination(EchoEntity echo, ServerPlayerEntity target, double standOff) {
        Vec3d away = echo.getPos().subtract(target.getPos());
        if (away.lengthSquared() < 0.01D) {
            away = target.getRotationVec(1.0F).multiply(-1.0D);
        }
        return target.getPos().add(away.normalize().multiply(standOff));
    }

    private static Vec3d retreatDestination(EchoEntity echo, ServerPlayerEntity target, double distance) {
        Vec3d away = echo.getPos().subtract(target.getPos());
        if (away.lengthSquared() < 0.01D) {
            away = target.getRotationVec(1.0F).multiply(-1.0D);
        }
        return echo.getPos().add(away.normalize().multiply(distance));
    }

    private static boolean isMovement(OriginalAction action) {
        return action == OriginalAction.WALK || action == OriginalAction.SLOW_WALK
                || action == OriginalAction.FAST_WALK || action == OriginalAction.APPROACH
                || action == OriginalAction.RETREAT;
    }

    private EchoState stateFor(OriginalAction action) {
        if (action == OriginalAction.DISAPPEAR) {
            return EchoState.LEAVING;
        }
        if (eventKind == OriginalEventKind.CONFRONTATION
                && (action == OriginalAction.APPROACH || action == OriginalAction.PAUSE)) {
            return EchoState.CONFRONTING;
        }
        if (isMovement(action)) {
            return EchoState.INHABITING;
        }
        return action == OriginalAction.LOOK_AT_PLAYER ? EchoState.RECOGNIZING : EchoState.OBSERVING;
    }

    private static double horizontalDistance(Vec3d left, Vec3d right) {
        double x = left.x - right.x;
        double z = left.z - right.z;
        return Math.sqrt(x * x + z * z);
    }

    public OriginalEventKind eventKind() { return eventKind; }

    public String status(EchoEntity echo) {
        OriginalMovementSegment segment = segmentIndex < plan.segments().size() ? plan.segments().get(segmentIndex) : null;
        String destination = movement.destination() == null ? "none" : String.format(Locale.ROOT, "%.1f,%.1f,%.1f",
                movement.destination().x, movement.destination().y, movement.destination().z);
        return "event=" + eventKind.commandName()
                + ", action=" + (segment == null ? "finished" : segment.action().name().toLowerCase(Locale.ROOT))
                + ", segment=" + (segmentIndex + 1) + "/" + plan.segments().size()
                + ", speed=" + String.format(Locale.ROOT, "%.3f", movement.currentSpeed())
                + ", destination=" + destination
                + ", waypoint=" + (movement.waypointIndex() + 1) + "/" + movement.waypointCount()
                + ", remaining=" + String.format(Locale.ROOT, "%.2f", movement.distanceRemaining(echo))
                + ", observation=" + observation.state().name().toLowerCase(Locale.ROOT)
                + ", approaching=" + observation.approaching()
                + ", retreating=" + observation.retreating()
                + ", following=" + observation.following()
                + ", thread=" + threadMetadata
                + ", stuck=" + movement.stuckCount()
                + ", replans=" + movement.replanCount()
                + ", age=" + age;
    }
}
