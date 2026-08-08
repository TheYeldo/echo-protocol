package dev.yeldos.echoprotocol.entity;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.EchoBehaviorController;
import dev.yeldos.echoprotocol.echo.EchoEventContext;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import dev.yeldos.echoprotocol.interaction.EchoWorldInteraction;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import dev.yeldos.echoprotocol.privacy.EchoPrivacy;
import dev.yeldos.echoprotocol.util.ReplayPathSafety;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EchoEntity extends MobEntity {
    private static final TrackedData<Float> OPACITY = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> SHARED = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> TARGET = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> REPLAY_SNEAKING = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_SPRINTING = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_SWIMMING = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<ItemStack> HELD_ITEM = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> ECHO_TYPE = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ECHO_STATE = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final List<RecordedFrame> replay = new ArrayList<>();
    private int sampleIntervalTicks = 2;
    private float baseOpacity = 0.45F;
    private boolean grantLookAdvancement;
    private EchoEventContext context;
    private EchoBehaviorController behavior;
    private EchoType echoType = EchoType.MEMORY;
    private EchoState echoState = EchoState.REPLAYING;
    private boolean eventFinished;
    private boolean replayMainHandSwing;
    private boolean replayOffHandSwing;
    private boolean worldInteractionsReady;
    private final EchoWorldInteraction worldInteraction = new EchoWorldInteraction();

    public EchoEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
        this.noClip = false;
        this.setNoGravity(true);
        this.setSilent(false);
        this.setCanPickUpLoot(false);
        this.experiencePoints = 0;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new Goal() {
            @Override
            public boolean canStart() {
                return false;
            }
        });
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OPACITY, 0.0F);
        builder.add(SHARED, false);
        builder.add(TARGET, "");
        builder.add(REPLAY_SNEAKING, false);
        builder.add(REPLAY_SPRINTING, false);
        builder.add(REPLAY_SWIMMING, false);
        builder.add(HELD_ITEM, ItemStack.EMPTY);
        builder.add(ECHO_TYPE, EchoType.MEMORY.ordinal());
        builder.add(ECHO_STATE, EchoState.REPLAYING.ordinal());
    }

    public void configure(UUID targetUuid, boolean shared, List<RecordedFrame> frames, int sampleIntervalTicks,
                          EchoConfig config, EchoEventContext context, EchoBehaviorController behavior) {
        this.dataTracker.set(TARGET, targetUuid.toString());
        this.dataTracker.set(SHARED, shared);
        this.context = context;
        this.behavior = behavior;
        this.echoType = behavior.type();
        this.echoState = behavior.state();
        this.dataTracker.set(ECHO_TYPE, this.echoType.ordinal());
        this.dataTracker.set(ECHO_STATE, this.echoState.ordinal());
        this.replay.clear();
        this.replay.addAll(frames);
        this.sampleIntervalTicks = Math.max(1, sampleIntervalTicks);
        this.baseOpacity = switch (behavior.type()) {
            case MEMORY, FALSE_MEMORY -> config.memoryEchoOpacity();
            case CORRUPTED -> config.corruptedEchoOpacity();
            case MIMIC -> config.mimicEchoOpacity();
            case ORIGINAL -> config.originalNearFullOpacity();
        };
        this.dataTracker.set(OPACITY, 0.0F);
        if (!frames.isEmpty()) {
            RecordedFrame first = frames.get(0);
            this.applyFrame(first);
            replayMainHandSwing = false;
            replayOffHandSwing = false;
        }
        behavior.onStarted(this);
    }

    @Override
    public void tick() {
        if (!getWorld().isClient() && echoType == EchoType.ORIGINAL) {
            setVelocity(Vec3d.ZERO);
        }
        super.tick();
        this.noClip = false;
        this.setNoGravity(true);
        if (getWorld().isClient()) {
            return;
        }
        if (behavior == null) {
            finishAndDiscard();
            return;
        }
        if (getTargetPlayer() == null) {
            behavior.onTargetUnavailable(this);
            return;
        }
        worldInteractionsReady = true;
        behavior.tick(this);
        if (!isRemoved()) {
            worldInteraction.tick(this);
        }
    }

    public boolean applyReplayFrame(int replayAge, boolean forceLookAtTarget) {
        if (replay.isEmpty()) {
            return false;
        }
        int lastTick = replayDurationTicks();
        if (replayAge > lastTick + 20) {
            return false;
        }
        float progress = Math.max(0, replayAge) / (float) sampleIntervalTicks;
        int index = MathHelper.clamp((int) progress, 0, replay.size() - 1);
        int nextIndex = MathHelper.clamp(index + 1, 0, replay.size() - 1);
        float tickDelta = MathHelper.clamp(progress - index, 0.0F, 1.0F);
        RecordedFrame frame = replay.get(index);
        RecordedFrame next = replay.get(nextIndex);

        double x = MathHelper.lerp(tickDelta, frame.x(), next.x());
        double y = MathHelper.lerp(tickDelta, frame.y(), next.y());
        double z = MathHelper.lerp(tickDelta, frame.z(), next.z());
        float yaw = MathHelper.lerpAngleDegrees(tickDelta, frame.bodyYaw(), next.bodyYaw());
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, frame.headYaw(), next.headYaw());
        float pitch = MathHelper.lerp(tickDelta, frame.pitch(), next.pitch());

        if (forceLookAtTarget && replayAge > sampleIntervalTicks * 10) {
            lookAtTarget(0.12F);
        } else {
            setHeadYaw(headYaw);
        }

        if (!moveRecordedFrameSafely(new Vec3d(x, y, z), yaw, pitch)) {
            return false;
        }
        bodyYaw = yaw;
        setHeldItemVisual(frame.heldItemVisual());
        applyRecordedPose(frame);

        int fadeTicks = 20;
        int duration = Math.max(1, (replay.size() - 1) * sampleIntervalTicks);
        float fadeIn = MathHelper.clamp(replayAge / (float) fadeTicks, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((duration + fadeTicks - replayAge) / (float) fadeTicks, 0.0F, 1.0F);
        dataTracker.set(OPACITY, baseOpacity * Math.min(fadeIn, fadeOut));
        return true;
    }

    public boolean applyFrameSequence(List<RecordedFrame> frames, int sequenceAge, boolean fadeIn) {
        if (frames.isEmpty()) {
            return false;
        }
        int duration = Math.max(1, (frames.size() - 1) * sampleIntervalTicks);
        if (sequenceAge > duration + sampleIntervalTicks) {
            return false;
        }
        float progress = Math.max(0, sequenceAge) / (float) sampleIntervalTicks;
        int index = MathHelper.clamp((int) progress, 0, frames.size() - 1);
        int nextIndex = MathHelper.clamp(index + 1, 0, frames.size() - 1);
        float delta = MathHelper.clamp(progress - index, 0.0F, 1.0F);
        RecordedFrame frame = frames.get(index);
        RecordedFrame next = frames.get(nextIndex);
        Vec3d pos = new Vec3d(
                MathHelper.lerp(delta, frame.x(), next.x()),
                MathHelper.lerp(delta, frame.y(), next.y()),
                MathHelper.lerp(delta, frame.z(), next.z()));
        float yaw = MathHelper.lerpAngleDegrees(delta, frame.bodyYaw(), next.bodyYaw());
        float headYaw = MathHelper.lerpAngleDegrees(delta, frame.headYaw(), next.headYaw());
        float pitch = MathHelper.lerp(delta, frame.pitch(), next.pitch());
        if (!moveRecordedFrameSafely(pos, yaw, pitch)) {
            return false;
        }
        bodyYaw = yaw;
        setHeadYaw(headYaw);
        setHeldItemVisual(frame.heldItemVisual());
        applyRecordedPose(frame);
        float opacity = baseOpacity;
        if (fadeIn) {
            opacity *= MathHelper.clamp(sequenceAge / 20.0F, 0.0F, 1.0F);
        }
        setReplayOpacity(opacity);
        return true;
    }

    public void applyFrame(RecordedFrame frame) {
        refreshPositionAndAngles(frame.x(), frame.y(), frame.z(), frame.bodyYaw(), frame.pitch());
        bodyYaw = frame.bodyYaw();
        setHeadYaw(frame.headYaw());
        setHeldItemVisual(frame.heldItemVisual());
        applyRecordedPose(frame);
    }

    public boolean applyLiveFrame(RecordedFrame frame) {
        if (!moveRecordedFrameSafely(frame.pos(), frame.bodyYaw(), frame.pitch())) {
            return false;
        }
        bodyYaw = frame.bodyYaw();
        setHeadYaw(frame.headYaw());
        setHeldItemVisual(frame.heldItemVisual());
        applyRecordedPose(frame);
        return true;
    }

    private boolean moveRecordedFrameSafely(Vec3d destination, float yaw, float pitch) {
        if (!(getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Vec3d movement = destination.subtract(getPos());
        if (!ReplayPathSafety.isSegmentClear(getBoundingBox(), movement,
                candidate -> world.isSpaceEmpty(this, candidate))) {
            setReplayOpacity(0.0F);
            return false;
        }
        refreshPositionAndAngles(destination.x, destination.y, destination.z, yaw, pitch);
        return true;
    }

    private void applyRecordedPose(RecordedFrame frame) {
        dataTracker.set(REPLAY_SNEAKING, frame.sneaking());
        dataTracker.set(REPLAY_SPRINTING, frame.sprinting());
        dataTracker.set(REPLAY_SWIMMING, frame.swimming() || frame.crawling());
        setSprinting(frame.sprinting());
        setPose(frame.swimming() || frame.crawling() ? EntityPose.SWIMMING
                : frame.sneaking() ? EntityPose.CROUCHING : EntityPose.STANDING);
        if (frame.mainHandSwing() && !replayMainHandSwing) {
            performEchoSwing(Hand.MAIN_HAND, null);
        }
        if (frame.offHandSwing() && !replayOffHandSwing) {
            performEchoSwing(Hand.OFF_HAND, null);
        }
        replayMainHandSwing = frame.mainHandSwing();
        replayOffHandSwing = frame.offHandSwing();
    }

    public void setHeldItemVisual(ItemStack stack) {
        ItemStack visual = stack.copyWithCount(Math.min(1, stack.getCount()));
        dataTracker.set(HELD_ITEM, visual);
        setStackInHand(Hand.MAIN_HAND, visual);
    }

    public void setReplayOpacity(float opacity) {
        dataTracker.set(OPACITY, MathHelper.clamp(opacity, 0.0F, 1.0F));
    }

    public void setFadeOpacity(int age, int duration) {
        int fadeTicks = 20;
        float fadeIn = MathHelper.clamp(age / (float) fadeTicks, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((duration + fadeTicks - age) / (float) fadeTicks, 0.0F, 1.0F);
        setReplayOpacity(baseOpacity * Math.min(fadeIn, fadeOut));
    }

    public void lookAtTarget(float lerp) {
        ServerPlayerEntity player = getTargetPlayer();
        if (player == null) {
            return;
        }
        Vec3d delta = player.getEyePos().subtract(getEyePos());
        float targetYaw = (float) (MathHelper.atan2(delta.z, delta.x) * 57.2957763671875D) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * 57.2957763671875D));
        setHeadYaw(MathHelper.lerpAngleDegrees(lerp, getHeadYaw(), targetYaw));
        setPitch(MathHelper.lerp(lerp, getPitch(), targetPitch));
        grantLookAdvancement = true;
    }

    public boolean moveToward(Vec3d target, double maxStep) {
        Vec3d delta = target.subtract(getPos());
        double distance = delta.length();
        if (distance < 0.05D) {
            return false;
        }
        Vec3d step = delta.normalize().multiply(Math.min(maxStep, distance));
        Vec3d destination = getPos().add(step);
        if (!isSafeEchoPosition(destination)) {
            return false;
        }
        float yaw = (float) (MathHelper.atan2(step.z, step.x) * 57.2957763671875D) - 90.0F;
        Vec3d before = getPos();
        move(MovementType.SELF, step);
        if (getPos().squaredDistanceTo(before) < 1.0E-8D) {
            return false;
        }
        setYaw(yaw);
        bodyYaw = yaw;
        setHeadYaw(yaw);
        return true;
    }

    public void moveOriginalStep(Vec3d step, boolean fast) {
        if (step.lengthSquared() <= 0.0000001D) {
            stopOriginalMotion();
            return;
        }
        setVelocity(step);
        move(MovementType.SELF, step);
        dataTracker.set(REPLAY_SPRINTING, fast);
        dataTracker.set(REPLAY_SNEAKING, false);
        setSprinting(fast);
        setPose(EntityPose.STANDING);
    }

    public void stopOriginalMotion() {
        setVelocity(Vec3d.ZERO);
        dataTracker.set(REPLAY_SPRINTING, false);
        setSprinting(false);
    }

    public void setOriginalCrouching(boolean crouching) {
        stopOriginalMotion();
        dataTracker.set(REPLAY_SNEAKING, crouching);
        setPose(crouching ? EntityPose.CROUCHING : EntityPose.STANDING);
    }

    public boolean performEchoSwing(Hand hand, Vec3d interactionTarget) {
        if (worldInteractionsReady && hand == Hand.MAIN_HAND
                && worldInteraction.performPlacement(this, interactionTarget)) {
            return true;
        }
        swingHand(hand, true);
        return false;
    }

    public float turnBodyToward(Vec3d direction, float maximumDegrees) {
        if (direction.x * direction.x + direction.z * direction.z < 0.000001D) {
            return 0.0F;
        }
        float desired = (float) (MathHelper.atan2(direction.z, direction.x) * 57.2957763671875D) - 90.0F;
        float difference = MathHelper.wrapDegrees(desired - bodyYaw);
        float change = MathHelper.clamp(difference, -maximumDegrees, maximumDegrees);
        float updated = bodyYaw + change;
        bodyYaw = updated;
        setYaw(updated);
        return Math.abs(difference);
    }

    public void turnHeadToward(Vec3d position, float maximumYawDegrees, float maximumPitchDegrees,
                               float maximumHeadBodyDifference) {
        Vec3d delta = position.subtract(getEyePos());
        if (delta.lengthSquared() < 0.000001D) {
            return;
        }
        float desiredYaw = (float) (MathHelper.atan2(delta.z, delta.x) * 57.2957763671875D) - 90.0F;
        float desiredPitch = (float) (-(MathHelper.atan2(delta.y,
                Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * 57.2957763671875D));
        float boundedYaw = bodyYaw + MathHelper.clamp(MathHelper.wrapDegrees(desiredYaw - bodyYaw),
                -maximumHeadBodyDifference, maximumHeadBodyDifference);
        setHeadYaw(stepAngle(getHeadYaw(), boundedYaw, maximumYawDegrees));
        setPitch(stepLinear(getPitch(), desiredPitch, maximumPitchDegrees));
        grantLookAdvancement = true;
    }

    public void resetOriginalHead(float maximumYawDegrees, float maximumPitchDegrees) {
        setHeadYaw(stepAngle(getHeadYaw(), bodyYaw, maximumYawDegrees));
        setPitch(stepLinear(getPitch(), 0.0F, maximumPitchDegrees));
    }

    private static float stepAngle(float current, float target, float maximumDegrees) {
        return current + MathHelper.clamp(MathHelper.wrapDegrees(target - current), -maximumDegrees, maximumDegrees);
    }

    private static float stepLinear(float current, float target, float maximumChange) {
        return current + MathHelper.clamp(target - current, -maximumChange, maximumChange);
    }

    public boolean isSafeEchoPosition(Vec3d pos) {
        if (!(getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Vec3d offset = pos.subtract(getPos());
        BlockPos blockPos = BlockPos.ofFloored(pos);
        net.minecraft.util.math.Box moved = getBoundingBox().offset(offset);
        return (world.isSpaceEmpty(this, moved)
                || EchoWorldInteraction.collisionContainsOnlyPassages(world, moved))
                && !world.getBlockState(blockPos).isOf(Blocks.LAVA)
                && !world.getBlockState(blockPos).isOf(Blocks.FIRE)
                && !world.getBlockState(blockPos).isOf(Blocks.SOUL_FIRE);
    }

    public void finishAndDiscard() {
        worldInteraction.clear(this);
        if (!eventFinished && context != null) {
            context.stageManager().state(context.targetUuid()).setActiveEvent(false);
            eventFinished = true;
        }
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        worldInteraction.clear(this);
        if (!eventFinished && context != null) {
            context.stageManager().state(context.targetUuid()).setActiveEvent(false);
            eventFinished = true;
        }
        super.remove(reason);
    }

    public int replayDurationTicks() {
        return Math.max(1, (replay.size() - 1) * sampleIntervalTicks);
    }

    public int sampleIntervalTicks() {
        return sampleIntervalTicks;
    }

    public List<RecordedFrame> replayFrames() {
        return List.copyOf(replay);
    }

    public RecordedFrame frameAt(int index) {
        return replay.get(MathHelper.clamp(index, 0, replay.size() - 1));
    }

    public ServerPlayerEntity getTargetPlayer() {
        if (!(getWorld() instanceof ServerWorld world)) {
            return null;
        }
        return getTargetUuid().map(uuid -> world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player ? player : null).orElse(null);
    }

    public EchoType echoType() {
        int index = MathHelper.clamp(dataTracker.get(ECHO_TYPE), 0, EchoType.values().length - 1);
        return EchoType.values()[index];
    }

    public EchoState echoState() {
        int index = MathHelper.clamp(dataTracker.get(ECHO_STATE), 0, EchoState.values().length - 1);
        return EchoState.values()[index];
    }

    public EchoBehaviorController behavior() {
        return behavior;
    }

    public void setEchoState(EchoState echoState) {
        this.echoState = echoState;
        this.dataTracker.set(ECHO_STATE, echoState.ordinal());
    }

    public boolean visibleTo(UUID viewerUuid) {
        return getTargetUuid()
                .map(target -> EchoPrivacy.mayReceiveVisual(dataTracker.get(SHARED), target, viewerUuid))
                .orElse(false);
    }

    @Override
    public boolean canBeSpectated(ServerPlayerEntity spectator) {
        return visibleTo(spectator.getUuid());
    }

    public Optional<UUID> getTargetUuid() {
        String encoded = dataTracker.get(TARGET);
        if (encoded.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(encoded));
        } catch (IllegalArgumentException invalidUuid) {
            return Optional.empty();
        }
    }

    public float getReplayOpacity() {
        return dataTracker.get(OPACITY);
    }

    public boolean replaySneaking() {
        return dataTracker.get(REPLAY_SNEAKING);
    }

    public boolean replaySprinting() {
        return dataTracker.get(REPLAY_SPRINTING);
    }

    public boolean replaySwimming() {
        return dataTracker.get(REPLAY_SWIMMING);
    }

    public ItemStack getHeldItemVisual() {
        return dataTracker.get(HELD_ITEM);
    }

    public boolean consumeLookAdvancementFlag() {
        boolean result = grantLookAdvancement;
        grantLookAdvancement = false;
        return result;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public void pushAwayFrom(net.minecraft.entity.Entity entity) {
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        super.move(MovementType.SELF, movement);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 0.08F;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
    }
}
