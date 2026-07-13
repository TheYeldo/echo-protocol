package dev.yeldos.echoprotocol.echo;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import dev.yeldos.echoprotocol.sound.EchoSoundPlayer;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;

import java.util.concurrent.ThreadLocalRandom;

public final class OriginalEchoBehavior implements EchoBehaviorController {
    private final EchoEventContext context;
    private final OriginalEventKind eventKind;
    private final Vec3d anchor;
    private final ItemStack heldItem;
    private EchoState state = EchoState.OBSERVING;
    private int age;
    private int stateAge;
    private boolean textSent;
    private boolean confrontationSoundPlayed;
    private boolean confrontationDamageApplied;
    private Vec3d walkTarget;

    public OriginalEchoBehavior(EchoEventContext context, OriginalEventKind eventKind, Vec3d anchor, ItemStack heldItem) {
        this.context = context;
        this.eventKind = eventKind;
        this.anchor = anchor;
        this.heldItem = heldItem.copyWithCount(Math.min(1, heldItem.getCount()));
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
        echo.setEchoState(state);
        echo.setReplayOpacity(0.0F);
        echo.setHeldItemVisual(heldItem);
    }

    @Override
    public void tick(EchoEntity echo) {
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null || !(echo.getWorld() instanceof ServerWorld world)) {
            echo.finishAndDiscard();
            return;
        }
        if (!world.isChunkLoaded(echo.getBlockPos())) {
            echo.finishAndDiscard();
            return;
        }
        age++;
        stateAge++;
        echo.setHeldItemVisual(heldItem);

        switch (state) {
            case OBSERVING -> tickObserving(echo, target);
            case INHABITING -> tickInhabiting(echo, target);
            case RECOGNIZING -> tickRecognizing(echo, target);
            case REPLACING -> tickReplacing(echo, target);
            case CONFRONTING -> tickConfronting(echo, target);
            case LEAVING -> tickLeaving(echo, target);
            default -> transition(EchoState.LEAVING, echo);
        }
    }

    private void tickObserving(EchoEntity echo, ServerPlayerEntity target) {
        echo.setReplayOpacity(Math.min(context.config().originalNearFullOpacity(), stateAge / 60.0F * context.config().originalNearFullOpacity()));
        if (shouldSendText()) {
            sendText(target);
        }
        if (stateAge > 50 && target.squaredDistanceTo(echo) < 18.0D * 18.0D) {
            transition(EchoState.RECOGNIZING, echo);
        } else if (stateAge > 100) {
            transition(eventKind == OriginalEventKind.CONFRONTATION ? EchoState.CONFRONTING : EchoState.INHABITING, echo);
        }
    }

    private void tickInhabiting(EchoEntity echo, ServerPlayerEntity target) {
        echo.setReplayOpacity(context.config().originalNearFullOpacity() * 0.92F);
        if (walkTarget == null || echo.getPos().squaredDistanceTo(walkTarget) < 0.4D) {
            walkTarget = chooseNearbyPoint(echo);
        }
        if (walkTarget != null && stateAge % 2 == 0) {
            echo.moveToward(walkTarget, 0.045D);
        }
        if (stateAge % 35 == 0) {
            echo.lookAtTarget(0.08F);
        }
        if (target.squaredDistanceTo(echo) < 10.0D * 10.0D) {
            transition(EchoState.RECOGNIZING, echo);
        } else if (stateAge > 180) {
            transition(EchoState.REPLACING, echo);
        }
    }

    private void tickRecognizing(EchoEntity echo, ServerPlayerEntity target) {
        echo.setReplayOpacity(context.config().originalNearFullOpacity());
        echo.lookAtTarget(0.18F);
        if (stateAge > 70) {
            transition(eventKind == OriginalEventKind.CONFRONTATION ? EchoState.CONFRONTING : EchoState.REPLACING, echo);
        }
    }

    private void tickReplacing(EchoEntity echo, ServerPlayerEntity target) {
        echo.setReplayOpacity(context.config().originalNearFullOpacity() * 0.86F);
        if (stateAge % 3 == 0) {
            Vec3d away = echo.getPos().subtract(target.getPos()).normalize();
            if (away.lengthSquared() > 0.0D) {
                echo.moveToward(echo.getPos().add(away.multiply(2.0D)), 0.035D);
            }
        }
        if (stateAge > 90) {
            transition(EchoState.LEAVING, echo);
        }
    }

    private void tickConfronting(EchoEntity echo, ServerPlayerEntity target) {
        echo.setReplayOpacity(context.config().originalNearFullOpacity());
        echo.lookAtTarget(0.22F);
        if (!confrontationSoundPlayed) {
            EchoSoundPlayer.playOriginalConfrontation(target, context.config(), echo.getPos());
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 45, 0, false, false, true));
            confrontationSoundPlayed = true;
        }
        double distance = Math.sqrt(target.squaredDistanceTo(echo));
        if (distance > 3.25D && stateAge % 2 == 0) {
            echo.moveToward(target.getPos().subtract(target.getRotationVec(1.0F).multiply(2.5D)), 0.04D);
        }
        if (distance <= 3.5D) {
            maybeApplyConfrontationDamage(echo, target);
        }
        if (stateAge > 120) {
            context.stageManager().grant(target, "which_one_is_real");
            transition(EchoState.LEAVING, echo);
        }
    }

    private void tickLeaving(EchoEntity echo, ServerPlayerEntity target) {
        echo.setReplayOpacity(MathHelper.clamp((80 - stateAge) / 80.0F, 0.0F, 1.0F) * context.config().originalNearFullOpacity());
        if (stateAge % 3 == 0) {
            Vec3d away = echo.getPos().subtract(target.getPos());
            if (away.lengthSquared() > 0.0D) {
                echo.moveToward(echo.getPos().add(away.normalize().multiply(2.5D)), 0.05D);
            }
        }
        if (stateAge == 1) {
            EchoSoundPlayer.playDisappear(target, EchoType.ORIGINAL, context.config(), echo.getPos());
        }
        if (stateAge > 80) {
            echo.finishAndDiscard();
        }
    }

    private Vec3d chooseNearbyPoint(EchoEntity echo) {
        for (int i = 0; i < 6; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0D);
            double distance = ThreadLocalRandom.current().nextDouble(1.5D, 5.0D);
            Vec3d candidate = anchor.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            if (echo.isSafeEchoPosition(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void transition(EchoState next, EchoEntity echo) {
        state = next;
        stateAge = 0;
        walkTarget = null;
        echo.setEchoState(next);
    }

    private boolean shouldSendText() {
        return !textSent && context.config().originalTextEvents() && ThreadLocalRandom.current().nextInt(4) == 0;
    }

    private void sendText(ServerPlayerEntity target) {
        textSent = true;
        int index = ThreadLocalRandom.current().nextInt(7);
        target.sendMessage(Text.translatable("text.echoprotocol.original.message." + index), false);
        context.stageManager().grant(target, "stop_following_me");
    }

    private void maybeApplyConfrontationDamage(EchoEntity echo, ServerPlayerEntity target) {
        EchoConfig config = context.config();
        if (confrontationDamageApplied || !config.originalDamageEnabled() || target.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        float damage = Math.min(config.originalDamage(), Math.max(0.0F, target.getHealth() - 1.0F));
        if (damage > 0.0F && target.damage(echo.getDamageSources().mobAttack(echo), damage)) {
            confrontationDamageApplied = true;
        }
    }
}
