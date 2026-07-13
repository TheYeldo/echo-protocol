package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.rendering.EchoVisualEffects;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import dev.yeldos.echoprotocol.util.EchoVisibility;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MimicEchoBehavior implements EchoBehaviorController {
    private final EchoEventContext context;
    private final ArrayDeque<RecordedFrame> delayedFrames = new ArrayDeque<>();
    private final List<ItemStack> oldItems = new ArrayList<>();
    private EchoState state = EchoState.OBSERVING;
    private int age;
    private int stateAge;
    private int delayTicks;
    private int chaseTicks;
    private int chaseDurationTicks;
    private int successfulCopies;
    private int independentMoves;
    private int hits;
    private int lastHitAge = -200;
    private boolean forcedHostile;
    private RecordedFrame previousCapture;

    public MimicEchoBehavior(EchoEventContext context) {
        this.context = context;
        this.forcedHostile = context.forcedHostile();
        this.delayTicks = ThreadLocalRandom.current().nextInt(context.config().mimicMovementDelayMinTicks(), context.config().mimicMovementDelayMaxTicks() + 1);
        this.chaseDurationTicks = ThreadLocalRandom.current().nextInt(context.config().mimicChaseMinSeconds(), context.config().mimicChaseMaxSeconds() + 1) * 20;
    }

    @Override
    public EchoType type() {
        return EchoType.MIMIC;
    }

    @Override
    public EchoState state() {
        return state;
    }

    @Override
    public boolean isHostile() {
        return forcedHostile;
    }

    @Override
    public void forceHostile(boolean hostile) {
        this.forcedHostile = hostile;
        if (hostile && state.ordinal() < EchoState.THREATENING.ordinal()) {
            state = EchoState.THREATENING;
            stateAge = 0;
        } else if (!hostile && state == EchoState.THREATENING) {
            state = EchoState.DISAPPEARING;
            stateAge = 0;
        }
    }

    @Override
    public void tick(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null) {
            echo.finishAndDiscard();
            return;
        }
        capture(target);
        echo.setEchoState(state);
        switch (state) {
            case OBSERVING -> tickObserving(echo, target);
            case COPYING -> tickCopying(echo, target);
            case DESYNCHRONIZING -> tickDesynchronizing(echo, target);
            case APPROACHING -> tickApproaching(echo, target);
            case THREATENING -> tickThreatening(echo, target);
            case DISAPPEARING, FADING -> tickDisappearing(echo);
            default -> transition(EchoState.DISAPPEARING, echo);
        }
        age++;
        stateAge++;
    }

    private void capture(ServerPlayerEntity target) {
        previousCapture = RecordedFrame.capture(target, age, previousCapture);
        delayedFrames.addLast(previousCapture);
        oldItems.add(previousCapture.heldItemVisual());
        while (delayedFrames.size() > 220) {
            delayedFrames.removeFirst();
        }
        while (oldItems.size() > 24) {
            oldItems.remove(0);
        }
        if (age % 80 == 0 && delayTicks > context.config().mimicMovementDelayMinTicks()) {
            delayTicks = Math.max(context.config().mimicMovementDelayMinTicks(), delayTicks - 5);
        }
    }

    private void tickObserving(EchoEntity echo, ServerPlayerEntity target) {
        echo.lookAtTarget(0.08F);
        echo.setReplayOpacity(Math.min(0.35F, stateAge / 80.0F));
        if (delayedFrames.size() > delayTicks) {
            transition(EchoState.COPYING, echo);
        }
    }

    private void tickCopying(EchoEntity echo, ServerPlayerEntity target) {
        applyDelayedFrame(echo, false);
        successfulCopies++;
        if (successfulCopies == 80) {
            context.stageManager().grant(target, "perfect_copy");
        }
        if (stateAge > 120 || ThreadLocalRandom.current().nextInt(90) == 0) {
            transition(EchoState.DESYNCHRONIZING, echo);
        }
    }

    private void tickDesynchronizing(EchoEntity echo, ServerPlayerEntity target) {
        boolean mistake = stateAge % 35 == 0 || ThreadLocalRandom.current().nextInt(50) == 0;
        applyDelayedFrame(echo, mistake);
        if (mistake) {
            independentMoves++;
            context.stageManager().grant(target, "not_me");
            if (!oldItems.isEmpty() && ThreadLocalRandom.current().nextBoolean()) {
                echo.setHeldItemVisual(oldItems.get(ThreadLocalRandom.current().nextInt(oldItems.size())));
            }
            if (ThreadLocalRandom.current().nextBoolean()) {
                echo.lookAtTarget(0.35F);
                context.stageManager().grant(target, "it_looked_back");
            }
        }
        if (context.config().echoMovesWhenUnobserved() && !EchoVisibility.isLookingAt(target, echo, 0.68D) && stateAge % 45 == 0) {
            Vec3d closer = target.getPos().subtract(target.getRotationVec(1.0F).multiply(3.2D));
            if (echo.moveToward(closer, 0.75D)) {
                context.stageManager().grant(target, "do_not_look_away");
                context.stageManager().grant(target, "behind_you");
                EchoSoundPlayer.playUnseenMove(target, context.config(), echo.getPos());
            }
        }
        if (stateAge > 140) {
            boolean hostile = forcedHostile || (context.config().mimicChaseEnabled() && ThreadLocalRandom.current().nextInt(4) == 0);
            transition(hostile ? EchoState.THREATENING : EchoState.APPROACHING, echo);
        }
    }

    private void tickApproaching(EchoEntity echo, ServerPlayerEntity target) {
        echo.lookAtTarget(0.2F);
        if (!EchoVisibility.isLookingAt(target, echo, 0.7D)) {
            Vec3d closer = target.getPos().subtract(target.getRotationVec(1.0F).multiply(2.4D));
            echo.moveToward(closer, 0.08D);
        }
        echo.setReplayOpacity(0.4F);
        if (stateAge > 80 || echo.squaredDistanceTo(target) < 4.0D) {
            transition(EchoState.DISAPPEARING, echo);
        }
    }

    private void tickThreatening(EchoEntity echo, ServerPlayerEntity target) {
        echo.lookAtTarget(0.25F);
        echo.setReplayOpacity(context.config().mimicThreateningOpacity());
        if (stateAge == 1) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 45, 0, false, false, true));
            EchoSoundPlayer.playThreat(target, context.config(), echo.getPos());
        }
        if (context.config().mimicChaseEnabled()) {
            chaseTicks++;
            Vec3d destination = target.getPos().subtract(target.getRotationVec(1.0F).multiply(1.6D));
            echo.moveToward(destination, 0.12D);
            tryDamage(echo, target);
        }
        if (chaseTicks > chaseDurationTicks || echo.squaredDistanceTo(target) > 24.0D || hits >= context.config().mimicMaxHitsPerEvent()) {
            if (hits > 0) {
                context.stageManager().grant(target, "copy_is_wrong");
            }
            transition(EchoState.DISAPPEARING, echo);
        }
    }

    private void tickDisappearing(EchoEntity echo) {
        echo.setReplayOpacity(Math.max(0.0F, 0.4F - stateAge / 45.0F));
        if (stateAge > 45) {
            EchoSoundPlayer.playDisappear(echo.getTargetPlayer(), EchoType.MIMIC, context.config(), echo.getPos());
            EchoVisualEffects.disappear(echo.getTargetPlayer(), context.config(), echo.getPos());
            echo.finishAndDiscard();
        }
    }

    private void applyDelayedFrame(EchoEntity echo, boolean mistake) {
        RecordedFrame frame = delayedFrame();
        if (frame == null) {
            return;
        }
        echo.applyFrame(frame);
        echo.setFadeOpacity(age, 20 * 30);
        if (mistake) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                echo.setYaw(echo.getYaw() + 35.0F);
                echo.setHeadYaw(echo.getHeadYaw() + 60.0F);
            } else {
                echo.lookAtTarget(0.3F);
            }
        }
    }

    private RecordedFrame delayedFrame() {
        if (delayedFrames.size() <= delayTicks) {
            return null;
        }
        int index = Math.max(0, delayedFrames.size() - delayTicks - 1);
        int i = 0;
        for (RecordedFrame frame : delayedFrames) {
            if (i++ == index) {
                return frame;
            }
        }
        return delayedFrames.peekFirst();
    }

    private void tryDamage(EchoEntity echo, ServerPlayerEntity target) {
        if (!context.config().mimicDamageEnabled()
                || context.config().mimicDamage() <= 0.0F
                || hits >= context.config().mimicMaxHitsPerEvent()
                || age - lastHitAge < 40
                || target.getServerWorld().getDifficulty() == Difficulty.PEACEFUL
                || echo.squaredDistanceTo(target) > 3.0D) {
            return;
        }
        float damage = Math.min(context.config().mimicDamage(), Math.max(0.0F, target.getHealth() - 1.0F));
        if (damage > 0.0F && target.damage(echo.getDamageSources().mobAttack(echo), damage)) {
            hits++;
            lastHitAge = age;
        }
    }

    private void transition(EchoState next, EchoEntity echo) {
        state = next;
        stateAge = 0;
        echo.setEchoState(next);
    }
}
